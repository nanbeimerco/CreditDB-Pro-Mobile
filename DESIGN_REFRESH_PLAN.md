# CreditDB Pro for Android — デザイン刷新 作業指示書

作成日: 2026-10-24
この文書は、デザイン刷新を実装する担当（AI）向けの指示書です。ここに書かれた方針は**決定済み**です。迷ったら本書を優先し、判断できない点だけオーナーに確認してください。

---

## 0. 前提

| 項目 | 内容 |
|---|---|
| 対象 | `C:\Users\masak\Desktop\animeevaluate\CreditDB Pro for Android`（git: `nanbeimerco/CreditDB-Pro-Mobile`、ブランチ `main`） |
| 対象外 | `CreditDB Pro for Android Bangumi Edition`、`CreditDB for Web`、`CompareScreen.kt`（どこからも呼ばれていない） |
| 参考実装 | `C:\Users\masak\Desktop\animeevaluate\Tinque-main`（Android 音楽アプリ。GPL-3.0） |
| ライセンス | **CreditDB Android を GPL-3.0 にする（オーナー了承済み）**。よって Tinque のコードは直接コピーしてよい |
| 技術 | Kotlin / Jetpack Compose / Material3 `1.5.0-beta02` / Compose BOM `2026.09.00` / minSdk 26 / compileSdk 37 |
| 作業ブランチ | `main` から `feature/liquid-glass-redesign` を切って作業。コミットはフェーズごと |
| 言語 | UI 文言は既存どおり `LanguageManager.isEnglish` で日英切り替え。コメントは日本語 |

## 1. ゴール

1. **表示領域を広げる**: 上下の帯（TopAppBar、検索バー行、件数行、NavigationBar）をなくし、コンテンツを画面の端から端まで描く。操作部品はコンテンツの上に浮かせる。
2. **ベースは Material 3 Expressive のまま**: カード、リスト、ボタングループ（`ui/components/Expressive.kt`）、タイポ、形状、ローディング表示は維持。
3. **常駐UI・ポップアップ・フローティングは Liquid Glass**: Tinque と同じガラス表現（背後をぼかし・屈折・ハイライト）にする。

### 現状の固定領域（参考）
| 画面 | 固定されているもの | 合計の目安 |
|---|---|---|
| 作品 | TopAppBar 64 + 検索バー 58 + 件数/ソート行 32 + ShortNavigationBar 64 + システムバー | 約 270dp |
| 制作陣 | 上記 + 役職ボタングループ 48 | 約 320dp |
| 予測 | TopAppBar + プリセット操作行 52 + ナビバー | 約 230dp |
| Tier | TopAppBar + 見出し行 + ナビバー + フローティングツールバー | 約 260dp |
| 詳細3画面・ガイド | TopAppBar 64（中身は「作品詳細」等の固定文言） | 約 90dp |

### 完成後に常時画面に残るもの
- 上: ガラスのボタン/カプセルが浮いている1行のみ（帯なし。ボタンの隙間からコンテンツが見える）
- 下: ガラスのフローティングタブバーのみ。下スクロールで「選択中タブだけの小さなカプセル」に縮み、上スクロールで戻る（Tinque と同じ挙動）

---

## 2. 決定済みのデザイン仕様

### 2.1 役割の割り振り
| 区分 | 対象 | 表現 |
|---|---|---|
| ベース | 作品/スタッフ/Tier カード、詳細画面の中身、ボタングループ、チップ、ローディング | M3 Expressive（既存のまま） |
| 常駐UI | 上部のボタン・検索カプセル・タイトルカプセル、下部タブバー、Tier のツールバー | Liquid Glass |
| ポップアップ | ボトムシート、ダイアログ、ドロップダウンメニュー | Liquid Glass |
| フローティング | Tier の追加 FAB、Snackbar | Liquid Glass |

