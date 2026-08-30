import Foundation
import SwiftUI
import Combine

/// アプリ全体の状態を保持するビューモデル。script.js の state / tick() / updateUI() 相当。
///
/// 「デジタルデトックスタイム」は `ScreenOffTracker`（画面ロック検出）を唯一の情報源とする。
/// つまりポイントが貯まるのは Web 版のようにこのアプリをバックグラウンドに回したときではなく、
/// 実際に **スマホの画面が消えている（ロックされている）時間** に対してのみ。
/// 「スクリーンタイム」は画面がついていたトータルの時間（このアプリを見ていた時間 +
/// 他のアプリを使っていたと推定される時間）で、デジタルデトックスタイムの対になる値。
final class AppState: ObservableObject {
    static let shared = AppState()

    @Published var username: String?
    @Published var goalSeconds: TimeInterval
    @Published var weeklyPoints: Int
    @Published var weeklyStartDate: String
    @Published var screenTimeSeconds: TimeInterval
    /// このアプリの操作時間でも画面ロック時間でもない、他のアプリを使っていたと推定される時間。
    @Published var otherAppSeconds: TimeInterval
    @Published var isForeground: Bool = true

    let tracker = ScreenOffTracker.shared

    private var lastPointsSnapshot: Int
    private var tickTimer: Timer?
    private var cancellables = Set<AnyCancellable>()

    private init() {
        let state = Persistence.loadState()
        goalSeconds = state.goalSeconds
        weeklyPoints = state.weeklyPoints
        weeklyStartDate = state.weeklyStartDate
        screenTimeSeconds = state.screenTimeSeconds
        otherAppSeconds = state.otherAppSeconds
        username = Persistence.currentUser()
        lastPointsSnapshot = Self.points(for: ScreenOffTracker.shared.combinedSeconds)

        resetWeeklyPointsIfNeeded()

        tracker.objectWillChange
            .receive(on: DispatchQueue.main)
            .sink { [weak self] _ in self?.handleDetoxChange() }
            .store(in: &cancellables)

        // 前回フォアグラウンドを離れたまま（バックグラウンドで一時停止、または
        // スワイプ等で完全終了）だった場合、起動直後にその間の未確定分を計上する。
        // scenePhase の変化通知だけに頼ると、強制終了→再起動のケースを取りこぼすため。
        reconcileBackgroundGap()

        startTicking()
    }

    // MARK: - Derived values

    var digitalDetoxSeconds: TimeInterval { tracker.combinedSeconds }
    /// 「スクリーンタイム」表示用の値。DopaDopa を見ていた時間だけでなく、
    /// 他のアプリを使っていた（＝画面はついていた）推定時間も合わせた、
    /// 画面が点灯していたトータルの時間。
    var totalScreenOnSeconds: TimeInterval { screenTimeSeconds + otherAppSeconds }
    var totalPoints: Int { Self.points(for: digitalDetoxSeconds) }
    var goalRatio: Double { min(digitalDetoxSeconds / max(goalSeconds, 1), 1) }
    var remainingSeconds: TimeInterval { max(goalSeconds - digitalDetoxSeconds, 0) }
    var isGoalAchieved: Bool { remainingSeconds <= 0 }
    var goalMinutes: Int { Int(goalSeconds / 60) }

    /// 内訳グラフ用の3分類（このアプリ／画面オフ／他のアプリ推定）。
    var usageBreakdown: [UsageSlice] {
        [
            UsageSlice(label: "DopaDopa", seconds: screenTimeSeconds, color: Theme.primary),
            UsageSlice(label: "画面オフ（デトックス）", seconds: digitalDetoxSeconds, color: Theme.accent),
            UsageSlice(label: "他のアプリ（推定）", seconds: otherAppSeconds, color: Theme.subtleInk),
        ]
    }

    static func points(for seconds: TimeInterval) -> Int {
        Int(floor(max(0, seconds) / 60))
    }

    // MARK: - Ticking (script.js の setInterval(tick, 1000) 相当)

    private func startTicking() {
        tickTimer?.invalidate()
        let timer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { [weak self] _ in
            self?.tick()
        }
        RunLoop.main.add(timer, forMode: .common)
        tickTimer = timer
    }

