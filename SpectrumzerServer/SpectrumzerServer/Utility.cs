using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace SpectrumzerServer
{
    public class Utility
    {
        public static readonly string CONNECTED = "CONNECTED";

    }
    public class ClientHeartBeat
    {
        public int timeout;//in ms
    }

    public class TCP_Packet
    {
        public long _presentationTimeStamp;
        public int _type = -1;
        public string _json;
    }

    public enum TCP_PacketType
    {
        ClientHeartBeatReq = (0),
        ClientHeartBeatRes = 1,
        Media_Info = (2),
        Media_Control_Req = (3),
        Media_Control_Res = (4),
        None = -1
    }

}
