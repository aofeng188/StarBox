import SwiftUI
import SwiftData

/// 塔罗手记：按时间倒序回看每次占卜的牌阵和解读
struct TarotJournalView: View {
    @Environment(\.modelContext) private var context
    @Query(sort: \TarotReading.date, order: .reverse) private var readings: [TarotReading]

    private var grouped: [(DayKey, [TarotReading])] {
        var result: [(DayKey, [TarotReading])] = []
        for reading in readings {
            let key = DayKey(reading.date)
            if let last = result.last, last.0 == key {
                result[result.count - 1].1.append(reading)
            } else {
                result.append((key, [reading]))
            }
        }
        return result
    }

    var body: some View {
        List {
            ForEach(grouped, id: \.0) { day, items in
                Section {
                    ForEach(items) { reading in
                        NavigationLink {
                            TarotReadingDetailView(reading: reading)
                        } label: {
                            TarotJournalRow(reading: reading)
                        }
                        .swipeActions {
                            Button("删除", systemImage: "trash", role: .destructive) { delete(reading) }
                        }
                        .contextMenu {
                            Button("删除", systemImage: "trash", role: .destructive) { delete(reading) }
                        }
                    }
                } header: {
                    Text(day == .today ? "今天" : "\(DateText.full(day)) \(DateText.weekday(day))")
                }
                .glassRows()
            }
        }
        .overlay {
            if readings.isEmpty {
                EmptyHint(symbol: "book.closed", title: "还没有塔罗手记", message: "完成一次塔罗占卜、翻开所有牌后，牌阵和解读会自动记在这里")
            }
        }
        .cosmicBackground()
        .navigationTitle("塔罗手记")
        .navigationBarTitleDisplayMode(.inline)
    }

    private func delete(_ reading: TarotReading) {
        context.delete(reading)
        try? context.save()
    }
}

private struct TarotJournalRow: View {
    let reading: TarotReading

    @Environment(\.palette) private var palette

    var body: some View {
        let spread = reading.spreadKind
        let entries = reading.drawnCards
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: spread.symbol)
                .font(.body)
                .frame(width: 34, height: 34)
                .foregroundStyle(palette.ink(palette.accent))
                .background(Circle().fill(palette.accent.opacity(0.14)))
                .accessibilityHidden(true)
            VStack(alignment: .leading, spacing: 4) {
                HStack(alignment: .firstTextBaseline) {
                    Text(spread.name).font(.headline)
                    Spacer()
                    Text(DateText.time(reading.date)).font(.caption).foregroundStyle(.secondary)
                }
                if !reading.question.isEmpty {
                    Text("「\(reading.question)」").font(.subheadline).lineLimit(2)
                }
                Text(entries.map { "\($0.card.name)（\($0.reversed ? "逆位" : "正位")）" }.joined(separator: " · "))
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
        .padding(.vertical, 4)
        .accessibilityElement(children: .combine)
    }
}

/// 重看某一次占卜：牌面全部翻开，释义与当时的塔罗页一致
private struct TarotReadingDetailView: View {
    let reading: TarotReading

    var body: some View {
        let spread = reading.spreadKind
        let entries = reading.drawnCards
        let day = DayKey(reading.date)
        let single = entries.count == 1
        ScrollView {
            VStack(spacing: 18) {
                VStack(spacing: 6) {
                    Text("\(DateText.full(day)) \(DateText.weekday(day)) \(DateText.time(reading.date))")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    if !reading.question.isEmpty {
                        Text("「\(reading.question)」").font(AppFont.serif(18)).foregroundStyle(.secondary).multilineTextAlignment(.center)
                    }
                }
                .padding(.top, 8)

                HStack(alignment: .top, spacing: 12) {
                    ForEach(Array(entries.enumerated()), id: \.offset) { slot, entry in
                        VStack(spacing: 8) {
                            FlippableTarotCard(entry: entry, isFaceUp: true)
                                .frame(width: single ? 160 : 100, height: single ? 260 : 162)
                            Text(spread.position(slot)).font(.caption.weight(.semibold)).foregroundStyle(.secondary)
                        }
                    }
                }

                TarotInterpretations(spread: spread, question: reading.question, entries: entries)
            }
            .pageContent()
            .padding(.bottom, 30)
        }
        .cosmicBackground()
        .navigationTitle(spread.name)
        .navigationBarTitleDisplayMode(.inline)
        .tarotParallax()
    }
}

extension TarotReading {
    var spreadKind: TarotSpread { TarotSpread(rawValue: spread) ?? .single }

    /// 解析 "0u,13r,5u"（牌号 + 正位 u / 逆位 r）
    var drawnCards: [TarotView.DrawnCard] {
        cards.split(separator: ",").enumerated().compactMap { item -> TarotView.DrawnCard? in
            let token = item.element
            guard let flag = token.last, flag == "u" || flag == "r",
                  let id = Int(token.dropLast()),
                  let card = TarotCard.deck.first(where: { $0.id == id }) else { return nil }
            return TarotView.DrawnCard(deckIndex: item.offset, card: card, reversed: flag == "r")
        }
    }
}
