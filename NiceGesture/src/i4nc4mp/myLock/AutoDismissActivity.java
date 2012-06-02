package i4nc4mp.myLock;

import ufrj.dcc.agil.NiceGestureActivity;
import android.app.Activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

//One shot dismiss_keyguard activity. Launch, wait for system to confirm KG is clear, minimize, exit
//avoids the pre-2.0 dismiss & secure exit commands (which are really strict and break often)

public class AutoDismissActivity extends Activity {
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
	    		//callback mediator for final handling of the stupid wake lock
	            Intent i = new Intent("i4nc4mp.myLock.lifecycle.LOCKSCREEN_EXITED");
	            getApplicationContext().sendBroadcast(i);
	    	   	moveTaskToBack(true);
	    	   	finish();
	    	}
	    	
	    	Intent myIntent = new Intent(AutoDismissActivity.this, NiceGestureActivity.class);
	    	AutoDismissActivity.this.startActivity(myIntent);
	    }
};

@Override
public void onDestroy() {
    super.onDestroy();      
   
    unregisterReceiver(unlockdone);
    Log.v("destroy_dismiss","Destroying");

    }
}