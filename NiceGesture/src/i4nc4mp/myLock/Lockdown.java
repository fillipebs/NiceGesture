package i4nc4mp.myLock;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

public class Lockdown extends Activity {
	//very basic lockdown - user input ignored, CPU/system kept awake but screen forced off
	//no way out besides power, to cause secure lock
	//when it goes off their first impulse is hit power, causing the lockdown.
	
	//we have to do this to force lockdown if someone is rebooting the phone on purpose
	//they basically unlock the phone immediately after boot, and so when our boothandler starts
	//it will receive the user present as soon as it is registered.
	
	private Handler serviceHandler;
    private Task myTask = new Task();
    
    private int count = 10;
    protected boolean sleep = false;
    
    
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        
        setContentView(R.layout.lockdown);
        
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
      		  | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        IntentFilter offfilter = new IntentFilter (Intent.ACTION_SCREEN_OFF);
		registerReceiver(screenoff, offfilter);     
		
		serviceHandler = new Handler();
    }
    
    public void startFade() {
    	if (!sleep) {
    		setBright((float) 1.0);
    		//full bright for long fade
    		serviceHandler.postDelayed(myTask, 1L);
    	}
    	else {
    		setBright((float) 0.0);
    	}
    }
	
    public void blockExit() {
    	//will be called anytime we get paused or stopped by anything
    	//shouldn't really happen since we force sleep and don't provide a way to re-wake
    	
    	Intent slap = new Intent("i4nc4mp.myLock.FORCE_LOCK");
	    sendBroadcast(slap);
    }
    
	public void setBright(float value) {
    	Window mywindow = getWindow();
    	
    	WindowManager.LayoutParams lp = mywindow.getAttributes();

		lp.screenBrightness = value;

		mywindow.setAttributes(lp);
    }
    

    class Task implements Runnable {
    	public void run() {                
    			//task moves brightness from 1 to 0
    			if (count > 0) {
    				Log.v("forcing sleep","count is " + count);
    				setBright((float) count/10);
    				count--;
    				serviceHandler.postDelayed(myTask, 1L);
    			}
    			else {
    				sleep = true;
    				setBright((float) 0.0);
    			}
    		
    	}
    }
    
    BroadcastReceiver screenoff = new BroadcastReceiver() {
    	//the OS is still going to call this as it is only our activity specifying the screen is off
    	//the OS still runs the flags that would make it be on for all other activities.
    	
        public static final String Screenoff = "android.intent.action.SCREEN_OFF";

        @Override
        public void onReceive(Context context, Intent intent) {
                if (!intent.getAction().equals(Screenoff)) return;
        
                //ManageKeyguard.initialize(context);
                //if (ManageKeyguard.inKeyguardRestrictedInputMode()) finish();
                //only if the lockscreen now exists, close.
                //we are using flag keep screen on to prevent timeout KG grace period
                //so this isn't necessary. we know it will exist
                
                moveTaskToBack(true);
                finish();
        
        }
    };
    
    @Override
    public void onPause() {
    	super.onPause();
    	Log.v("lockdown got paused","lockdown got paused");
    	if (!sleep) blockExit();
    }
    
    @Override
    public void onStop() {
    	super.onStop();
    	Log.v("lockdown got stopped","lockdown got stopped");
    	if (!sleep) blockExit();
    }
    
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
    	super.onWindowFocusChanged(hasFocus);
    	if (!hasFocus && !sleep) {
    		Log.v("lockdown lost focus","lockdown lost focus");
    		blockExit();
    	}
    }
    
    @Override
    public void onStart() {
    	super.onStart();
    	
    	Toast.makeText(Lockdown.this, "Security Activated", Toast.LENGTH_SHORT).show();
    	startFade();
    }
    
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
       serviceHandler.removeCallbacks(myTask);
       serviceHandler = null;
       
       unregisterReceiver(screenoff);    
}
    
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
    
    	return true;//no buttons work, consume all events
    	
    }
}