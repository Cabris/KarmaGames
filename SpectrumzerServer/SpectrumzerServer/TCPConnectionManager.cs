using Newtonsoft.Json;
using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
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

        internal void HandleServer(IPEndPoint endpoint)
        {
            IPAddress peerIp = endpoint.Address;
            int peerPort = endpoint.Port;

            string ip = peerIp.ToString();
            if (!_TCPConnections.ContainsKey(ip))
            {
                TCPConnection connection = new TCPConnection(peerIp, peerPort);
                Thread _TCPThread = new Thread(new ThreadStart(connection.Run));
                _TCPThread.IsBackground = true;
                _TCPThread.Start();
                _TCPConnections.Add(ip, connection);
                Console.WriteLine("start TCP connection with peer ip: " + ip);
            }
            else
            {
                Console.WriteLine("TCP connection with peer ip: " + ip + " already existed, skip.");
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
        ConcurrentQueue<byte[]> _tcpSendQueue;
        public TCPConnection(IPAddress peerIp, int peerPort)
        {
            _peerIp = peerIp;
            _peerPort = peerPort;
        }

        public void Run()//run in thread
        {
            _tcpSendQueue = new ConcurrentQueue<byte[]>();
            Cancellation = new CancellationTokenSource();
            CancellationTokenSource sendTcpframes = null;
            CancellationTokenSource receiveTcpframes = null;

            System.Timers.Timer heartBeatTimer = null;
            try
            {
                StartTCPConnection(_peerIp, _peerPort);//block
                heartBeatTimer = StartHeartBeat();
                sendTcpframes = StartSendTcpFrames();
                receiveTcpframes = StartReceiveTcpFrames();
                var token = Cancellation.Token;
                while (true)
                {
                    token.ThrowIfCancellationRequested();
                }
            }
            catch (OperationCanceledException e)
            {
                Console.WriteLine("Start TCP Connection: CancellationRequested");
                Console.WriteLine(e.Message);
            }
            catch (Exception e)
            {
                Console.WriteLine(e.Message);
            }
            finally
            {
                sendTcpframes?.Cancel();
                receiveTcpframes?.Cancel();
                heartBeatTimer?.Stop();
                heartBeatTimer?.Dispose();
            }
        }

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
                    Console.WriteLine("Start TCP Connection: StartReceiveTcpFrames: frameSize: " + frameSize);

                    byte[] frameBytes = new byte[frameSize - 4];
                    ReadBufferFromStream(ns, frameBytes, 0, frameBytes.Length);
                    Console.WriteLine("Start TCP Connection: StartReceiveTcpFrames: frameBytes.Length: " + frameBytes.Length);

                    FrameData frameData = FrameData.FromByteArray(frameBytes);
                    Console.WriteLine("Start TCP Connection: StartReceiveTcpFrames: _presentationTimeStamp: " + frameData._presentationTimeStamp);

                    if (token.IsCancellationRequested)
                    {
                        Console.WriteLine("Start TCP Connection: StartReceiveTcpFrames: CancellationRequested");
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

        private void StartTCPConnection(IPAddress peerAddress, int peerPort)
        {
            Console.WriteLine("Start TCP Connection: peerAddress: " + peerAddress.ToString() + ", peerPort: " + peerPort);
            IPEndPoint ipe = new IPEndPoint(peerAddress, peerPort);
            //try start connection to peer...
            TryCreateTcpConnection(ipe);//block
        }

        private void TryCreateTcpConnection(IPEndPoint ipe)
        {
            try
            {
                _tcpClient = new TcpClient();
                bool retryCreateConnection = true;
                while (retryCreateConnection)
                {
                    Console.WriteLine(string.Format("Try to connect to peer {0}", ipe.Address.ToString()));
                    _tcpClient.Connect(ipe);//block util connected or error
                    if (_tcpClient.Connected) //connected!
                    {
                        break;
                    }
                    else
                    {
                        Console.WriteLine(string.Format("TCP Connection to peer {0} failed, retry.", ipe.Address.ToString()));
                        continue;
                    }
                }
            }
            catch (Exception e)
            {
                Console.WriteLine(e.Message);
                if (_tcpClient != null)
                    _tcpClient.Close();
                throw e;
            }
            finally
            {

            }
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
                        Console.WriteLine("Start TCP Connection: StartSendTcpFrames: CancellationRequested");
                        break;
                    }
                }
            }
            Thread t = new Thread(new ThreadStart(SendTcpFrames));
            t.IsBackground = true;
            t.Start();
            return tokenSource;
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
            IPEndPoint iPEndPoint = (IPEndPoint)_tcpClient.Client.LocalEndPoint;
            ClientHeartBeat heartBeat = new ClientHeartBeat()
            {
                client_ip = iPEndPoint.Address.ToString(),
                client_port = iPEndPoint.Port,
                timeout = _heartBeatTimeout
            };
            string msg = JsonConvert.SerializeObject(heartBeat);

            FrameData frame = new FrameData()
            {
                _type = FrameData.TYPE_JSON,
                _flag = FrameData.FLAG_HB,
                _presentationTimeStamp = DateTimeOffset.Now.ToUnixTimeSeconds(),
                _data = FrameData.JsonStringToBytes(msg)
            };
            byte[] frameBytes = frame.ToByteArray();
            _tcpSendQueue.Enqueue(frameBytes);
        }

        private class ClientHeartBeat
        {
            public string client_ip;
            public int client_port;
            public int timeout;
        }
    }

}
