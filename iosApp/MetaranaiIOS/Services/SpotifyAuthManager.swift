import Foundation
import AuthenticationServices
import CryptoKit
import Security
import Combine
import UIKit

@MainActor
final class SpotifyAuthManager: NSObject, ObservableObject, ASWebAuthenticationPresentationContextProviding {
    static let redirectURI = "metaranai-login://spotify/callback"
    private static let accessAccount = "access_token"
    private static let refreshAccount = "refresh_token"
    private static let expiryAccount = "expiry"

    @Published private(set) var isAuthenticated = false
    @Published private(set) var status = "未接続"
    private var authSession: ASWebAuthenticationSession?

    override init() {
        super.init()
        isAuthenticated = (KeychainStore.get(account: Self.accessAccount)?.isEmpty == false) || (KeychainStore.get(account: Self.refreshAccount)?.isEmpty == false)
        status = isAuthenticated ? "接続済み" : "未接続"
    }

    func presentationAnchor(for session: ASWebAuthenticationSession) -> ASPresentationAnchor {
        let scenes = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
        return scenes.flatMap(\.windows).first(where: \.isKeyWindow) ?? ASPresentationAnchor()
    }

    func importLegacyTokens(from defaults: UserDefaults = .standard) {
        if let access = defaults.string(forKey: "spotify_access_token"), !access.isEmpty { KeychainStore.set(access, account: Self.accessAccount) }
        if let refresh = defaults.string(forKey: "spotify_refresh_token"), !refresh.isEmpty { KeychainStore.set(refresh, account: Self.refreshAccount) }
        let expiry = defaults.object(forKey: "spotify_token_expiry") as? NSNumber
        if let expiry { KeychainStore.set(String(expiry.int64Value), account: Self.expiryAccount) }
        isAuthenticated = (KeychainStore.get(account: Self.accessAccount)?.isEmpty == false) || (KeychainStore.get(account: Self.refreshAccount)?.isEmpty == false)
        status = isAuthenticated ? "接続済み（バックアップから移行）" : "未接続"
    }

    func portableTokenOverrides() -> [String: Any] {
        var out: [String: Any] = [:]
        if let value = KeychainStore.get(account: Self.accessAccount) { out["spotify_access_token"] = value }
        if let value = KeychainStore.get(account: Self.refreshAccount) { out["spotify_refresh_token"] = value }
        if let raw = KeychainStore.get(account: Self.expiryAccount), let value = Int64(raw) { out["spotify_token_expiry"] = value }
        return out
    }

    func disconnect() {
        KeychainStore.delete(account: Self.accessAccount)
        KeychainStore.delete(account: Self.refreshAccount)
        KeychainStore.delete(account: Self.expiryAccount)
        isAuthenticated = false
        status = "未接続"
    }

    func login(clientID: String) async throws {
        let id = clientID.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !id.isEmpty else { throw NSError(domain: "Metaranai", code: 1, userInfo: [NSLocalizedDescriptionKey: "Spotify Client IDを設定してください"]) }
        let verifier = Self.randomURLSafe(length: 64)
        let challenge = Self.sha256Base64URL(verifier)
        let state = Self.randomURLSafe(length: 28)
        var components = URLComponents(string: "https://accounts.spotify.com/authorize")!
        components.queryItems = [
            .init(name: "client_id", value: id), .init(name: "response_type", value: "code"),
            .init(name: "redirect_uri", value: Self.redirectURI),
            .init(name: "scope", value: "user-top-read user-read-recently-played"),
            .init(name: "code_challenge_method", value: "S256"), .init(name: "code_challenge", value: challenge),
            .init(name: "state", value: state)
        ]
        guard let authURL = components.url else { throw URLError(.badURL) }
        status = "Spotify認証中"
        let callbackURL: URL = try await withCheckedThrowingContinuation { continuation in
            let session = ASWebAuthenticationSession(url: authURL, callbackURLScheme: "metaranai-login") { url, error in
                if let error { continuation.resume(throwing: error); return }
                guard let url else { continuation.resume(throwing: URLError(.badServerResponse)); return }
                continuation.resume(returning: url)
            }
            session.presentationContextProvider = self
            session.prefersEphemeralWebBrowserSession = false
            self.authSession = session
            if !session.start() { continuation.resume(throwing: URLError(.cannotConnectToHost)) }
        }
        let items = URLComponents(url: callbackURL, resolvingAgainstBaseURL: false)?.queryItems ?? []
        let returnedState = items.first(where: { $0.name == "state" })?.value
        guard returnedState == state else { throw NSError(domain: "Metaranai", code: 2, userInfo: [NSLocalizedDescriptionKey: "Spotify state検証に失敗しました"]) }
        if let error = items.first(where: { $0.name == "error" })?.value { throw NSError(domain: "Metaranai", code: 3, userInfo: [NSLocalizedDescriptionKey: "Spotify認証: \(error)"]) }
        guard let code = items.first(where: { $0.name == "code" })?.value else { throw URLError(.badServerResponse) }
        let token = try await exchange(code: code, verifier: verifier, clientID: id)
        save(token: token)
        isAuthenticated = true
        status = "接続済み"
    }

