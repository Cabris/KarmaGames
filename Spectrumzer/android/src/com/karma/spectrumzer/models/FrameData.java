package com.karma.spectrumzer.models;

import java.nio.charset.StandardCharsets;

public class FrameData {

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

    public static byte[] JsonStringToBytes(String json) {
        byte[] msgByte = json.getBytes(StandardCharsets.UTF_8);
        return msgByte;
    }

    public byte[] ToByteArray() {
        int totalFrameSizeInByte = 4 + 1 + 1 + 8 + _data.length;
        byte[] frameData = new byte[totalFrameSizeInByte];
        byte[] frameSizeInBytes = ByteUtils.intToBytes(totalFrameSizeInByte);

        System.arraycopy(frameSizeInBytes, 0, frameData, 0, frameSizeInBytes.length);//total length, size=4
        frameData[4] = _type;//type, size=1
        frameData[5] = _flag;//flag, size=1
        byte[] longToBytes = ByteUtils.longToBytes(_presentationTimeStamp);
        System.arraycopy(longToBytes, 0, frameData, 6, longToBytes.length);//presentation timestamp, size=8
        System.arraycopy(_data, 0, frameData, 14, _data.length);//data payload, size=_data.Length
        return frameData;
    }

    public static FrameData FromByteArray(byte[] frameData) {
        byte[] totalSizeInByte = new byte[4];
        System.arraycopy(frameData, 0, totalSizeInByte, 0, totalSizeInByte.length);
        int totalSize = ByteUtils.bytesToInt(totalSizeInByte);
        byte type = frameData[4];//type, size=1
        byte flag = frameData[5];//flag, size=1
        byte[] timeStamp = new byte[8];
        System.arraycopy(frameData, 6, timeStamp, 0, timeStamp.length);
        long presentationTimeStamp = ByteUtils.bytesToLong(timeStamp);
        int dataLength = frameData.length - 14;
        byte[] data = new byte[dataLength];
        System.arraycopy(frameData, 14, data, 0, data.length);
        FrameData frame = new FrameData();

        frame._type = type;
        frame._flag = flag;
        frame._presentationTimeStamp = presentationTimeStamp;
        frame._data = data;
        return frame;
    }
}
