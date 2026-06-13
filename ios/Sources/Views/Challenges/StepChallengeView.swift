import SwiftUI
import HealthKit

struct StepChallengeView: View {
    let target: Int
    let onSolved: () -> Void

    @State private var count: Int = 0
    @State private var status = "건강 앱 권한이 필요해요"
    private let health = HKHealthStore()

    var body: some View {
        VStack(spacing: 10) {
            Text("\(target) 걸음 걸으면 알람이 꺼져요")
                .font(.headline).foregroundStyle(.purple)
            Text("\(count) / \(target)")
                .font(.system(size: 48, weight: .bold))
                .foregroundStyle(.purple)
            ProgressView(value: Double(count) / Double(target)).tint(.purple)
            Text(status).font(.caption).foregroundStyle(.white.opacity(0.6))
        }
        .padding(18)
        .background(.white.opacity(0.05))
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .onAppear(perform: start)
    }

    private func start() {
        guard HKHealthStore.isHealthDataAvailable() else {
            status = "이 기기는 HealthKit을 지원하지 않아요"; return
        }
        let stepType = HKQuantityType.quantityType(forIdentifier: .stepCount)!
        health.requestAuthorization(toShare: [], read: [stepType]) { ok, _ in
            guard ok else {
                DispatchQueue.main.async { status = "권한이 거부됐어요" }
                return
            }
            DispatchQueue.main.async { status = "걷기를 측정 중이에요" }
            startObserving(stepType: stepType)
        }
    }

    private func startObserving(stepType: HKQuantityType) {
        let start = Date()
        let predicate = HKQuery.predicateForSamples(
            withStart: start, end: nil, options: [],
        )
        let query = HKObserverQuery(sampleType: stepType, predicate: predicate) { _, _, _ in
            fetchSum(stepType: stepType, since: start)
        }
        health.execute(query)
        fetchSum(stepType: stepType, since: start)
    }

    private func fetchSum(stepType: HKQuantityType, since start: Date) {
        let predicate = HKQuery.predicateForSamples(
            withStart: start, end: nil, options: [],
        )
        let q = HKStatisticsQuery(
            quantityType: stepType,
            quantitySamplePredicate: predicate,
            options: .cumulativeSum,
        ) { _, stats, _ in
            guard let qty = stats?.sumQuantity() else { return }
            let n = Int(qty.doubleValue(for: .count()))
            DispatchQueue.main.async {
                count = n
                if n >= target { onSolved() }
            }
        }
        health.execute(q)
    }
}
