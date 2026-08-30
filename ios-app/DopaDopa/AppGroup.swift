import Foundation

/// メインアプリとウィジェット（別プロセスで動く）がデータを共有するための App Group。
///
/// Xcode で本プロジェクトを開き「Automatically manage signing」が有効な状態で初めて
/// ビルドすると、この App Group はあなたの Apple ID / Team に自動で登録される
/// （Apple の追加審査は不要）。もし登録に失敗する場合は、Xcode の
/// Signing & Capabilities タブで DopaDopa / DopaDopaWidgetExtension 両方の
/// ターゲットに "App Groups" capability が付いていて、同じグループ ID を
/// 指しているか確認してください。
enum AppGroup {
    static let identifier = "group.com.example.dopadopa"
    static let defaults: UserDefaults = UserDefaults(suiteName: identifier) ?? .standard
}
