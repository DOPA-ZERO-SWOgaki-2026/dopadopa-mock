import SwiftUI

@main
struct SW_ogakiApp: App {
    // アプリ起動と同時に画面オフの自動計測を開始する
    @StateObject private var screenOffTracker = ScreenOffTracker.shared

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
