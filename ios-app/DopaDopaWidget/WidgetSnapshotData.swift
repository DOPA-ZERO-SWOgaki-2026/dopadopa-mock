import Foundation

/// ウィジェット（別プロセス）から見た、その瞬間の状態のスナップショット。
/// メインアプリの `AppState` のように常時動いているわけではなく、
/// タイムライン更新のたびに App Group 経由で一度だけ読み直す。
struct WidgetSnapshotData {
    let totalPoints: Int
    let digitalDetoxSeconds: TimeInterval
    let goalSeconds: TimeInterval
    let goalRatio: Double

    /// メインアプリと同じ App Group 上の UserDefaults から、現時点の値を読み取る。
    static func load() -> WidgetSnapshotData {
        let defaults = AppGroup.defaults

        // ScreenOffTracker が保存している累積オフ時間に、進行中のロックがあれば
        // その分の経過時間も足し合わせる（ScreenOffTracker.combinedSeconds と同じ考え方）。
        var seconds = defaults.double(forKey: ScreenOffTracker.Keys.totalSeconds)
        if defaults.object(forKey: ScreenOffTracker.Keys.offStartDate) != nil {
            let start = Date(timeIntervalSince1970: defaults.double(forKey: ScreenOffTracker.Keys.offStartDate))
            seconds += Date().timeIntervalSince(start)
        }
        seconds = max(0, seconds)

        let goalSeconds = Persistence.loadState().goalSeconds
        let points = Int(floor(seconds / 60))
        let ratio = goalSeconds > 0 ? min(seconds / goalSeconds, 1) : 0

        return WidgetSnapshotData(
            totalPoints: points,
            digitalDetoxSeconds: seconds,
            goalSeconds: goalSeconds,
            goalRatio: ratio
        )
    }

    /// ウィジェットギャラリーのプレビューや、初回読み込み前に使うダミーデータ。
    static let placeholder = WidgetSnapshotData(
        totalPoints: 128,
        digitalDetoxSeconds: 95 * 60,
        goalSeconds: 180 * 60,
        goalRatio: 95.0 / 180.0
    )
}
