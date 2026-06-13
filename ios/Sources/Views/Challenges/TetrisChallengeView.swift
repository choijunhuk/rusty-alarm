import SwiftUI

/// Minimal Tetris built on SwiftUI Canvas. Mirrors the Android version's
/// rules: clear at least one line to dismiss the alarm.
struct TetrisChallengeView: View {
    let onSolved: () -> Void

    private static let cols = 10
    private static let rows = 16

    @State private var board: [[Int]] = Array(
        repeating: Array(repeating: 0, count: cols),
        count: rows,
    )
    @State private var piece = TetrisPiece.spawn()
    @State private var pos: (row: Int, col: Int) = (0, cols / 2 - 2)
    @State private var gameOver = false
    @State private var ticker = Timer.publish(every: 0.8, on: .main, in: .common).autoconnect()

    var body: some View {
        VStack(spacing: 10) {
            Text("한 줄 만들면 알람이 꺼져요")
                .font(.headline).foregroundStyle(.purple)
            Text("← → 이동 · ⟳ 회전 · ↓ 한 칸")
                .font(.caption).foregroundStyle(.white.opacity(0.6))

            Canvas { ctx, size in
                let cw = size.width / CGFloat(Self.cols)
                let ch = size.height / CGFloat(Self.rows)
                // grid
                for r in 0..<Self.rows {
                    for c in 0..<Self.cols {
                        let v = board[r][c]
                        if v != 0 {
                            let rect = CGRect(x: CGFloat(c) * cw + 1, y: CGFloat(r) * ch + 1,
                                              width: cw - 2, height: ch - 2)
                            ctx.fill(Path(rect), with: .color(color(for: v)))
                        }
                    }
                }
                for (dc, dr) in piece.cells {
                    let c = pos.col + dc
                    let r = pos.row + dr
                    guard r >= 0, r < Self.rows, c >= 0, c < Self.cols else { continue }
                    let rect = CGRect(x: CGFloat(c) * cw + 1, y: CGFloat(r) * ch + 1,
                                      width: cw - 2, height: ch - 2)
                    ctx.fill(Path(rect), with: .color(color(for: piece.colorIndex)))
                }
            }
            .frame(maxWidth: .infinity, maxHeight: 380)
            .background(Color.black.opacity(0.4))
            .clipShape(RoundedRectangle(cornerRadius: 14))

            HStack(spacing: 12) {
                ctrl("chevron.left") { move(dc: -1) }
                ctrl("arrow.triangle.2.circlepath") { rotate() }
                ctrl("chevron.right") { move(dc: 1) }
                ctrl("arrow.down", prominent: true) { move(dr: 1) }
            }
        }
        .padding(18)
        .background(.white.opacity(0.05))
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .onReceive(ticker) { _ in tick() }
        .onDisappear { ticker.upstream.connect().cancel() }
    }

    @ViewBuilder
    private func ctrl(_ sys: String, prominent: Bool = false, _ action: @escaping () -> Void) -> some View {
        if prominent {
            Button(action: action) {
                Image(systemName: sys).frame(width: 56, height: 56)
            }
            .buttonStyle(.borderedProminent)
        } else {
            Button(action: action) {
                Image(systemName: sys).frame(width: 56, height: 56)
            }
            .buttonStyle(.bordered)
        }
    }

    private func tick() {
        guard !gameOver else { return }
        if canPlace(piece: piece, row: pos.row + 1, col: pos.col) {
            pos.row += 1
        } else {
            lock()
        }
    }

    private func move(dc: Int = 0, dr: Int = 0) {
        if canPlace(piece: piece, row: pos.row + dr, col: pos.col + dc) {
            pos.row += dr; pos.col += dc
        }
    }

    private func rotate() {
        let r = piece.rotated()
        if canPlace(piece: r, row: pos.row, col: pos.col) { piece = r }
    }

    private func lock() {
        for (dc, dr) in piece.cells {
            let r = pos.row + dr
            let c = pos.col + dc
            if r >= 0, r < Self.rows, c >= 0, c < Self.cols {
                board[r][c] = piece.colorIndex
            }
        }
        let cleared = clearFullRows()
        if cleared > 0 {
            onSolved(); return
        }
        piece = TetrisPiece.spawn()
        pos = (0, Self.cols / 2 - 2)
        if !canPlace(piece: piece, row: pos.row, col: pos.col) {
            gameOver = true
        }
    }

    private func clearFullRows() -> Int {
        let kept = board.filter { row in row.contains(0) }
        let cleared = Self.rows - kept.count
        let pad = Array(repeating: Array(repeating: 0, count: Self.cols), count: cleared)
        board = pad + kept
        return cleared
    }

    private func canPlace(piece: TetrisPiece, row: Int, col: Int) -> Bool {
        for (dc, dr) in piece.cells {
            let r = row + dr
            let c = col + dc
            if c < 0 || c >= Self.cols || r >= Self.rows { return false }
            if r >= 0, board[r][c] != 0 { return false }
        }
        return true
    }

    private func color(for idx: Int) -> Color {
        let palette: [Color] = [
            .clear, .cyan, .yellow, .purple, .green, .red, .orange, .indigo,
        ]
        return palette[min(idx, palette.count - 1)]
    }
}

struct TetrisPiece {
    let cells: [(Int, Int)]     // (dc, dr)
    let colorIndex: Int

    static let shapes: [[(Int, Int)]] = [
        [(0, 0), (1, 0), (2, 0), (3, 0)],  // I
        [(0, 0), (1, 0), (0, 1), (1, 1)],  // O
        [(0, 0), (1, 0), (2, 0), (1, 1)],  // T
        [(1, 0), (2, 0), (0, 1), (1, 1)],  // S
        [(0, 0), (1, 0), (1, 1), (2, 1)],  // Z
        [(0, 0), (0, 1), (0, 2), (1, 2)],  // L
        [(1, 0), (1, 1), (0, 2), (1, 2)],  // J
    ]

    static func spawn() -> TetrisPiece {
        let idx = Int.random(in: 0..<shapes.count)
        return TetrisPiece(cells: shapes[idx], colorIndex: idx + 1)
    }

    func rotated() -> TetrisPiece {
        let r = cells.map { (x, y) in (-y, x) }
        let minX = r.map(\.0).min() ?? 0
        let minY = r.map(\.1).min() ?? 0
        let shifted = r.map { (x, y) in (x - minX, y - minY) }
        return TetrisPiece(cells: shifted, colorIndex: colorIndex)
    }
}
