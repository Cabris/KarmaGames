using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace SpectrumzerServer
{
    public class FrameData
    {
        public static readonly byte TYPE_JSON = 0;
        public static readonly byte TYPE_DATA = 1;

        public static readonly byte FLAG_HB = 0;
        public static readonly byte FLAG_INFO = 1;
        public static readonly byte FLAG_SIGNAL = 2;

        //0: json
        //1: data
        public byte _type;

        //json:
        //0: heartBeat
        //1: media info
        //2: control signal

        //data
        //3: audio stream
        public byte _flag;

        public long _presentationTimeStamp;

        public byte[] _data;

        public static byte[] JsonStringToBytes(string json)
        {
            byte[] msgByte = Encoding.UTF8.GetBytes(json);
            return msgByte;
        }

        public byte[] ToByteArray()
        {
            int totalFrameSizeInByte = 4 + 1 + 1 + 8 + _data.Length;
            byte[] frameData = new byte[totalFrameSizeInByte];
            using (MemoryStream ms = new MemoryStream(frameData))
            {
                byte[] frameSizeInBytes = BitConverter.GetBytes(totalFrameSizeInByte);
                byte[] tpsInBytes = BitConverter.GetBytes(_presentationTimeStamp);

                if (BitConverter.IsLittleEndian)
                {
                    Array.Reverse(frameSizeInBytes);
                    Array.Reverse(tpsInBytes);
                }

                ms.Write(frameSizeInBytes, 0, frameSizeInBytes.Length);//total length, size=4
                ms.WriteByte(_type);//type, size=1
                ms.WriteByte(_flag);//flag, size=1
                ms.Write(tpsInBytes, 0, tpsInBytes.Length);//presentation timestamp, size=8
                ms.Write(_data, 0, _data.Length);//data payload, size=_data.Length
            }
            return frameData;
        }

        public static FrameData FromByteArray(byte[] frameData)
        {
            using (MemoryStream ms = new MemoryStream(frameData))
            {
                try
                {
                    byte[] frameSizeInBytes = new byte[4];
                    ms.Read(frameSizeInBytes, 0, frameSizeInBytes.Length);//total length, size=4
                    int totalFrameSizeInByte = BitConverter.ToInt32(frameSizeInBytes, 0);
                    byte type = (byte)ms.ReadByte();//type, size=1
                    byte flag = (byte)ms.ReadByte();//flag, size=1
                    byte[] tpsInBytes = new byte[8];
                    ms.Write(tpsInBytes, 0, tpsInBytes.Length);//presentation timestamp, size=8
                    long presentationTimeStamp = BitConverter.ToInt64(tpsInBytes, 0);
                    int dataSize = totalFrameSizeInByte - 4 - 1 - 1 - 8;
                    if (dataSize < 0)
                    {
                        throw new Exception(string.Format("frame type: {0}, flag: {1}, tps: {2}" +
                            " has invaid data payload size: {3}"
                            , type, flag, presentationTimeStamp, dataSize));
                    }
                    byte[] data = new byte[dataSize];
                    ms.Read(data, 0, data.Length);//data payload, size=_data.Length

                    FrameData frame = new FrameData()
                    {
                        _type = type,
                        _flag = flag,
                        _presentationTimeStamp = presentationTimeStamp,
                        _data = data
                    };
                    return frame;
                }
                catch (Exception e)
                {
                    Console.WriteLine(e.Message);
                    return null;
                }
            }

        }
    }
}
