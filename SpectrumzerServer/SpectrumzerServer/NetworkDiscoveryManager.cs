using Newtonsoft.Json;
using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using System.Timers;

namespace SpectrumzerServer
{
    class NetworkDiscoveryManager : Singleton<NetworkDiscoveryManager>
    {
        CancellationTokenSource _udpBroacast;
        int UDP_BROACAST_PORT = 8080;

        public NetworkDiscoveryManager()
        {
            _udpBroacast = new CancellationTokenSource();
        }

        public void Interupt()
        {
            _udpBroacast.Cancel();
        }

        public void StartUDPMonitor()
        {
            Thread receThread = new Thread(new ThreadStart(StartReceiveUDPDataFromBroacast));
            receThread.IsBackground = true;
            receThread.Start();
            Debug.WriteLine("start receiving UDP");
        }

        private void StartReceiveUDPDataFromBroacast()
        {
            UdpClient UDPrece = new UdpClient(new IPEndPoint(IPAddress.Any, UDP_BROACAST_PORT));
            IPEndPoint endpoint = new IPEndPoint(IPAddress.Any, 0);
            var token = _udpBroacast.Token;
            while (!token.IsCancellationRequested)
            {
                byte[] buf = UDPrece.Receive(ref endpoint);
                string msg = Encoding.UTF8.GetString(buf);

                Debug.WriteLine(msg);
                var serverInfo = JsonConvert.DeserializeObject<ServerInfo>(msg);
                if (serverInfo != null && IsVaidRequest(serverInfo))
                {
                    string ip = serverInfo.server_ip;
                    int tcp_port = serverInfo.server_tcp_port;
                    //Debug.WriteLine("Network discovery request detected!, peer ip: " + ip +
                    //", peerPort: " + peerPort);
                    TCPConnectionManager.Instance.HandleServer(ip, tcp_port);
                }
            }
        }

        static private bool IsVaidRequest(ServerInfo request)
        {
            return request.server_key == "dSgVkYp3";
        }


        private class ServerInfo
        {
            public String server_ip;
            public int server_tcp_port;
            public int server_udp_port;
            public String server_key;
        }
    }
}
