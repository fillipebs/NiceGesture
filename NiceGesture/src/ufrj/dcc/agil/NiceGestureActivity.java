package ufrj.dcc.agil;

import i4nc4mp.myLock.R;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class NiceGestureActivity extends Activity implements SensorEventListener  {
	private float mLastX, mLastY, mLastZ;
	private boolean mInitialized;
	private SensorManager mSensorManager;
	private Sensor mAccelerometer;
	private final float NOISE = (float) 6.0;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		 final Button button = (Button) findViewById(R.id.Button01);
		 button.setOnClickListener(new View.OnClickListener() {
			 public void onClick(View v) {
				 abreAplicativo("com.android.calculator2",
				 "com.android.calculator2.Calculator");
			 }
		 });
		 
		 final Button button2 = (Button) findViewById(R.id.Button02);
		 button2.setOnClickListener(new View.OnClickListener() {
			 public void onClick(View v) {
				 Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivity(cameraIntent);
			 }
		 });
		 
		 final Button button3 = (Button) findViewById(R.id.Button03);
		 button3.setOnClickListener(new View.OnClickListener() {
			 public void onClick(View v) {
				 Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.google.com"));
				 startActivity(browserIntent);
			 }
		 });
		 
		 final Button button4 = (Button) findViewById(R.id.Button04);
		 button4.setOnClickListener(new View.OnClickListener() {
			 public void onClick(View v) {
				 Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
				 emailIntent.setType("plain/text");
				 startActivity(emailIntent);
			 }
		 });
		 
		 final Button button5 = (Button) findViewById(R.id.Button05);
		 button5.setOnClickListener(new View.OnClickListener() {
			 public void onClick(View v) {
				 Intent fbIntent = new Intent("android.intent.category.LAUNCHER");
				 fbIntent.setClassName("com.facebook.katana", "com.facebook.katana.LoginActivity");
				 startActivity(fbIntent);
			 }
		 });
		 
		 final Button button6 = (Button) findViewById(R.id.Button06);
		 button6.setOnClickListener(new View.OnClickListener() {
			 public void onClick(View v) {
				 Intent sendIntent = new Intent(Intent.ACTION_VIEW);         
				 sendIntent.setData(Uri.parse("sms:"));
				 startActivity(sendIntent);
			 }
		 });

		 
		 mInitialized = false;
		 mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		 mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		 mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
	}
	protected void onResume() {
		super.onResume();
		mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
	}
	protected void onPause() {
		super.onPause();
		mSensorManager.unregisterListener(this);
	}
	public void abreAplicativo(String caminho, String caminhoNome) {
		Intent i = new Intent();
		i.setClassName(caminho, caminhoNome);
		startActivity(i);
	}

	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {		
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		float x = event.values[0];
		float y = event.values[1];
		float z = event.values[2];

		if (!mInitialized) {
			mLastX = x;
			mLastY = y;
			mLastZ = z;
			mInitialized = true;
		} else {
			float deltaX = Math.abs(mLastX - x);
			float deltaY = Math.abs(mLastY - y);
			float deltaZ = Math.abs(mLastZ - z);

			if (deltaX < NOISE) deltaX = (float)0.0;
			if (deltaY < NOISE) deltaY = (float)0.0;
			if (deltaZ < NOISE) deltaZ = (float)0.0;
			
			mLastX = x;
			mLastY = y;
			mLastZ = z;
			
			if (deltaX > 0) {
				Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivity(cameraIntent);
			}
			else if (deltaY > 0) {
				abreAplicativo("com.android.calculator2",
				 "com.android.calculator2.Calculator");
			}
			else if (deltaZ > 0) {
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.google.com"));
				startActivity(browserIntent);
			}
		}
		
	}
}
