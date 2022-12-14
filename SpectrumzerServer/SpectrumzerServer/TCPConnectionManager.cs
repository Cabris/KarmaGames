using Newtonsoft.Json;
using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Diagnostics;
using System.Net;
using System.Net.Sockets;
using System.Threading;
using System.Timers;

namespace SpectrumzerServer
{
    public class TCPConnectionManager : Singleton<TCPConnectionManager>
    {
        Dictionary<string, TCPConnection> _TCPConnections;

        public TCPConnectionManager()
        {
            _TCPConnections = new Dictionary<string, TCPConnection>();
        }

        internal void HandleServer(string peerIp, int peerPort)
        {
            if (!_TCPConnections.ContainsKey(peerIp))
            {
                TCPConnection connection = new TCPConnection(IPAddress.Parse(peerIp), peerPort);
                Thread _TCPThread = new Thread(new ThreadStart(connection.Run));
                _TCPThread.IsBackground = true;
                _TCPThread.Start();
                _TCPConnections.Add(peerIp, connection);
                Debug.WriteLine("start TCP connection with peer ip: " + peerIp);
            }
            else
            {
                //Debug.WriteLine("TCP connection with peer ip: " + peerIp + " already existed, skip.");
            }
        }
    }

    public class TCPConnection
    {
        TcpClient _tcpClient;
        IPAddress _peerIp;
        int _peerPort;
        int _heartBeatInterval = 10;
        int _heartBeatTimeout = 15;
        public CancellationTokenSource Cancellation { get; private set; }
        //ConcurrentQueue<byte[]> _tcpSendQueue;
        public TCPConnection(IPAddress peerIp, int peerPort)
        {
            _peerIp = peerIp;
            _peerPort = peerPort;
        }

        public void Run()//run in thread
        {
            //_tcpSendQueue = new ConcurrentQueue<byte[]>();
            Cancellation = new CancellationTokenSource();
            //CancellationTokenSource sendTcpframes = null;
            //CancellationTokenSource receiveTcpframes = null;

            System.Timers.Timer heartBeatTimer = null;
            try
            {
                StartTCPConnection(_peerIp, _peerPort);//block
                heartBeatTimer = StartHeartBeat();
                //sendTcpframes = StartSendTcpFrames();
                //receiveTcpframes = StartReceiveTcpFrames();
                var token = Cancellation.Token;
                HandleReadFromServer(token);
            }
            catch (Exception e)
            {
                Debug.WriteLine(e.Message + ", " + e.StackTrace);
            }
            finally
            {
                //sendTcpframes?.Cancel();
                //receiveTcpframes?.Cancel();
                heartBeatTimer?.Stop();
                heartBeatTimer?.Dispose();
                Debug.WriteLine("TCPConnection: Run: exit.");
            }
        }

        private void HandleReadFromServer(CancellationToken token)
        {
            while (true)
            {
                token.ThrowIfCancellationRequested();

                string msg = ReadUTF8StringFromServer();
                Debug.WriteLine("TCPConnection: HandleReadFromServer: " + msg);

                var packet = JsonConvert.DeserializeObject<TCP_Packet>(msg);
                if (packet != null)
                {
                    TCP_PacketType packetType = (TCP_PacketType)packet._type;
                    if (packetType == TCP_PacketType.ClientHeartBeatRes)
                    {
                        Debug.WriteLine("TCPConnection: HandleReadFromServer: packetType: " + packetType);
                    }
                }
            }
        }

        private void StartTCPConnection(IPAddress peerAddress, int peerPort)
        {
            Debug.WriteLine("TCPConnection: StartTCPConnection: peerAddress: " + peerAddress.ToString() + ", peerPort: " + peerPort);
            IPEndPoint ipe = new IPEndPoint(peerAddress, peerPort);
            //try start connection to peer...
            TryCreateTcpConnection(ipe);//block

            string msg = ReadUTF8StringFromServer();
            if (msg != Utility.CONNECTED)
            {
                throw new Exception("TCPConnection: StartTCPConnection: Server not response normal when connected.");
            }
            Debug.WriteLine("TCPConnection: StartTCPConnection: Start TCP Connection: Connection completed!");

        }

        string ReadUTF8StringFromServer()
        {
            NetworkStream ns = _tcpClient.GetStream();
            Loon.Java.DataInputStream dataInputStream = new Loon.Java.DataInputStream(ns);
            string msg = dataInputStream.ReadUTF();
            Debug.WriteLine(string.Format("TCPConnection:ReadUTF8StringFromServer: Server said: {0}", msg));
            return msg;
        }

        void WriteUTF8StringToServer(string msg)
        {
            NetworkStream ns = _tcpClient.GetStream();
            Loon.Java.DataOutputStream dataOutputStream = new Loon.Java.DataOutputStream(ns);
            dataOutputStream.WriteUTF(msg);
            Debug.WriteLine(string.Format("TCPConnection: WriteUTF8StringToServer: Client said: {0}", msg));
        }

