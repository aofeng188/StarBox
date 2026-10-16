import SwiftUI

/// 读屏播报。随机结果落定时调用，让 VoiceOver 用户不用去找结果文字。
@MainActor
enum Announce {
    static func post(_ text: String) {
        guard UIAccessibility.isVoiceOverRunning else { return }
        AccessibilityNotification.Announcement(text).post()
    }
}
