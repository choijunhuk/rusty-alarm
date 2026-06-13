import SwiftUI

struct AlarmListView: View {
    @EnvironmentObject var store: AlarmStore
    @State private var editing: Alarm? = nil
    @State private var now: Date = Date()

    var body: some View {
        NavigationStack {
            ZStack {
                LinearGradient(
                    colors: [Color(red: 0.06, green: 0.05, blue: 0.15),
                             Color(red: 0.11, green: 0.10, blue: 0.25)],
                    startPoint: .top, endPoint: .bottom,
                )
                .ignoresSafeArea()

                ScrollView {
                    VStack(spacing: 12) {
                        Hero(next: nextAlarm(), now: now)
                            .padding(.top)
                        ForEach(store.alarms) { alarm in
                            AlarmRow(alarm: alarm,
                                     onTap: { editing = alarm },
                                     onToggle: { store.toggle(alarm, enabled: $0) })
                        }
                        Spacer(minLength: 80)
                    }
                    .padding(.horizontal, 16)
                }
            }
            .navigationTitle("Rusty Alarm")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        editing = Alarm()
                    } label: {
                        Image(systemName: "plus")
                    }
                }
            }
            .sheet(item: $editing) { a in
                AlarmEditView(initial: a) { saved in
                    store.upsert(saved)
                    editing = nil
                } onDelete: {
                    store.delete(a)
                    editing = nil
                }
            }
            .onAppear {
                Timer.scheduledTimer(withTimeInterval: 30, repeats: true) { _ in
                    now = Date()
                }
            }
        }
    }

    private func nextAlarm() -> (Alarm, Date)? {
        let candidates: [(Alarm, Date)] = store.alarms
            .filter { $0.enabled }
            .compactMap { alarm in
                guard let trigger = AlarmMath.nextTrigger(for: alarm, after: now)
                else { return nil }
                return (alarm, trigger)
            }
        return candidates.min { $0.1 < $1.1 }
    }
}

private struct Hero: View {
    let next: (Alarm, Date)?
    let now: Date

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("좋은 \(period())이에요")
                .font(.subheadline)
                .foregroundStyle(.white.opacity(0.85))

            if let next {
                Text(timeText(next.1))
                    .font(.system(size: 56, weight: .thin))
                    .foregroundStyle(.white)
                HStack(spacing: 6) {
                    Image(systemName: "alarm.fill")
                    Text("\(next.0.title) · \(countdown(next.1.timeIntervalSince(now)))")
                }
                .font(.callout)
                .foregroundStyle(.white.opacity(0.9))
            } else {
                Text("예정된 알람이 없어요")
                    .font(.title3)
                    .foregroundStyle(.white)
                Text("+ 버튼으로 알람을 추가하세요")
                    .font(.caption)
                    .foregroundStyle(.white.opacity(0.7))
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(20)
        .background(
            LinearGradient(
                colors: [Color(red: 0.36, green: 0.25, blue: 0.89),
                         Color(red: 1.0,  green: 0.48, blue: 0.27),
                         Color(red: 1.0,  green: 0.70, blue: 0.0 )],
                startPoint: .topLeading, endPoint: .bottomTrailing,
            )
        )
        .clipShape(RoundedRectangle(cornerRadius: 24))
    }

    private func timeText(_ d: Date) -> String {
        let f = DateFormatter(); f.dateFormat = "HH:mm"
        return f.string(from: d)
    }
    private func period() -> String {
        let h = Calendar.current.component(.hour, from: Date())
        switch h {
        case 0..<6:   return "밤"
        case 6..<12:  return "아침"
        case 12..<18: return "오후"
        default:      return "저녁"
        }
    }
    private func countdown(_ s: TimeInterval) -> String {
        let mins = max(1, Int(s) / 60)
        if mins >= 1440 { return "\(mins/1440)일 후" }
        if mins >= 60 { return "\(mins/60)시간 \(mins%60)분 후" }
        return "\(mins)분 후"
    }
}

private struct AlarmRow: View {
    let alarm: Alarm
    let onTap: () -> Void
    let onToggle: (Bool) -> Void

    var body: some View {
        HStack(alignment: .center, spacing: 12) {
            Rectangle()
                .fill(alarm.enabled ? Color(red: 0.36, green: 0.25, blue: 0.89)
                                    : Color.gray.opacity(0.3))
                .frame(width: 4)
                .clipShape(RoundedRectangle(cornerRadius: 2))
                .frame(height: 60)
            VStack(alignment: .leading, spacing: 2) {
                Text(String(format: "%02d:%02d", alarm.hour, alarm.minute))
                    .font(.system(size: 32, weight: .light))
                    .foregroundStyle(alarm.enabled ? .white : .white.opacity(0.45))
                Text(alarm.title)
                    .font(.footnote)
                    .foregroundStyle(.white.opacity(0.75))
            }
            Spacer()
            Toggle("", isOn: Binding(get: { alarm.enabled }, set: onToggle))
                .labelsHidden()
        }
        .padding(.vertical, 12)
        .padding(.horizontal, 14)
        .background(.white.opacity(0.06))
        .clipShape(RoundedRectangle(cornerRadius: 22))
        .onTapGesture(perform: onTap)
    }
}

enum AlarmMath {
    static func nextTrigger(for alarm: Alarm, after now: Date) -> Date? {
        let cal = Calendar.current
        var comps = cal.dateComponents([.year, .month, .day, .hour, .minute], from: now)
        comps.hour = alarm.hour
        comps.minute = alarm.minute
        comps.second = 0
        if let specific = alarm.specificDate {
            comps.year  = cal.component(.year,  from: specific)
            comps.month = cal.component(.month, from: specific)
            comps.day   = cal.component(.day,   from: specific)
            return cal.date(from: comps).flatMap { $0 > now ? $0 : nil }
        }
        guard let base = cal.date(from: comps) else { return nil }
        if alarm.repeatDays.isEmpty {
            return base > now ? base : cal.date(byAdding: .day, value: 1, to: base)
        }
        for offset in 0..<8 {
            guard let candidate = cal.date(byAdding: .day, value: offset, to: base)
            else { continue }
            if candidate <= now { continue }
            // iOS weekday: 1=Sun…7=Sat. Map to Android: 0=Sun…6=Sat.
            let weekday = cal.component(.weekday, from: candidate) - 1
            if alarm.repeatDays.contains(weekday) { return candidate }
        }
        return nil
    }
}
