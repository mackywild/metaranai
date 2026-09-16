import SwiftUI

@main
struct MetaranaiIOSApp: App {
    @StateObject private var state = MetaranaiAppState()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(state)
        }
    }
}
