# メタらない？ v0.5.1

「まだ知らない、自分に刺さるMetal」を掘るAndroidアプリ。

## V0.5.1の柱

1. **Unlimited Local Metal DB**  
   Last.fm / MusicBrainz / Spotify照合で得たArtistを端末へ蓄積。500組上限を撤廃。

2. **5段階評価**  
   `全部好き / 普通に刺さる / 何曲か刺さる / イマイチ / 興味なし` をMETAL DNA・VOCAL DNA・Genre分析へ反映。

3. **Global Artist Search**  
   端末DBにいないArtistはLast.fm `artist.search`で外部探索。Metal判定後にLocal DBへ保存。

4. **Genre Lens即時更新**  
   Lens切替時に「今日のおすすメタル」を即時再計算し、裏で対象ジャンルの外部候補も補充。

5. **V0.4/V0.5データ互換**  
   `applicationId = jp.metaranai.app`、SharedPreferences `metaranai`、既存キーを維持。

## 旧3段階評価の移行

- `HIT` → `🔥 普通に刺さる`
- `MAYBE` → `🎵 何曲か刺さる`
- `MISS` → `😐 イマイチ`

旧データから`全部好き`や`興味なし`を勝手に生成しない。

## Build

GitHub Actions: `.github/workflows/android.yml`

- Android API 36
- Java 17
- Gradle 9.5.0
- versionCode 6
- versionName 0.5.1

固定署名Secretsがあればrelease APK、無ければdebug APKを生成する。

## Update safety

既存APKと同じ署名なら通常の上書き更新でデータを保持できる。署名が異なる場合は、旧データをバックアップしてから移行すること。詳細は `DATA_COMPATIBILITY.md` / `STABLE_SIGNING.md` を参照。