    func accessToken(clientID: String) async throws -> String {
        if let access = KeychainStore.get(account: Self.accessAccount),
           let expiryRaw = KeychainStore.get(account: Self.expiryAccount),
           let expiry = Int64(expiryRaw), expiry > Int64(Date().timeIntervalSince1970 * 1000) + 60_000 {
            return access
        }
        guard let refresh = KeychainStore.get(account: Self.refreshAccount), !refresh.isEmpty else {
            throw NSError(domain: "Metaranai", code: 4, userInfo: [NSLocalizedDescriptionKey: "Spotifyへ接続してください"])
        }
        let token = try await refreshToken(refresh, clientID: clientID)
        save(token: token, preserveRefresh: refresh)
        return token.accessToken
    }

    private struct TokenResponse: Decodable {
        let accessToken: String
        let tokenType: String
        let expiresIn: Int
        let refreshToken: String?
        enum CodingKeys: String, CodingKey { case accessToken = "access_token", tokenType = "token_type", expiresIn = "expires_in", refreshToken = "refresh_token" }
    }

    private func exchange(code: String, verifier: String, clientID: String) async throws -> TokenResponse {
        try await tokenRequest(["grant_type": "authorization_code", "code": code, "redirect_uri": Self.redirectURI, "client_id": clientID, "code_verifier": verifier])
    }

    private func refreshToken(_ refresh: String, clientID: String) async throws -> TokenResponse {
        try await tokenRequest(["grant_type": "refresh_token", "refresh_token": refresh, "client_id": clientID])
    }

    private func tokenRequest(_ form: [String: String]) async throws -> TokenResponse {
        var request = URLRequest(url: URL(string: "https://accounts.spotify.com/api/token")!)
        request.httpMethod = "POST"
        request.setValue("application/x-www-form-urlencoded", forHTTPHeaderField: "Content-Type")
        request.httpBody = form.map { "\(Self.formEncode($0.key))=\(Self.formEncode($0.value))" }.sorted().joined(separator: "&").data(using: .utf8)
        let (data, response) = try await URLSession.shared.data(for: request)
        guard let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode) else {
            let text = String(data: data, encoding: .utf8) ?? ""
            throw NSError(domain: "Metaranai", code: 5, userInfo: [NSLocalizedDescriptionKey: "Spotify token error: \(text)"])
        }
        return try JSONDecoder().decode(TokenResponse.self, from: data)
    }

    private func save(token: TokenResponse, preserveRefresh: String? = nil) {
        KeychainStore.set(token.accessToken, account: Self.accessAccount)
        if let refresh = token.refreshToken ?? preserveRefresh { KeychainStore.set(refresh, account: Self.refreshAccount) }
        let expiry = Int64(Date().timeIntervalSince1970 * 1000) + Int64(token.expiresIn * 1000)
        KeychainStore.set(String(expiry), account: Self.expiryAccount)
    }

    private static func randomURLSafe(length: Int) -> String {
        let alphabet = Array("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-._~")
        var bytes = [UInt8](repeating: 0, count: length)
        _ = SecRandomCopyBytes(kSecRandomDefault, bytes.count, &bytes)
        return String(bytes.map { alphabet[Int($0) % alphabet.count] })
    }

    private static func sha256Base64URL(_ value: String) -> String {
        let digest = SHA256.hash(data: Data(value.utf8))
        return Data(digest).base64EncodedString().replacingOccurrences(of: "+", with: "-").replacingOccurrences(of: "/", with: "_").replacingOccurrences(of: "=", with: "")
    }

    private static func formEncode(_ value: String) -> String {
        var allowed = CharacterSet.urlQueryAllowed
        allowed.remove(charactersIn: "+&=")
        return value.addingPercentEncoding(withAllowedCharacters: allowed) ?? value
    }
}
