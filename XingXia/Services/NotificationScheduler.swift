import UserNotifications
import UIKit

/// 每日运势本地通知。
/// 因为每天的运势内容不同，不能用一个重复通知，所以一次性排好未来 30 天，每次回到前台时刷新。
@MainActor
enum NotificationScheduler {
    private static let prefix = "daily-fortune-"

    static func requestAuthorization() async -> Bool {
        let center = UNUserNotificationCenter.current()
        let settings = await center.notificationSettings()
        switch settings.authorizationStatus {
        case .authorized, .provisional, .ephemeral: return true
        case .denied: return false
        default:
            return (try? await center.requestAuthorization(options: [.alert, .sound, .badge])) ?? false
        }
    }

    static func reschedule() {
        let enabled = AppSettings.shared.dailyReminderEnabled
        let hour = AppSettings.shared.reminderHour
        let minute = AppSettings.shared.reminderMinute
        let birth = AppState.shared.birth
        let name = AppState.shared.primary?.name

        Task.detached(priority: .utility) {
            let center = UNUserNotificationCenter.current()
            let pending = await center.pendingNotificationRequests()
            center.removePendingNotificationRequests(withIdentifiers: pending.map(\.identifier).filter { $0.hasPrefix(prefix) })
            guard enabled else { return }

            let now = Date()
            let calendar = Calendar.gregorianLocal
            for offset in 0..<30 {
                let day = DayKey.today.adding(days: offset)
                var components = DateComponents(year: day.year, month: day.month, day: day.day, hour: hour, minute: minute)
                components.calendar = calendar
                guard let fireDate = calendar.date(from: components), fireDate > now else { continue }

                let fortune = FortuneEngine.daily(for: birth, on: day)
                let content = UNMutableNotificationContent()
                content.title = "\(day.month)月\(day.day)日 · \(fortune.levelName) \(fortune.score)分"
                if let name, !name.isEmpty { content.subtitle = "\(name)的今日运势" }
                content.body = "\(fortune.summary)\n宜 \(fortune.funYi.joined(separator: "、"))  忌 \(fortune.funJi.joined(separator: "、"))"
                content.sound = .default
                content.threadIdentifier = "daily-fortune"
                content.userInfo = ["url": "xingxia://fortune"]
                let trigger = UNCalendarNotificationTrigger(dateMatching: components, repeats: false)
                try? await center.add(UNNotificationRequest(identifier: prefix + day.description, content: content, trigger: trigger))
            }
        }
    }
}

/// 点按通知时跳到对应页面
final class NotificationDelegate: NSObject, UNUserNotificationCenterDelegate {
    static let shared = NotificationDelegate()

    func userNotificationCenter(_ center: UNUserNotificationCenter, didReceive response: UNNotificationResponse) async {
        if let string = response.notification.request.content.userInfo["url"] as? String, let url = URL(string: string) {
            await MainActor.run { AppState.shared.handle(url: url) }
        }
    }

    func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification) async -> UNNotificationPresentationOptions {
        [.banner, .sound]
    }
}
