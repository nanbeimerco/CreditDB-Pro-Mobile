# CreditDB Pro for Android

アニメ作品・偏差値・制作陣能力評価 Web図鑑（CreditDB）のネイティブ Android アプリ版です。
Google 公式の **Material 3 (Material You)** デザインシステムに 100% 準拠し、全5,452作品・22,896名の制作陣データをオフライン完結・ミリ秒単位で高速検索・閲覧できます。

---

## 🌟 主な特徴 & 実装内容

### 1. 全機能の 100% 完全再現
- **📊 作品一覧 & 並列比較**:
  - 全5,452作品の高速無限スクロールリスト
  - タイトル（日/英）および制作陣名のリアルタイム・インクリメンタル検索（ひらがな・カタカナ・全角半角正規化対応）
  - Tier別絞り込み（All, S+, S, A+, A, B+, B, C, D）および年代別フィルタ（全年代、2020年代、2010年代、2000年代、1990年代、1980年代以前）
  - 偏差値・AniList素点・公開年・五十音による多角ソート
- **🎯 作品詳細**:
  - 偏差値・年代補正順位・上位パーセンタイル
  - AniList素点・全世界素点順位
  - 判定Tierおよび詳細解説
  - 9大部門（監督、シリーズ構成、キャラデザ、作画監督、原画、演出、音楽、美術監督、制作スタジオ）の完全クレジット
  - 各スタッフごとの `[総合実力Tier / 生涯累積Tier]` バッジ
  - 大人数スタッフ（原画など数十〜数百名）の折りたたみ・「全て引き出す」展開アニメーション
  - スタッフ名タップによる「スタッフ詳細画面」へのシームレスな画面遷移
- **🏆 制作陣 能力評価 & キャリアリーダーボード**:
  - 10部門切り替え（全役職、監督、シリーズ構成、キャラデザ、作画監督、原画、演出、音楽、美術監督、制作スタジオ）
  - スタッフ名リアルタイム検索
  - 「🎯 総合実力 S(a) 順」 vs 「🏛️ 生涯累積実績 ΣZ 順」のワンタップ切り替え
  - 順位、スタッフ名、Tier、参加作品数、S(a)、ΣZ、最高評価代表作（年 / Z値）
- **👤 スタッフ詳細**:
  - 基本情報（主役職、参加作品本数）
  - 総合実力 S(a)（スコア、実力Tier、全スタッフ順位）
  - 生涯累積実績 ΣZ（スコア、累積Tier、通算貢献順位）
  - 全役職別の実績カード（各役職ごとの参加本数、母数、実力・順位・Tier、累積・順位・Tier）
  - キャリア参加作品タイムライン年表（公開年、作品名、担当役職、年代補正Z値）
  - タイムライン作品タップによる「作品詳細画面」への双方向遷移
  - `profiles.json` 非収録スタッフへの動的フォールバック自動集計機能
- **📖 指標の見方・ガイド**:
  - 偏差値・AniList素点・Tier判定基準表（パーセンタイル・解釈）
  - 年代補正Z値（$Z_i$）の二段階数理モデル（Item-User Bias ALS分解 ＋ 局所移動窓標準化）解説
  - 制作陣能力評価指標（総合実力 $S(a)$、生涯累積実績 $\Sigma Z$、`[実力Tier / 累積Tier]` の見方）
  - 部門別平滑化パラメータ $m_{\text{role}}$（中央値）の設計表

### 2. Google 公式 Material 3 (Material You) 準拠
- **Material Theme**: `darkColorScheme` / `lightColorScheme` および Android 12+ の `Dynamic Color`（壁紙連動）対応
- **M3 コンポーネント**: `NavigationBar`, `TopAppBar`, `OutlinedTextField`, `FilterChip`, `SingleChoiceSegmentedButtonRow`, `ElevatedCard`, `OutlinedCard`
- **M3 HCT色空間に調和した Tier 専用カラーパレット**（ゴールド、エメラルド、スカイブルー、インディゴ、ティール、スレート等）
- **アニメ調ディープダークテーマ（デフォルト）** による極上の視認性とモダンな佇まい

### 3. 超高速・省メモリのプリビルド SQLite アーキテクチャ
- 40MB の元データ JSON をインデックス付き SQLite データベース（`creditdb.db`）に事前コンパイルして `assets/` に同梱。
- 初回起動待機時間は 0.05 秒。メモリ消費もわずか数 MB。5,452作品および 22,896名のスタッフに対するインクリメンタル検索・ソートが 1ms 未満でサクサク動作します。

---

## 🛠️ ビルド & 実行方法

### 動作要件
- Android 8.0 (API 26) 以上（Target: Android 15 / API 35）
- JDK 17 または JDK 21
- Android SDK (API 35, Build-Tools 35.0.0+)

### コマンドラインからのビルド
```bash
# Debug APK のビルド
./gradlew assembleDebug

# 単体テストの実行
./gradlew testDebugUnitTest
```
ビルド完了後、`app/build/outputs/apk/debug/app-debug.apk` が生成されます。

### Android Studio での実行
1. Android Studio を起動し、「Open」から本フォルダ（`CreditDB Pro for Android`）を選択します。
2. Gradle Sync が完了したら、Run ボタン（Shift + F10）を押して実機またはエミュレータで実行します。

---

## 📁 ディレクトリ構成

```
CreditDB Pro for Android/
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── assets/
│       │   │   └── creditdb.db              # インデックス付き SQLite データベース (54MB)
│       │   ├── java/com/creditdb/pro/
│       │   │   ├── MainActivity.kt          # M3 Scaffold, NavigationBar, NavHost
│       │   │   ├── data/                    # データベースヘルパー、リポジトリ、データモデル
│       │   │   ├── ui/
│       │   │   │   ├── theme/               # Material 3 テーマ、Color、Type、TierColors
│       │   │   │   ├── navigation/          # NavRoutes
│       │   │   │   ├── components/          # M3 共通コンポーネント
│       │   │   │   ├── works/               # 作品一覧 & 作品詳細画面
│       │   │   │   ├── staff/               # 制作陣ランキング & スタッフ詳細画面
│       │   │   │   └── guide/               # 指標の見方・ガイド画面
│       │   │   └── utils/                   # テキスト正規化ユーティリティ
│       │   └── res/                         # values (strings, colors, themes)
│       └── test/                            # 単体テストスイート
├── tools/
│   └── convert_json_to_sqlite.py            # データ再コンパイル用 Python スクリプト
├── gradle/
│   ├── libs.versions.toml                   # バージョンカタログ
│   └── wrapper/                             # Gradle 8.10.2 Wrapper
├── build.gradle.kts
└── settings.gradle.kts
```
