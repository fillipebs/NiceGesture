package i4nc4mp.myLock;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.view.WindowManager;

//this is an advanced keyguard replacing window
//spawned at sleep cancels all keyguard mediation.
//they use this technique in the dock app to stop keyguard from ever happening.


//FIXME need to import advanced mode code so we can handle focus handoffs and do auto exit on user wake
//eventually will support advanced power save to customize buttons and add slide to unlock

//the cool thing about this is that we can let user customize which buttons would cause auto unlock.
//i like waking the incredible with the nav key then pressing back to clear the myLock screen
public class BasicUnguardActivity extends Activity {

	protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
                
        setContentView(R.layout.unguard);//cool green overlay that shows what's behind
        
        //setBright((float) 0.0);
        
        //IntentFilter offfilter = new IntentFilter (Intent.ACTION_SCREEN_OFF);
		//registerReceiver(screenoff, offfilter);
        
        //serviceHandler = new Handler();
        
      /*
       * retrieve the user's normal timeout setting - SCREEN_OFF_TIMEOUT
       
    	try {
            timeoutpref = android.provider.Settings.System.getInt(getContentResolver(), android.provider.Settings.System.SCREEN_OFF_TIMEOUT);
    } catch (SettingNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
    }//this setting will be restored at finish
    
    //Next, change the setting to 0 seconds
    android.provider.Settings.System.putInt(getContentResolver(), 
            android.provider.Settings.System.SCREEN_OFF_TIMEOUT, 0);
    //the device behavior ends up as just over 5 seconds when we do this.
    //when we set 1 here, it comes out 6.5 to 7 seconds between timeouts.
     * 
     */
        }
	
	//first focus gain after first onStart is the official point of being initialized
	//that's when we callback the mediator service
	
	//resume after that if have focus still means user wake
	//loss of focus or getting stopped after that means events waking phone
	
	@Override
    public void onDestroy() {
        super.onDestroy();
        
       //serviceHandler.removeCallbacks(myTask);
       //serviceHandler = null;
       
       //unregisterReceiver(screenoff);
      
       
       /*restore the users preference for timeout so that the screen will sleep as they expect
		android.provider.Settings.System.putInt(getContentResolver(),
				android.provider.Settings.System.SCREEN_OFF_TIMEOUT, timeoutpref);
		//then send a new userActivity call to the power manager
		PowerManager pm = (PowerManager) getSystemService (Context.POWER_SERVICE); 
    	pm.userActivity(SystemClock.uptimeMillis(), false);
    	*/
    	
        Log.v("destroyWelcome","Destroying");
    }
	
}