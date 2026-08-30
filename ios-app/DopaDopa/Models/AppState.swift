import Foundation
import SwiftUI
import Combine

/// アプリ全体の状態を保持するビューモデル。script.js の state / tick() / updateUI() 相当。
///
/// 「デジタルデトックスタイム」は `ScreenOffTracker`（画面ロック検出）を唯一の情報源とする。
/// つまりポイントが貯まるのは Web 版のようにこのアプリをバックグラウンドに回したときではなく、
/// 実際に **スマホの画面が消えている（ロックされている）時間** に対してのみ。
/// 「スクリーンタイム」はこのアプリを開いて見ている時間（フォアグラウンド時間）。
final class AppState: ObservableObject {
    static let shared = AppState()

    @Published var username: String?
    @Published var goalSeconds: TimeInterval
    @Published var weeklyPoints: Int
    @Published var weeklyStartDate: String
    @Published var screenTimeSeconds: TimeInterval
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
        username = Persistence.currentUser()
        lastPointsSnapshot = Self.points(for: ScreenOffTracker.shared.combinedSeconds)

        resetWeeklyPointsIfNeeded()

        tracker.objectWillChange
            .receive(on: DispatchQueue.main)
            .sink { [weak self] _ in self?.handleDetoxChange() }
            .store(in: &cancellables)

        startTicking()
    }

    // MARK: - Derived values

    var digitalDetoxSeconds: TimeInterval { tracker.combinedSeconds }
    var totalPoints: Int { Self.points(for: digitalDetoxSeconds) }
    var goalRatio: Double { min(digitalDetoxSeconds / max(goalSeconds, 1), 1) }
    var remainingSeconds: TimeInterval { max(goalSeconds - digitalDetoxSeconds, 0) }
    var isGoalAchieved: Bool { remainingSeconds <= 0 }
    var goalMinutes: Int { Int(goalSeconds / 60) }

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
        isForeground = (phase == .active)
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
            screenTimeSeconds: screenTimeSeconds
        )
        Persistence.saveState(state)

        if let username {
            Persistence.syncCurrentDeviceMetrics(
                username: username,
                screenTimeSeconds: screenTimeSeconds,
                digitalDetoxSeconds: digitalDetoxSeconds,
                weeklyPoints: weeklyPoints
            )
        }
    }
}