- ガラス上の文字・アイコン色は Tinque の `GlassPalette` / `glassContentColor()` に従う（ライトテーマは黒、ダークは白）。タブバーの選択中インジケーターだけ、テーマの `primary` を使ってよい。
- Liquid Glass は **API 31 以上で既定オン**。API 26〜30、または設定でオフのときは、Tinque の `lightweightLiquidGlass` / フォールバック（半透明の塗り＋細い縁）で同じレイアウトのまま表示する。
- アプリ名ロゴ「CreditDB PRO」は常時表示しない。⋮ メニューの先頭に見出しとして表示し、起動画面（`StartupScreen`）はそのまま残す。

### 2.2 各画面の上部（浮かせる1行）
高さは Tinque の `TopBarContentHeight`（52dp）、ボタン径 `TopBarGlassButtonSize`（42dp）、左右余白 12dp に合わせる。

| 画面 | 上部の浮かせる1行（左 → 右） |
|---|---|
| 作品 | [ガラスの検索カプセル（残り幅いっぱい）] [フィルタ（有効時バッジ）] [⋮] |
| 制作陣 | 同上（プレースホルダは既存の「スタッフ・声優・制作スタジオ名で検索...」） |
| Tier | [タイトルカプセル「Tier表」] … [⋮] |
| 予測 | [プリセット選択カプセル（タップでガラスメニュー）] [保存] [一括クリア] [⋮] |
| 作品詳細・スタッフ詳細・スタジオ詳細・ガイド | [戻る] … [タイトルカプセル: ヘッダーがスクロールで画面外に出たら表示] … |

⋮ メニュー（ガラスのドロップダウン）の中身: 見出し「CreditDB PRO」/ 言語切替（JP⇔EN）/ データベース更新 / 数理解説ガイド / テーマ・言語設定。現在の TopAppBar にある機能をすべてここへ移す。

### 2.3 スクロールで流れる要素（リストの先頭アイテムにする）
- 作品: 件数ラベル＋ソートピル（`CountLabel` / `SortLabelPill`）
- 制作陣: 役職ボタングループ（`SingleChoiceButtonGroup`）＋件数・条件ピル
- Tier: 見出し（タイトル・件数・相関ρ）
- 予測: 既存の本文そのまま

### 2.4 下部
- タブバー: Tinque の `floatingtabbar/FloatingTabBar` を使用。タブは既存 `Screen.bottomNavItems`（作品 / 制作陣 / Tier / 予測）の4つ。**standaloneTab は使わない**（全部通常タブ）。
- トップレベル以外（詳細画面・ガイド）ではタブバーをスライドアウトで隠す（現在の `AnimatedVisibility` と同じ挙動）。
- Tier のツールバー（`HorizontalFloatingToolbar` + FAB）: ガラス化してタブバーのすぐ上に置く。タブバーが縮んだときは一緒に下へ寄せる。
- Snackbar: ガラスのカプセルにして、タブバー（＋Tier ツールバー）の上に出す。
- 下端には Tinque の `BottomFadeScrim` を敷く。上端には `TopFadeBlur` を敷く（ステータスバーとの文字被り防止）。

### 2.5 タブ・セグメント
- スタジオ詳細の `PrimaryTabRow`（主要制作陣 / 制作作品一覧）は、既存の `SingleChoiceButtonGroup`（M3 Expressive の接続ボタングループ）に置き換えて帯をなくす。

---

## 3. Tinque から持ってくるもの

### 3.1 コピー先とパッケージ
- コピー先: `app/src/main/java/com/creditdb/pro/ui/glass/`（サブパッケージ構成は Tinque のまま）
- パッケージ名を `com.music.bitchord.ui.components…` → `com.creditdb.pro.ui.glass…` に置換。
- 各ファイル先頭の既存のライセンス/出典コメントは**削除しない**。そのうえで1行追加: `// Adapted from Tinque (https://github.com/nanbeimerco/Tinque), GPL-3.0`

### 3.2 ファイル一覧（Tinque 側パス: `app/src/main/java/com/music/bitchord/`）

