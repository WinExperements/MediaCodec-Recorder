package com.mycompany.mediacodec;
import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;

public class RecordService extends Service
{
	private Worker worker; //for changing params
	public class MBinder extends Binder {
		public RecordService getService() {
			return RecordService.this;
		}
	}
	@Override
	public IBinder onBind(Intent p1)
	{
		return new MBinder();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		worker = new Worker();
		return START_STICKY;
	}
	public void setConfiguration(int w,int h,int fps,int mills) {
		if (worker == null) {worker = new Worker();}
		worker.width = w;
		worker.height = h;
		worker.videoFramePerSecond = fps;
		worker.microseconds = mills;
	}
	public void prepare() {
		worker.prepare();
	}
	public void run() {
		worker.setRunning(true);
		worker.run();
	}
	public void stop() {
		worker.setRunning(false); //its stops the worker
	}
	public Surface getSurface() {
		return worker.mSurface;
	}
}
