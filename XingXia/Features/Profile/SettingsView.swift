import SwiftUI
import SwiftData

struct SettingsView: View {
    @Environment(AppSettings.self) private var settings
    @Environment(\.modelContext) private var context
    @State private var showClearHistory = false
    @State private var notificationDenied = false

    var body: some View {
        @Bindable var settings = settings
        Form {
            Section {
                Toggle(isOn: $settings.soundEnabled) { Label("音效", systemImage: "speaker.wave.2.fill") }
                if settings.soundEnabled {
                    HStack {
                        Image(systemName: "speaker.fill").foregroundStyle(.secondary)
                        Slider(value: $settings.soundVolume, in: 0...1) { editing in
                            if !editing { SoundEngine.shared.play(.chime) }
                        }
                        Image(systemName: "speaker.wave.3.fill").foregroundStyle(.secondary)
                    }
                }
                Toggle(isOn: $settings.hapticsEnabled) { Label("震动反馈", systemImage: "iphone.radiowaves.left.and.right") }
                    .onChange(of: settings.hapticsEnabled) { _, on in if on { Haptics.impact() } }
            } header: {
                Text("声音与触感")
            } footer: {
                Text("音效会跟随手机静音开关，也不会打断你正在播放的音乐。")
            }
            .glassRows()

            Section {
                Toggle(isOn: $settings.shakeEnabled) { Label("摇一摇触发", systemImage: "iphone.gen3.radiowaves.left.and.right") }
                Toggle(isOn: $settings.backgroundAnimation) { Label("星空背景动画", systemImage: "sparkles") }
            } header: {
                Text("交互")
            } footer: {
                Text("在求签、骰子、硬币、吃什么等页面摇动手机即可开始。系统开启「减弱动态效果」或低电量模式时，背景会自动静止。")
            }
            .glassRows()

            Section {
                Toggle(isOn: $settings.dailyRitualEnabled) { Label("每日仪式", systemImage: "moon.circle.fill") }
            } header: {
                Text("每日仪式")
            } footer: {
                Text("每天第一次打开星匣时，先看看今天的月亮、运势、签和心情，随时可以跳过。走完仪式的日子会点亮当晚的月相，一轮集齐八种月相得一枚满月徽章。")
            }
            .glassRows()

            Section {
                Toggle(isOn: $settings.dailyReminderEnabled) { Label("每日运势提醒", systemImage: "bell.badge.fill") }
                    .onChange(of: settings.dailyReminderEnabled) { _, on in
                        Task {
                            if on {
                                let granted = await NotificationScheduler.requestAuthorization()
                                if !granted {
                                    settings.dailyReminderEnabled = false
                                    notificationDenied = true
                                }
                            }
                            NotificationScheduler.reschedule()
                        }
                    }
                if settings.dailyReminderEnabled {
                    DatePicker("提醒时间", selection: Binding(
                        get: { Calendar.current.date(from: DateComponents(hour: settings.reminderHour, minute: settings.reminderMinute)) ?? Date() },
                        set: {
                            let c = Calendar.current.dateComponents([.hour, .minute], from: $0)
                            settings.reminderHour = c.hour ?? 8
                            settings.reminderMinute = c.minute ?? 0
                            NotificationScheduler.reschedule()
                        }
                    ), displayedComponents: .hourAndMinute)
                }
                // 天象日历页也有同一个开关，两个页面会同时存活，所以只在自己被拨动时申请授权，不用 onChange
                Toggle(isOn: Binding(get: { settings.skyReminders }, set: { setSkyReminders($0) })) {
                    Label("天象提醒", systemImage: "moon.stars.fill")
                }
            } header: {
                Text("提醒")
            } footer: {
                Text("每日运势提醒在设定时间推送当天的运势摘要与宜忌；天象提醒在满月、新月和流星雨极大的前一天晚上 8 点提醒。")
            }
            .glassRows()

            Section {
                Toggle(isOn: $settings.profileBirthdaysInCountdowns) { Label("档案生日自动倒数", systemImage: "gift.fill") }
            } header: {
                Text("倒数日")
            } footer: {
                Text("档案里的生日会自动出现在倒数日列表、首页和小组件里，农历录入的按农历每年重复。关掉后只显示你自己添加的倒数日。")
            }
            .glassRows()

            Section {
                NavigationLink(value: AppRoute.backup) {
                    Label("数据备份", systemImage: "externaldrive.fill")
                }
                Button("清空决策历史", role: .destructive) { showClearHistory = true }
                Button("恢复默认首页布局") {
                    settings.homeCards = HomeCard.allCases
                    settings.hiddenHomeCards = []
                    Haptics.success()
                }
            } header: {
                Text("数据")
            } footer: {
                Text("数据只保存在本机。换手机、侧载重签前，可以在「数据备份」导出一份，iPhone 和 Android 版可以互相导入。")
            }
            .glassRows()

            Section {
                LabeledContent("版本", value: Bundle.main.appVersionText)
                LabeledContent("数据存储", value: "仅本机")
            } header: {
                Text("关于")
            } footer: {
                Text("星匣中的运势、星座、签文、塔罗等内容均为离线生成的娱乐内容，仅供参考，请理性看待。黄历节气与干支按天文算法计算。")
            }
            .glassRows()
        }
        .cosmicBackground(dimmed: true)
        .navigationTitle("设置")
        .confirmationDialog("清空所有决策历史？", isPresented: $showClearHistory, titleVisibility: .visible) {
            Button("清空", role: .destructive) {
                try? context.delete(model: DecisionRecord.self)
                try? context.save()
            }
        } message: {
            Text("此操作无法撤销。")
        }
        .alert("通知权限未开启", isPresented: $notificationDenied) {
            Button("去设置") {
                if let url = URL(string: UIApplication.openNotificationSettingsURLString) { UIApplication.shared.open(url) }
            }
            Button("好", role: .cancel) {}
        } message: {
            Text("请在系统设置中允许「星匣」发送通知。")
        }
    }

    private func setSkyReminders(_ on: Bool) {
        settings.skyReminders = on
        Task {
            if on {
                let granted = await NotificationScheduler.requestAuthorization()
                if !granted {
                    settings.skyReminders = false
                    notificationDenied = true
                }
            }
            NotificationScheduler.reschedule()
        }
    }
}
