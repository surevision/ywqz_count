import os
import json
import datetime
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qsl

HOST = "0.0.0.0"
PORT = 8080


def log(message):
    print("[%s] %s" % (time.strftime("%Y-%m-%d %H:%M:%S"), message))


def on_post(params):
    timestamp = params.get("timestamp")
    print("timestamp =", timestamp)

    date = datetime.datetime.now()
    filepath = os.path.join(os.path.dirname(os.path.abspath(__file__)), f"data/{date.year}-{date.month}-{date.day}.txt")
    line = append_and_count_lines(filepath, f"{time.time()}\n")
    return {"data": line}


def on_get(params):
    date = datetime.datetime.now()
    filepath = os.path.join(os.path.dirname(os.path.abspath(__file__)), f"data/{date.year}-{date.month}-{date.day}.txt")
    line = count_lines(filepath)
    return {"data": line}
    

def on_del(params):
    pass


def on_index(params):
    pass


ROUTES = {
    ("POST", "/post"): on_post,
    ("GET", "/get"): on_get,
    ("POST", "/del"): on_del,
    ("GET", "/index"): on_index,
}

def append_and_count_lines(filepath, text):
    lines = []
    if not os.path.exists(filepath):
        if not os.path.exists(os.path.dirname(filepath)):
            os.makedirs(os.path.dirname(filepath))
        with open(filepath, 'w') as f:
            pass
    with open(filepath, 'a+', encoding='utf-8') as f:
        f.seek(0, os.SEEK_END)
        
        f.write(text)
        f.flush()
        
        f.seek(0, os.SEEK_SET)
        lines = f.readlines()
    
    return len(lines)

def count_lines(filepath):
    lines = []
    if os.path.exists(filepath):
        with open(filepath, 'a+', encoding='utf-8') as f:
            f.seek(0, os.SEEK_SET)
            lines = f.readlines()
    
    return len(lines)

class ApiHandler(BaseHTTPRequestHandler):
    server_version = "YWQZ/1.0"

    def do_GET(self):
        self._dispatch("GET")

    def do_POST(self):
        self._dispatch("POST")

    def _dispatch(self, method):
        path, _, query = self.path.partition("?")
        params = dict(parse_qsl(query)) if query else {}

        if method == "POST":
            raw = self._read_body()
            if raw:
                try:
                    data = json.loads(raw.decode("utf-8", "replace"))
                except ValueError:
                    params["_raw"] = raw.decode("utf-8", "replace")
                else:
                    if isinstance(data, dict):
                        params.update(data)
                    else:
                        params["_body"] = data

        log("%s -> %s %s params=%s" % (self.address_string(), method, self.path, params))

        handler = ROUTES.get((method, path))
        if handler is None:
            self._reply(404, {"code": 404, "method": method, "path": path})
            return

        try:
            result = handler(params)
        except Exception as exc:
            self._reply(500, {"code": 500, "error": str(exc)})
            return

        self._reply(200, {} if result is None else result)

    def _read_body(self):
        try:
            length = int(self.headers.get("Content-Length") or 0)
        except ValueError:
            return b""
        if length <= 0:
            return b""
        return self.rfile.read(length)

    def _reply(self, status, payload):
        body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        try:
            self.send_response(status)
            self.send_header("Content-Type", "application/json; charset=utf-8")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
        except OSError:
            pass

    def log_message(self, fmt, *args):
        print("%s - %s" % (self.address_string(), fmt % args))


def main():
    server = ThreadingHTTPServer((HOST, PORT), ApiHandler)
    print("YWQZ server listening on http://%s:%d" % (HOST, PORT))
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()


if __name__ == "__main__":
    main()
