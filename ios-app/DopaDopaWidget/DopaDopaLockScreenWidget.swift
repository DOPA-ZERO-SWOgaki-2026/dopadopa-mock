import WidgetKit
import SwiftUI

/// ロック画面用ウィジェット表示。ロック画面ウィジェットは iOS がシステムの配色で
/// 自動的にレンダリングするため、独自の色は付けず、標準のテキスト色や Gauge を使う。
struct DopaDopaLockScreenWidgetView: View {
    @Environment(\.widgetFamily) private var family
    let entry: DopaDopaEntry

    var body: some View {
        Group {
            switch family {
            case .accessoryCircular:
                Gauge(value: entry.data.goalRatio) {
                    Text("P")
                } currentValueLabel: {
                    Text("\(entry.data.totalPoints)")
                        .font(.system(size: 14, weight: .bold))
                        .minimumScaleFactor(0.5)
                }
                .gaugeStyle(.accessoryCircular)

            case .accessoryRectangular:
                VStack(alignment: .leading, spacing: 2) {
                    Text("DopaDopa")
                        .font(.caption2.weight(.semibold))
                    Text("デトックス \(Formatters.duration(entry.data.digitalDetoxSeconds))")
                        .font(.caption)
                        .monospacedDigit()
                    Text("累計 \(entry.data.totalPoints)P")
                        .font(.caption2)
                }
                .widgetAccentable()

            case .accessoryInline:
                Text("デトックス \(Formatters.duration(entry.data.digitalDetoxSeconds)) ・ \(entry.data.totalPoints)P")

            default:
                EmptyView()
            }
        }
        .containerBackground(.clear, for: .widget)
    }
}

struct DopaDopaLockScreenWidget: Widget {
    let kind = "DopaDopaLockScreenWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: DopaDopaTimelineProvider()) { entry in
            DopaDopaLockScreenWidgetView(entry: entry)
        }
        .configurationDisplayName("DopaDopa")
        .description("ロック画面で累計ポイントと今日のデジタルデトックスタイムを確認できます。")
        .supportedFamilies([.accessoryCircular, .accessoryRectangular, .accessoryInline])
    }
}

#Preview(as: .accessoryCircular) {
    DopaDopaLockScreenWidget()
} timeline: {
    DopaDopaEntry(date: .now, data: .placeholder)
}

#Preview(as: .accessoryRectangular) {
    DopaDopaLockScreenWidget()
} timeline: {
    DopaDopaEntry(date: .now, data: .placeholder)
}