| Tinque のファイル | 扱い |
|---|---|
| `ui/components/backdrop/**`（全 30 ファイル程度、約 3,000 行。Kyant0/backdrop の同梱版） | **そのままコピー**。外部依存なし |
| `ui/components/LiquidGlass.kt` | コピー。`AppSettings` 参照を 3.3 の `GlassSettings` に置換。`tintedGlass` / `glassArtTint` 関連は削除（CreditDB では使わない） |
| `ui/components/GlassPalette.kt` | コピー。`ThemeMode.ARTWORK` 判定は削除して常に `artworkTheme = false` |
| `ui/components/GlassFloating.kt` | コピー（`GlassModalBottomSheet` / `GlassAlertDialog` / `GlassDropdownMenu` / `GlassContentTheme` / `GlassWindowScope` / `NoDialogDim`） |
| `ui/components/GlassSheet.kt` | コピー |
| `ui/components/GlassMenu.kt` | コピー |
| `ui/haptics/Haptics.kt` | コピー（`GlassMenu` / `FloatingTabBar` が使う） |
| `ui/components/floatingtabbar/FloatingTabBar.kt` / `GlassSelectionPill.kt` | コピー。`AppSettings` 参照を `GlassSettings` に置換 |
| `ui/components/GlassNavBar.kt` | **参考にして新規作成** → `ui/glass/GlassTabBar.kt`。再生中バー（accessory、`Song`、`PlayerDock`、coil3）関連はすべて削除し、タブだけを持つ版にする |
| `ui/components/FloatingBottomBar.kt` | 定数（`GlassSpring`、`PILL_INSET`、`TAB_VERTICAL_PADDING`、`TAB_ICON_LABEL_GAP`、`floatingBarInsets`）だけを `ui/glass/GlassDefaults.kt` に移す。本体（Haze版の別バー）は不要 |
| `ui/components/Common.kt` | 定数 `PAGE_GUTTER`（10dp）、`FLOATING_BAR_MAX_WIDTH`（440dp）だけ `GlassDefaults.kt` に移す |
| `ui/components/FrostedTopBar.kt` | コピーして調整 → `ui/glass/FloatingTopBar.kt`。`TopBarAccountButton`（Account / coil3）、`TabLogoHeader`（`R.drawable.ic_logo`）、`BuildConfig` 参照は削除。`TopBarGlassButton` / `TopBarTitleCapsule` / `topBarHeight()` / `topSafeInset()` / `topBarContentPadding()` は残す |
| `ui/components/TopFadeBlur.kt` | コピー（Haze 依存） |
| `ui/components/BottomFadeScrim.kt` | コピー（`FloatingBarsTapGuard` 含む） |
| `data/settings/GlassRenderStyle.kt` | コピー → `com.creditdb.pro.ui.glass.GlassRenderStyle` |
| `MainActivity.kt` の 370〜430 行付近、2690〜2950 行付近 | **コピーせず参考に**。バックドロップの記録（`rememberLayerBackdrop` + 背景色を先に塗る `paintBackdrop`）、`CompositionLocalProvider` の構成、上部バー・下部バー・シート/メニューのホストの重ね順を真似る |

コピーしないもの: `OptimizedHaze.kt`、`FrostedGlassBackdrop.kt`、`MiniPlayer.kt`、`ui/player/**`、`ArtworkPalette.kt`、`AppSettings.kt` 本体。

### 3.3 設定クラス（新規 `ui/glass/GlassSettings.kt`）
Tinque の `AppSettings` は巨大なので持ち込まない。ガラス部品が参照する項目だけを持つ小さな object を作る。値は `MutableStateFlow`（Tinque のコードが `collectAsStateWithLifecycle()` で読んでいるため）。保存は既存 `com.creditdb.pro.data.AppSettings` と同じく SharedPreferences（キー名は `glass_*`）。

