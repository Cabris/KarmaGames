package com.karma.spectrumzer.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPConnectionService extends Service {
    private static final String LOG_TAG = "Spectrumzer";
    TCPConnectionBinder _binder;
    private TCPConnection _tcpConnection;
    private int _port = 8090;

    public ITCPConnectionListener getListener() {
        return _listener;
    }

    public void setListener(ITCPConnectionListener _listener) {
        this._listener = _listener;
    }

    private ITCPConnectionListener _listener;

    @Override
    public void onCreate() {
        super.onCreate();
        _binder = new TCPConnectionBinder(this);
        Log.d(LOG_TAG, "TCPConnectionService: onCreate");
    }

    public void startListeningClient(int port) {
        Log.d(LOG_TAG, "TCPConnectionService: startListeningClient");
        if (_tcpConnection != null) {
            _tcpConnection.interrupt();
            _tcpConnection = null;
        }
        _port = port;
        _tcpConnection = new TCPConnection();
        _tcpConnection.start();
    }

    public void stopListeningClient() {
        Log.d(LOG_TAG, "TCPConnectionService: stopListeningClient");
        if (_tcpConnection != null) {
            _tcpConnection.interrupt();
            _tcpConnection = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(LOG_TAG, "TCPConnectionService: onBind");
        return _binder;
    }

    private class TCPConnection extends Thread {
        ServerSocket server;

        @Override
        public void run() {
            Log.d(LOG_TAG, "TCPConnectionService: TCPConnection: run");
            try {
                server = new ServerSocket(_port);
                System.out.println("伺服器已啟動 !");

                while (!isInterrupted()) {

                    Socket socket = server.accept();
                    System.out.println("取得連線 : InetAddress = "
                            + socket.getInetAddress());
                    // TimeOut時間
                    socket.setSoTimeout(15000);
                    readFromClient(socket);
                    socket.close();
                }
            } catch (java.io.IOException e) {
                Log.e(LOG_TAG, e.getMessage());
            }
        }

        void readFromClient(Socket socket) throws IOException {
            java.io.BufferedInputStream in = new java.io.BufferedInputStream(socket.getInputStream());
            int heaerLength = 14;

            //read header
            //TODO
            //try read header length(14) of bytes from stream
            //get total length of frameData
            //data payload length = total length - header length(14)


            //read data payload
            //TODO
            //try read data payload length of bytes from stream
            //FrameData.FromByteArray()
            //_listener.OnReceiveFrameFromClient()


            byte[] b = new byte[1024];
            String data = "";
            int length;
            while ((length = in.read(b)) > 0)// <=0的話就是結束了
            {
                data += new String(b, 0, length);
            }
            System.out.println("我取得的值:" + data);
            in.close();
            in = null;
        }


    }
}
