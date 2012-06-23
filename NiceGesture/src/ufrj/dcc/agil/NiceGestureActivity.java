package ufrj.dcc.agil;

import i4nc4mp.myLock.R;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
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
		
		 final Button button = (Button) findViewById(R.id.button1);
		 button.setOnClickListener(new View.OnClickListener() {
			 public void onClick(View v) {
			 abreAplicativo("com.android.calculator2",
			 "com.android.calculator2.Calculator");
			
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
			
			if (deltaX > 0 || deltaY > 0 || deltaZ > 0) {
				abreAplicativo("com.android.camera",
				 "com.android.camera.Camera");
			}
		}
		
	}
}