| 項目 | 既定値 | 備考 |
|---|---|---|
| `liquidGlass` | `true` | 設定画面でオン/オフ |
| `reduceDynamicBlur` | `false` | 設定画面に「透明度を下げる」として出す |
| `liquidGlassBlurDp` | `8f` | 固定（UI なし） |
| `liquidGlassOpacity` | `0.40f` | 固定 |
| `glassRenderStyle` | `GlassRenderStyle.FROSTED` | 固定 |
| `glassClarity` | `0.75f` | 固定 |
| `glassShade` | `0.5f` | 固定 |
| `pillMotionStyle` | `FLUID` | 固定（`PillMotionStyle` enum も一緒に移す） |
| `reduceAnimation` | `false` | 固定 |
| `hideNavigationBarLabels` | `false` | 固定 |
| `lockBottomBarExpanded` | `false` | 固定 |

`MainActivity.onCreate` で `GlassSettings.init(this)` を呼ぶ。

### 3.4 依存関係（`gradle/libs.versions.toml` と `app/build.gradle.kts`）
- 追加: `dev.chrisbanes.haze:haze` と `haze-materials`（`TopFadeBlur` 用）。Tinque は 1.3.1。Compose BOM 2026.09 でビルドが通る最新安定版を選ぶ。
- 追加: `androidx.lifecycle:lifecycle-runtime-compose`（既にあれば不要。`collectAsStateWithLifecycle` 用）
- `backdrop` は同梱ソースなので Maven 依存は不要。Compose のバージョン差でコンパイルエラーが出たら、そのファイルだけ最小限の修正で通す（挙動を変えない）。
- Haze がどうしても通らない場合は、`TopFadeBlur` を「背景色 → 透明」の縦グラデーションに置き換えてよい（その旨をコミットメッセージに書く）。

### 3.5 ライセンス対応
- `CreditDB Pro for Android/LICENSE` を新規作成し、`Tinque-main/LICENSE`（GPL-3.0 全文）をそのままコピー。
- `README.md` に「License: GPL-3.0」と、出典（Tinque、Kyant0/backdrop〈Apache-2.0〉、Haze〈Apache-2.0〉）の節を追加。

---

## 4. CreditDB 側の変更

### 4.1 `MainActivity.kt` の `MainApp()`
- `Scaffold` の `topBar` / `bottomBar` を削除し、次の重ね合わせにする（下から順）:
  1. ルート `Box(Modifier.fillMaxSize())`
  2. `NavHost` を包む `Box(Modifier.fillMaxSize().layerBackdrop(appBackdrop).nestedScroll(tabBarScrollConnection))` — **padding を付けない**（全画面で描く）
  3. `TopFadeBlur`
  4. `BottomFadeScrim` ＋ `FloatingBarsTapGuard`
  5. 画面ごとの上部1行（`FloatingTopBar`。中身は 4.2 の仕組みで各画面から受け取る）
  6. `GlassTabBar`（トップレベル画面のみ）
  7. `GlassSheetHost` / `GlassMenuHost`
- `CompositionLocalProvider` で `LocalLiquidGlassEnabled` / `LocalAppBackdrop` / `LocalGlassMenu` / `LocalGlassSheets` を配る（Tinque `MainActivity.kt` 421〜428 行と同じ）。
- バックドロップ記録時は必ず `MaterialTheme.colorScheme.background` を先に塗る（Tinque の `paintBackdrop`。これがないとガラス越しに文字がくっきり透ける）。
- `DatabaseUpdateDialog` / `ThemePaletteBottomSheet` の呼び出し位置は今のままでよい（中身を 4.4 でガラス化する）。

### 4.2 余白と上部1行の受け渡し（新規 `ui/glass/Chrome.kt`）
- `LocalChromePadding: PaddingValues` … 各画面のスクロールコンテンツに足す余白。
  - top = `topBarContentPadding()`（Tinque: ステータスバー + 52dp + 12dp）
  - bottom = タブバーの実測高さ（`onSizeChanged`）+ ナビゲーションバー inset + 12dp。Tier はツールバー分を加算。
