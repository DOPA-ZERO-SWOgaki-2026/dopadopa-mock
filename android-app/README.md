# DopaDopa Android App (Kotlin / Jetpack Compose)

このフォルダは、Web 版（`index.html` / `script.js`）・iOS 版（`ios-app/`）と同じ
「スマホを見ない時間でポイントが貯まる」体験を、ネイティブの Kotlin（Jetpack Compose）で
実装したものです。iOS 版と異なり、Android には画面が消灯したことを直接教えてくれる
公開ブロードキャスト（`Intent.ACTION_SCREEN_OFF` / `ACTION_SCREEN_ON`）があるため、
近似ではなく実際の画面ロック時間をそのまま計測できます。

## Requirements
- Android Studio Ladybug 以降
- JDK 17
- Android SDK 34（`minSdk 24` / `compileSdk 34` / `targetSdk 34`）

## 開き方
1. Android Studio で「Open」から `android-app` フォルダを選択
2. Gradle Sync が終わるのを待つ
3. 実機またはエミュレータで Run

## 画面が消えている時間の計測について

`tracker/ScreenTrackingService.kt` が常駐フォアグラウンドサービスとして
`Intent.ACTION_SCREEN_OFF` / `ACTION_SCREEN_ON` を動的に登録・監視し、
`tracker/ScreenOffTracker.kt`（アプリのライフサイクルとは独立したシングルトン）に
画面オフ⇔オンの区間を積み上げていきます。

- 画面をロックした瞬間から、ロックを解除した瞬間までの差分を「デジタルデトックスタイム」として累積
- 1 分ごとに 1 ポイント付与（Web 版の `POINTS_PER_MINUTE` と同じ仕様）
- サービスがシステムに終了させられても、次に開始したときに未確定分を合算して取りこぼしを防止
- 端末再起動後も `BootCompletedReceiver` がサービスを自動的に再開
- ロックのたびに「消えた時刻 → ついた時刻・差分・獲得ポイント」を記録し、「今日の記録」タイムラインに表示

常駐サービスには（通知チャンネルの重要度を最低にした）常時通知が必要です。これは
Android 8.0 以降でバックグラウンドサービスの実行が制限されているための仕様で、
「画面ロック計測中」という控えめな通知が表示され続けます。

## iOS 版との違い

| | iOS 版 | Android 版 |
| --- | --- | --- |
| 画面オフ検出 | `protectedDataWillBecomeUnavailable` 通知（アプリロック検出の近似） | `ACTION_SCREEN_OFF` / `ACTION_SCREEN_ON`（実際の画面点消灯） |
| バックグラウンド計測 | OS 制限が厳しく、アプリ再開時に遡って合算 | フォアグラウンドサービスで常時計測、取りこぼしがほぼ無い |
| 常駐通知 | 不要 | 必要（Android の仕様） |

## Web 版との対応関係

| Web 版 (`script.js`) | Android 版 |
| --- | --- |
| `localStorage` の `dopadopa-user` / `dopadopa-state` / `dopadopa-accounts` | `SharedPreferences`（`data/Persistence.kt`） |
| `document.visibilitychange`（タブの表示・非表示） | `ScreenOffTracker`（実際の画面ロック検出）＋ `ProcessLifecycleOwner`（アプリの前面/背面） |
| `state.digitalDetoxSeconds` | `ScreenOffTracker` の累積秒数 |
| `state.screenTimeSeconds` | アプリがフォアグラウンドにある時間 |
| 目標時間設定モーダル | `GoalSheetSheet`（ModalBottomSheet） |
| ログイン画面 | `LoginScreen` |
| メイン画面（リング・週間ポイント・特典など） | `DashboardScreen` |

## フォルダ構成

```
android-app/
  app/src/main/java/com/example/dopadopa/
    DopaDopaApplication.kt      # アプリのエントリーポイント（計測サービスを起動）
    MainActivity.kt             # ログイン画面 / メイン画面の出し分け
    data/
      Models.kt                 # 永続化用データモデル（JSON シリアライズ）
      Persistence.kt            # SharedPreferences への保存・読み込み
      Formatters.kt             # 表示用フォーマッタ
    tracker/
      ScreenOffTracker.kt       # 画面ロック時間の累積管理（シングルトン）
      ScreenBroadcastReceiver.kt
      ScreenTrackingService.kt  # 常駐フォアグラウンドサービス
      BootCompletedReceiver.kt  # 端末再起動後の自動再開
    state/
      AppState.kt                # 画面横断のビューモデル（ポイント計算・週間リセットなど）
    ui/
      theme/Theme.kt             # 配色（style.css の色を移植）
      components/ProgressRing.kt # リング型プログレス表示
      components/DonutChart.kt   # 使用時間内訳のドーナツグラフ
      screens/LoginScreen.kt
      screens/DashboardScreen.kt
      screens/GoalSheetSheet.kt
```

## Note
このプロジェクトはブラウザ版ではなく、実際にインストールして使うネイティブ Android アプリとして
Jetpack Compose で構築されています。
