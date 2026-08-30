import Foundation

/// Web 版 (`script.js`) の `localStorage` 相当のキー構成を UserDefaults に移植したもの。
enum StorageKey {
    static let user = "dopadopa-user"
    static let state = "dopadopa-state"
    static let accounts = "dopadopa-accounts"
    static let deviceId = "dopadopa-device-id"
}

/// `state` オブジェクト（画面オフ時間を除く、ゴール・週間ポイントなどの値）の永続化用モデル。
/// スマホの画面オフ時間そのものは `ScreenOffTracker` が自分自身で保存・復元する。
struct PersistedState: Codable {
    var goalSeconds: TimeInterval
    var weeklyPoints: Int
    var weeklyStartDate: String
    var screenTimeSeconds: TimeInterval

    static let dailyGoalSeconds: TimeInterval = 180 * 60

    static let initial = PersistedState(
        goalSeconds: dailyGoalSeconds,
        weeklyPoints: 0,
        weeklyStartDate: Self.currentWeekKey(),
        screenTimeSeconds: 0
    )

    /// 月曜始まりの週キー（例: "2026-08-24"）。script.js の getWeekKey() と同じ考え方。
    static func currentWeekKey(from date: Date = Date()) -> String {
        var calendar = Calendar(identifier: .iso8601)
        calendar.timeZone = TimeZone.current
        let weekday = calendar.component(.weekday, from: date) // 1 = Sunday ... 7 = Saturday
        // 月曜日を週の開始日とみなす
        let daysSinceMonday = (weekday + 5) % 7
        let monday = calendar.date(byAdding: .day, value: -daysSinceMonday, to: calendar.startOfDay(for: date)) ?? date
        return DateFormatter.isoDateOnly.string(from: monday)
    }
}

/// 1 台のデバイス上での累積値。Web 版のマルチデバイス同期構想を単純化したもの。
struct DeviceMetrics: Codable {
    var screenTimeSeconds: TimeInterval
    var digitalDetoxSeconds: TimeInterval
    var weeklyPoints: Int
}

struct AccountProfile: Codable {
    var devices: [String: DeviceMetrics] = [:]
}

enum Persistence {
    static func loadState() -> PersistedState {
        guard let data = UserDefaults.standard.data(forKey: StorageKey.state),
              let decoded = try? JSONDecoder().decode(PersistedState.self, from: data) else {
            return .initial
        }
        return decoded
    }

    static func saveState(_ state: PersistedState) {
        guard let data = try? JSONEncoder().encode(state) else { return }
        UserDefaults.standard.set(data, forKey: StorageKey.state)
    }

    static func currentUser() -> String? {
        UserDefaults.standard.string(forKey: StorageKey.user)
    }

    static func setCurrentUser(_ username: String?) {
        if let username, !username.isEmpty {
            UserDefaults.standard.set(username, forKey: StorageKey.user)
        } else {
            UserDefaults.standard.removeObject(forKey: StorageKey.user)
        }
    }

    static func deviceId() -> String {
        if let existing = UserDefaults.standard.string(forKey: StorageKey.deviceId) {
            return existing
        }
        let generated = "device-\(UUID().uuidString)"
        UserDefaults.standard.set(generated, forKey: StorageKey.deviceId)
        return generated
    }

    static func accountProfiles() -> [String: AccountProfile] {
        guard let data = UserDefaults.standard.data(forKey: StorageKey.accounts),
              let decoded = try? JSONDecoder().decode([String: AccountProfile].self, from: data) else {
            return [:]
        }
        return decoded
    }

    static func saveAccountProfiles(_ profiles: [String: AccountProfile]) {
        guard let data = try? JSONEncoder().encode(profiles) else { return }
        UserDefaults.standard.set(data, forKey: StorageKey.accounts)
    }

    @discardableResult
    static func ensureAccountProfile(for username: String) -> AccountProfile {
        var profiles = accountProfiles()
        if let existing = profiles[username] {
            return existing
        }
        let fresh = AccountProfile()
        profiles[username] = fresh
        saveAccountProfiles(profiles)
        return fresh
    }

    static func syncCurrentDeviceMetrics(username: String, screenTimeSeconds: TimeInterval, digitalDetoxSeconds: TimeInterval, weeklyPoints: Int) {
        var profiles = accountProfiles()
        var profile = profiles[username] ?? AccountProfile()
        profile.devices[deviceId()] = DeviceMetrics(
            screenTimeSeconds: max(0, screenTimeSeconds),
            digitalDetoxSeconds: max(0, digitalDetoxSeconds),
            weeklyPoints: max(0, weeklyPoints)
        )
        profiles[username] = profile
        saveAccountProfiles(profiles)
    }
}

extension DateFormatter {
    static let isoDateOnly: DateFormatter = {
        let formatter = DateFormatter()
        formatter.calendar = Calendar(identifier: .iso8601)
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.timeZone = TimeZone.current
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter
    }()
}

enum Formatters {
    /// 秒数を `00:00:00` 形式にフォーマットする（script.js の formatDuration と同じ）。
    static func duration(_ totalSeconds: TimeInterval) -> String {
        let seconds = max(0, Int(totalSeconds.rounded()))
        let hours = seconds / 3600
        let minutes = (seconds % 3600) / 60
        let remaining = seconds % 60
        return String(format: "%02d:%02d:%02d", hours, minutes, remaining)
    }

    /// ポイントを `1,234P` 形式にフォーマットする（script.js の formatPoints と同じ）。
    static func points(_ value: Int) -> String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .decimal
        let text = formatter.string(from: NSNumber(value: value)) ?? "\(value)"
        return "\(text)P"
    }

    static func hoursAndMinutes(_ totalSeconds: TimeInterval) -> String {
        let seconds = max(0, Int(totalSeconds.rounded()))
        let hours = seconds / 3600
        let minutes = (seconds % 3600) / 60
        return "\(hours)h \(minutes)m"
    }
}
