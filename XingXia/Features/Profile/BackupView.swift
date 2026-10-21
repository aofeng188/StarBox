import SwiftUI
import SwiftData
import UniformTypeIdentifiers

/// 数据备份：导出为 JSON 文件、从文件恢复（与 Android 版互通）
struct BackupView: View {
    @Environment(\.modelContext) private var context
    @Environment(\.palette) private var palette
    @Environment(\.colorScheme) private var scheme
    @AppStorage(Backup.lastExportKey) private var lastExport: Double = 0

    @State private var counts: [BackupCategory: Int] = [:]
    @State private var document: BackupDocument?
    @State private var exporting = false
    @State private var importing = false
    @State private var pending: BackupFile?
    @State private var confirmReplace = false
    @State private var message: BackupMessage?
    #if DEBUG
    @State private var selfTestResult: String?
    #endif

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                intro
                exportCard
                restoreCard
                #if DEBUG
                selfTestCard
                #endif
                Text("备份文件是明文 JSON，含生日等个人信息，请存在自己的 iCloud 云盘或电脑里，不要随意转发。")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
            }
            .pageContent()
            .padding(.bottom, 30)
        }
        .cosmicBackground()
        .navigationTitle("数据备份")
        .onAppear(perform: refreshCounts)
        .confirmationDialog("用备份替换本机数据？", isPresented: $confirmReplace, titleVisibility: .visible) {
            Button("全部替换", role: .destructive) { restore(.replace) }
            Button("取消", role: .cancel) {}
        } message: {
            Text("本机对应的档案、记录、倒数日等会先被删除，再换成备份里的数据，无法撤销。不放心的话，先导出一份当前数据。")
        }
        .alert(message?.title ?? "", isPresented: Binding(get: { message != nil }, set: { if !$0 { message = nil } }), presenting: message) { _ in
            Button("好", role: .cancel) {}
        } message: { item in
            Text(item.text)
        }
    }

    // MARK: - 说明

    private var intro: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(spacing: 14) {
                GlowIcon(symbol: "externaldrive.fill", color: palette.accent, secondary: palette.accentSecondary, size: 52)
                VStack(alignment: .leading, spacing: 4) {
                    Text("把数据存成一个文件").font(.title3.bold())
                    Text("星匣的数据只保存在这台设备上，定期导出一份更安心。")
                        .font(.subheadline)
                        .subtleForeground()
                        .fixedSize(horizontal: false, vertical: true)
                }
            }
            VStack(alignment: .leading, spacing: 12) {
                BackupTip(symbol: "arrow.triangle.2.circlepath", title: "侧载安装",
                          text: "自签名安装的 App 每 7 天要重签一次，重签或重装前先导出，出了问题也能找回。")
                BackupTip(symbol: "iphone.gen3", title: "换手机",
                          text: "在旧手机上导出，把文件传到新手机，再用「从文件恢复」导入。")
                BackupTip(symbol: "arrow.left.arrow.right", title: "iPhone 和 Android 互通",
                          text: "两个版本用同一种备份格式，可以互相导入。")
            }
        }
        .glassCard()
    }

    // MARK: - 导出

    private var exportCard: some View {
        VStack(alignment: .leading, spacing: 14) {
            SectionHeader(title: "导出备份", symbol: "square.and.arrow.up")
            Label(lastExportText, systemImage: exportIsStale ? "exclamationmark.triangle.fill" : "checkmark.circle.fill")
                .font(.subheadline)
                .foregroundStyle(exportIsStale ? Color.caution(scheme) : Color.positive(scheme))
            let present = BackupCategory.allCases.filter { (counts[$0] ?? 0) > 0 }
            if !present.isEmpty {
                FlowLayout(spacing: 6) {
                    ForEach(present) { category in
                        TagChip(text: "\(category.name) \(counts[category] ?? 0) \(category.unit)")
                    }
                }
                .accessibilityElement(children: .combine)
                .accessibilityLabel("本机数据：" + present.map { "\($0.name) \(counts[$0] ?? 0) \($0.unit)" }.joined(separator: "，"))
            }
            Text("包括档案、决策记录、签与塔罗、心情、倒数日、转盘、菜单、抽奖池等全部数据，以及首页模块和快捷工具收藏；主题、音效等设置不在其中。")
                .font(.footnote)
                .subtleForeground()
                .fixedSize(horizontal: false, vertical: true)
            BigActionButton(title: "导出备份", symbol: "square.and.arrow.up", action: export)
        }
        .glassCard()
        .fileExporter(isPresented: $exporting, document: document, contentType: .json, defaultFilename: Backup.defaultFileName) { result in
            switch result {
            case .success:
                Backup.lastExportAt = Date()
                Haptics.success()
                Announce.post("备份已导出")
            case .failure(let error):
                if !Self.isCancel(error) {
                    message = BackupMessage(title: "导出失败", text: error.localizedDescription)
                }
            }
            document = nil
        }
    }

    private var daysSinceExport: Int? {
        guard lastExport > 0 else { return nil }
        return DayKey(Date(timeIntervalSince1970: lastExport)).days(to: .today)
    }

    /// 没导出过，或者离上次导出已满 7 天（侧载重签周期）
    private var exportIsStale: Bool { (daysSinceExport ?? 7) >= 7 }

    private var lastExportText: String {
        guard lastExport > 0, let days = daysSinceExport else { return "还没有导出过备份" }
        let date = Date(timeIntervalSince1970: lastExport)
        let when = "\(DateText.full(DayKey(date))) \(DateText.time(date))"
        return days >= 7 ? "上次导出：\(when)，已经 \(days) 天了" : "上次导出：\(when)"
    }

    private func export() {
        do {
            let data = try Backup.export(from: context, settings: .current)
            document = BackupDocument(data: data)
            exporting = true
        } catch {
            Haptics.error()
            message = BackupMessage(title: "导出失败", text: error.localizedDescription)
        }
    }

    // MARK: - 恢复

    private var restoreCard: some View {
        VStack(alignment: .leading, spacing: 14) {
            SectionHeader(title: "从文件恢复", symbol: "square.and.arrow.down")
            if let pending {
                preview(pending)
            } else {
                Text("选择星匣导出的备份文件（iPhone 或 Android 导出的都可以），会先列出文件里有哪些数据，再决定合并还是替换。")
                    .font(.subheadline)
                    .subtleForeground()
                    .fixedSize(horizontal: false, vertical: true)
                Button {
                    importing = true
                } label: {
                    Label("选择备份文件", systemImage: "folder.fill")
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 6)
                }
                .buttonStyle(.glass)
            }
        }
        .glassCard()
        .fileImporter(isPresented: $importing, allowedContentTypes: [.json, .plainText]) { result in
            switch result {
            case .success(let url):
                load(url)
            case .failure(let error):
                if !Self.isCancel(error) {
                    message = BackupMessage(title: "打不开这个文件", text: error.localizedDescription)
                }
            }
        }
    }

    @ViewBuilder
    private func preview(_ file: BackupFile) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("来自 \(file.platformName) 的备份").font(.headline)
            if let date = file.exportedAt {
                Text("\(DateText.full(DayKey(date))) \(DateText.time(date)) 导出")
                    .font(.subheadline)
                    .subtleForeground()
            }
        }
        .accessibilityElement(children: .combine)

        let present = BackupCategory.allCases.filter { (file.count(of: $0) ?? 0) > 0 }
        if present.isEmpty {
            Text("这份备份里没有数据。").font(.subheadline).subtleForeground()
        } else {
            FlowLayout(spacing: 6) {
                ForEach(present) { category in
                    TagChip(text: "\(category.name) \(file.count(of: category) ?? 0) \(category.unit)", color: palette.accentSecondary)
                }
            }
            .accessibilityElement(children: .combine)
            .accessibilityLabel("备份里有：" + present.map { "\($0.name) \(file.count(of: $0) ?? 0) \($0.unit)" }.joined(separator: "，"))
        }
        if file.includesSettings {
            Text("还包括首页模块和快捷工具收藏。").font(.footnote).subtleForeground()
        }

        Text("合并：保留本机数据，只添加备份里没有的。\n全部替换：本机对应的数据先删掉，再换成备份里的。")
            .font(.footnote)
            .subtleForeground()
            .fixedSize(horizontal: false, vertical: true)

        BigActionButton(title: "合并到本机", symbol: "plus.circle.fill", isEnabled: file.totalCount > 0 || file.includesSettings) {
            restore(.merge)
        }
        // 两个玻璃按钮放在同一个容器里，按下时彼此融合；一行放不下（超大字号）时竖排
        GlassEffectContainer(spacing: 12) {
            ViewThatFits(in: .horizontal) {
                HStack(spacing: 12) { secondaryActions(file) }
                VStack(spacing: 12) { secondaryActions(file) }
            }
        }
    }

    @ViewBuilder
    private func secondaryActions(_ file: BackupFile) -> some View {
        Button(role: .destructive) {
            confirmReplace = true
        } label: {
            Label("全部替换", systemImage: "arrow.triangle.2.circlepath")
                .frame(maxWidth: .infinity)
                .padding(.vertical, 4)
        }
        .buttonStyle(.glass)
        .disabled(file.totalCount == 0 && !file.includesSettings)
        Button {
            pending = nil
        } label: {
            Text("取消")
                .frame(maxWidth: .infinity)
                .padding(.vertical, 4)
        }
        .buttonStyle(.glass)
    }

    private func load(_ url: URL) {
        let access = url.startAccessingSecurityScopedResource()
        defer { if access { url.stopAccessingSecurityScopedResource() } }
        do {
            let data = try Data(contentsOf: url)
            pending = try Backup.parse(data)
            Haptics.selection()
            Announce.post("已读取备份文件，请选择合并或全部替换")
        } catch {
            Haptics.error()
            message = BackupMessage(title: "无法恢复", text: error.localizedDescription)
        }
    }

    private func restore(_ mode: BackupMode) {
        guard let file = pending else { return }
        do {
            let result = try Backup.restore(file, mode: mode, into: context)
            Backup.didRestore(context: context)
            pending = nil
            refreshCounts()
            Haptics.success()
            message = BackupMessage(title: "恢复完成", text: Self.summary(result, mode: mode))
        } catch {
            Haptics.error()
            message = BackupMessage(title: "恢复失败", text: "本机数据没有改动。\(error.localizedDescription)")
        }
    }

    private static func summary(_ result: BackupRestoreResult, mode: BackupMode) -> String {
        switch mode {
        case .merge:
            if result.totalAdded == 0 { return "本机已经包含备份里的全部内容，没有需要添加的。" }
            let skipped = result.skipped > 0 ? "，跳过 \(result.skipped) 项已有或重复的" : ""
            return "新增 \(result.totalAdded) 项数据\(skipped)。"
        case .replace:
            let skipped = result.skipped > 0 ? "（\(result.skipped) 项重复或不完整，已跳过）" : ""
            return "已换成备份里的数据，共恢复 \(result.totalAdded) 项\(skipped)。"
        }
    }

    private func refreshCounts() {
        counts = Backup.categoryCounts(in: context)
    }

    private static func isCancel(_ error: Error) -> Bool {
        (error as? CocoaError)?.code == .userCancelled
    }

    // MARK: - 自检

    #if DEBUG
    private var selfTestCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            SectionHeader(title: "开发自检", subtitle: "仅调试版", symbol: "checkmark.seal.fill")
            Button("导出再导入（内存数据库，不动真实数据）") {
                selfTestResult = Backup.selfTest()
            }
            .buttonStyle(.glass)
            if let selfTestResult {
                Text(selfTestResult).font(.footnote).subtleForeground()
            }
        }
        .glassCard()
    }
    #endif
}

private struct BackupMessage {
    let title: String
    let text: String
}

private struct BackupTip: View {
    let symbol: String
    let title: String
    let text: String
    @Environment(\.palette) private var palette

    var body: some View {
        HStack(alignment: .firstTextBaseline, spacing: 10) {
            Image(systemName: symbol)
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(palette.accentGradient)
                .frame(width: 22)
                .accessibilityHidden(true)
            VStack(alignment: .leading, spacing: 2) {
                Text(title).font(.subheadline.weight(.semibold))
                Text(text)
                    .font(.footnote)
                    .subtleForeground()
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .accessibilityElement(children: .combine)
    }
}

/// 导出用的 JSON 文件
struct BackupDocument: FileDocument {
    static var readableContentTypes: [UTType] { [.json] }

    let data: Data

    init(data: Data) { self.data = data }

    init(configuration: ReadConfiguration) throws {
        guard let data = configuration.file.regularFileContents else { throw CocoaError(.fileReadCorruptFile) }
        self.data = data
    }

    func fileWrapper(configuration: WriteConfiguration) throws -> FileWrapper {
        FileWrapper(regularFileWithContents: data)
    }
}
