package com.karma.spectrumzer.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.util.Log;

import com.google.gson.Gson;
import com.karma.spectrumzer.utility.ServerInfo;
import com.karma.spectrumzer.utility.Utility;

import java.io.IOException;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteOrder;

public class NetworkDiscoveryService extends Service {
    private static final String LOG_TAG = "Spectrumzer";
    private NetworkDiscoveryBinder _binder;
    private UDPBroadcast _udpThread = null;
    private int _port = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        _binder = new NetworkDiscoveryBinder(this);
        Log.d(LOG_TAG, "NetworkDiscoveryService: onCreate");

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOG_TAG, "NetworkDiscoveryService: onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(LOG_TAG, "NetworkDiscoveryService: onBind");
        return _binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(LOG_TAG, "NetworkDiscoveryService: onUnbind");
        stopNetworkDiscovery();
        return super.onUnbind(intent);
    }

    public void startNetworkDiscovery(int port) {
        Log.d(LOG_TAG, "NetworkDiscoveryService: startNetworkDiscovery");
        _port = port;
        if (_udpThread != null) {
            _udpThread.interrupt();
            _udpThread = null;
        }
        _udpThread = new UDPBroadcast(1);
        _udpThread.start();
    }

    public void stopNetworkDiscovery() {
        Log.d(LOG_TAG, "NetworkDiscoveryService: stopNetworkDiscovery");

        if (_udpThread != null) {
            _udpThread.interrupt();
            _udpThread = null;
        }
    }

    private class UDPBroadcast extends Thread {

        long _lastTime;
        long _interval;

        public UDPBroadcast(int fps) {
            _lastTime = 0;
            _interval = 1000 / fps;
        }

        @Override
        public void run() {
            try {
                Log.d(LOG_TAG, "NetworkDiscoveryService: UDPBroadcast: run");

                _lastTime = java.lang.System.currentTimeMillis();
                while (!isInterrupted()) {
                    long timeNow = java.lang.System.currentTimeMillis();
                    if ((timeNow - _lastTime) < _interval)//idle to keep in fps
                        continue;
                    else {
                        _lastTime = java.lang.System.currentTimeMillis();
                        broadcast();
                    }
                }
            } catch (Exception e) {
                Log.e(LOG_TAG, e.getMessage());
            }
        }

        private void broadcast() {
            //Log.d(LOG_TAG, "NetworkDiscoveryService: UDPBroadcast: send UDP packet");

            try {
                InetAddress localAddress = InetAddress.getLocalHost();

                ServerInfo serverInfo = new ServerInfo();
                serverInfo.server_ip = getWifiIpAddress();
                serverInfo.server_tcp_port = Utility.TCP_PORT;
                serverInfo.server_udp_port = Utility.UDP_PORT;
                serverInfo.server_key = Utility.KEY;

                Gson gson = new Gson();
                String json = gson.toJson(serverInfo);

                sendBroadcast(json);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        void sendBroadcast(String data) throws Exception {

            //Log.d(LOG_TAG, "NetworkDiscoveryService: sendBroadcast: data: " + data);
            DatagramSocket socket = new DatagramSocket();
            socket.setBroadcast(true);
            DatagramPacket packet = new DatagramPacket(data.getBytes(), data.length(), getBroadcastAddress(), _port);
            socket.send(packet);
        }

        protected String getWifiIpAddress() {
            WifiManager wifiManager = (WifiManager) _binder.getAppContext().getApplicationContext().getSystemService(WIFI_SERVICE);
            int ipAddress = wifiManager.getConnectionInfo().getIpAddress();

            // Convert little-endian to big-endianif needed
            if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
                ipAddress = Integer.reverseBytes(ipAddress);
            }

            byte[] ipByteArray = BigInteger.valueOf(ipAddress).toByteArray();

            String ipAddressString;
            try {
                ipAddressString = InetAddress.getByAddress(ipByteArray).getHostAddress();
            } catch (UnknownHostException ex) {
                Log.e("WIFIIP", "Unable to get host address.");
                ipAddressString = null;
            }

            return ipAddressString;
        }

        InetAddress getBroadcastAddress() throws IOException {
            WifiManager wifi = (WifiManager) _binder.getAppContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            DhcpInfo dhcp = wifi.getDhcpInfo(); // handle null somehow

            int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
            byte[] quads = new byte[4];
            for (int k = 0; k < 4; k++)
                quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
            InetAddress address = InetAddress.getByAddress(quads);
            //Log.d(LOG_TAG, "NetworkDiscoveryService: getBroadcastAddress: address: " + address.getHostAddress());
            return address;
        }


//        void foo2() throws Exception {
//            DatagramSocket socket = new DatagramSocket(PORT);
//            DatagramPacket packet = new DatagramPacket(buf, buf.length);
//            socket.receive(packet);
//        }
    }

}
