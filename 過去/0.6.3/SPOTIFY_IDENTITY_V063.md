# Spotify Identity Resolver v0.6.3

## Goal
Spotify上で同名の別Artistへ誤誘導せず、地下Artistでも正しいArtistページへ直接遷移できる確率を上げる。

## Resolution order
1. 高信頼MBID -> MusicBrainz Spotify relation
2. Local DB metadata -> MusicBrainz identity -> Spotify relation
3. Spotify Artist名完全一致候補を最大20件相当（10件 x 2ページ）取得
4. Last.fm Top TracksとSpotify候補の収録曲を照合
5. 2曲以上一致 -> Track Fingerprint verified
6. 1曲一致 + 独立したmetadata/genre evidence -> verified
7. 同名候補は一意なTrack Fingerprint winnerのみverified
8. 判定不能はSpotify Searchへfallback

## Safety
- Artist名の部分一致はdirect禁止。
- Spotify曲はcandidate Artist IDを含むtrackだけ採用。
- `Intro` / `Outro` 等の汎用曲名はfingerprintから除外。
- 旧cacheは自動利用しない。

## Spotify 2026 API compatibility
Spotify Searchはlimit最大10のため、artist/track searchはlimit=10 + offsetでpagingする。削除された `/artists/{id}/top-tracks` は使用しない。
