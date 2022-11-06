using Newtonsoft.Json;
using System;
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
    class NetworkDiscoveryManager
    {
        CancellationTokenSource _cancellation;
        Dictionary<string, TCPConnection> _TCPConnections = new Dictionary<string, TCPConnection>();
        public NetworkDiscoveryManager()
        {
            _cancellation = new CancellationTokenSource();
        }

        public void Interupt()
        {
            _cancellation.Cancel();
        }

        public void StartUDPMonitor()
        {
            Thread receThread = new Thread(new ThreadStart(this.RecvThread));
            receThread.IsBackground = true;
            receThread.Start();
            Console.WriteLine("start receiving UDP");
        }

        private void RecvThread()
        {
            UdpClient UDPrece = new UdpClient(new IPEndPoint(IPAddress.Any, 8080));
            IPEndPoint endpoint = new IPEndPoint(IPAddress.Any, 0);
            while (!_cancellation.Token.IsCancellationRequested)
            {
                byte[] buf = UDPrece.Receive(ref endpoint);
                string msg = Encoding.UTF8.GetString(buf);
                //Console.WriteLine(msg);
                var json = JsonConvert.DeserializeObject<NetworkDiscoveryRequest>(msg);
                if (json != null && IsVaidRequest(json))
                {
                    IPAddress peerIp = endpoint.Address;
                    int peerPort = endpoint.Port;
                    string ip = peerIp.ToString();
                    if (!_TCPConnections.ContainsKey(ip))
                    {
                        Console.WriteLine("Network discovery request detected!, peer ip: " + ip +
                        ", peerPort: " + peerPort);

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
        }
        static private bool IsVaidRequest(NetworkDiscoveryRequest request)
        {
            return request.req == "ndreq" && request.key == "dSgVkYp3";
        }

        private class TCPConnection
        {
            TcpClient _tcpClient;
            IPAddress _peerIp;
            int _peerPort;
            int _heartBeatInterval = 10;
            int _heartBeatTimeout = 15;
            System.Timers.Timer _heartBeatTimer;

            public TCPConnection(IPAddress peerIp, int peerPort)
            {
                _peerIp = peerIp;
                _peerPort = peerPort;
            }

            public void Run()//run in thread
            {
                try
                {
                    StartTCPConnection(_peerIp, _peerPort);//block
                    InTCPConnection();//block
                }
                catch (Exception e)
                {
                    Console.WriteLine(e.Message);
                    throw e;
                }

            }

            private void StartTCPConnection(IPAddress peerAddress, int peerPort)
            {
                Console.WriteLine("Start TCP Connection: peerAddress: " + peerAddress.ToString() + ", peerPort: " + peerPort);
                IPEndPoint ipe = new IPEndPoint(peerAddress, peerPort);

                //try start connection to peer...
                TryCreateTcpConnection(peerAddress, ipe);//block
                IPEndPoint iPEndPoint = (IPEndPoint)_tcpClient.Client.LocalEndPoint;

                void OnHeartBeatEvent(object sender, ElapsedEventArgs e)
                {
                    ClientHeartBeat heartBeat = new ClientHeartBeat()
                    {
                        client_ip = iPEndPoint.Address.ToString(),
                        client_port = iPEndPoint.Port,
                        timeout = _heartBeatTimeout
                    };
                    string msg = JsonConvert.SerializeObject(heartBeat);

                    NetworkStream ns = _tcpClient.GetStream();
                    FrameData frame = new FrameData()
                    {
                        _type = FrameData.TYPE_JSON,
                        _flag = FrameData.FLAG_HB,
                        _presentationTimeStamp = DateTimeOffset.Now.ToUnixTimeSeconds(),
                        _data = FrameData.JsonStringToBytes(msg)
                    };
                    byte[] frameBytes = frame.ToByteArray();
                    ns.Write(frameBytes, 0, frameBytes.Length);
                }

                _heartBeatTimer = new System.Timers.Timer(_heartBeatInterval * 1000);
                _heartBeatTimer.Elapsed += OnHeartBeatEvent;
                _heartBeatTimer.AutoReset = true;
                _heartBeatTimer.Enabled = true;
            }

            private void TryCreateTcpConnection(IPAddress peerAddress, IPEndPoint ipe)
            {
                bool retryCreateConnection = true;
                while (retryCreateConnection)
                {
                    _tcpClient = new TcpClient();
                    try
                    {
                        _tcpClient.Connect(ipe);//block util connect or error
                        if (_tcpClient.Connected) //connected!
                        {
                            break;
                        }
                        else
                        {
                            Console.WriteLine(string.Format("TCP Connection to peer {0} failed, retry.", peerAddress.ToString()));
                            continue;
                        }
                    }
                    catch (Exception e)
                    {
                        Console.WriteLine(e.Message);
                        _tcpClient = null;
                        throw e;
                    }
                }
                _tcpClient = null;
                throw new Exception("CreateTcpConnection failed.");
            }

            private void InTCPConnection()
            {

            }



            private void StopHeartBeat()
            {
                _heartBeatTimer.Stop();
                _heartBeatTimer.Dispose();
            }

        }

        private class NetworkDiscoveryRequest
        {
            public string req;
            public string key;
        }

        private class ClientHeartBeat
        {
            public string client_ip;
            public int client_port;
            public int timeout;
        }
    }
}
