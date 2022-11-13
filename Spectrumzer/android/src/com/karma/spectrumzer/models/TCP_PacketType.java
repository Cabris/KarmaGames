package com.karma.spectrumzer.models;

public enum TCP_PacketType {
    ClientHeartBeatReq(0),
    ClientHeartBeatRes(1),
    Media_Info(2),
    Media_Control_Req(3),
    Media_Control_Res(4),
    None(-1);

    private int _value;

    TCP_PacketType(int value) {
        _value = value;
    }

    public int getValue() {
        return _value;
    }

    public boolean Compare(int i) {
        return _value == i;
    }

    public static TCP_PacketType GetValue(int _id) {
        TCP_PacketType[] As = TCP_PacketType.values();
        for (int i = 0; i < As.length; i++) {
            if (As[i].Compare(_id))
                return As[i];
        }
        return TCP_PacketType.None;
    }
}
