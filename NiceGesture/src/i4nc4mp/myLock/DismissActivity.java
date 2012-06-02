package i4nc4mp.myLock;

import i4nc4mp.myLock.ManageKeyguard.LaunchOnKeyguardExit;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

//One shot dismiss_keyguard activity. functions by launching, then finishing 50 ms later
//we use it so we don't have to use the pre-2.0 dismiss & secure exit commands (which are really strict)


//after the start dismiss - essentially we set a pendingDismiss flag, then when resume happens during the flag
//we then call finish - this seems to create a visible lag
//it seems this could be fixed by doing a moveTaskToBack on the guard activity once dismiss actually gains focus

public class DismissActivity extends Activity {
      public boolean done = false;
      
      protected void onCreate(Bundle icicle) {
      super.onCreate(icicle);

      
      requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
      getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
    		  //| WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
    		  | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
    		  | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
      
      Log.v("dismiss","creating dismiss window");
      
      //when using the show when locked the dismiss doesn't actually happen. lol.    
      updateLayout();
      
      //register for user present so we don't have to manually check kg with the keyguard manager
      IntentFilter userunlock = new IntentFilter (Intent.ACTION_USER_PRESENT);
      registerReceiver(unlockdone, userunlock);

}      
      protected View inflateView(LayoutInflater inflater) {
      return inflater.inflate(R.layout.dismisslayout, null);
  }

  private void updateLayout() {
      LayoutInflater inflater = LayoutInflater.from(this);

      setContentView(inflateView(inflater));
  }
  
  BroadcastReceiver unlockdone = new BroadcastReceiver() {
	    
	    public static final String present = "android.intent.action.USER_PRESENT";

	    @Override
	    public void onReceive(Context context, Intent intent) {
	    	if (!intent.getAction().equals(present)) return;
	    	if (!done) {
	    		Log.v("dismiss user present","sending to back");
	    		done = true;
	    	   	moveTaskToBack(true);
	    	}
	    }
  };
  
  /*
  @Override
  public void onWindowFocusChanged (boolean hasFocus) { 
	  	Log.v("dismiss gained focus","now waiting for user present");
	  }

          }*/
  
  @Override
  public void onDestroy() {
      super.onDestroy();
      
      
      //Context mCon = getApplicationContext();
      //ManageKeyguard.initialize(mCon);
      //boolean kg = ManageKeyguard.inKeyguardRestrictedInputMode();
      
      unregisterReceiver(unlockdone);
      Log.v("destroy_dismiss","Destroying");
      
      //if we get destroyed and the KG is still active, we have a problem
      //Try to recover via the KM secure exit API.
      /*if (kg) {
    	  ManageKeyguard.disableKeyguard(mCon);
    	  ManageKeyguard.exitKeyguardSecurely(new LaunchOnKeyguardExit() {
  	        public void LaunchOnKeyguardExitSuccess() {
  	           Log.v("handoff fallout", "successfully recovered via secure exit");
  	           
  	        	}
  			});*/
      }
  }