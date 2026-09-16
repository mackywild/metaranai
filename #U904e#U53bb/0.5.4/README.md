# メタらない？ v0.5.4

自分のMETAL DNAを学習し、地下まで掘って「今日の1組」を推薦するAndroidアプリ。

## V0.5.4

Genre Lensの「候補を食い尽くすと評価済みArtistが再登場する」問題を修正しました。

- Genre Lensは指定ジャンルの必須条件を維持
- 補充判定は総候補数ではなく **未評価候補数**
- 未評価が各ジャンル10組未満で自動補充
- 補充は未評価20組を目標に行い、評価済みArtistは目標件数へ含めない
- 過去評価済みArtistは通常推薦から除外
- 評価ボタンは成功/当日評価済みのどちらでも画面フィードバックを返す
- Genre Lens画面に `未評価○組・総数○組` を表示
- V0.5.3のGlam / Japanese / Progressive / Nu Metalを含む18 Lensを継続

## Compatibility

- applicationId: `jp.metaranai.app`
- SharedPreferences: `metaranai`
- versionCode: 9
- versionName: 0.5.4
- V0.4/V0.5/V0.5.1/V0.5.2/V0.5.3の既存データを維持
