package i4nc4mp.myLock;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener; 

public class IdleSetup extends Activity {
	
	SharedPreferences prefs = null;
	int min; 
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.idlesetup);
       		 
		 prefs = getSharedPreferences("myLock", 0);
		 //Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE);
		 
		 min = prefs.getInt("idletime", 0);
		 
		 final TextView result = (TextView) findViewById(R.id.barpos);
		 
		 if (min != 0) result.setText(String.valueOf(min) + " minutes");
		 else result.setText("timeout disabled");
		 
		 //the bar is defined as 0 to 60
		 final SeekBar minbar = (SeekBar) findViewById(R.id.time);
		 minbar.setProgress(min);
		 //show the user's pref
		 minbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				// TODO Auto-generated method stub
				if (fromUser) {
					min = progress;
					if (min != 0) result.setText(String.valueOf(min) + " minutes");
					else result.setText("timeout disabled");
				}
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				
				//we don't care
				
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				
				//we don't care
			}
			 
		 });
		 
		 Button done = (Button) findViewById(R.id.done);
		 done.setOnClickListener(new OnClickListener() {
	          	public void onClick(View v){
	          		int oldpref = prefs.getInt("idletime", 0);
	          		
	          		//Trying to set a time but hasn't launched myLock settings while a pattern was in effect
	          		if (min != 0 && !prefs.getBoolean("security", false)) {
          				Toast.makeText(IdleSetup.this, "Set up a pattern via system prefs first", Toast.LENGTH_LONG).show();
          			}
          			else {
	          		if (min != oldpref) {
	          			
	          			SharedPreferences.Editor e = prefs.edit();
	          			e.putInt("idletime", min);
	          			
	          			e.commit();
	          			
	          			//we only need to notify active mediator if coming from or going to 0
	          			if ((min == 0 || oldpref == 0) && prefs.getBoolean("serviceactive", false))
	          			{
	          			Intent i = new Intent();
	          			i.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.Toggler");
	          			i.putExtra("i4nc4mp.myLock.TargetState", true);
	          			startService(i);
	          				}
	          			}
	          			
	          		//it appears due to sharing across processes limitation
	          		//the listener doesn't know about this change in real time
	          			
	          	}
	          		finish();
	          		
		 }});
}
}