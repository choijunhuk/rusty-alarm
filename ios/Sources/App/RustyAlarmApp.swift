import SwiftUI

@main
struct RustyAlarmApp: App {
    @StateObject private var alarms = AlarmStore()
    @StateObject private var pets   = PetStore()
    @StateObject private var events = EventStore()

    init() {
        Task { await AlarmScheduler.shared.requestAuthorization() }
        WatchSync.shared.start()
        Backup.register()
    }

    var body: some Scene {
        WindowGroup {
            RootTabView()
                .environmentObject(alarms)
                .environmentObject(pets)
                .environmentObject(events)
                .preferredColorScheme(.dark)
        }
    }
}
