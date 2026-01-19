package com.example.testserverprod;

import java.io.IOException;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class MyHttpServer extends NanoHTTPD {

    public MyHttpServer(int port) {
        super(port);
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        Map<String, String> params = session.getParms();

        if ("/".equals(uri)) {
            String html =
                    "<html><body>" +
                            "<h2>NanoHTTPD running!</h2>" +
                            "<p>Try: <code>/hello</code> or <code>/echo?msg=hi</code></p>" +
                            "</body></html>";
            return newFixedLengthResponse(Response.Status.OK, "text/html", html);
        }

        if ("/hello".equals(uri)) {
            return newFixedLengthResponse("Hello from Android NanoHTTPD!");
        }

        if ("/echo".equals(uri)) {
            String msg = params.get("msg");
            if (msg == null) msg = "";
            return newFixedLengthResponse("Echo: " + msg);
        }

        return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not found: " + uri);
    }

    public void startServer() throws IOException {
        // socket read timeout: 5000ms, daemon: false
        start(5000, false);
    }
}
