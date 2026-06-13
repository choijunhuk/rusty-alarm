import SwiftUI
import AVFoundation

struct QrChallengeView: View {
    let requiredValue: String?
    let onSolved: () -> Void

    @State private var status = "QR 코드를 카메라에 비춰주세요"
    @State private var solved = false

    var body: some View {
        VStack(spacing: 10) {
            Text("QR 코드를 스캔해서 알람을 꺼주세요")
                .font(.footnote).foregroundStyle(.white.opacity(0.7))
            QrCameraView(onScan: handle)
                .frame(height: 240)
                .clipShape(RoundedRectangle(cornerRadius: 14))
            Text(status).font(.caption).foregroundStyle(.white.opacity(0.65))
        }
        .padding(18)
        .background(.white.opacity(0.05))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private func handle(_ value: String) {
        guard !solved else { return }
        let ok = (requiredValue ?? "").isEmpty || requiredValue == value
        if ok {
            solved = true
            status = "✓ 인식됨"
            onSolved()
        } else {
            status = "다른 QR 코드를 비춰주세요"
        }
    }
}

private struct QrCameraView: UIViewControllerRepresentable {
    let onScan: (String) -> Void

    func makeUIViewController(context: Context) -> QrVC {
        let vc = QrVC()
        vc.onScan = onScan
        return vc
    }
    func updateUIViewController(_ uiViewController: QrVC, context: Context) {}
}

private final class QrVC: UIViewController, AVCaptureMetadataOutputObjectsDelegate {
    var onScan: ((String) -> Void)?
    private let session = AVCaptureSession()
    private var preview: AVCaptureVideoPreviewLayer?
    private var lastScanned: String?

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .black
        configureSession()
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

    private func configureSession() {
        guard let device = AVCaptureDevice.default(for: .video),
              let input = try? AVCaptureDeviceInput(device: device),
              session.canAddInput(input) else { return }
        session.addInput(input)
        let out = AVCaptureMetadataOutput()
        guard session.canAddOutput(out) else { return }
        session.addOutput(out)
        out.setMetadataObjectsDelegate(self, queue: .main)
        out.metadataObjectTypes = [.qr]
        let prev = AVCaptureVideoPreviewLayer(session: session)
        prev.videoGravity = .resizeAspectFill
        view.layer.addSublayer(prev)
        preview = prev
    }

    func metadataOutput(
        _ output: AVCaptureMetadataOutput,
        didOutput metadataObjects: [AVMetadataObject],
        from connection: AVCaptureConnection,
    ) {
        guard let obj = metadataObjects.first as? AVMetadataMachineReadableCodeObject,
              let value = obj.stringValue, value != lastScanned else { return }
        lastScanned = value
        onScan?(value)
    }
}
