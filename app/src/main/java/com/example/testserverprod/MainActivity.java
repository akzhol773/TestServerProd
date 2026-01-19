package com.example.testserverprod;

import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.format.Formatter;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class MainActivity extends AppCompatActivity {

    private TextView txtInfo;
    private MyHttpServer server;

    // Pick a port. If you want “random free port”, that’s a bit different; this is simplest.
    private final int PORT = 8080;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtInfo = findViewById(R.id.txtInfo);
        Button btnStart = findViewById(R.id.btnStart);
        Button btnShow = findViewById(R.id.btnShow);
        Button btnStop = findViewById(R.id.btnStop);

        btnStart.setOnClickListener(v -> startNanoServer());
        btnShow.setOnClickListener(v -> showIpAndPort());
        btnStop.setOnClickListener(v -> stopNanoServer());
    }

    private void startNanoServer() {
        if (server != null) {
            txtInfo.setText("Status: Already running");
            return;
        }

        server = new MyHttpServer(PORT);
        try {
            server.startServer();
            String ip = getDeviceIpAddress();
            txtInfo.setText("Status: Running\nURL: http://" + ip + ":" + PORT + "/");
        } catch (IOException e) {
            txtInfo.setText("Status: Failed to start\n" + e.getMessage());
            server = null;
        }
    }

    private void stopNanoServer() {
        if (server != null) {
            server.stop();
            server = null;
            txtInfo.setText("Status: Stopped");
        } else {
            txtInfo.setText("Status: Not running");
        }
    }

    private void showIpAndPort() {
        String ip = getDeviceIpAddress();
        txtInfo.setText("IP: " + ip + "\nPort: " + PORT + "\nTry: http://" + ip + ":" + PORT + "/");
    }

    /**
     * Gets LAN IP (Wi-Fi) so other devices can reach it.
     * Works for common cases (Wi-Fi). If you use hotspot/cellular, results may differ.
     */
    private String getDeviceIpAddress() {
        try {
            // More robust than WifiManager-only, handles Ethernet etc.
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress addr = enumIpAddr.nextElement();
                    if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                        String ip = addr.getHostAddress();
                        // Ignore link-local 169.254.x.x
                        if (ip != null && !ip.startsWith("169.254")) return ip;
                    }
                }
            }
        } catch (Exception ignored) {}

        // Fallback (Wi-Fi only)
        try {
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            if (wm != null) {
                return Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
            }
        } catch (Exception ignored) {}

        return "0.0.0.0";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (server != null) {
            server.stop();
            server = null;
        }
    }
}
