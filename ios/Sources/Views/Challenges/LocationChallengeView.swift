import SwiftUI
import CoreLocation

struct LocationChallengeView: View {
    let targetLat: Double
    let targetLng: Double
    let radiusMeters: Int
    let onSolved: () -> Void

    @StateObject private var loc = LocationDriver()
    @State private var distance: Int? = nil
    @State private var status: String? = nil
    @State private var checking = false

    var body: some View {
        VStack(spacing: 10) {
            Text("지정된 장소에 도착해야 알람이 꺼집니다.")
                .font(.footnote).foregroundStyle(.white.opacity(0.7))
            Text("허용 반경: \(radiusMeters)m")
                .font(.caption).foregroundStyle(.white.opacity(0.6))
            if let d = distance {
                Text("현재 거리: \(d)m")
                    .font(.title3)
                    .foregroundStyle(d <= radiusMeters ? .purple : .white)
            }
            if let s = status {
                Text(s).font(.caption).foregroundStyle(.red)
            }
            Button {
                checking = true
                status = nil
                loc.requestOnce { result in
                    checking = false
                    switch result {
                    case .success(let coord):
                        let target = CLLocation(latitude: targetLat, longitude: targetLng)
                        let here = CLLocation(latitude: coord.latitude, longitude: coord.longitude)
                        let d = Int(here.distance(from: target))
                        distance = d
                        if d <= radiusMeters { onSolved() }
                        else { status = "아직 도착하지 않았어요" }
                    case .failure(let err):
                        status = err.localizedDescription
                    }
                }
            } label: {
                Label(checking ? "확인 중…" : "현재 위치 확인", systemImage: "location.fill")
                    .frame(maxWidth: .infinity, minHeight: 48)
            }
            .buttonStyle(.borderedProminent)
            .disabled(checking)
        }
        .padding(18)
        .background(.white.opacity(0.05))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }
}

private final class LocationDriver: NSObject, ObservableObject, CLLocationManagerDelegate {
    private let mgr = CLLocationManager()
    private var continuation: ((Result<CLLocationCoordinate2D, Error>) -> Void)?

    override init() {
        super.init()
        mgr.delegate = self
    }

    func requestOnce(_ completion: @escaping (Result<CLLocationCoordinate2D, Error>) -> Void) {
        continuation = completion
        mgr.requestWhenInUseAuthorization()
        mgr.requestLocation()
    }

    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        if let coord = locations.last?.coordinate {
            continuation?(.success(coord))
            continuation = nil
        }
    }
    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        continuation?(.failure(error))
        continuation = nil
    }
}