- 各画面は `LazyColumn` / `verticalScroll` の `contentPadding` にこれを足す。**固定値の `Spacer(96.dp)`（`TierScreen.kt`）や `padding(bottom = 96.dp)` は削除**。
- 上部1行の中身: `TopBarState`（タイトル、戻る、左スロット、右スロット）を `MainApp` が持ち、各画面は `ProvideTopBar { … }`（`DisposableEffect` で登録・解除）で中身を差し込む。実装方式は任せるが、画面遷移時に前画面のボタンが残らないこと。
- 詳細画面のタイトルカプセル表示判定は、`LazyListState.firstVisibleItemIndex > 0` または 1 個目のアイテムのスクロール量がヘッダー高さを超えたとき。

### 4.3 画面ごとの作業
| ファイル | 作業 |
|---|---|
| `MainActivity.kt` | 4.1。TopAppBar の機能（言語切替、DB 更新、ガイド、テーマ）を ⋮ のガラスメニューへ |
| `ui/works/WorksScreen.kt` | `SmartSearchBar` と件数行を削除し、検索カプセル＋フィルタ＋⋮ を上部1行へ。件数・ソートはリスト先頭アイテムへ。`contentPadding` に `LocalChromePadding` |
| `ui/staff/StaffScreen.kt` | 同上。役職ボタングループ＋件数行をリスト先頭アイテムへ |
| `ui/components/CommonComponents.kt` | `SmartSearchBar` をガラス版に作り直す（検索カプセル = `liquidGlass(CircleShape)`、フィルタ = `TopBarGlassButton` + バッジ）。旧版は削除 |
| `ui/tier/TierScreen.kt` | 見出しをスクロール内へ。`HorizontalFloatingToolbar` をガラスのカプセル（アイコン4つ）＋ガラスの丸い追加ボタンに置き換え、タブバー上に配置。Snackbar をガラスに |
| `ui/predict/PredictScreen.kt` | プリセット操作行を上部1行へ（プリセット一覧は `GlassDropdownMenu`）。`AlertDialog` ×2 → `GlassAlertDialog`、`CandidatePickerBottomSheet` → `GlassModalBottomSheet` |
| `ui/works/WorkDetailScreen.kt` / `ui/staff/StaffDetailScreen.kt` / `ui/staff/StudioDetailScreen.kt` / `ui/guide/GuideScreen.kt` | `Scaffold` + `TopAppBar` を削除。戻るボタン（`TopBarGlassButton`）とタイトルカプセルを上部1行へ。`contentPadding` に `LocalChromePadding`。スタジオ詳細の `PrimaryTabRow` → `SingleChoiceButtonGroup` |

### 4.4 ポップアップのガラス化
呼び出しを差し替えるだけで済むよう、Tinque の同名引数の関数を使う。

| 現在 | 置き換え先 |
|---|---|
| `WorkFilterBottomSheet` / `StaffFilterBottomSheet`（`CommonComponents.kt`） | 中の `ModalBottomSheet` → `GlassModalBottomSheet` |
| `ThemePaletteBottomSheet.kt` | 同上。ここに「Liquid Glass」「透明度を下げる」のスイッチを追加（API 30 以下では Liquid Glass スイッチを無効表示＋「Android 12 以上で利用可」） |
| `TierSearchBottomSheet` / `TierAnimeActionSheet` / `TierCorrelationSheet` / `TierStaffAffinitySheet` | 同上 |
| `DatabaseUpdateDialog`（`CommonComponents.kt`）/ `TierExportDialog` / 予測画面の `AlertDialog` | `GlassAlertDialog` |
| `TierCustomizationDialog`（`Dialog` + `Surface`） | `GlassWindowScope` を使うか、`GlassSheet(kind = Dialog)` に載せ替え。中の `OutlinedTextField` が入力できることを必ず確認 |
| すべての `DropdownMenu`（MainActivity の ⋮、予測のプリセット） | `GlassDropdownMenu` |

