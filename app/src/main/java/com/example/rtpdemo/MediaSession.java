package com.example.rtpdemo;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;

import android.media.MediaFormat;
import android.media.MediaRecorder;

import android.util.Log;
import java.io.IOException;
import java.nio.ByteBuffer;

import static android.media.MediaFormat.MIMETYPE_AUDIO_AAC;

public class MediaSession {

    private static final String TAG = MediaSession.class.getSimpleName();

    // Input Source: Microphone
    private final static int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
    // Sample Rate: 44.1kHz
    private final static int SAMPLE_RATE = 44100;
    // Channel: mono
    private final static int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int CHANNEL_COUNT = 1;
    // PCM 16BIT Format
    private final static int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    // Bit Rate
    private static final int BIT_RATE = 96000;

    private int mBufferSizeInBytes;
    private AudioRecord mAudioRecord;
    private AudioTrack mAudioTrack;
    private MediaCodec mAudioEncoder;
    private MediaCodec mAudioDecoder;

    private RtpSession mRtpSession;
    private Boolean mStopped = true;


    public void createRecorder() {
        mBufferSizeInBytes = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        if (mBufferSizeInBytes <= 0) {
            throw new RuntimeException("AudioRecord is not available, minBufferSize: " + mBufferSizeInBytes);
        }
        Log.i(TAG, "createAudioRecord minBufferSize: " + mBufferSizeInBytes);

        mAudioRecord = new AudioRecord(AUDIO_SOURCE, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, mBufferSizeInBytes);
        int state = mAudioRecord.getState();
        Log.i(TAG, "createAudio state: " + state + ", initialized: " + (state == AudioRecord.STATE_INITIALIZED));
    }