    private func tick() {
        if isForeground {
            screenTimeSeconds += 1
        }
        resetWeeklyPointsIfNeeded()
        persist()
    }

    /// ScreenOffTracker の累積値が変化した（＝画面ロックが解除された）タイミングで、
    /// 新たに繰り上がった分のポイントを週間ポイントへ加算する。
    private func handleDetoxChange() {
        let newPoints = Self.points(for: tracker.combinedSeconds)
        let gained = newPoints - lastPointsSnapshot
        if gained > 0 {
            resetWeeklyPointsIfNeeded()
            weeklyPoints += gained
        }
        lastPointsSnapshot = newPoints
        persist()
    }

    private func resetWeeklyPointsIfNeeded() {
        let currentWeek = PersistedState.currentWeekKey()
        if weeklyStartDate != currentWeek {
            weeklyStartDate = currentWeek
            weeklyPoints = 0
        }
    }

    // MARK: - Scene phase (script.js の visibilitychange 相当)

    func handleScenePhaseChange(_ phase: ScenePhase) {
        let wasForeground = isForeground
        isForeground = (phase == .active)

        if wasForeground && !isForeground {
            // フォアグラウンドを離れた瞬間を UserDefaults に記録しておく。
            // アプリが完全終了させられても次回起動時に読み直せるように、
            // メモリ上の変数ではなく永続化した値を使う。
            Persistence.setPendingBackgroundStart(Date())
        } else if !wasForeground && isForeground {
            reconcileBackgroundGap()
        }
    }

    /// バックグラウンドにいた間（アプリを閉じていた間）の経過時間のうち、
    /// 画面ロック（ScreenOffTracker が計測済み）と重ならない残りを
    /// 「他のアプリを使っていた時間」として概算計上する。
    /// これにより、アプリを閉じて他のアプリを使っていた時間が集計から丸ごと
    /// 抜け落ちて「スクリーンタイムが増えない」ように見える問題を解消する。
    /// UserDefaults 経由で記録しているため、バックグラウンドで一時停止していた
    /// 場合だけでなく、スワイプ等でアプリを完全終了して後から開き直した場合も
    /// 正しく遡って計上できる。
    private func reconcileBackgroundGap() {
        guard let start = Persistence.pendingBackgroundStart() else { return }
        defer { Persistence.setPendingBackgroundStart(nil) }

        let now = Date()
        let elapsed = now.timeIntervalSince(start)
        guard elapsed > 0 else { return }

        let lockedDuringGap = tracker.events
            .filter { $0.offAt >= start && $0.onAt <= now }
            .reduce(0) { $0 + $1.duration }

        let otherSeconds = max(0, elapsed - lockedDuringGap)
        if otherSeconds > 0 {
            otherAppSeconds += otherSeconds
            persist()
        }
    }

    // MARK: - Account

    func login(username raw: String) {
        let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        Persistence.ensureAccountProfile(for: trimmed)
        Persistence.setCurrentUser(trimmed)
        username = trimmed
        persist()
    }

    func logout() {
        persist()
        Persistence.setCurrentUser(nil)
        username = nil
    }

    // MARK: - Goal

    func updateGoalMinutes(_ minutes: Int) {
        guard minutes > 0 else { return }
        goalSeconds = TimeInterval(minutes * 60)
        persist()
    }

    // MARK: - Persistence

    private func persist() {
        let state = PersistedState(
            goalSeconds: goalSeconds,
            weeklyPoints: weeklyPoints,
            weeklyStartDate: weeklyStartDate,
            screenTimeSeconds: screenTimeSeconds,
            otherAppSeconds: otherAppSeconds
        )
        Persistence.saveState(state)

        if let username {
            Persistence.syncCurrentDeviceMetrics(
                username: username,
                screenTimeSeconds: screenTimeSeconds,
                digitalDetoxSeconds: digitalDetoxSeconds,
                otherAppSeconds: otherAppSeconds,
                weeklyPoints: weeklyPoints
            )
        }
    }
}

/// 内訳グラフ（Swift Charts）の1スライス分。
struct UsageSlice: Identifiable {
    let label: String
    let seconds: TimeInterval
    let color: Color

    var id: String { label }
}
