package com.karma.spectrumzer.services;

import android.os.Binder;

public class TCPConnectionBinder extends Binder {
    TCPConnectionService _service;

    public TCPConnectionBinder(TCPConnectionService _service) {
        this._service = _service;
    }

    public void startListeningClient(int port) {
        _service.startListeningClient(port);
    }

    public void stopListeningClient() {
        _service.stopListeningClient();
    }
}