        private void TryCreateTcpConnection(IPEndPoint ipe)
        {
            try
            {
                _tcpClient = new TcpClient();
                bool retryCreateConnection = true;
                while (retryCreateConnection)
                {
                    Debug.WriteLine(string.Format("TCPConnection: TryCreateTcpConnection: Try to connect to peer {0}", ipe.Address.ToString()));
                    _tcpClient.Connect(ipe);//block util connected or error
                    if (_tcpClient.Connected) //connected!
                    {
                        break;
                    }
                    else
                    {
                        Debug.WriteLine(string.Format("TCPConnection: TryCreateTcpConnection: TCP Connection to peer {0} failed, retry.", ipe.Address.ToString()));
                        continue;
                    }
                }
            }
            catch (Exception e)
            {
                Debug.WriteLine(e.Message);
                if (_tcpClient != null)
                    _tcpClient.Close();
                throw e;
            }
            finally
            {

            }
        }

        private System.Timers.Timer StartHeartBeat()
        {
            System.Timers.Timer heartBeatTimer = new System.Timers.Timer(_heartBeatInterval * 1000);
            heartBeatTimer.Elapsed += OnHeartBeatEvent;
            heartBeatTimer.AutoReset = true;
            heartBeatTimer.Enabled = true;
            return heartBeatTimer;
        }

        void OnHeartBeatEvent(object sender, ElapsedEventArgs e)
        {
            ClientHeartBeat heartBeat = new ClientHeartBeat()
            {
                timeout = _heartBeatTimeout
            };
            string hbJson = JsonConvert.SerializeObject(heartBeat);
            TCP_Packet packet = new TCP_Packet()
            {
                _type = (int)TCP_PacketType.ClientHeartBeatReq,
                _presentationTimeStamp = DateTimeOffset.Now.ToUnixTimeMilliseconds(),
                _json = hbJson
            };

            string packetJson = JsonConvert.SerializeObject(packet);
            WriteUTF8StringToServer(packetJson);
        }

        /*
        private CancellationTokenSource StartReceiveTcpFrames()
        {
            CancellationTokenSource tokenSource = new CancellationTokenSource();
            CancellationToken token = tokenSource.Token;
            void ReceiveTcpFrames()
            {
                NetworkStream ns = _tcpClient.GetStream();
                while (true)//keep reading from server
                {
                    //read 4 byte of total frame size(int)
                    int lenToRead = 4;
                    byte[] sizeBytes = new byte[lenToRead];
                    ReadBufferFromStream(ns, sizeBytes, 0, lenToRead);

                    int frameSize = BitConverter.ToInt32(sizeBytes, 0);
                    Debug.WriteLine("Start TCP Connection: StartReceiveTcpFrames: frameSize: " + frameSize);

                    byte[] frameBytes = new byte[frameSize - 4];
                    ReadBufferFromStream(ns, frameBytes, 0, frameBytes.Length);
                    Debug.WriteLine("Start TCP Connection: StartReceiveTcpFrames: frameBytes.Length: " + frameBytes.Length);

                    FrameData frameData = FrameData.FromByteArray(frameBytes);
                    Debug.WriteLine("Start TCP Connection: StartReceiveTcpFrames: _presentationTimeStamp: " + frameData._presentationTimeStamp);

                    if (token.IsCancellationRequested)
                    {
                        Debug.WriteLine("Start TCP Connection: StartReceiveTcpFrames: CancellationRequested");
                        break;
                    }
                }
            }

            Thread t = new Thread(new ThreadStart(ReceiveTcpFrames));
            t.IsBackground = true;
            t.Start();
            return tokenSource;
        }

        private void ReadBufferFromStream(NetworkStream src, byte[] dst, int offset, int len)
        {
            int totalReaded = 0;
            do
            {
                int readed = src.Read(dst, offset, len - totalReaded);
                totalReaded += readed;
            } while (totalReaded < len);
        }

        private CancellationTokenSource StartSendTcpFrames()
        {
            CancellationTokenSource tokenSource = new CancellationTokenSource();
            CancellationToken token = tokenSource.Token;

            void SendTcpFrames()
            {
                NetworkStream ns = _tcpClient.GetStream();
                while (true)
                {
                    if (_tcpSendQueue.TryDequeue(out byte[] frameBytes))
                    {
                        ns.Write(frameBytes, 0, frameBytes.Length);
                    }
                    if (token.IsCancellationRequested)
                    {
                        Debug.WriteLine("Start TCP Connection: StartSendTcpFrames: CancellationRequested");
                        break;
                    }
                }
            }
            Thread t = new Thread(new ThreadStart(SendTcpFrames));
            t.IsBackground = true;
            t.Start();
            return tokenSource;
        }
        */

    }

}
