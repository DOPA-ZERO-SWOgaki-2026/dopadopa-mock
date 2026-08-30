import SwiftUI

/// index.html のログイン画面 / アプリ本体の出し分け相当。
struct ContentView: View {
    @EnvironmentObject private var appState: AppState

    var body: some View {
        Group {
            if appState.username != nil {
                DashboardView()
            } else {
                LoginView()
            }
        }
        .animation(.easeInOut, value: appState.username)
    }
}

#Preview {
    ContentView().environmentObject(AppState.shared)
}
