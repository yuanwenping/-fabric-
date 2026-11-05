#!/usr/bin/env python3
import argparse
import os
import posixpath
import socketserver
import urllib.error
import urllib.parse
import urllib.request
from http.server import HTTPServer, SimpleHTTPRequestHandler


STATIC_DIR = ""
API_TARGET = ""


class GatewayHandler(SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)

    def translate_path(self, path):
        path = urllib.parse.urlparse(path).path
        path = posixpath.normpath(urllib.parse.unquote(path))
        parts = [part for part in path.split("/") if part not in ("", ".", "..")]
        result = os.path.join(STATIC_DIR, *parts)
        if os.path.isdir(result):
            result = os.path.join(result, "index.html")
        return result

    def do_GET(self):
        if self.path.startswith("/api/"):
            self.proxy_request("GET")
        else:
            super().do_GET()

    def do_POST(self):
        self.proxy_request("POST")

    def do_PUT(self):
        self.proxy_request("PUT")

    def do_DELETE(self):
        self.proxy_request("DELETE")

    def do_OPTIONS(self):
        self.send_response(204)
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Headers", "*")
        self.send_header("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS")
        self.end_headers()

    def proxy_request(self, method):
        length = int(self.headers.get("Content-Length", "0"))
        body = self.rfile.read(length) if length else None
        target = API_TARGET.rstrip("/") + self.path
        headers = {
            key: value
            for key, value in self.headers.items()
            if key.lower() not in {"host", "connection", "origin"}
        }
        request = urllib.request.Request(target, data=body, headers=headers, method=method)
        try:
            response = urllib.request.urlopen(request, timeout=60)
            data = response.read()
            self.send_response(response.status)
            for key, value in response.headers.items():
                if key.lower() not in {"transfer-encoding", "connection", "content-length"}:
                    self.send_header(key, value)
            self.send_header("Content-Length", str(len(data)))
            self.end_headers()
            self.wfile.write(data)
        except urllib.error.HTTPError as error:
            data = error.read()
            self.send_response(error.code)
            content_type = error.headers.get("Content-Type", "application/json;charset=UTF-8")
            self.send_header("Content-Type", content_type)
            self.send_header("Content-Length", str(len(data)))
            self.end_headers()
            self.wfile.write(data)
        except Exception as error:
            message = ("Backend connection failed: " + str(error)).encode("utf-8")
            self.send_response(502)
            self.send_header("Content-Type", "text/plain;charset=UTF-8")
            self.send_header("Content-Length", str(len(message)))
            self.end_headers()
            self.wfile.write(message)

    def send_head(self):
        requested = self.translate_path(self.path)
        if not os.path.exists(requested) and "." not in posixpath.basename(self.path):
            self.path = "/index.html"
        return super().send_head()


class ThreadingHTTPServer(socketserver.ThreadingMixIn, HTTPServer):
    daemon_threads = True


def main():
    global STATIC_DIR, API_TARGET
    parser = argparse.ArgumentParser(description="Supplier trace frontend and API gateway")
    parser.add_argument("--host", default="0.0.0.0")
    parser.add_argument("--port", type=int, default=5173)
    parser.add_argument("--static-dir", required=True)
    parser.add_argument("--api-target", default="http://127.0.0.1:8080")
    args = parser.parse_args()
    STATIC_DIR = os.path.abspath(args.static_dir)
    API_TARGET = args.api_target
    server = ThreadingHTTPServer((args.host, args.port), GatewayHandler)
    print("Serving %s at http://%s:%d, API -> %s" % (
        STATIC_DIR, args.host, args.port, API_TARGET
    ))
    server.serve_forever()


if __name__ == "__main__":
    main()
