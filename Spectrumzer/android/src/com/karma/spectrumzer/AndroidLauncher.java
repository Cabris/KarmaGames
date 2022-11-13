package com.karma.spectrumzer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.karma.spectrumzer.services.INetworkDiscoveryListener;
import com.karma.spectrumzer.services.NetworkDiscoveryBinder;
import com.karma.spectrumzer.services.NetworkDiscoveryService;
import com.karma.spectrumzer.services.TCPConnectionBinder;
import com.karma.spectrumzer.services.TCPConnectionService;
import com.karma.spectrumzer.utility.Utility;

public class AndroidLauncher extends AndroidApplication {
    NetworkDiscoveryBinder _networkDiscoveryServiceBinder;
    TCPConnectionBinder _tcpConnectionBinder;
    boolean _isNetworkDiscoveryServiceBinded = false;
    boolean _isTcpServiceBinded = false;
    ServiceConnection _networkDiscoveryServiceConnection;
    ServiceConnection _tcpServiceConnection;
    private static final String LOG_TAG = "Spectrumzer";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        initialize(new SpectrumzerGame(), config);
        startNetworkDiscovery();
        startTcpService();
    }

    private void startTcpService() {
        _tcpServiceConnection = new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                _tcpConnectionBinder = (TCPConnectionBinder) service;
                _isTcpServiceBinded = true;
                _tcpConnectionBinder.startListeningClient(Utility.TCP_PORT);
                Log.d(LOG_TAG, "onServiceConnected: _isTcpServiceBinded: " + _isTcpServiceBinded);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                _tcpConnectionBinder = null;
                _isTcpServiceBinded = false;
                _tcpConnectionBinder.stopListeningClient();
                Log.d(LOG_TAG, "onServiceDisconnected: _isTcpServiceBinded: " + _isTcpServiceBinded);
            }
        };

        Intent intent = new Intent(this, TCPConnectionService.class);
        bindService(intent, _tcpServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void startNetworkDiscovery() {
        _networkDiscoveryServiceConnection = new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                _networkDiscoveryServiceBinder = (NetworkDiscoveryBinder) service;
                _isNetworkDiscoveryServiceBinded = true;
                _networkDiscoveryServiceBinder.setListener(_networkDiscoveryListener);
                _networkDiscoveryServiceBinder.setAppContext(AndroidLauncher.this);
                _networkDiscoveryServiceBinder.startNetworkDiscovery(Utility.DISCOVERY_PORT);
                Log.d(LOG_TAG, "onServiceConnected: _isNetworkDiscoveryServiceBinded: " + _isNetworkDiscoveryServiceBinded);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                _networkDiscoveryServiceBinder.stopNetworkDiscovery();
                _networkDiscoveryServiceBinder.setListener(null);
                _networkDiscoveryServiceBinder.setAppContext(null);
                _networkDiscoveryServiceBinder = null;
                _isNetworkDiscoveryServiceBinded = false;
                Log.d(LOG_TAG, "onServiceDisconnected: _isNetworkDiscoveryServiceBinded: " + _isNetworkDiscoveryServiceBinded);
            }
        };

        Intent intent = new Intent(this, NetworkDiscoveryService.class);
        bindService(intent, _networkDiscoveryServiceConnection, Context.BIND_AUTO_CREATE);

    }

    private INetworkDiscoveryListener _networkDiscoveryListener = new INetworkDiscoveryListener() {

        @Override
        public void onPeerConnected() {
            Log.d(LOG_TAG, "onPeerConnected");

        }

        @Override
        public void onPeerDisconnected() {
            Log.d(LOG_TAG, "onPeerDisconnected");

        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(_networkDiscoveryServiceConnection);
        unbindService(_tcpServiceConnection);
    }
}
