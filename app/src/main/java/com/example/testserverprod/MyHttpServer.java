package com.example.testserverprod;

import android.util.Log;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class MyHttpServer extends NanoHTTPD {

    private static final String TAG = "MyHttpServer";

    // Prevent log spam / huge payloads
    private static final int MAX_BODY_LOG_CHARS = 16 * 1024; // 16KB

    public MyHttpServer(int port) {
        super(port);
    }

    @Override
    public Response serve(IHTTPSession session) {
        long t0 = System.nanoTime();

        Method method = session.getMethod();
        String uri = session.getUri();
        String query = session.getQueryParameterString(); // may be null
        Map<String, String> headers = session.getHeaders();

        // Parse body early (only for methods that may carry a body)
        // This also populates session.getParms() for form submissions.
        Map<String, String> files = new HashMap<>();
        String bodyForLog = null;

        if (method == Method.POST || method == Method.PUT || method == Method.DELETE) {
            try {
                session.parseBody(files);
                bodyForLog = files.get("postData"); // may be null (e.g. multipart uploads)
            } catch (Exception e) {
                // Don’t fail the request because logging failed
                bodyForLog = "[body parse error: " + e.getClass().getSimpleName() + "] " + e.getMessage();
            }
        }

        Map<String, String> params = session.getParms();

        // Build request dump
        String requestDump = buildRequestDump(session, method, uri, query, headers, params, files, bodyForLog);
        Log.d(TAG, requestDump);

        // --- Your routing logic (unchanged) ---
        Response resp;

        if ("/".equals(uri)) {
            String html =
                    "<html><body>" +
                            "<h2>NanoHTTPD running!</h2>" +
                            "<p>Try: <code>/hello</code> or <code>/echo?msg=hi</code></p>" +
                            "</body></html>";
            resp = newFixedLengthResponse(Response.Status.OK, "text/html", html);

        } else if ("/hello".equals(uri)) {
            resp = newFixedLengthResponse("Hello from Android NanoHTTPD!");

        } else if ("/echo".equals(uri)) {
            String msg = params.get("msg");
            if (msg == null) msg = "";
            resp = newFixedLengthResponse("Echo: " + msg);

        } else {
            resp = newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not found: " + uri);
        }

        long ms = (System.nanoTime() - t0) / 1_000_000L;
        Log.d(TAG, "Response: status=" + (resp != null ? resp.getStatus() : "null")
                + " mime=" + (resp != null ? resp.getMimeType() : "null")
                + " durationMs=" + ms);

        return resp;
    }

    private String buildRequestDump(
            IHTTPSession session,
            Method method,
            String uri,
            String query,
            Map<String, String> headers,
            Map<String, String> params,
            Map<String, String> files,
            String body
    ) {
        StringBuilder sb = new StringBuilder(4096);

        int serverPort = getListeningPort();
        String clientIp = getClientIp(headers);
        String host = safeHeader(headers, "host");
        String ua = safeHeader(headers, "user-agent");

        sb.append("----- HTTP REQUEST BEGIN -----\n");
        sb.append("method=").append(method).append("\n");
        sb.append("path=").append(uri);
        if (query != null && !query.trim().isEmpty()) sb.append("?").append(query);
        sb.append("\n");

        sb.append("serverPort=").append(serverPort).append("\n");
        sb.append("clientIp=").append(clientIp).append("\n");
        sb.append("host=").append(host).append("\n");
        sb.append("userAgent=").append(ua).append("\n");

        sb.append("\n-- Headers --\n");
        if (headers != null) {
            for (Map.Entry<String, String> e : headers.entrySet()) {
                String k = e.getKey();
                String v = e.getValue();

                // Redact common sensitive headers
                if (k != null) {
                    String lk = k.toLowerCase();
                    if (lk.equals("authorization") || lk.equals("cookie") || lk.equals("set-cookie")) {
                        v = "***";
                    }
                }
                sb.append(k).append(": ").append(v).append("\n");
            }
        }

        sb.append("\n-- Params --\n");
        if (params != null && !params.isEmpty()) {
            for (Map.Entry<String, String> e : params.entrySet()) {
                sb.append(e.getKey()).append(" = ").append(e.getValue()).append("\n");
            }
        } else {
            sb.append("(none)\n");
        }

        sb.append("\n-- Files map (parseBody) --\n");
        if (files != null && !files.isEmpty()) {
            for (Map.Entry<String, String> e : files.entrySet()) {
                // For multipart uploads, NanoHTTPD often stores temp file paths here
                sb.append(e.getKey()).append(" = ").append(e.getValue()).append("\n");
            }
        } else {
            sb.append("(none)\n");
        }

        sb.append("\n-- Body --\n");
        if (body == null || body.isEmpty()) {
            sb.append("(empty)\n");
        } else {
            String toLog = body;
            if (toLog.length() > MAX_BODY_LOG_CHARS) {
                toLog = toLog.substring(0, MAX_BODY_LOG_CHARS) + "\n... (truncated)\n";
            }
            sb.append(toLog).append("\n");
        }

        sb.append("----- HTTP REQUEST END -----\n");
        return sb.toString();
    }

    private String getClientIp(Map<String, String> headers) {
        if (headers == null) return "unknown";

        // If behind a proxy / reverse proxy
        String xff = headers.get("x-forwarded-for");
        if (xff != null && !xff.trim().isEmpty()) {
            int comma = xff.indexOf(',');
            return (comma > 0 ? xff.substring(0, comma) : xff).trim();
        }

        String cip = headers.get("http-client-ip");
        if (cip != null && !cip.trim().isEmpty()) return cip.trim();

        // NanoHTTPD commonly provides this
        String remoteAddr = headers.get("remote-addr");
        if (remoteAddr != null && !remoteAddr.trim().isEmpty()) return remoteAddr.trim();

        return "unknown";
    }

    private String safeHeader(Map<String, String> headers, String key) {
        if (headers == null) return "unknown";
        String v = headers.get(key);
        return (v == null || v.trim().isEmpty()) ? "unknown" : v.trim();
    }

    public void startServer() throws IOException {
        // socket read timeout: 5000ms, daemon: false
        start(5000, false);
        Log.i(TAG, "Server started on port=" + getListeningPort());
    }
}
