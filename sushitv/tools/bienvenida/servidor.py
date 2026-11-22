"""Tiny local lab server: serves <dir> and saves POST /save?name=<file> bodies into <dir>/out/."""
import http.server, os, sys, urllib.parse

root, port = os.path.abspath(sys.argv[1]), int(sys.argv[2])
os.makedirs(os.path.join(root, 'out'), exist_ok=True)


class H(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *a, **k):
        super().__init__(*a, directory=root, **k)

    def end_headers(self):
        self.send_header('Cache-Control', 'no-store')
        super().end_headers()

    def do_POST(self):
        q = urllib.parse.parse_qs(urllib.parse.urlparse(self.path).query)
        name = os.path.basename(q.get('name', ['out.bin'])[0])
        body = self.rfile.read(int(self.headers.get('Content-Length', 0)))
        with open(os.path.join(root, 'out', name), 'wb') as f:
            f.write(body)
        self.send_response(200)
        self.end_headers()
        self.wfile.write(b'ok')


http.server.ThreadingHTTPServer(('127.0.0.1', port), H).serve_forever()
