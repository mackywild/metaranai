# V0.9.1 → V0.9.1.1

V0.9.1.1はDNA操作仕様の修正版です。既存データはそのまま引き継げます。

## 変更点
- DNAの手動編集・手動生成・ランダム生成を廃止
- 8軸DNAは評価・探索・Spotify解析で自動学習
- DNA名はDNA数値が5回変動するごとに自動再生成
- 生成済みDNA名と再生成カウンタを端末へ保存
- Androidは versionCode 19 / versionName 0.9.1.1
- iOSはMarketing Version 0.9.1 / Build 19（App Storeのバージョン形式に合わせる）

## データ互換性
SharedPreferences / UserDefaults / portable JSON の既存キーは維持しています。新しいDNA生成メタデータが無い旧バックアップを復元した場合は、現在のDNAから名称を自動生成して継続します。
