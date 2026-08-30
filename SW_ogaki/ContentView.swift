import SwiftUI

struct ContentView: View {
    @ObservedObject private var tracker = ScreenOffTracker.shared

    var body: some View {
        VStack(spacing: 0) {
            VStack(spacing: 20) {
                Text("SW大垣")
                    .font(.largeTitle.bold())

                Text("画面オフ時間 自動計測")
                    .font(.headline)
                    .foregroundStyle(.secondary)

                VStack(spacing: 8) {
                    Text("累積画面オフ時間")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                    Text(format(tracker.totalScreenOffSeconds + tracker.currentOffSeconds))
                        .font(.system(size: 44, weight: .semibold, design: .rounded))
                        .monospacedDigit()
                }
                .padding()
                .frame(maxWidth: .infinity)
                .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 16))

                HStack(spacing: 8) {
                    Circle()
                        .fill(tracker.isScreenOff ? Color.red : Color.green)
                        .frame(width: 10, height: 10)
                    Text(tracker.isScreenOff ? "現在: 画面オフ中" : "現在: 画面オン")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }

                Button(role: .destructive) {
                    tracker.reset()
                } label: {
                    Text("計測をリセット")
                }
                .buttonStyle(.bordered)
            }
            .padding()

            Divider()

            // 消えた時刻・ついた時刻・その差分の履歴一覧
            List(tracker.events) { event in
                VStack(alignment: .leading, spacing: 4) {
                    HStack {
                        Label(timeString(event.offAt), systemImage: "moon.fill")
                        Image(systemName: "arrow.right")
                            .foregroundStyle(.secondary)
                        Label(timeString(event.onAt), systemImage: "sun.max.fill")
                    }
                    .font(.subheadline)

                    Text("差分: \(format(event.duration))")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
                .padding(.vertical, 2)
            }
            .listStyle(.plain)
            .overlay {
                if tracker.events.isEmpty {
                    Text("まだ記録がありません\n画面をロックして解除すると記録されます")
                        .multilineTextAlignment(.center)
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
            }
        }
    }

    private func format(_ seconds: TimeInterval) -> String {
        let total = Int(seconds.rounded())
        let h = total / 3600
        let m = (total % 3600) / 60
        let s = total % 60
        return String(format: "%02d:%02d:%02d", h, m, s)
    }

    private func timeString(_ date: Date) -> String {
        date.formatted(date: .omitted, time: .standard)
    }
}

#Preview {
    ContentView()
}
