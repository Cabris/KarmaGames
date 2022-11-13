package com.karma.spectrumzer.services;

import com.karma.spectrumzer.models.FrameData;

public interface ITCPConnectionListener {
    void OnReceiveFrameFromClient(FrameData frameData);
    void OnReceiveUTF8StringFromClient(String frameData);

}
