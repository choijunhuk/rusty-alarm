import SwiftUI

struct AlarmEditView: View {
    @State private var alarm: Alarm
    private let isNew: Bool
    private let onSave: (Alarm) -> Void
    private let onDelete: () -> Void

    init(initial: Alarm,
         onSave: @escaping (Alarm) -> Void,
         onDelete: @escaping () -> Void) {
        _alarm = State(initialValue: initial)
        // Heuristic: a newly created Alarm has empty title default & no
        // history; we treat unsaved items as "new".
        isNew = initial.title == "알람" && initial.hour == 8 && initial.minute == 0
        self.onSave = onSave
        self.onDelete = onDelete
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("시간") {
                    DatePicker("알람 시간",
                               selection: timeBinding,
                               displayedComponents: .hourAndMinute,
                    )
                    .datePickerStyle(.wheel)
                }

                Section("기본") {
                    TextField("알람 이름", text: $alarm.title)
                    TextField("한 줄 메시지 (선택)", text: $alarm.message)
                }

                Section("반복") {
                    HStack {
                        ForEach(["일", "월", "화", "수", "목", "금", "토"]
                            .enumerated().map { ($0.offset, $0.element) }, id: \.0) { (idx, label) in
                            Toggle(label, isOn: dayBinding(idx))
                                .toggleStyle(.button)
                                .tint(.purple)
                        }
                    }
                }

                Section("소리") {
                    Toggle("알람 소리", isOn: $alarm.soundEnabled)
                    Toggle("진동", isOn: $alarm.vibrate)
                    if alarm.soundEnabled {
                        Stepper("음량 \(alarm.alarmVolumePercent)%",
                                value: $alarm.alarmVolumePercent,
                                in: 0...100, step: 10)
                    }
                    TextField("YouTube URL (선택)",
                              text: Binding(get: { alarm.youtubeUrl ?? "" },
                                            set: { alarm.youtubeUrl = $0.isEmpty ? nil : $0 }))
                }

                Section("챌린지") {
                    Picker("챌린지 유형", selection: $alarm.challengeType) {
                        ForEach(ChallengeType.allCases) { c in
                            Text(c.label).tag(c)
                        }
                    }
                }

                Section("기상 후 루틴") {
                    TextField("한 줄에 하나씩 (저장 시 줄바꿈 → 항목)",
                              text: Binding(get: { alarm.routineItems.joined(separator: "\n") },
                                            set: { alarm.routineItems = $0
                                                .split(separator: "\n")
                                                .map(String.init)
                                                .filter { !$0.isEmpty } }),
                              axis: .vertical)
                    .lineLimit(3...6)
                }

                if !isNew {
                    Section {
                        Button("삭제", role: .destructive, action: onDelete)
                    }
                }
            }
            .navigationTitle(isNew ? "알람 추가" : "알람 수정")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("저장") { onSave(alarm) }
                }
            }
        }
    }

    private var timeBinding: Binding<Date> {
        Binding(
            get: {
                Calendar.current.date(
                    bySettingHour: alarm.hour,
                    minute: alarm.minute,
                    second: 0,
                    of: Date(),
                ) ?? Date()
            },
            set: { newDate in
                let comps = Calendar.current.dateComponents([.hour, .minute], from: newDate)
                alarm.hour = comps.hour ?? 8
                alarm.minute = comps.minute ?? 0
            }
        )
    }

    private func dayBinding(_ idx: Int) -> Binding<Bool> {
        Binding(
            get: { alarm.repeatDays.contains(idx) },
            set: { on in
                if on { alarm.repeatDays.append(idx) }
                else  { alarm.repeatDays.removeAll { $0 == idx } }
                alarm.repeatDays.sort()
            },
        )
    }
}
