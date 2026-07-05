import Charts
import SwiftUI

struct StatsView: View {
    @EnvironmentObject var events: EventStore

    private var stats: StatsData { StatsData(events: events.events) }

    var body: some View {
        ScrollView {
            VStack(spacing: 14) {
                HStack(spacing: 12) {
                    StatCard(label: "총 알람 울림", value: "\(stats.totalFired)회")
                    StatCard(label: "총 스누즈",    value: "\(stats.totalSnoozed)회")
                }
                HStack(spacing: 12) {
                    StatCard(label: "평균 반응 시간", value: stats.avgResponseLabel)
                    StatCard(label: "기상 완료율",   value: stats.completionRateLabel)
                }

                // Last-7-days bar chart
                VStack(alignment: .leading, spacing: 10) {
                    Text("최근 7일 알람 횟수")
                        .font(.subheadline).foregroundStyle(.secondary)
                    if stats.dailyCounts.allSatisfy({ $0.count == 0 }) {
                        Text("데이터 없음")
                            .font(.subheadline).foregroundStyle(.tertiary)
                    } else {
                        Chart(stats.dailyCounts) { item in
                            BarMark(
                                x: .value("요일", item.label),
                                y: .value("횟수", item.count)
                            )
                            .foregroundStyle(Color.purple)
                            .cornerRadius(4)
                        }
                        .frame(height: 160)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(18)
                .background(.thinMaterial)
                .clipShape(RoundedRectangle(cornerRadius: 20))

                // Challenge usage breakdown
                VStack(alignment: .leading, spacing: 10) {
                    Text("챌린지 사용 현황")
                        .font(.subheadline).foregroundStyle(.secondary)
                    if stats.challengeBreakdown.isEmpty {
                        Text("데이터 없음")
                            .font(.subheadline).foregroundStyle(.tertiary)
                    } else {
                        ForEach(stats.challengeBreakdown, id: \.type) { item in
                            HStack {
                                Text(item.type.label)
                                    .font(.subheadline).foregroundStyle(.secondary)
                                Spacer()
                                Text("\(item.count)회")
                                    .fontWeight(.medium).foregroundStyle(.purple)
                            }
                        }
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(18)
                .background(.thinMaterial)
                .clipShape(RoundedRectangle(cornerRadius: 20))
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 32)
        }
        .navigationTitle("알람 통계")
        .navigationBarTitleDisplayMode(.inline)
    }
}

// MARK: - Supporting views

private struct StatCard: View {
    let label: String
    let value: String

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(label).font(.caption).foregroundStyle(.secondary)
            Text(value).font(.title3).bold().foregroundStyle(.purple)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .background(.thinMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }
}

// MARK: - Data model

private struct DailyItem: Identifiable {
    let id    = UUID()
    let label: String
    let count: Int
}

private struct ChallengeItem {
    let type:  ChallengeType
    let count: Int
}

private struct StatsData {
    let totalFired:          Int
    let totalSnoozed:        Int
    let avgResponseLabel:    String
    let completionRateLabel: String
    let dailyCounts:         [DailyItem]
    let challengeBreakdown:  [ChallengeItem]

    init(events: [AlarmEvent]) {
        totalFired   = events.filter { $0.type == .fired }.count
        totalSnoozed = events.filter { $0.type == .snoozed }.count

        let dismissed = events.filter { $0.type == .dismissed }
        let responses = dismissed.compactMap(\.responseSec)
        let avgSec    = responses.isEmpty ? 0 : responses.reduce(0, +) / responses.count
        avgResponseLabel = avgSec == 0 ? "—"
            : avgSec < 60  ? "\(avgSec)초"
            : "\(avgSec / 60)분 \(avgSec % 60)초"
        completionRateLabel = totalFired == 0 ? "—" : "\(dismissed.count * 100 / totalFired)%"

        let cal      = Calendar.current
        let dayFmt   = DateFormatter(); dayFmt.dateFormat   = "yyyy-MM-dd"
        let labelFmt = DateFormatter(); labelFmt.dateFormat = "E"
        labelFmt.locale = Locale(identifier: "ko_KR")
        let firedBuckets = Dictionary(
            grouping: events.filter { $0.type == .fired },
            by: { dayFmt.string(from: $0.timestamp) }
        )
        // Build entries oldest → newest (index 0 = 6 days ago, index 6 = today).
        dailyCounts = (0..<7).map { offset in
            let day = cal.date(byAdding: .day, value: -(6 - offset), to: Date())!
            return DailyItem(
                label: labelFmt.string(from: day),
                count: firedBuckets[dayFmt.string(from: day)]?.count ?? 0
            )
        }

        let withChallenge = dismissed.filter {
            guard let ct = $0.challengeType else { return false }
            return ct != .none
        }
        challengeBreakdown = Dictionary(grouping: withChallenge, by: { $0.challengeType! })
            .map { ChallengeItem(type: $0.key, count: $0.value.count) }
            .sorted { $0.count > $1.count }
    }
}
