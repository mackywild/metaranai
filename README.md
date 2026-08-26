# メタらない？ v0.5.2

「まだ知らない、自分に刺さるMetal」を掘るAndroidアプリ。

## V0.5.2の柱

1. **Strict Genre Lens**  
   Genre Lensは加点条件ではなく必須条件。Black Metalを選んだら、Black Metal候補からのみ「今日」を選ぶ。複数ジャンル指定時はOR条件。

2. **Genre-first auto digging**  
   指定ジャンルがLocal Metal DBに各10組未満なら、Last.fm `tag.getTopArtists` から先に候補を補充してから再推薦する。探索中は別ジャンルのArtistを表示しない。

3. **Personal ranking inside the genre**  
   ジャンルで候補母集団を固定した後、METAL DNA / HIDDEN / novelty / exploration / discovery /検索興味で順位付けする。

4. **Unlimited Local Metal DB + Global Search + 5段階評価**  
   V0.5.1の無制限キャッシュ、Last.fm外部検索、Spotify Artist直行、5段階評価を継続。

5. **V0.4/V0.5/V0.5.1データ互換**  
   `applicationId = jp.metaranai.app`、SharedPreferences `metaranai`、既存キーを維持。

## Build

- Android API 36
- Java 17
- Gradle 9.5.0
- versionCode 7
- versionName 0.5.2

GitHub Actions: `.github/workflows/android.yml`
