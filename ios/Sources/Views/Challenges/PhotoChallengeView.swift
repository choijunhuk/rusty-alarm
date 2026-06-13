import SwiftUI
import AVFoundation

struct PhotoChallengeView: View {
    let onSolved: () -> Void
    @State private var status = "침대 밖 사진을 찍어 알람을 꺼주세요"

    var body: some View {
        VStack(spacing: 10) {
            Text(status).font(.footnote).foregroundStyle(.white.opacity(0.7))
            CameraPreview()
                .frame(height: 240)
                .clipShape(RoundedRectangle(cornerRadius: 14))
            Button {
                onSolved()
            } label: {
                Label("셔터", systemImage: "camera.fill")
                    .frame(maxWidth: .infinity, minHeight: 48)
            }
            .buttonStyle(.borderedProminent)
        }
        .padding(18)
        .background(.white.opacity(0.05))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }
}

private struct CameraPreview: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> CameraVC { CameraVC() }
    func updateUIViewController(_ vc: CameraVC, context: Context) {}
}

private final class CameraVC: UIViewController {
    private let session = AVCaptureSession()
    private var preview: AVCaptureVideoPreviewLayer?

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .black
        guard let device = AVCaptureDevice.default(for: .video),
              let input = try? AVCaptureDeviceInput(device: device),
              session.canAddInput(input) else { return }
        session.addInput(input)
        let prev = AVCaptureVideoPreviewLayer(session: session)
        prev.videoGravity = .resizeAspectFill
        view.layer.addSublayer(prev)
        preview = prev
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        preview?.frame = view.bounds
    }
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        if !session.isRunning { DispatchQueue.global().async { self.session.startRunning() } }
    }
    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        if session.isRunning { session.stopRunning() }
    }
}
