# メタらない？ v0.5.0

「自分のMetal DNAを忘れず、曜日や気分で別ジャンルの地下へ潜る」Android向け個人音楽発掘アプリ。

## V0.5の主な追加

- **Spotify Artist Direct**
  - Spotify Web API `GET /search` でArtist完全一致を照合する
  - 一致した場合 `external_urls.spotify` へ直接遷移
  - Spotifyに存在しない/未認証/照合失敗時は従来のSpotify検索へ安全にフォールバック
  - 解決済みURLはローカルキャッシュ

- **Genre Lens**
  - OFF / 手動 / 曜日モード
  - 15ジャンルから複数選択
  - 元のMETAL DNAを最重要シグナルとして維持しつつ、指定ジャンルを20%の探索レンズとして加算
  - Genre Lens有効時の外部発掘はLast.fm `tag.getTopArtists` からも候補を取得し、そのジャンル自体の地下へ潜る

- **曜日別ジャンル**
  - 月〜日の各曜日へ複数ジャンル登録
  - 例: 火曜 = Gothic Metal + Symphonic Metal / 金曜 = Metalcore + Melodic Death Metal

- **VOCAL DNA**
  - 男性Vo / 女性Vo / 混成Vo
  - Last.fm Top Tagsから判定可能な候補を学習
  - HIT/MAYBE/MISSで独立プロファイル更新
  - V0.4の8軸METAL DNAは変更しない
  - 十分なVo観測が貯まると既存のタイプ名へ「女性Vo偏愛」等を追加

- **LISTENING MAP**
  - V0.4までの既存評価履歴を再利用して、現在の上位ジャンル傾向を表示

- **Data Safety**
  - V0.4の保存先・保存キーを完全維持
  - V0.5専用データは新規キーに追加
  - JSONバックアップ/復元をアプリ内に追加
  - `android:hasFragileUserData="true"` を追加

## 更新互換性

`applicationId` はV0.4と同じ `jp.metaranai.app`、SharedPreferences名も `metaranai` のままです。

**同じ署名鍵のAPKなら、通常の上書きインストールで既存の50件以上の分析データを保持します。**

ただしGitHub Actionsのdebug APKは実行ごとにdebug署名が変わる可能性があるため、V0.4からの初回更新前は `UPDATE_FROM_V0.4.md` のADBバックアップを必ず推奨します。

## ビルド

- compileSdk 36
- targetSdk 36
- minSdk 26
- AGP 9.3.1
- Gradle 9.5.0 (GitHub Actions)
- Java 17
- Compose BOM 2026.04.01

GitHubへpushすると `.github/workflows/android.yml` で `app-debug.apk` を生成します。

## Spotify Redirect URI

```
http://127.0.0.1:8888/callback
```

Spotify Developer Dashboard側にも完全一致で登録してください。

## V0.6候補

- YouTube Data APIによる公式MV/楽曲候補
- Spotify未収録Artistの再生導線強化
- Vocal判定の追加データソース
- 動的命名パターン拡張
- Genre Lensの重み調整UI

## 署名を固定する
V0.5以降を毎回確実に上書き更新する場合は `STABLE_SIGNING.md` を参照してください。
