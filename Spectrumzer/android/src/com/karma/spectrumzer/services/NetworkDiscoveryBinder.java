package com.karma.spectrumzer.services;

import android.content.Context;
import android.os.Binder;
import android.util.Log;

public class NetworkDiscoveryBinder extends Binder {
    private static final String LOG_TAG = "Spectrumzer";
    private INetworkDiscoveryListener _listener;
    private NetworkDiscoveryService _service;
    private Context _appContext;

    public Context getAppContext() {
        return _appContext;
    }

    public void setAppContext(Context _appContext) {
        this._appContext = _appContext;
    }

    public INetworkDiscoveryListener getListener() {
        return _listener;
    }

    public void setListener(INetworkDiscoveryListener _listener) {
        this._listener = _listener;
    }

    public NetworkDiscoveryBinder(NetworkDiscoveryService service) {
        Log.d(LOG_TAG, "NetworkDiscoveryBinder(service): service: " + service);
        _service = service;
    }

    public void startNetworkDiscovery(int port) {
        _service.startNetworkDiscovery(port);
    }

    public void stopNetworkDiscovery() {
        _service.stopNetworkDiscovery();
    }
}
