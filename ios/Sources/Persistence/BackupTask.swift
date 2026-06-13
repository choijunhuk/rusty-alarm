import Foundation
import BackgroundTasks

/// Weekly background snapshot of all alarms to JSON inside `Documents/backups/`.
/// Mirrors the Android `BackupWorker`. Register the identifier
/// `com.example.rustyalarm.backup` in the app's Info.plist under
/// `BGTaskSchedulerPermittedIdentifiers` and call `Backup.register()` from
/// the App init.
enum Backup {
    private static let id = "com.example.rustyalarm.backup"

    static func register() {
        BGTaskScheduler.shared.register(
            forTaskWithIdentifier: id, using: nil,
        ) { task in
            run(task: task as! BGProcessingTask)
        }
        schedule()
    }

    static func schedule() {
        let req = BGProcessingTaskRequest(identifier: id)
        req.requiresNetworkConnectivity = false
        req.requiresExternalPower = false
        req.earliestBeginDate = Calendar.current.date(
            byAdding: .day, value: 7, to: Date(),
        )
        try? BGTaskScheduler.shared.submit(req)
    }

    private static func run(task: BGProcessingTask) {
        task.expirationHandler = { task.setTaskCompleted(success: false) }
        do {
            try snapshot()
            task.setTaskCompleted(success: true)
        } catch {
            task.setTaskCompleted(success: false)
        }
        schedule()
    }

    /// Writes the current `AlarmStore` payload to a dated JSON file and
    /// trims to the latest 8 backups.
    static func snapshot() throws {
        guard let docs = FileManager.default.urls(
            for: .documentDirectory, in: .userDomainMask,
        ).first else { return }
        let dir = docs.appendingPathComponent("backups", isDirectory: true)
        try? FileManager.default.createDirectory(
            at: dir, withIntermediateDirectories: true,
        )
        let fmt = DateFormatter()
        fmt.dateFormat = "yyyy-MM-dd"
        let url = dir.appendingPathComponent("alarms-\(fmt.string(from: Date())).json")
        // Pull straight from UserDefaults to avoid requiring a live store
        let data = UserDefaults.standard.data(forKey: "rusty.alarms.v1") ?? Data("[]".utf8)
        try data.write(to: url, options: .atomic)
        prune(in: dir, keep: 8)
    }

    private static func prune(in dir: URL, keep: Int) {
        guard let files = try? FileManager.default.contentsOfDirectory(
            at: dir, includingPropertiesForKeys: [.contentModificationDateKey],
        ) else { return }
        let sorted = files.sorted {
            let a = (try? $0.resourceValues(forKeys: [.contentModificationDateKey])
                .contentModificationDate) ?? .distantPast
            let b = (try? $1.resourceValues(forKeys: [.contentModificationDateKey])
                .contentModificationDate) ?? .distantPast
            return a > b
        }
        for old in sorted.dropFirst(keep) {
            try? FileManager.default.removeItem(at: old)
        }
    }
}
