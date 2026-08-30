import SwiftUI

@main
struct DopaDopaApp: App {
    // アプリ起動と同時に画面オフの自動計測（SW_ogaki 由来の ScreenOffTracker）を開始する
    @StateObject private var appState = AppState.shared
    @Environment(\.scenePhase) private var scenePhase

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(appState)
        }
        .onChange(of: scenePhase) { _, newPhase in
            appState.handleScenePhaseChange(newPhase)
        }
    }
}
