import Foundation
import UIKit
import Combine

/// 1 回分の「画面オフ→オン」の記録（消えた時刻・ついた時刻・その差分）
struct ScreenOffEvent: Codable, Identifiable {
    let id: UUID
    /// 画面が消えた（ロックされた）時刻
    let offAt: Date
    /// 画面がついた（ロック解除された）時刻
    let onAt: Date
    /// offAt から onAt までの差分（秒）
    var duration: TimeInterval { onAt.timeIntervalSince(offAt) }

    init(offAt: Date, onAt: Date) {
        self.id = UUID()
        self.offAt = offAt
        self.onAt = onAt
    }
}

/// 画面オフ（デバイスのロック）時間を自動で計測し、累積時間を UserDefaults に保存するクラス。
///
/// iOS のサードパーティアプリには「画面が消灯した」ことを直接知らせる公開 API は無いため、
/// 実用上もっとも近い代替として `UIApplication` の
/// `protectedDataWillBecomeUnavailable` / `protectedDataDidBecomeAvailable`
/// 通知（＝デバイスがロックされる／ロック解除される瞬間）を利用して自動計測する。
///
/// 注意: iOS はバックグラウンドで動くアプリの実行時間を厳しく制限しているため、
/// 画面オフが長時間続くとアプリ自体が一時停止・終了させられることがある。
/// そのため「ロック開始時刻」を UserDefaults に保存しておき、次にアプリが
/// 起動・復帰した際に未確定分を合算することで、計測の抜け漏れをできるだけ防いでいる。
final class ScreenOffTracker: ObservableObject {

    static let shared = ScreenOffTracker()

    /// これまでに累積した画面オフ時間（秒）
    @Published private(set) var totalScreenOffSeconds: TimeInterval

    /// 現在画面オフ中かどうか
    @Published private(set) var isScreenOff: Bool = false

    /// 現在進行中の画面オフの継続時間（秒）。画面オフ中のみ 1 秒ごとに更新される。
    @Published private(set) var currentOffSeconds: TimeInterval = 0

    /// 「消えた時刻／ついた時刻／差分」の履歴。新しいものが先頭。
    @Published private(set) var events: [ScreenOffEvent] = []

    /// 保存しておく履歴の最大件数
    private let maxStoredEvents = 200

    private var offStartDate: Date? {
        didSet {
            if let offStartDate {
                UserDefaults.standard.set(offStartDate.timeIntervalSince1970, forKey: Keys.offStartDate)
            } else {
                UserDefaults.standard.removeObject(forKey: Keys.offStartDate)
            }
        }
    }

    private var displayTimer: Timer?
    private var cancellables = Set<AnyCancellable>()

    private enum Keys {
        static let totalSeconds = "screenOffTracker.totalSeconds"
        static let offStartDate = "screenOffTracker.offStartDate"
        static let events = "screenOffTracker.events"
    }

    private init() {
        let defaults = UserDefaults.standard
        totalScreenOffSeconds = defaults.double(forKey: Keys.totalSeconds)

        if let data = defaults.data(forKey: Keys.events),
           let decoded = try? JSONDecoder().decode([ScreenOffEvent].self, from: data) {
            events = decoded
        }

        // 前回、画面オフの記録中にアプリが終了させられていた場合は、
        // ここでその分を累積時間に合算してから改めて計測を再開する。
        if defaults.object(forKey: Keys.offStartDate) != nil {
            let savedStart = Date(timeIntervalSince1970: defaults.double(forKey: Keys.offStartDate))
            totalScreenOffSeconds += Date().timeIntervalSince(savedStart)
            persistTotal()
            UserDefaults.standard.removeObject(forKey: Keys.offStartDate)
        }

        observeNotifications()
    }

    private func observeNotifications() {
        let center = NotificationCenter.default

        // 画面がロックされる（＝画面オフになる）瞬間
        center.publisher(for: UIApplication.protectedDataWillBecomeUnavailableNotification)
            .sink { [weak self] _ in self?.handleScreenOff() }
            .store(in: &cancellables)

        // 画面のロックが解除される（＝画面オンに戻る）瞬間
        center.publisher(for: UIApplication.protectedDataDidBecomeAvailableNotification)
            .sink { [weak self] _ in self?.handleScreenOn() }
            .store(in: &cancellables)
    }

    private func handleScreenOff() {
        guard offStartDate == nil else { return }
        offStartDate = Date()
        isScreenOff = true
        currentOffSeconds = 0
        startDisplayTimer()
    }

    private func handleScreenOn() {
        guard let start = offStartDate else { return }
        let now = Date()
        let elapsed = now.timeIntervalSince(start)
        totalScreenOffSeconds += elapsed
        persistTotal()

        events.insert(ScreenOffEvent(offAt: start, onAt: now), at: 0)
        if events.count > maxStoredEvents {
            events.removeLast(events.count - maxStoredEvents)
        }
        persistEvents()

        offStartDate = nil
        isScreenOff = false
        currentOffSeconds = 0
        stopDisplayTimer()
    }

    private func startDisplayTimer() {
        stopDisplayTimer()
        let timer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { [weak self] _ in
            guard let self, let start = self.offStartDate else { return }
            self.currentOffSeconds = Date().timeIntervalSince(start)
        }
        RunLoop.main.add(timer, forMode: .common)
        displayTimer = timer
    }

    private func stopDisplayTimer() {
        displayTimer?.invalidate()
        displayTimer = nil
    }

    private func persistTotal() {
        UserDefaults.standard.set(totalScreenOffSeconds, forKey: Keys.totalSeconds)
    }

    private func persistEvents() {
        guard let data = try? JSONEncoder().encode(events) else { return }
        UserDefaults.standard.set(data, forKey: Keys.events)
    }

    /// 累積計測時間・履歴をリセットする
    func reset() {
        totalScreenOffSeconds = 0
        events = []
        persistTotal()
        persistEvents()
    }
}
