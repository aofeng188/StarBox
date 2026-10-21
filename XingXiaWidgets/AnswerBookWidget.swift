import SwiftUI
import WidgetKit
import AppIntents

struct AnswerBookEntry: TimelineEntry {
    let date: Date
    /// 今天最近翻到的一页，今天还没翻是 nil
    let answer: WidgetToys.Answer?
}

/// 答案只在小组件自己用，不进决策历史，所以 App Group 不可用时也照常能翻
struct AnswerBookProvider: TimelineProvider {
    func placeholder(in context: Context) -> AnswerBookEntry {
        AnswerBookEntry(date: Date(), answer: nil)
    }

    func getSnapshot(in context: Context, completion: @escaping (AnswerBookEntry) -> Void) {
        if context.isPreview {
            completion(AnswerBookEntry(date: Date(), answer: WidgetToys.Answer(text: "相信你的直觉", day: DayKey.today.rawValue, count: 1)))
        } else {
            completion(AnswerBookEntry(date: Date(), answer: WidgetToys.answer(on: .today)))
        }
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<AnswerBookEntry>) -> Void) {
        // 过了零点合上书，回到「默念一个问题」
        let now = Date()
        let today = DayKey(now)
        let tomorrow = today.adding(days: 1).startOfDay
        let entries = [AnswerBookEntry(date: now, answer: WidgetToys.answer(on: today)),
                       AnswerBookEntry(date: tomorrow, answer: nil)]
        completion(Timeline(entries: entries, policy: .after(tomorrow.addingTimeInterval(5))))
    }
}

struct AnswerBookWidgetView: View {
    let entry: AnswerBookEntry
    @Environment(\.widgetFamily) private var family
    @Environment(\.colorScheme) private var scheme

    var body: some View {
        if family == .systemMedium {
            HStack(spacing: 14) {
                cover.frame(width: 76, height: 100)
                VStack(alignment: .leading, spacing: 8) {
                    header
                    Spacer(minLength: 0)
                    answerText(size: 20)
                    Spacer(minLength: 0)
                    flipButton
                }
            }
        } else {
            VStack(alignment: .leading, spacing: 6) {
                header
                Spacer(minLength: 0)
                answerText(size: 17)
                Spacer(minLength: 0)
                flipButton
            }
        }
    }

    private var header: some View {
        HStack(spacing: 4) {
            Image(systemName: "book.fill")
            Text("答案之书")
        }
        .font(.caption.weight(.semibold))
        .foregroundStyle(.secondary)
        .accessibilityElement(children: .combine)
    }

    @ViewBuilder
    private func answerText(size: CGFloat) -> some View {
        if let answer = entry.answer {
            Text(answer.text)
                .font(.system(size: size, weight: .semibold, design: .serif))
                .lineLimit(3)
                .minimumScaleFactor(0.7)
                .contentTransition(.opacity)
                .accessibilityLabel("答案：\(answer.text)")
        } else {
            Text("默念一个问题，\n再翻开一页")
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .lineLimit(2)
                .minimumScaleFactor(0.8)
        }
    }

    private var flipButton: some View {
        let palette = WidgetPalette.current(scheme)
        return Button(intent: AskAnswerBookIntent()) {
            Text(entry.answer == nil ? "翻开" : "再翻一页")
                .font(.caption.weight(.semibold))
                .foregroundStyle(palette.onAccent)
                .padding(.horizontal, 14)
                .padding(.vertical, 6)
                .frame(maxWidth: family == .systemMedium ? nil : CGFloat.infinity)
                .background(palette.accentGradient, in: .capsule)
        }
        .buttonStyle(.plain)
        .accessibilityLabel(entry.answer == nil ? "翻开答案之书" : "再翻一页答案之书")
    }

    /// 中号左边的书：金色封面，和 App 里答案之书的配色一致
    private var cover: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 10, style: .continuous)
                .fill(LinearGradient(colors: [Color(hex: 0xF5B83D), Color(hex: 0xB45309)], startPoint: .topLeading, endPoint: .bottomTrailing))
            RoundedRectangle(cornerRadius: 7, style: .continuous)
                .strokeBorder(Color(hex: 0xFFF1B8).opacity(0.6), lineWidth: 1)
                .padding(6)
            Image(systemName: "sparkles")
                .font(.title2)
                .foregroundStyle(Color(hex: 0xFFF1B8))
        }
        .shadow(color: Color(hex: 0xB45309).opacity(0.4), radius: 6, y: 3)
        .accessibilityHidden(true)
    }
}

struct AnswerBookWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: WidgetKinds.answerBook, provider: AnswerBookProvider()) { entry in
            AnswerBookWidgetView(entry: entry)
                .containerBackground(for: .widget) { WidgetCosmicBackground() }
                .widgetURL(URL(string: "xingxia://answers"))
        }
        .configurationDisplayName("答案之书")
        .description("心里默念一个问题，点一下翻开一页。")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}
