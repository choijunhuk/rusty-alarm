import SwiftUI

struct ReportView: View {
    @EnvironmentObject var events: EventStore
    @State private var report: WeeklyReport = WeeklyReport()

    var body: some View {
        ScrollView {
            VStack(spacing: 14) {
                streakCard
                metricRow
                responseMetricRow
                insightCard
                summaryCard
                heatmapCard
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 32)
        }
        .navigationTitle("주간 리포트")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear { report = events.weeklyReport() }
    }

    private var streakCard: some View {
        HStack(spacing: 16) {
            Image(systemName: "flame.fill")
                .font(.system(size: 40))
                .foregroundStyle(.orange)
            VStack(alignment: .leading, spacing: 2) {
                Text("연속 기상 \(report.streakDays)일").font(.title3).bold()
                Text(streakHint)
                    .font(.footnote).foregroundStyle(.secondary)
            }
            Spacer()
        }
        .padding(20)
        .background(.thinMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }

    private var streakHint: String {
        switch report.streakDays {
        case 7...:  return "일주일 연속 잘 일어나고 있어요"
        case 1...6: return "오늘도 잘 일어났어요"
        default:    return "오늘 첫 기상으로 시작해보세요"
        }
    }

    private var metricRow: some View {
        HStack(spacing: 12) {
            MetricBox(icon: "clock", title: "평균 기상 시각", value: report.avgWakeupHHMM)
            MetricBox(
                icon: "chart.line.uptrend.xyaxis",
                title: "기상 완료율",
                value: report.fired == 0
                    ? "—"
                    : "\(report.completionRatePercent)%",
            )
        }
    }

    private var responseMetricRow: some View {
        HStack(spacing: 12) {
            MetricBox(icon: "timer", title: "평균 끄기 시간", value: report.avgResponseLabel)
            MetricBox(
                icon: "arrow.triangle.2.circlepath",
                title: "스누즈 비율",
                value: report.fired == 0 ? "—" : "\(report.snoozeRatePercent)%",
            )
        }
    }

    private var insightCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("다음 개선 액션").font(.subheadline).foregroundStyle(.secondary)
            ForEach(report.insights.prefix(3)) { insight in
                VStack(alignment: .leading, spacing: 2) {
                    Text(insight.title).font(.subheadline).bold()
                    Text(insight.detail).font(.footnote).foregroundStyle(.secondary)
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(18)
        .background(.thinMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }

    private var summaryCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("이번 주 요약").font(.subheadline).foregroundStyle(.secondary)
            row("울린 알람", "\(report.fired)회")
            row("끈 알람", "\(report.dismissed)회")
            row("스누즈", "\(report.snoozed)회")
            if report.challengesCompleted > 0 {
                row("챌린지 완수", "\(report.challengesCompleted)회")
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(18)
        .background(.thinMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }

    private func row(_ label: String, _ value: String) -> some View {
        HStack {
            Text(label).foregroundStyle(.secondary).font(.subheadline)
            Spacer()
            Text(value).fontWeight(.medium)
        }
    }

    private var heatmapCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("최근 30일 기상 히트맵")
                .font(.subheadline).foregroundStyle(.secondary)
            HeatmapGrid(data: report.heatmap)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(18)
        .background(.thinMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }
}

private struct MetricBox: View {
    let icon: String
    let title: String
    let value: String

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Image(systemName: icon).foregroundStyle(.orange)
            Text(title).font(.caption).foregroundStyle(.secondary)
            Text(value).font(.title3).bold().foregroundStyle(.purple)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .background(.thinMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }
}

private struct HeatmapGrid: View {
    let data: [(String, Int)]

    var body: some View {
        let cols = 6
        let rows = 5
        let cells = data.prefix(rows * cols)
        let maxCount = max(1, cells.map(\.1).max() ?? 1)
        VStack(spacing: 4) {
            ForEach(0..<rows, id: \.self) { r in
                HStack(spacing: 4) {
                    ForEach(0..<cols, id: \.self) { c in
                        let idx = r * cols + c
                        let cnt = idx < cells.count ? cells[idx].1 : 0
                        let level = Double(cnt) / Double(maxCount)
                        RoundedRectangle(cornerRadius: 4)
                            .fill(Color.purple.opacity(0.1 + level * 0.8))
                            .frame(width: 28, height: 28)
                    }
                }
            }
        }
    }
}