シート内の中身は `GlassContentTheme` が自動で効くので、色の個別指定（`containerColor = surface` など）は外す。

---

## 5. フェーズと完了条件

各フェーズ末に `./gradlew :app:assembleDebug` が通ること、そしてコミットすること。

| # | 内容 | 完了条件 |
|---|---|---|
| 1 | 依存追加、`ui/glass/` へのコピーとパッケージ置換、`GlassSettings`、LICENSE | ビルドが通る（まだ画面は変わらなくてよい） |
| 2 | `MainActivity` の重ね合わせ化、`GlassTabBar`、`FloatingTopBar`＋⋮メニュー、`LocalChromePadding` | 全画面で TopAppBar と NavigationBar が消え、ガラスのタブバーが出る。下スクロールで縮み、上で戻る。リストの先頭・末尾が部品に隠れない |
| 3 | 作品・制作陣・Tier・予測の上部移設 | 2.2 / 2.3 のとおりの配置。検索・フィルタ・ソートが従来どおり動く |
| 4 | 詳細3画面・ガイド | TopAppBar なし。戻るが効く（システムの戻るジェスチャーも）。タイトルカプセルがスクロールで出る |
| 5 | ポップアップのガラス化、Snackbar | 4.4 の全項目がガラスで表示され、閉じる・入力・ボタンが動く |
| 6 | 設定スイッチ、README、最終確認 | 6 章のチェックをすべて満たす |

## 6. 最終確認チェックリスト

- [ ] API 31 以上の実機またはエミュレータで、ガラスがぼけ・屈折して見える
- [ ] API 26〜30 のエミュレータで、同じ配置のまま半透明の塗りで表示され、クラッシュしない
- [ ] Liquid Glass オフ／透明度を下げる、の両設定で表示が崩れない
- [ ] ライト・ダーク両方、カラープリセット全種で、ガラス上の文字が読める
- [ ] 英語表示、システム文字サイズ「最大」でボタンや検索欄が崩れない
- [ ] ジェスチャーナビと3ボタンナビの両方で、タブバーがシステムバーと重ならない
- [ ] 検索欄にフォーカスしてキーボードを出しても、入力欄が隠れない。シート内の入力欄も同様
- [ ] 作品一覧の無限スクロール（末尾5件で追加読み込み）が引き続き動く
- [ ] 一覧を速くスクロールしてもカクつきが目立たない（気になる場合は `GLASS_RESOLUTION_SCALE` は 0.33 のまま、ガラス面の数を減らす方向で対処）
- [ ] Tier 表の画像エクスポートにガラス部品が写り込まない
- [ ] 横向きで、タブバーが `FLOATING_BAR_MAX_WIDTH`（440dp）で中央に収まる
- [ ] `LICENSE`（GPL-3.0）と README のライセンス節がある

## 7. 注意点

- **Material の `ModalBottomSheet` / `AlertDialog` / `DropdownMenu` は別ウィンドウに描かれるため、ガラスが背後を取り込めない**。必ず Tinque の Glass 版（同一ウィンドウ内のホストに描く）を使う。
- ガラスは「動くコンテンツの上に浮いている」ときだけ意味がある。帯状の背景（全幅の塗り）を上下に新しく作らないこと。
- `liquidGlass()` に渡す shape は `CornerBasedShape`（`RoundedCornerShape` / `CircleShape`）に限る（他の shape だとレンズ効果が例外を投げる）。
- 既存の M3 Expressive 部品（`Expressive.kt` の `SingleChoiceButtonGroup`、`AppLoadingIndicator`、`*Emphasized` タイポ）はガラス化しない。
- データ層（`data/`）、ViewModel のロジック、DB 処理には触れない。
- 迷ったら Tinque の実際の見た目・数値を正とする。
