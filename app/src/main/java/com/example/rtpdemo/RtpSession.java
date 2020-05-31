package com.example.rtpdemo;

public class RtpSession {

    static {
        System.loadLibrary("jrtplib");
    }

    public native int createRtpSession(byte[] ip, int port);
    public native int sendAudioPacket( byte[] data, int dataLen);
    public native byte[]  getNextAudioPacket();
    public native void destroyRtpSession();
}
