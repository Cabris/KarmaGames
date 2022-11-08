package com.karma.spectrumzer.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.karma.spectrumzer.models.ByteUtils;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;

public class TCPConnectionService extends Service {
    private static final String LOG_TAG = "Spectrumzer";
    TCPConnectionBinder _binder;
    private ServerThread _tcpConnection;
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
        _tcpConnection = new ServerThread(_port);
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

    private class ServerThread extends Thread {
        ServerSocket _server;
        ArrayList<ClientThread> _clients;
        int _maxClient = 1;
        int _port;

        public ServerThread(int port) {
            _port = port;
            _clients = new ArrayList<ClientThread>();
        }

        @Override
        public void run() {
            Log.d(LOG_TAG, "TCPConnectionService: TCPConnection: run");

            try {
                _server = new ServerSocket(_port);
                //server accept loop, looking for client forever but idle while reaches max client.
                while (true) {
                    if (isInterrupted())
                        throw new InterruptedException();

                    for (ClientThread ct : _clients) {
                        //connection is dead.
                        if (!ct.isAlive()) {
                            _clients.remove(ct);
                        }
                    }

                    //idle while reaches max client.
                    if (_clients.size() == _maxClient) {
                        continue;
                    }

                    //looking for client
                    Socket socket = _server.accept();
                    Log.d(LOG_TAG, "TCPConnectionService: TCPConnection:connected: address: "
                            + socket.getInetAddress() + ", port: " + socket.getPort());
                    startClientConnectionThread(socket);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                closeServer();
            }
        }

        private void startClientConnectionThread(Socket socket) {
            Log.d(LOG_TAG, "TCPConnectionService: TCPConnection: startClientConnectionThread");
            ClientThread clientThread = new ClientThread(socket);
            clientThread.start();
            _clients.add(clientThread);
        }

        private void closeServer() {
            if (_server != null) {
                try {
                    _server.close();
                    _server = null;
                    Log.d(LOG_TAG, "TCPConnectionService: TCPConnection: closeServer");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private class ClientThread extends Thread {
        private Socket _client;

        public ClientThread(Socket _client) {
            this._client = _client;
        }

        @Override
        public void run() {
            try {
                _client.setSoTimeout(15000);
                java.io.BufferedInputStream in = new java.io.BufferedInputStream(_client.getInputStream());
                while (true) {
                    if (isInterrupted())
                        throw new InterruptedException();
                    readFromClient(in);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    _client.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        void readFromClient(java.io.BufferedInputStream in) throws Exception {


            //read header
            //try read header length(14) of bytes from stream
            final int headerLength = 14;
            byte[] headerBytes = new byte[headerLength];
            int totalReaded = 0;
            do {
                int readed = in.read(headerBytes, totalReaded, headerLength - totalReaded);
                totalReaded += readed;
            } while (totalReaded < headerLength);

            //get total length of frameData
            //TODO
            //headers       size
            //int size      4
            //byte type     1
            //byte flag     1
            //long pts      8
            //byte[] _data  n
            //data payload length = total length - header length(14)
            //TODO
            int totalSize = ByteUtils.bytesToInt(headerBytes);
            int dataPayloadSize = totalSize - headerLength;

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
            Log.d(LOG_TAG, "我取得的值:" + data);
            in.close();
            in = null;
        }
    }
}
