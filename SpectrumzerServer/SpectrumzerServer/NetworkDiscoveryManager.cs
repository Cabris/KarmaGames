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
            Console.WriteLine("start receiving UDP");
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

                //Console.WriteLine(msg);
                var json = JsonConvert.DeserializeObject<NetworkDiscoveryRequest>(msg);
                if (json != null && IsVaidRequest(json))
                {
                    IPAddress peerIp = endpoint.Address;
                    int peerPort = endpoint.Port;

                    string ip = peerIp.ToString();
                    Console.WriteLine("Network discovery request detected!, peer ip: " + ip +
                    ", peerPort: " + peerPort);
                    TCPConnectionManager.Instance.HandleServer(endpoint);
                }
            }
        }

        static private bool IsVaidRequest(NetworkDiscoveryRequest request)
        {
            return request.req == "ndreq" && request.key == "dSgVkYp3";
        }


        private class NetworkDiscoveryRequest
        {
            public string req;
            public string key;
        }
    }
}
