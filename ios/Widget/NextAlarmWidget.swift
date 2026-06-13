import WidgetKit
import SwiftUI

/// Home Screen / Lock Screen widget showing the next upcoming alarm.
/// Reads the shared `App Group` UserDefaults so the host app and widget
/// see the same alarm payload. Wire the App Group up in Xcode after
/// importing this file.
struct NextAlarmEntry: TimelineEntry {
    let date: Date
    let triggerAt: Date?
    let title: String?
    let streak: Int
}

struct NextAlarmProvider: TimelineProvider {
    func placeholder(in context: Context) -> NextAlarmEntry {
        NextAlarmEntry(date: Date(),
                       triggerAt: Date().addingTimeInterval(3600),
                       title: "아침 알람", streak: 3)
    }

    func getSnapshot(in context: Context, completion: @escaping (NextAlarmEntry) -> Void) {
        completion(load())
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<NextAlarmEntry>) -> Void) {
        let now = Date()
        let entries: [NextAlarmEntry] = [load(at: now), load(at: now.addingTimeInterval(60 * 15))]
        completion(Timeline(entries: entries, policy: .after(now.addingTimeInterval(60 * 15))))
    }

    private func load(at date: Date = Date()) -> NextAlarmEntry {
        let defaults = UserDefaults(suiteName: "group.com.example.rustyalarm")
        let ts = defaults?.double(forKey: "next.alarm.ts") ?? 0
        let title = defaults?.string(forKey: "next.alarm.title")
        let streak = defaults?.integer(forKey: "next.alarm.streak") ?? 0
        return NextAlarmEntry(
            date: date,
            triggerAt: ts > 0 ? Date(timeIntervalSince1970: ts) : nil,
            title: title,
            streak: streak,
        )
    }
}

struct NextAlarmWidgetView: View {
    let entry: NextAlarmEntry

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [Color(red: 0.11, green: 0.10, blue: 0.25),
                         Color(red: 0.16, green: 0.12, blue: 0.36)],
                startPoint: .topLeading, endPoint: .bottomTrailing,
            )
            VStack(alignment: .leading, spacing: 4) {
                Text("다음 알람").font(.caption2).foregroundStyle(.white.opacity(0.6))
                if let t = entry.triggerAt {
                    Text(t, style: .time)
                        .font(.system(size: 28, weight: .semibold))
                        .foregroundStyle(.white)
                    if let title = entry.title {
                        Text(title).font(.caption).foregroundStyle(.white.opacity(0.75))
                    }
                } else {
                    Text("없음").font(.title3).foregroundStyle(.white)
                }
                if entry.streak > 0 {
                    Label("\(entry.streak)일 연속", systemImage: "flame.fill")
                        .labelStyle(.titleAndIcon)
                        .font(.caption2)
                        .foregroundStyle(.orange)
                        .padding(.top, 2)
                }
            }
            .padding(12)
        }
    }
}

@main
struct NextAlarmWidget: Widget {
    let kind: String = "NextAlarmWidget"
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: NextAlarmProvider()) { entry in
            NextAlarmWidgetView(entry: entry)
        }
        .configurationDisplayName("다음 알람")
        .description("다가오는 알람과 연속 기상 일수를 표시합니다.")
        .supportedFamilies([
            .systemSmall, .systemMedium,
            .accessoryRectangular, .accessoryCircular,
        ])
    }
}
