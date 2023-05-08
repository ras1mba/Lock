package com.astar.lock;

interface ConnectionEvents {

    void onConnected();

    void onDisconnected();

    void onWriteData(byte[] data);

    void onReadData(byte[] data);
}