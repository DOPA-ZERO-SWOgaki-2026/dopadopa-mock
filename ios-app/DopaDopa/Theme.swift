import SwiftUI

/// style.css の配色トークンを Swift 側に移植したもの。
enum Theme {
    static let primary = Color(red: 0x3D / 255, green: 0x7B / 255, blue: 0xFF / 255) // #3d7bff
    static let accent = Color(red: 0x1A / 255, green: 0xC2 / 255, blue: 0x9A / 255) // #1ac29a
    static let ink = Color(red: 0x18 / 255, green: 0x20 / 255, blue: 0x33 / 255) // #182033
    static let subtleInk = Color(red: 0x66 / 255, green: 0x72 / 255, blue: 0x8A / 255) // #66728a
    static let background = Color(red: 0xF4 / 255, green: 0xF7 / 255, blue: 0xFB / 255) // #f4f7fb
    static let cardBackground = Color.white.opacity(0.92)
    static let positive = accent
    static let ringGradient = LinearGradient(
        colors: [primary, accent],
        startPoint: .topLeading,
        endPoint: .bottomTrailing
    )

    static let cardCorner: CGFloat = 24
    static let controlCorner: CGFloat = 14
}
