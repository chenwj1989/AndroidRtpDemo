#include <jni.h>
#include <string>
#include "jrtplib/rtpsession.h"
#include "jrtplib/rtpudpv4transmitter.h"
#include "jrtplib/rtpipv4address.h"
#include "jrtplib/rtpsessionparams.h"
#include "jrtplib/rtperrors.h"
#include "jrtplib/rtplibraryversion.h"
#include "jrtplib/rtppacket.h"
#include "PacketQueue.h"
#include "log.h"
#include <stdlib.h>

using namespace jrtplib;

PacketQueue* audioQueue = NULL;
int rcvStop = 0;
pthread_t rcvAudioId;
//RTPSession videoSession;
RTPSession audioSession;
#define DEFAULT_AUDIO_PT 96
#define MEDIA_DURATION 10
//#define LOCAL_VIDEO_PORT 5000
#define LOCAL_AUDIO_PORT 5002

// This function checks if there was a RTP error. If so, it displays an error message and exists.
#define CHECK_ERROR_JRTPLIB(status) \
    if (status < 0) { \
        LOGE("ERROR: %s", jrtplib::RTPGetErrorString(status).c_str()); \
        exit(-1);\
    }

int receiveAudioPacket();
static void* run_rcv_audio(void *d) {
    while (!rcvStop) {
        receiveAudioPacket();
    }
    LOGD("rcv_audio thread exits");
    pthread_exit(0);
}

int createMediaSession(const uint8_t *ip, int port) {
    int status;
    audioQueue = new PacketQueue(10);
    if(audioQueue==NULL)
        return -1;

    uint8_t destIp[] = {ip[0], ip[1], ip[2], ip[3]};
    RTPIPv4Address destAddr(destIp, port);

    RTPSessionParams sessionparams2;
    sessionparams2.SetOwnTimestampUnit(1.0 / 9000.0);
    sessionparams2.SetAcceptOwnPackets(true);

    RTPUDPv4TransmissionParams transparams2;
    transparams2.SetPortbase(LOCAL_AUDIO_PORT);
    status = audioSession.Create(sessionparams2, &transparams2);
    CHECK_ERROR_JRTPLIB(status);

    status = audioSession.AddDestination(destAddr);
    CHECK_ERROR_JRTPLIB(status);

    audioSession.SetDefaultPayloadType(DEFAULT_AUDIO_PT);
    audioSession.SetDefaultMark(false);
    audioSession.SetDefaultTimestampIncrement(0);

    pthread_create(&rcvAudioId, NULL, run_rcv_audio, NULL);
    return 0;
}

int destroyMediaSession() {
    RTPTime delay = RTPTime(2.0);
    audioSession.BYEDestroy(delay, "stop rtp audioSession", strlen("stop rtp audioSession"));
    return 0;
}

int sendAudioPacket(const void *data, size_t len) {
    audioSession.SendPacket(data, len, DEFAULT_AUDIO_PT, true, MEDIA_DURATION);

    return 0;
}


int receiveAudioPacket() {
    RTPTime delay(0.010);
    audioSession.BeginDataAccess();
    if (audioSession.GotoFirstSource()) {
        do {
            RTPPacket *packet;
            while ((packet = audioSession.GetNextPacket()) != 0) {
                Packet queuePkt(packet->GetPayloadLength(), packet->GetPayloadData());
                audioQueue->put(queuePkt);
                audioSession.DeletePacket(packet);
            }
        } while (audioSession.GotoNextSource());
    }
    audioSession.EndDataAccess();
    RTPTime::Wait(delay);
    return 0;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_example_rtpdemo_RtpSession_sendAudioPacket(JNIEnv *env, jobject thiz, jbyteArray data_,
                                                    jint data_len) {
    jbyte *data = env->GetByteArrayElements(data_, NULL);
    //for(int i=0; i<7; i++)
    //    LOGD("ADTS header[%d/%d] : %x", i, data_len, data[i]);
    int ret =  sendAudioPacket(data, data_len);
    env->ReleaseByteArrayElements(data_, data, 0);
    return ret;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_example_rtpdemo_RtpSession_createRtpSession(JNIEnv *env, jobject thiz,jbyteArray ip_, jint port){
    jbyte *ip = env->GetByteArrayElements(ip_, NULL);
    createMediaSession((const uint8_t *) ip, port);
    env->ReleaseByteArrayElements(ip_, ip, 0);
    return 0;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_rtpdemo_RtpSession_destroyRtpSession(JNIEnv *env, jobject thiz) {
    destroyMediaSession();
}

extern "C"
JNIEXPORT  jbyteArray JNICALL
Java_com_example_rtpdemo_RtpSession_getNextAudioPacket(JNIEnv *env, jobject thiz) {

    Packet nextPkt;
    audioQueue->get(nextPkt);
    int len_arr = nextPkt.size();
    jbyteArray c_result = env->NewByteArray(len_arr);
    env->SetByteArrayRegion(c_result, 0, len_arr, (const jbyte*)nextPkt.data());
    return c_result;
}