    public void createPlayer() {
        int streamType = AudioManager.STREAM_MUSIC;
        int mode = AudioTrack.MODE_STREAM;
        int minBufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AUDIO_FORMAT);
        if (mBufferSizeInBytes <= 0) {
            throw new RuntimeException("AudioTrack is not available, minBufferSize: " + mBufferSizeInBytes);
        }
        Log.i(TAG, "AudioTrack minBufferSize: " + mBufferSizeInBytes);
        mAudioTrack = new AudioTrack(streamType, SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AUDIO_FORMAT, Math.max(minBufferSize, 2048), mode);
        int state = mAudioTrack.getState();
        Log.i(TAG, "createPlayer state: " + state + ", initialized: " + (state == AudioTrack.STATE_INITIALIZED));
    }

    public void createEncoder()  throws IOException {

        MediaFormat format = MediaFormat.createAudioFormat(MIMETYPE_AUDIO_AAC, SAMPLE_RATE, CHANNEL_COUNT);
        format.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 10*1024);

        mAudioEncoder = MediaCodec.createEncoderByType(MIMETYPE_AUDIO_AAC);
        mAudioEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mAudioEncoder.start();
    }

    public void createDecoder()  throws IOException {
        MediaFormat format = new MediaFormat();
        format.setString(MediaFormat.KEY_MIME, MIMETYPE_AUDIO_AAC);
        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, CHANNEL_COUNT);
        format.setInteger(MediaFormat.KEY_SAMPLE_RATE, SAMPLE_RATE);
        format.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
        format.setInteger(MediaFormat.KEY_IS_ADTS, 1);
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        byte[] bytes = new byte[]{(byte) 0x12, (byte)0x08};
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        format.setByteBuffer("csd-0", bb);
        mAudioDecoder = MediaCodec.createDecoderByType(MIMETYPE_AUDIO_AAC);
        mAudioDecoder.configure(format, null, null, 0);
        mAudioDecoder.start();
    }

    public int start(String dstIP, int dstPort){
        Log.i(TAG, "Media session starts!");
        try {
            createEncoder();
            createDecoder();
            createRecorder();
            createPlayer();
        }catch (Exception e) {
            Log.e(TAG, "create Failed ", e);
            return -1;
        }
        mRtpSession = new RtpSession();
        String[] tmp = dstIP.split("\\.");
        byte ip0 = (byte) Integer.valueOf(tmp[0]).intValue();
        byte ip1 = (byte) Integer.valueOf(tmp[1]).intValue();
        byte ip2 = (byte) Integer.valueOf(tmp[2]).intValue();
        byte ip3 = (byte) Integer.valueOf(tmp[3]).intValue();
        mRtpSession.createRtpSession(new byte[] {ip0, ip1, ip2, ip3}, dstPort);
        mStopped = false;
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        new Thread(new RecordTask()).start();
        new Thread(new EncodeTask()).start();
        new Thread(new DecodeTask()).start();
        new Thread(new PlayTask()).start();
        return 0;
    }

    public int stop(){
        Log.i(TAG, "Media session stops!");
        mStopped = true;
        mRtpSession.destroyRtpSession();
        return 0;
    }

    private class RecordTask implements Runnable {
        @Override
        public void run() {
            mAudioRecord.startRecording();
            try {
                byte[] buffer = new byte[mBufferSizeInBytes];
                while (!mStopped) {
                    //Log.d(TAG, "Reading buffer");
                    int readSize = mAudioRecord.read(buffer, 0, mBufferSizeInBytes);
                    if (readSize > 0) {
                        int inputBufferIndex = mAudioEncoder.dequeueInputBuffer(-1);
                        if (inputBufferIndex >= 0) {
                            ByteBuffer inputBuffer = mAudioEncoder.getInputBuffer(inputBufferIndex);
                            inputBuffer.clear();
                            inputBuffer.put(buffer);
                            inputBuffer.limit(buffer.length);
                            mAudioEncoder.queueInputBuffer(inputBufferIndex, 0, readSize, 0, 0);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Recording Failed, exception: ", e);
            } finally {
                mAudioRecord.stop();
                mAudioRecord.release();
            }
        }
    }

    private class PlayTask implements Runnable {

        @Override
        public void run() {
             mAudioTrack.play();
            try {
                while (!mStopped) {
                    MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                    int outputBufferIndex = mAudioDecoder.dequeueOutputBuffer(bufferInfo, 0);
                    while (outputBufferIndex >= 0) {
                        ByteBuffer outputBuffer = mAudioDecoder.getOutputBuffer(outputBufferIndex);
                        outputBuffer.position(bufferInfo.offset);
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
                        byte[] chunkAudio = new byte[bufferInfo.size];
                        outputBuffer.get(chunkAudio, 0, bufferInfo.size);
                        outputBuffer.position(bufferInfo.offset);
                        mAudioTrack.write(chunkAudio, 0, bufferInfo.size);
                        mAudioDecoder.releaseOutputBuffer(outputBufferIndex, false);
                        outputBufferIndex = mAudioDecoder.dequeueOutputBuffer(bufferInfo, 0);
                    }

                }
            }  catch (Exception e) {
                Log.e(TAG, "Player Failed, exception: ", e);
            } finally {
                Log.i(TAG, "Player stops");
                mAudioTrack.stop();
                mAudioTrack.release();
            }
        }
    }

    private class EncodeTask implements Runnable {

        @Override
        public void run() {
            try {
                while (!mStopped) {
                    MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                    int outputBufferIndex = mAudioEncoder.dequeueOutputBuffer(bufferInfo, 0);
                    while (outputBufferIndex >= 0) {
                             ByteBuffer outputBuffer = mAudioEncoder.getOutputBuffer(outputBufferIndex);
                        outputBuffer.position(bufferInfo.offset);
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
                        byte[] chunkAudio = new byte[bufferInfo.size + 7];// 7 is ADTS size
                        addADTStoPacket(chunkAudio, chunkAudio.length, 2, 4, CHANNEL_COUNT);
                        outputBuffer.get(chunkAudio, 7, bufferInfo.size);
                        outputBuffer.position(bufferInfo.offset);
                        Log.d(TAG, "Encoded audio length" + chunkAudio.length);
                        mRtpSession.sendAudioPacket(chunkAudio, chunkAudio.length);
                        mAudioEncoder.releaseOutputBuffer(outputBufferIndex, false);
                        outputBufferIndex = mAudioEncoder.dequeueOutputBuffer(bufferInfo, 0);
                    }

                }
            } catch (Exception e) {
                Log.e(TAG, "Encoder Failed, exception: ", e);
            }  finally {
                Log.i(TAG, "Encoder stops");
                mAudioEncoder.stop();
                mAudioEncoder.release();
            }
        }
    }

    private class DecodeTask implements Runnable {

        @Override
        public void run() {
            try {
                while (!mStopped) {
                    byte[] buffer = mRtpSession.getNextAudioPacket();

                    if (buffer.length >0 ) {  //valid ADTS header = 7bytes
                        int inputBufferIndex = mAudioDecoder.dequeueInputBuffer(-1);
                        if (inputBufferIndex >= 0) {
                            ByteBuffer inputBuffer = mAudioDecoder.getInputBuffer(inputBufferIndex);
                            inputBuffer.clear();
                            Log.d(TAG, "Decode audio length " + buffer.length);
                            inputBuffer.put(buffer, 0, buffer.length);
                            mAudioDecoder.queueInputBuffer(inputBufferIndex, 0, buffer.length, 0, 0);
                       }
                    }

                }
            } catch (Exception e) {
                Log.e(TAG, "Decoder Failed, exception: ", e);
            }  finally {
                Log.i(TAG, "Decoder stops");
                mAudioDecoder.stop();
                mAudioDecoder.release();
            }
        }
    }

    //add ADTS header before delivering
    static public void addADTStoPacket(byte[] packet, int packetLen, int profile, int freqIdx, int chanCfg) {
        //profile = 2;  //AAC LC
        //freqIdx = 4;  //44.1KHz
        //chanCfg = 2;  //CPE
        // fill in ADTS data
        packet[0] = (byte) 0xFF;
        packet[1] = (byte) 0xF9;
        packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
        packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
        packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
        packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
        packet[6] = (byte) 0xFC;
    }
}
