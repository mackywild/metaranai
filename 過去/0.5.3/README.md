# メタらない？ v0.5.3

自分のMETAL DNAを学習し、地下まで掘って「今日の1組」を推薦するAndroidアプリ。

## V0.5.3

Genre Lensを18種類へ拡張。V0.5.2のStrict Genre Lensを維持しつつ、以下を追加・強化しました。

- Glam Metal
- Japanese Metal
- Progressive Metal（別名判定強化）
- Nu Metal

Japanese MetalはタグだけでなくJapanの国・地域情報も利用します。指定LensのLocal DB候補が不足するとLast.fmから先に補充し、そのジャンル内でPersonal METAL DNA / HIDDEN / 未知度などにより推薦します。

## Compatibility

- applicationId: `jp.metaranai.app`
- SharedPreferences: `metaranai`
- versionCode: 8
- versionName: 0.5.3
- V0.4/V0.5/V0.5.1/V0.5.2の既存データを維持
