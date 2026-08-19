# メタらない？ V0.4

「まだ知らない、でも刺さるメタル」を毎日1組発掘するAndroidアプリ。

## V0.4の主眼
V0.3のLast.fm類似アーティスト探索に、以下を追加した。

- Last.fm `artist.getInfo` から `listeners` / `playcount` / MBID を取得
- MusicBrainz Artist Searchで MBID / 国・地域 / 活動開始・終了 / 活動終了フラグを照合
- `HiddenScoreEngine` による HIDDEN SCORE 0-100
- 推薦式に HIDDEN SCORE を20%の独立軸として組み込み
- 外部Artist詳細に listeners / plays / MBID / since / area / metadata confidence を表示
- MusicBrainzの公共API保護のため、照合は1.1秒間隔・1回の発掘につき上位12候補まで

## HIDDEN SCORE
単純な「listenersが少ないほど高得点」ではない。

```
HIDDEN SCORE =
  rarity(listeners, log scale) * 58%
+ devotion(playcount / listener) * 17%
+ discovery score * 15%
+ metadata confidence * 10%
```

listeners/playcount欠損時は中立寄りの既定値を使い、データ欠損だけで高得点にならないようにしている。

## Recommendation V4

```
Affinity       50%
Hidden         20%
Novelty        15%
Exploration    10%
Discovery       5%
```

ユーザー評価は引き続きMETAL DNA学習の最強シグナル。Hidden Scoreは「知名度/発掘価値」であり、嗜好そのものには混ぜない。

## セットアップ
1. Android StudioまたはGitHub Actionsでビルド
2. Last.fm API Keyを設定画面に入力
3. 「未知のMetalを発掘」を実行
4. 必要に応じてSpotify Client IDも設定

MusicBrainz API Keyは不要。ただし意味のあるUser-Agentとレート制限順守が必要。

## APK
`.github/workflows/android.yml` で `app-debug.apk` を生成する。

Version: `0.4.0` / versionCode `4`
