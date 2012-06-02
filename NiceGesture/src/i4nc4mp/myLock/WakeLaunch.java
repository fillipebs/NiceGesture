package i4nc4mp.myLock;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;


//launch the activity once the system deems the user is there and has unlocked.
//so when running with myLock, it will launch the activity
//when running and pattern mode is required, will do the launch on successful unlock

public class WakeLaunch extends Service {
	
	@Override
	public IBinder onBind(Intent arg0) {
		Log.d(getClass().getSimpleName(), "onBind()");
		return null;//we don't bind
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		Log.d(getClass().getSimpleName(),"onDestroy()");
		
		unregisterReceiver(unlockdone);
		
		
		}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		
		IntentFilter userunlock = new IntentFilter (Intent.ACTION_USER_PRESENT);
		
		registerReceiver (unlockdone, userunlock);
		
		return 1;
	}

	BroadcastReceiver unlockdone = new BroadcastReceiver() {
	    
	    public static final String present = "android.intent.action.USER_PRESENT";

	    @Override
	    public void onReceive(Context context, Intent intent) {
	    	if (!intent.getAction().equals(present)) return;
	    	Log.v("user unlocking","Keyguard was completed by user");
	    	//Context mCon = getApplicationContext();
	    	
	    	Intent i = new Intent();
	    	//com.larvalabs.slidescreenpro/.SlideScreenPro
	    	i.setClassName("com.larvalabs.slidescreenpro", "com.larvalabs.slidescreenpro.SlideScreenPro");
	    	
	    	i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(i);
			//stopSelf();
	    	return;
	    
	}};

}