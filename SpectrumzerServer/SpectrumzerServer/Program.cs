using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Threading;
using System.Threading.Tasks;

namespace SpectrumzerServer
{
    class Program
    {
        static void Main(string[] args)
        {
            NetworkDiscoveryManager networkDiscoveryManager = new NetworkDiscoveryManager();
            networkDiscoveryManager.StartUDPMonitor();

            

            Console.ReadKey();
        }

    }
}
