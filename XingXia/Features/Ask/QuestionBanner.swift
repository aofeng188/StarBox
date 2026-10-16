import SwiftUI
import SwiftData

/// 工具页、占卜页顶部的「你在问：……」。从问一问带着问题进来时显示，点 × 清掉
struct QuestionBanner: View {
    @Environment(AppState.self) private var app
    @Environment(\.palette) private var palette

    var body: some View {
        if let question = app.currentQuestion, !question.isEmpty {
            HStack(spacing: 10) {
                Image(systemName: "questionmark.bubble.fill")
                    .font(.subheadline)
                    .foregroundStyle(palette.accentGradient)
                    .accessibilityHidden(true)
                VStack(alignment: .leading, spacing: 1) {
                    Text("你在问").font(.caption2).foregroundStyle(.secondary)
                    Text(question).font(.subheadline.weight(.semibold)).lineLimit(2)
                }
                .accessibilityElement(children: .ignore)
                .accessibilityLabel("你在问：\(question)")
                Spacer(minLength: 0)
                Button {
                    withAnimation(.easeOut(duration: 0.2)) { app.currentQuestion = nil }
                    Haptics.selection()
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .font(.title3)
                        .symbolRenderingMode(.hierarchical)
                        .foregroundStyle(.secondary)
                        .frame(width: 36, height: 36)
                        .contentShape(.rect)
                }
                .buttonStyle(.plain)
                .accessibilityLabel("清除问题")
            }
            .padding(.leading, 14)
            .padding(.trailing, 4)
            .padding(.vertical, 4)
            .frame(maxWidth: .infinity, alignment: .leading)
            .glassEffect(.regular, in: .rect(cornerRadius: 20))
            .transition(.opacity)
        }
    }
}

extension View {
    /// 页面自带问题框时（硬币、塔罗、答案之书）：进来时把问一问的问题预填进去，不再显示横幅。
    /// 预填过的页面，之后改问题会同步回 currentQuestion，记进历史的问题和框里一致。
    func prefillsQuestion(_ text: Binding<String>) -> some View {
        modifier(QuestionPrefill(text: text))
    }
}

private struct QuestionPrefill: ViewModifier {
    @Binding var text: String
    @Environment(AppState.self) private var app
    @State private var prefilled = false

    func body(content: Content) -> some View {
        content
            .onAppear {
                guard !prefilled, text.isEmpty, let question = app.currentQuestion, !question.isEmpty else { return }
                text = question
                prefilled = true
            }
            .onChange(of: text) { _, newValue in
                guard prefilled else { return }
                let trimmed = newValue.trimmingCharacters(in: .whitespacesAndNewlines)
                let value: String? = trimmed.isEmpty ? nil : trimmed
                if app.currentQuestion != value { app.currentQuestion = value }
            }
    }
}

/// 问一问拆出的选项写进专用的「问一问」转盘（只建一个，之后每次覆盖它的选项）
@MainActor
enum AskWheel {
    private static let templateKey = "ask.wheelTemplate"
    static let templateName = "问一问"

    /// 有待预填的选项时写进转盘并返回它的 id；没有时返回 nil
    static func applyPending(in context: ModelContext, templates: [WheelTemplate]) -> UUID? {
        guard let options = AppState.shared.takePendingWheelOptions(), options.count >= 2 else { return nil }
        let storedID = UserDefaults.standard.string(forKey: templateKey)
        let template: WheelTemplate
        if let existing = templates.first(where: { $0.id.uuidString == storedID }) {
            template = existing
            let old = existing.options ?? []
            existing.options = []
            for option in old { context.delete(option) }
        } else {
            template = WheelTemplate(name: templateName)
            context.insert(template)
            UserDefaults.standard.set(template.id.uuidString, forKey: templateKey)
        }
        for (index, title) in options.enumerated() {
            let option = WheelOption(title: title, sortIndex: index)
            option.template = template
            context.insert(option)
        }
        try? context.save()
        return template.id
    }
}
