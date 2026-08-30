import WidgetKit
import SwiftUI

struct DopaDopaEntry: TimelineEntry {
    let date: Date
    let data: WidgetSnapshotData
}

/// ホーム画面ウィジェット・ロック画面ウィジェット共通のタイムラインプロバイダ。
/// ウィジェットは常時動いているわけではないため、一定間隔でこの `getTimeline` が
/// 呼ばれるたびに App Group から値を読み直す（＝疑似的な「更新」）。
struct DopaDopaTimelineProvider: TimelineProvider {
    func placeholder(in context: Context) -> DopaDopaEntry {
        DopaDopaEntry(date: Date(), data: .placeholder)
    }

    func getSnapshot(in context: Context, completion: @escaping (DopaDopaEntry) -> Void) {
        let data = context.isPreview ? WidgetSnapshotData.placeholder : WidgetSnapshotData.load()
        completion(DopaDopaEntry(date: Date(), data: data))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<DopaDopaEntry>) -> Void) {
        let entry = DopaDopaEntry(date: Date(), data: .load())
        // 15分ごとに再読み込みをリクエストする。実際の反映タイミングは iOS のバジェットに依存する。
        let nextUpdate = Calendar.current.date(byAdding: .minute, value: 15, to: Date()) ?? Date().addingTimeInterval(15 * 60)
        completion(Timeline(entries: [entry], policy: .after(nextUpdate)))
    }
}

/// ホーム画面用（小・中サイズ）のウィジェット表示。
struct DopaDopaHomeWidgetView: View {
    @Environment(\.widgetFamily) private var family
    let entry: DopaDopaEntry

    var body: some View {
        Group {
            switch family {
            case .systemMedium:
                mediumBody
            default:
                smallBody
            }
        }
        .containerBackground(for: .widget) {
            Theme.background
        }
    }

    private var ring: some View {
        ZStack {
            Circle().stroke(Theme.subtleInk.opacity(0.18), lineWidth: 9)
            Circle()
                .trim(from: 0, to: max(0, min(entry.data.goalRatio, 1)))
                .stroke(Theme.ringGradient, style: StrokeStyle(lineWidth: 9, lineCap: .round))
                .rotationEffect(.degrees(-90))
        }
    }

    private var smallBody: some View {
        VStack(spacing: 6) {
            ring
                .overlay(
                    VStack(spacing: 1) {
                        Text("\(entry.data.totalPoints)")
                            .font(.title3.bold())
                            .minimumScaleFactor(0.6)
                            .lineLimit(1)
                        Text("累計P")
                            .font(.system(size: 9))
                            .foregroundStyle(.secondary)
                    }
                )
                .padding(6)

            Text("デトックス \(Formatters.duration(entry.data.digitalDetoxSeconds))")
                .font(.system(size: 10))
                .minimumScaleFactor(0.7)
                .lineLimit(1)
                .foregroundStyle(.secondary)
        }
        .padding(4)
    }

    private var mediumBody: some View {
        HStack(spacing: 16) {
            ring
                .overlay(
                    VStack(spacing: 2) {
                        Text("\(entry.data.totalPoints)")
                            .font(.title.bold())
                            .minimumScaleFactor(0.6)
                            .lineLimit(1)
                        Text("累計P")
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                    }
                )
                .frame(width: 84, height: 84)

            VStack(alignment: .leading, spacing: 6) {
                Text("DopaDopa")
                    .font(.headline)
                    .foregroundColor(Theme.ink)
                Text("今日のデジタルデトックス")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
                Text(Formatters.duration(entry.data.digitalDetoxSeconds))
                    .font(.title3.bold())
                    .monospacedDigit()
                    .foregroundColor(Theme.ink)
                Text("目標達成率 \(Int((entry.data.goalRatio * 100).rounded()))%")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }
            Spacer(minLength: 0)
        }
    }
}

struct DopaDopaHomeWidget: Widget {
    let kind = "DopaDopaHomeWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: DopaDopaTimelineProvider()) { entry in
            DopaDopaHomeWidgetView(entry: entry)
        }
        .configurationDisplayName("DopaDopa")
        .description("累計ポイントと今日のデジタルデトックスタイムをホーム画面で確認できます。")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}

#Preview(as: .systemSmall) {
    DopaDopaHomeWidget()
} timeline: {
    DopaDopaEntry(date: .now, data: .placeholder)
}

#Preview(as: .systemMedium) {
    DopaDopaHomeWidget()
} timeline: {
    DopaDopaEntry(date: .now, data: .placeholder)
}
