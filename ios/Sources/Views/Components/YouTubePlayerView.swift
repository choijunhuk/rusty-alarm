import SwiftUI
import WebKit

/// Embeds a YouTube video/playlist via the IFrame player. Mirrors the
/// Android `YouTubePlayerCard` security model — only alphanumeric IDs allow.
struct YouTubePlayerView: UIViewRepresentable {
    let url: String

    func makeUIView(context: Context) -> WKWebView {
        let cfg = WKWebViewConfiguration()
        cfg.allowsInlineMediaPlayback = true
        cfg.mediaTypesRequiringUserActionForPlayback = []
        let web = WKWebView(frame: .zero, configuration: cfg)
        web.backgroundColor = .black
        web.isOpaque = false
        web.scrollView.isScrollEnabled = false
        return web
    }

    func updateUIView(_ web: WKWebView, context: Context) {
        guard let html = buildHtml(for: url) else {
            web.loadHTMLString("<html><body style='background:#000'></body></html>", baseURL: nil)
            return
        }
        web.loadHTMLString(html, baseURL: URL(string: "https://www.youtube.com"))
    }

    private func buildHtml(for raw: String) -> String? {
        guard let parsed = YouTubeRef.parse(raw) else { return nil }
        let src: String
        if let v = parsed.videoId, let list = parsed.playlistId {
            src = "https://www.youtube.com/embed/\(v)?list=\(list)&autoplay=1&playsinline=1"
        } else if let list = parsed.playlistId {
            src = "https://www.youtube.com/embed/videoseries?list=\(list)&autoplay=1&playsinline=1"
        } else if let v = parsed.videoId {
            src = "https://www.youtube.com/embed/\(v)?autoplay=1&playsinline=1&loop=1&playlist=\(v)"
        } else {
            return nil
        }
        return """
        <!doctype html>
        <html><head>
          <meta name='viewport' content='width=device-width, initial-scale=1, user-scalable=no'/>
          <style>
            html,body{margin:0;padding:0;background:#000;height:100%;width:100%;overflow:hidden}
            iframe{width:100%;height:100%;border:0;display:block}
          </style>
        </head><body>
          <iframe allow='autoplay; fullscreen; encrypted-media'
            allowfullscreen src='\(src)'></iframe>
        </body></html>
        """
    }
}

private struct YouTubeRef {
    let videoId: String?
    let playlistId: String?

    static func parse(_ raw: String) -> YouTubeRef? {
        guard let comps = URLComponents(string: raw.trimmingCharacters(in: .whitespaces)),
              let host = comps.host?.lowercased()
        else { return nil }
        guard host == "youtube.com"
            || host == "music.youtube.com"
            || host.hasSuffix(".youtube.com")
            || host == "youtu.be"
        else { return nil }

        let list = safeId(value(comps, "list"))
        if host == "youtu.be" {
            let raw = comps.path.replacingOccurrences(of: "/", with: "")
            return YouTubeRef(videoId: safeId(raw), playlistId: list)
        }
        if let v = safeId(value(comps, "v")) {
            return YouTubeRef(videoId: v, playlistId: list)
        }
        if comps.path.contains("playlist"), let l = list {
            return YouTubeRef(videoId: nil, playlistId: l)
        }
        return nil
    }

    private static func value(_ comps: URLComponents, _ name: String) -> String? {
        comps.queryItems?.first(where: { $0.name == name })?.value
    }

    private static func safeId(_ s: String?) -> String? {
        guard let s, s.count <= 64,
              s.range(of: "^[A-Za-z0-9_-]+$", options: .regularExpression) != nil
        else { return nil }
        return s
    }
}
