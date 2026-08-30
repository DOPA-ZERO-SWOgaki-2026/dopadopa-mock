import SwiftUI
import Charts

/// index.html の #appContainer 相当のメイン画面。
struct DashboardView: View {
    @EnvironmentObject private var appState: AppState
    @ObservedObject private var tracker = ScreenOffTracker.shared
    @State private var isGoalSheetPresented = false
    @State private var isShowingMoreEvents = false
    @State private var isShowingAllRewards = false

    private let collapsedEventCount = 3
    private let maxExpandedEvents = 8

    private var dayFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateFormat = "HH:mm"
        return formatter
    }()

    /// 各記録（画面ロック区間）に何ポイント割り当てるかを、時系列順の累積秒数から算出する。
    /// 区間ごとに秒数を個別に切り捨てるのではなく、累積の切り捨て差分を使うことで、
    /// 一覧に表示される +○P をすべて合計すると、必ずリング中央の累計ポイント（totalPoints）
    /// と一致するようにしている。
    private func eventPoints() -> [UUID: Int] {
        let chronological = tracker.events.sorted { $0.offAt < $1.offAt }
        var cumulativeSeconds: TimeInterval = 0
        var previousPoints = 0
        var mapping: [UUID: Int] = [:]
        for event in chronological {
            cumulativeSeconds += event.duration
            let currentPoints = AppState.points(for: cumulativeSeconds)
            mapping[event.id] = currentPoints - previousPoints
            previousPoints = currentPoints
        }
        return mapping
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                header
                heroCard
                usageBreakdownCard
                statsGrid
                bottomGrid
            }
            .padding(20)
        }
        .background(Theme.background.ignoresSafeArea())
        .sheet(isPresented: $isGoalSheetPresented) {
            GoalSheetView().environmentObject(appState)
        }
    }

    // MARK: - Header

    private var header: some View {
        HStack(alignment: .top) {
            HStack(spacing: 12) {
                Circle()
                    .fill(Theme.ringGradient)
                    .frame(width: 44, height: 44)
                    .overlay(Text("D").font(.headline.bold()).foregroundColor(.white))

                VStack(alignment: .leading, spacing: 2) {
                    Text("DopaDopa")
                        .font(.headline.bold())
                        .foregroundColor(Theme.ink)
                    Text(appState.username.map { "\($0) さんの記録" } ?? "スマホを見ない時間が、今日のご褒美に")
                        .font(.caption)
                        .foregroundColor(Theme.subtleInk)
                }
            }

            Spacer()

            VStack(alignment: .trailing, spacing: 8) {
                Button("目標時間設定") { isGoalSheetPresented = true }
                    .font(.caption.weight(.semibold))
                    .foregroundColor(Theme.primary)
                Button("ログアウト") { appState.logout() }
                    .font(.caption.weight(.semibold))
                    .foregroundColor(Theme.subtleInk)
            }
        }
    }

    // MARK: - Hero (progress ring)

    private var heroCard: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("今日の達成度")
                .font(.caption.weight(.bold))
                .foregroundColor(Theme.primary)

            Text("スマホフリー時間で\n点数が貯まる")
                .font(.system(size: 24, weight: .bold))
                .foregroundColor(Theme.ink)

            Text("1分スマホを見ない（画面ロックしている）ごとにポイントが増える。\n今日の休憩時間を、気持ちよく自分に還元しよう。")
                .font(.footnote)
                .foregroundColor(Theme.subtleInk)

            HStack(spacing: 20) {
                ProgressRing(ratio: appState.goalRatio, centerLabel: "累計", centerValue: Formatters.points(appState.totalPoints))
                    .frame(width: 150, height: 150)

                VStack(alignment: .leading, spacing: 12) {
                    miniStat(label: "スクリーンタイム", value: Formatters.duration(appState.totalScreenOnSeconds))
                    miniStat(label: "デジタルデトックスタイム", value: Formatters.duration(appState.digitalDetoxSeconds))
                    miniStat(label: "目標達成率", value: "\(Int((appState.goalRatio * 100).rounded()))%")
                }
                Spacer(minLength: 0)
            }
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Theme.cardBackground)
        .clipShape(RoundedRectangle(cornerRadius: Theme.cardCorner))
    }

    private func miniStat(label: String, value: String) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(label).font(.caption2).foregroundColor(Theme.subtleInk)
            Text(value).font(.subheadline.bold()).foregroundColor(Theme.ink).monospacedDigit()
        }
    }

    // MARK: - Usage breakdown (donut chart)

    /// 「DopaDopa／画面オフ／他のアプリ推定」の内訳をドーナツグラフで直感的に見せるカード。
    ///
    /// 注記: iOS では Apple の Screen Time API（Family Controls entitlement、Apple の
    /// 追加審査が必要）を使わない限り、サードパーティアプリが「どのアプリを何分」という
    /// 実際のアプリ別データを取得することはできない。そのためここでは「このアプリ」
    /// 「画面オフ（デトックス）」「他のアプリ（推定）」の3分類にとどめている。
    private var usageBreakdownCard: some View {
        let slices = appState.usageBreakdown
        let total = slices.reduce(0) { $0 + $1.seconds }

        return VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("使用時間の内訳").font(.headline).foregroundColor(Theme.ink)
                Spacer()
                Text("推定を含む")
                    .font(.caption2)
                    .foregroundColor(Theme.subtleInk)
            }

            if total <= 0 {
                Text("記録がまだありません")
                    .font(.footnote)
                    .foregroundColor(Theme.subtleInk)
                    .padding(.vertical, 24)
                    .frame(maxWidth: .infinity, alignment: .center)
            } else {
                HStack(spacing: 20) {
                    Chart(slices) { slice in
                        SectorMark(
                            angle: .value("秒数", max(slice.seconds, 0)),
                            innerRadius: .ratio(0.62),
                            angularInset: 1.5
                        )
                        .foregroundStyle(slice.color)
                        .cornerRadius(4)
                    }
                    .frame(width: 130, height: 130)
                    .chartLegend(.hidden)

                    VStack(alignment: .leading, spacing: 10) {
                        ForEach(slices) { slice in
                            HStack(spacing: 8) {
                                Circle().fill(slice.color).frame(width: 8, height: 8)
                                Text(slice.label)
                                    .font(.caption)
                                    .foregroundColor(Theme.subtleInk)
                                Spacer()
                                Text(Formatters.duration(slice.seconds))
                                    .font(.caption.weight(.semibold))
                                    .foregroundColor(Theme.ink)
                                    .monospacedDigit()
                                Text("(\(Int((slice.seconds / total * 100).rounded()))%)")
                                    .font(.caption2)
                                    .foregroundColor(Theme.subtleInk)
                            }
                        }
                    }
                }
            }

            Text("※ アプリ単位の内訳（Instagram○分、など）にはApple公式のScreen Time APIが必要で、別途Apple審査が必要な特別な権限になります。")
                .font(.caption2)
                .foregroundColor(Theme.subtleInk)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Theme.cardBackground)
        .clipShape(RoundedRectangle(cornerRadius: Theme.cardCorner))
    }

    // MARK: - Stats grid

    private var statsGrid: some View {
        VStack(spacing: 12) {
            HStack(spacing: 12) {
                statCard(title: "今週の獲得ポイント", value: Formatters.points(appState.weeklyPoints), trend: "今週", trendColor: Theme.accent)
                statCard(title: "最高記録", value: bestRecordText, trend: bestRecordTrend, trendColor: Theme.subtleInk)
            }
            statCard(title: "今日の目標", value: "\(appState.goalMinutes)分", trend: dailyProgressText, trendColor: appState.isGoalAchieved ? Theme.accent : Theme.primary, fullWidth: true)
        }
    }

    /// 1 回のロック区間として一番長かった記録。まだ 1 回も記録が無ければ、
    /// これまでの累積デトックスタイムを暫定値として表示する。
    private var bestRecordText: String {
        let longestEvent = tracker.events.map(\.duration).max()
        return Formatters.hoursAndMinutes(longestEvent ?? appState.digitalDetoxSeconds)
    }

    private var bestRecordTrend: String {
        guard let best = tracker.events.max(by: { $0.duration < $1.duration }) else {
            return "記録なし"
        }
        let formatter = DateFormatter()
        formatter.dateFormat = "M/d(E)"
        formatter.locale = Locale(identifier: "ja_JP")
        return formatter.string(from: best.offAt)
    }

    private var dailyProgressText: String {
        if appState.isGoalAchieved {
            return "目標達成!"
        }
        let remainingMinutes = Int((appState.remainingSeconds / 60).rounded(.up))
        return "あと \(remainingMinutes)分"
    }

    private func statCard(title: String, value: String, trend: String, trendColor: Color, fullWidth: Bool = false) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title).font(.caption).foregroundColor(Theme.subtleInk)
            Text(value).font(.title2.bold()).foregroundColor(Theme.ink)
            Text(trend).font(.caption.weight(.semibold)).foregroundColor(trendColor)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Theme.cardBackground)
        .clipShape(RoundedRectangle(cornerRadius: Theme.cardCorner))
    }

    // MARK: - Bottom grid (activity timeline + rewards)

    private var bottomGrid: some View {
        VStack(spacing: 12) {
            activityCard
            rewardsCard
        }
    }

    private var activityCard: some View {
        // 表示件数の内訳ごとの獲得ポイントを、累積秒数の端数丸め差分から算出する。
        // こうすることで「一覧に並んだ +○P の合計」が必ずリング中央の累計ポイントと一致する
        // （各イベントの秒数を個別に切り捨てると端数が積み重なってズレるため）。
        let pointsByEvent = eventPoints()
        let visibleCount = isShowingMoreEvents ? maxExpandedEvents : collapsedEventCount
        let visibleEvents = Array(tracker.events.prefix(visibleCount))

        return VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("今日の記録").font(.headline).foregroundColor(Theme.ink)
                Spacer()
                Text(tracker.isScreenOff ? "画面オフ中" : "継続中")
                    .font(.caption2.weight(.bold))
                    .padding(.horizontal, 10)
                    .padding(.vertical, 4)
                    .background(tracker.isScreenOff ? Theme.primary.opacity(0.15) : Theme.accent.opacity(0.15))
                    .foregroundColor(tracker.isScreenOff ? Theme.primary : Theme.accent)
                    .clipShape(Capsule())
            }

            if tracker.events.isEmpty {
                Text("まだ記録がありません。\n画面をロックして解除すると、ここに記録されます。")
                    .font(.footnote)
                    .foregroundColor(Theme.subtleInk)
                    .frame(maxWidth: .infinity, alignment: .center)
                    .multilineTextAlignment(.center)
                    .padding(.vertical, 12)
            } else {
                ForEach(visibleEvents) { event in
                    HStack {
                        Text("\(dayFormatter.string(from: event.offAt))〜\(dayFormatter.string(from: event.onAt))")
                            .font(.caption)
                            .foregroundColor(Theme.subtleInk)
                            .frame(width: 96, alignment: .leading)

                        Text("画面オフ \(Formatters.duration(event.duration))")
                            .font(.footnote)
                            .foregroundColor(Theme.ink)

                        Spacer()

                        Text("+\(pointsByEvent[event.id] ?? 0)P")
                            .font(.footnote.weight(.bold))
                            .foregroundColor(Theme.accent)
                    }
                    .padding(.vertical, 4)
                }

                if tracker.events.count > collapsedEventCount {
                    Button {
                        withAnimation { isShowingMoreEvents.toggle() }
                    } label: {
                        Text(isShowingMoreEvents ? "閉じる" : "もっと見る")
                            .font(.footnote.weight(.semibold))
                            .foregroundColor(Theme.primary)
                            .frame(maxWidth: .infinity)
                    }
                    .padding(.top, 4)
                }
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Theme.cardBackground)
        .clipShape(RoundedRectangle(cornerRadius: Theme.cardCorner))
    }

    /// 獲得できる特典の一覧。今後アイテムが増える想定なので、ここに追加していけば
    /// 自動的に「もっと見る」ボタンで一覧表示されるようになる。
    private static let allRewards: [Reward] = [
        Reward(emoji: "☕", title: "カフェ無料ドリンク", points: "1,200P"),
        Reward(emoji: "🎧", title: "シャープペン", points: "2,500P"),
        Reward(emoji: "📚", title: "図書券", points: "4,800P"),
    ]

    private let rewardsCollapsedCount = 3

    private var rewardsCard: some View {
        let visibleRewards = isShowingAllRewards
            ? Self.allRewards
            : Array(Self.allRewards.prefix(rewardsCollapsedCount))

        return VStack(alignment: .leading, spacing: 12) {
            Text("獲得できる特典").font(.headline).foregroundColor(Theme.ink)

            ForEach(visibleRewards) { reward in
                rewardRow(emoji: reward.emoji, title: reward.title, points: reward.points)
            }

            if Self.allRewards.count > rewardsCollapsedCount {
                Button {
                    withAnimation { isShowingAllRewards.toggle() }
                } label: {
                    Text(isShowingAllRewards ? "閉じる" : "もっと見る")
                        .font(.footnote.weight(.semibold))
                        .foregroundColor(Theme.primary)
                        .frame(maxWidth: .infinity)
                }
                .padding(.top, 4)
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Theme.cardBackground)
        .clipShape(RoundedRectangle(cornerRadius: Theme.cardCorner))
    }

    private func rewardRow(emoji: String, title: String, points: String) -> some View {
        HStack(spacing: 12) {
            Text(emoji).font(.title2)
            VStack(alignment: .leading, spacing: 2) {
                Text(title).font(.subheadline.bold()).foregroundColor(Theme.ink)
                Text(points).font(.caption).foregroundColor(Theme.subtleInk)
            }
            Spacer()
        }
    }
}

/// 獲得できる特典 1 件分。
private struct Reward: Identifiable {
    let id = UUID()
    let emoji: String
    let title: String
    let points: String
}

/// #ringProgress の SVG リングを SwiftUI で再現したもの。
private struct ProgressRing: View {
    let ratio: Double
    let centerLabel: String
    let centerValue: String

    var body: some View {
        ZStack {
            Circle()
                .stroke(Color.black.opacity(0.06), lineWidth: 14)

            Circle()
                .trim(from: 0, to: max(0, min(ratio, 1)))
                .stroke(Theme.ringGradient, style: StrokeStyle(lineWidth: 14, lineCap: .round))
                .rotationEffect(.degrees(-90))
                .animation(.easeInOut(duration: 0.4), value: ratio)

            VStack(spacing: 4) {
                Text(centerLabel).font(.caption2).foregroundColor(Theme.subtleInk)
                Text(centerValue)
                    .font(.title2.bold())
                    .foregroundColor(Theme.ink)
                    .minimumScaleFactor(0.6)
                    .lineLimit(1)
            }
            .padding(8)
        }
    }
}

#Preview {
    DashboardView().environmentObject(AppState.shared)
}
