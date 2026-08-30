import SwiftUI

/// index.html の #goalModal 相当。
struct GoalSheetView: View {
    @EnvironmentObject private var appState: AppState
    @Environment(\.dismiss) private var dismiss
    @State private var minutesText: String

    init() {
        _minutesText = State(initialValue: String(AppState.shared.goalMinutes))
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("目標時間（分）") {
                    TextField("180", text: $minutesText)
                        .keyboardType(.numberPad)
                }
            }
            .navigationTitle("目標時間を設定")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("キャンセル") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("保存") {
                        if let minutes = Int(minutesText), minutes > 0 {
                            appState.updateGoalMinutes(minutes)
                            dismiss()
                        }
                    }
                }
            }
        }
        .presentationDetents([.medium])
    }
}

#Preview {
    GoalSheetView().environmentObject(AppState.shared)
}
