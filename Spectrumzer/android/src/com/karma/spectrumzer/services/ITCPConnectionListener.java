package com.karma.spectrumzer.services;

import com.karma.spectrumzer.models.FrameData;

public interface ITCPConnectionListener {
    void OnReceiveFrameFromClient(FrameData frameData);
}
