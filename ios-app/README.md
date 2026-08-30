# DopaDopa iOS App (Swift / SwiftUI)

このフォルダは、Web 版（`index.html` / `script.js`）と同じ「スマホを見ない時間でポイントが貯まる」体験を、
ネイティブの Swift（SwiftUI）で実装したものです。あわせて `SW_ogaki` プロジェクト（画面ロック検出の仕組み）を
そのまま `ScreenOffTracker.swift` として移植し、**スマホの画面が実際に消えている（ロックされている）時間**を
計測の中心に据えています。

## Requirements
- Xcode 16 以降
- iOS 17.0 以降（シミュレータ or 実機）

## 開き方
1. Xcode で `ios-app/DopaDopa.xcodeproj` を開く
2. 実行ターゲットを選んで Run

## 画面が消えている時間の計測について（重要）

iOS のサードパーティアプリには、Android の `ACTION_SCREEN_OFF` のような「画面が消灯した」ことを
直接教えてくれる公開 API がありません。そこで `ScreenOffTracker.swift` では、実用上もっとも近い代替として
`UIApplication.protectedDataWillBecomeUnavailableNotification` /
`protectedDataDidBecomeAvailableNotification`（＝デバイスがロックされる／ロック解除される瞬間に届く通知）を
使って自動計測しています。これは `SW_ogaki` プロジェクトで実証されていた手法をそのまま採用したものです。

- 画面をロックした瞬間から、ロックを解除した瞬間までの差分を「デジタルデトックスタイム」として累積
- 1 分ごとに 1 ポイント付与（Web 版の `POINTS_PER_MINUTE` と同じ仕様）
- ロック中にアプリがバックグラウンドで終了させられても、次に開いたときに未確定分を合算して取りこぼしを防止
- ロックのたびに「消えた時刻 → ついた時刻・差分・獲得ポイント」を記録し、「今日の記録」タイムラインに表示

なお、iOS はバックグラウンドアプリの実行を厳しく制限しているため、この方式はあくまで
サードパーティアプリとして実現できる最善の近似です。デバイス全体の画面点灯時間を完全かつ正確に
取得するには Apple の Screen Time API（`DeviceActivity` / Family Controls）などの特別な権限が必要で、
利用には Apple の審査・承認が必要です。

## Web 版との対応関係

| Web 版 (`script.js`) | iOS 版 |
| --- | --- |
| `localStorage` の `dopadopa-user` / `dopadopa-state` / `dopadopa-accounts` | `UserDefaults`（`Persistence.swift`） |
| `document.visibilitychange`（タブの表示・非表示） | `ScreenOffTracker`（実際の画面ロック検出）＋ `ScenePhase`（アプリの前面/背面） |
| `state.digitalDetoxSeconds` | `ScreenOffTracker.combinedSeconds`（画面オフ時間の累積） |
| `state.screenTimeSeconds` | アプリがフォアグラウンドにある時間 |
| 目標時間設定モーダル | `GoalSheetView`（シート表示） |
| ログイン画面 | `LoginView` |
| メイン画面（リング・週間ポイント・特典など） | `DashboardView` |

## フォルダ構成

```
ios-app/
  DopaDopa.xcodeproj/
  DopaDopa/
    DopaDopaApp.swift        # アプリのエントリーポイント
    ContentView.swift        # ログイン画面 / メイン画面の出し分け
    ScreenOffTracker.swift   # 画面ロック検出（SW_ogaki 由来）
    Persistence.swift        # UserDefaults への保存・読み込み、フォーマッタ
    Theme.swift              # 配色（style.css の色を移植）
    Models/
      AppState.swift         # 画面横断のビューモデル（ポイント計算・週間リセットなど）
    Views/
      LoginView.swift
      DashboardView.swift
      GoalSheetView.swift
```
