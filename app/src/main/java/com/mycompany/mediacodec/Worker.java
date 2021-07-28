package com.mycompany.mediacodec;

import android.graphics.*;
import android.media.*;
import android.media.MediaCodec.*;
import android.view.*;
import java.io.*;
import java.nio.*;
import android.util.*;
import android.hardware.display.*;
import android.media.projection.*;

public class Worker extends Thread
{
	BufferInfo mBufferInfo; // хранит информацию о текущем буфере
	MediaCodec mEncoder; // кодер
	Surface mSurface; // Surface как вход данных для кодера
	volatile boolean mRunning,muxerStarted,mPrepared; //флаги
	MediaMuxer muxer; //Миксер для сохранения видео
	int trackID; //ID трека mixer
	int width = 720;
	int height = 1280;
	int videoBitrate = 0; // any changes ignored :)
	int videoFramePerSecond = 60;
	int microseconds;
	public Worker() {
        mBufferInfo = new BufferInfo();
	}
	public void setRunning(boolean running) {
		mRunning = running;
	}
	@Override
	public void run() {
		if (!mPrepared) {prepare();}
		try {
			while (mRunning) {
				encode();
			}
		} finally {
			release();
		}
	}
	void prepare() {
		Log.i("WorkerThreadMy","Prepare");
		Log.i("WorkerThreadMy",String.format("Configuring parameters: width=%d, heigth=%d, fps=%d, bitrate: %d, wait time for buffer: %d",width,height,videoFramePerSecond,videoBitrate,microseconds));
        int colorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface;
        MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat);
        format.setInteger(MediaFormat.KEY_BIT_RATE, -1);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, videoFramePerSecond);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 0);
		format.setInteger(MediaFormat.KEY_BITRATE_MODE,MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CQ);
        try
		{
			mEncoder = MediaCodec.createEncoderByType("video/avc");
		}
		catch (IOException e)
		{
			throw new RuntimeException("Failed to init media encoder: ",e);
		}
        mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mSurface = mEncoder.createInputSurface();
		if (mSurface == null) {
			throw new IllegalStateException("Encoder surface has null!");
		}
        mEncoder.start();
		try {
			muxer = new MediaMuxer("/sdcard/ScreenRecordCodec/" + System.currentTimeMillis() + ".mp4",MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
		} catch (IOException e) {
			throw new RuntimeException("Failed to initialize MediaMuxer: ",e);
		}
		mPrepared = true;
	}
	void encode() {
        if (!mRunning) {
            mEncoder.signalEndOfInputStream();
        }
        ByteBuffer[] outputBuffers = mEncoder.getOutputBuffers();
        for (;;) {
            int status = mEncoder.dequeueOutputBuffer(mBufferInfo, microseconds);
            if (status == MediaCodec.INFO_TRY_AGAIN_LATER) {
				Log.i("WorkerThreadMy","Buffer timeout");
                if (!mRunning) break;
            } else if (status == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                outputBuffers = mEncoder.getOutputBuffers();
			} else if (status == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
				if (muxerStarted) {
					throw new RuntimeException("Format changed twice");
				}
				MediaFormat outputFormat = mEncoder.getOutputFormat();
				trackID = muxer.addTrack(outputFormat);
				muxer.start();
				muxerStarted = true;
            } else if (status < 0) {} else {
				if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    mBufferInfo.size = 0;
                }
                ByteBuffer data = outputBuffers[status];
                data.position(mBufferInfo.offset);
                data.limit(mBufferInfo.offset + mBufferInfo.size);
				if (mBufferInfo.size != 0) {
					if (!muxerStarted) {throw new RuntimeException("Muxer not started");}
					muxer.writeSampleData(trackID,data,mBufferInfo);
				}
                mEncoder.releaseOutputBuffer(status, false);
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                    break;
                }
            }
        }
    }
	void release() {
        try {
			mEncoder.stop();
			mEncoder.release();
		} catch (IllegalStateException e) {}
		if (muxerStarted) {
			muxer.stop();
			muxer.release();
		}
        mSurface.release();
	}
}
