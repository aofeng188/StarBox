import SwiftUI
import WidgetKit
import AppIntents

/// 控制中心按钮：一键打开某个工具
struct QuickToolControl: ControlWidget {
    var body: some ControlWidgetConfiguration {
        AppIntentControlConfiguration(kind: "XingXiaQuickTool", intent: QuickToolControlIntent.self) { configuration in
            ControlWidgetButton(action: OpenXingXiaIntent(destination: configuration.destination)) {
                Label(XingXiaDestination.caseDisplayRepresentations[configuration.destination]?.title ?? "星匣",
                      systemImage: configuration.destination.symbol)
            }
        }
        .displayName("星匣快捷工具")
        .description("从控制中心直接打开吃什么、抛硬币、求签等工具。")
    }
}

struct QuickToolControlIntent: ControlConfigurationIntent {
    static let title: LocalizedStringResource = "选择工具"

    @Parameter(title: "工具", default: .food)
    var destination: XingXiaDestination
}
