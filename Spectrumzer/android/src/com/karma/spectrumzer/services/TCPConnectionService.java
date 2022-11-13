package com.karma.spectrumzer.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.karma.spectrumzer.models.TCP_Packet;
import com.karma.spectrumzer.models.TCP_PacketType;
import com.karma.spectrumzer.utility.Utility;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
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
            _clients = new ArrayList<>();
        }

        @Override
        public void run() {
            Log.d(LOG_TAG, "TCPConnectionService: ServerThread: run");

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
                    Log.d(LOG_TAG, "TCPConnectionService: ServerThread:accept: address: "
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
            Log.d(LOG_TAG, "TCPConnectionService: ServerThread: startClientConnectionThread");
            ClientThread clientThread = new ClientThread(socket);
            clientThread.start();
            _clients.add(clientThread);
        }

        private void closeServer() {
            if (_server != null) {
                try {
                    _server.close();
                    _server = null;
                    Log.d(LOG_TAG, "TCPConnectionService: ServerThread: closeServer");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private class ClientThread extends Thread {
        private Socket _client;
        DataInputStream _inputStream = null;
        DataOutputStream _outputStream = null;

        public ClientThread(Socket _client) {
            this._client = _client;
        }

        @Override
        public void run() {

            try {
                _client.setSoTimeout(15000);
                _inputStream = new DataInputStream(_client.getInputStream());
                _outputStream = new DataOutputStream(_client.getOutputStream());
                writeUTF8StringToClient(Utility.CONNECTED);
                handleReadFromClient();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (_inputStream != null) {
                        _inputStream.close();
                    }
                    _client.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void handleReadFromClient() throws Exception {
            while (true) {
                if (isInterrupted())
                    throw new InterruptedException();
                String msg = readUTF8StringFromClient();

                try {
                    Gson gson = new Gson();
                    TCP_Packet packet = gson.fromJson(msg, TCP_Packet.class);
                    if (packet != null) {
                        Log.d(LOG_TAG, "TCPConnectionService: ClientThread: handleReadFromClient: msg readed, msg: " + msg);
                        TCP_PacketType type = TCP_PacketType.GetValue(packet._type);
                        Log.d(LOG_TAG, "TCPConnectionService: ClientThread: handleReadFromClient: msg readed, type: " + type);
                        if (type == TCP_PacketType.ClientHeartBeatReq) {
                            TCP_Packet packetRes = new TCP_Packet();
                            packetRes._type = TCP_PacketType.ClientHeartBeatRes.getValue();
                            packetRes._presentationTimeStamp = System.currentTimeMillis();
                            packetRes._json = "";
                            String packetResJson = gson.toJson(packetRes);
                            writeUTF8StringToClient(packetResJson);
                        }
                    }
                } catch (JsonSyntaxException e) {
                    Log.e(LOG_TAG, "TCPConnectionService: ClientThread: handleReadFromClient: msg is not a TCP_Packet json!! msg: " + msg);
                    e.printStackTrace();
                }


                if (_listener != null) {
                    _listener.OnReceiveUTF8StringFromClient(msg);
                }
            }
        }

        void writeUTF8StringToClient(String string) throws IOException {
            //String connected = Utility.CONNECTED + ": " + System.currentTimeMillis();
            _outputStream.writeUTF(string);
            Log.d(LOG_TAG, "TCPConnectionService: ClientThread: writeUTF8StringToClient: " + string);
        }

        String readUTF8StringFromClient() throws IOException {
            String msg = _inputStream.readUTF();
            Log.d(LOG_TAG, "TCPConnectionService: ClientThread: readUTF8StringFromClient: " + msg);
            return msg;
        }

        /*
        void readBytesFromInputStream(BufferedInputStream src, byte[] dst, int offset, int len) throws IOException {
            int totalReaded = 0;
            do {
                int readed = src.read(dst, totalReaded, len - totalReaded);
                totalReaded += readed;
            } while (totalReaded < len);
        }

        void readFrameFromClient(BufferedInputStream in) throws Exception {
            //read frame size
            //try read the first 4 bytes to get thr frame size.
            final int sizeBytesLen = 4;
            byte[] sizeBytes = new byte[sizeBytesLen];
            readBytesFromInputStream(in, sizeBytes, 0, sizeBytesLen);
            int totalSize = ByteUtils.bytesToInt(sizeBytes);
            Log.d(LOG_TAG, "TCPConnectionService: ClientThread: readFromClient: totalSize: " + totalSize);

            //get total length of frameData
            //members       size
            //int size      4

            //need to read the following:
            //byte type     1
            //byte flag     1
            //long pts      8
            //byte[] _data  n
            //read length = totalSize - int size in bytes(4)
            int frameSize = totalSize - 4;
            byte[] frameBytes = new byte[frameSize];
            Log.d(LOG_TAG, "TCPConnectionService: ClientThread: readFromClient: frameSize: " + frameSize);
            readBytesFromInputStream(in, frameBytes, 0, frameBytes.length);
            FrameData frameData = FrameData.FromByteArray(frameBytes);
            Log.d(LOG_TAG, "TCPConnectionService: ClientThread: readFromClient: frameData._presentationTimeStamp: "
                    + frameData._presentationTimeStamp);
            if (_listener != null)
                _listener.OnReceiveFrameFromClient(frameData);
        }
*/
    }
}
