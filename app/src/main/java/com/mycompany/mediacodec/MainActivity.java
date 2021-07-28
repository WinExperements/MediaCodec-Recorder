package com.mycompany.mediacodec;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import android.media.*;
import java.io.*;
import android.media.projection.*;
import android.hardware.display.*;
import android.util.*;

public class MainActivity extends Activity 
{
	private Button b;
	private MediaProjectionManager mgr;
	private MediaProjection pr;
	private VirtualDisplay d;
	private boolean m;
	private RecordService service;
	private EditText width,heigth,bitrate,fps,microsend;
	private ServiceConnection conn;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		File check = new File("/sdcard/ScreenRecordCodec");
		if (!check.exists() || !check.isDirectory()) {
			check.mkdir();
		}
		mgr = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
		b = (Button) findViewById(R.id.mainButton);
		width = (EditText) findViewById(R.id.widthEdit);
		heigth = (EditText) findViewById(R.id.heigthEdit);
		bitrate = (EditText) findViewById(R.id.bitrateEdit);
		fps = (EditText) findViewById(R.id.fpsEdit);
		microsend = (EditText) findViewById(R.id.microsecondEdit);
		DisplayMetrics mu = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(mu);
		width.setText(String.valueOf(mu.widthPixels));
		heigth.setText(String.valueOf(mu.heightPixels));
		bitrate.setText(String.valueOf(-1));
		fps.setText(String.valueOf((int)getWindowManager().getDefaultDisplay().getRefreshRate()));
		final SharedPreferences p = getSharedPreferences("settings",MODE_PRIVATE);
		microsend.setText(p.getString("millsec",""));
		conn = new ServiceConnection() {

			@Override
			public void onServiceConnected(ComponentName p1, IBinder p2)
			{
				Log.i(getClass().getName(),p2.toString());
				RecordService.MBinder b = (RecordService.MBinder)p2;
				service = b.getService();
				m = service.isRecording();
			}

			@Override
			public void onServiceDisconnected(ComponentName p1)
			{
				service = null;
			}
		};
		bindService(new Intent(this,RecordService.class),conn,BIND_AUTO_CREATE);
		b.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View p1)
				{
					if (!m) {
						if (microsend.getText().toString().isEmpty()) {
							Toast.makeText(MainActivity.this,"Microseconds must be not empty!",0).show();
							return;
						}
						p.edit().putString("millsec",microsend.getText().toString()).apply();
						service.setConfiguration(Integer.parseInt(width.getText().toString()),Integer.parseInt(heigth.getText().toString()),Integer.parseInt(fps.getText().toString()),Integer.parseInt(microsend.getText().toString()));
						service.prepare();
						startActivityForResult(mgr.createScreenCaptureIntent(),1000);
					} else {
						stop(true);
					}
				}
			});
    }
	private void stop(boolean killProcess) {
		if (service != null) {
			service.stop();
			unbindService(conn);
			service = null;
		}
		if (d != null) {
			d.release();
			d = null;
		}
		if (pr != null) {
			pr.stop();
			pr = null;
		}
		if (killProcess) {finish();}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (requestCode == 1000 && resultCode == RESULT_OK) {
			pr = mgr.getMediaProjection(resultCode,data);
			DisplayMetrics m = new DisplayMetrics();
			getWindowManager().getDefaultDisplay().getMetrics(m);
			d = pr.createVirtualDisplay("VirtualDisplay",760,1280,m.densityDpi,DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,service.getSurface(),null,null);
			service.run();
			this.m = true;
		}
	}

	@Override
	public void onBackPressed()
	{
		stop(true);
	}
}
