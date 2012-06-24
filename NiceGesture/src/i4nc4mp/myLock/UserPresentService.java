package i4nc4mp.myLock;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.app.KeyguardManager.KeyguardLock;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

public class UserPresentService extends Service {
	//this will be started when myLock is shut off due to idle timeout, or at bootup
	//it starts everything up once we receive user present broadcast (meaning the lockscreen was completed)
	//just bridges the gap between an idle timeout or first startup and the user authentication of their pattern
	
	//it is only used when the user has turned on security mode
	
	//there is a special lockdown activity we use if no KG is detected on startup
	
	public boolean secured = false;
	
	Handler serviceHandler;
    Task myTask = new Task();
    
    public static void launch(Context c) {
    	Intent u = new Intent();
	    u.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.UserPresentService");
	    c.startService(u);
    }
	
	@Override
	public IBinder onBind(Intent arg0) {
		Log.d(getClass().getSimpleName(), "onBind()");
		return null;//we don't care
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		Log.d(getClass().getSimpleName(),"onDestroy()");
		
		unregisterReceiver(unlockdone);
		
		serviceHandler.removeCallbacks(myTask);

		serviceHandler = null;
				
		}
	
	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(getClass().getSimpleName(),"User Present - setting foreground");
		           
            int icon = R.drawable.icone;
            CharSequence tickerText = "myLock";
            
            long when = System.currentTimeMillis();

            Notification notification = new Notification(icon, tickerText, when);
            
            Context context = getApplicationContext();
            CharSequence contentTitle = "myLock";
            CharSequence contentText = "initializing";

            Intent notificationIntent = new Intent(this, SettingsActivity.class);
            PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

            notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
            
            final int SVC_ID = 1;
            
            
            startForeground(SVC_ID, notification);
    
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		
		IntentFilter userunlock = new IntentFilter (Intent.ACTION_USER_PRESENT);
		
		registerReceiver (unlockdone, userunlock);
				
		serviceHandler = new Handler();
		
		ManageKeyguard.initialize(getApplicationContext());
		if (ManageKeyguard.inKeyguardRestrictedInputMode()) {
			//case - user has not unlocked the non-secure KG. Screen will probably be asleep
			//clear the standard keyguard. the delay waits 50 ms then forces the secure keyguard on
			ManageKeyguard.disableKeyguard(getApplicationContext());
			serviceHandler.postDelayed(myTask, 50L);
		}
		else {
			//case - user has unlocked immediately on startup
			//they are going about the phone like a boss
			//TIME TO SLAP THEM WITH A LOCKDOWN
			
			/*
			Intent slap = new Intent();
	    	slap.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.Lockdown");
	    	slap.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
	    			Intent.FLAG_ACTIVITY_NEW_TASK);
	    	getApplicationContext().startActivity(slap);
	    	
	    	*
	    	*The smooth "fading out" lockdown is broken while we have the home key bug
	    	*
	    	*Just use instant version for now.
	    	*/
			
			Intent slap = new Intent("i4nc4mp.myLock.FORCE_LOCK");
		    getApplicationContext().sendBroadcast(slap);
		    
			//KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
			//KeyguardLock kl = km.newKeyguardLock("myLock");
			//kl.disableKeyguard();
			//kl.reenableKeyguard();
			//try to make the keyguard appear.
		    
	    	secured = true;
		}
		
		return START_NOT_STICKY;
		//we would never get killed while sitting idle waiting for a user to come back and unlock
	}
	

	BroadcastReceiver unlockdone = new BroadcastReceiver() {
	    
	    public static final String present = "android.intent.action.USER_PRESENT";

	    @Override
	    public void onReceive(Context context, Intent intent) {
	    	if (!intent.getAction().equals(present)) return;
	    	if (!secured) return;
	    	//just in case it sends us a user present from the non-secure kg
	    	//if user is actively unlocking it immediately on boot
	    	
	    	Log.v("user unlocking","Keyguard was completed by user");
	    	
	    	//send myLock start intent
	    	Intent i = new Intent();
	    
			i.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.Toggler");
			i.putExtra("i4nc4mp.myLock.TargetState", true);
			startService(i);
			
			stopForeground(true);
			stopSelf();
	    	return;
	    
	}};
	
	class Task implements Runnable {
        public void run() {
        	/*ManageKeyguard.exitKeyguardSecurely(new LaunchOnKeyguardExit() {
                public void LaunchOnKeyguardExitSuccess() {
                   Log.v("doExit", "This is the exit callback");
                   //the callback isn't really necessary, we already get user present
                    }});*/
        	ManageKeyguard.reenableKeyguard();
        	secured = true;
        }
	}

}