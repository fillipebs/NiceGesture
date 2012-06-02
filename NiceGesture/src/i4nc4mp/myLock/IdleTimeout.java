package i4nc4mp.myLock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class IdleTimeout extends BroadcastReceiver {
	
	//advanced mode is the only one where its tricky since no KG exists at timeout call
	//advanced mode has to maintain a Disable call before every StartLock
	//then cause a wake + exit and re-enable call at timeout
	
	//so this is only invoked for the basic modes
	
	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		if("i4nc4mp.myLock.IDLE_TIMEOUT".equals(intent.getAction())) {
			//this is the action we are registered for via manifest declaration
			
			SharedPreferences prefs = context.getSharedPreferences("myLock", 0);
			boolean security = prefs.getBoolean("security", false);
			
			if (security) {
				Log.v("idle lock","timeout reached, locking down");
			
			//close the current service via Toggler with target false
			Intent i = new Intent();
			i.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.Toggler");
			i.putExtra("i4nc4mp.myLock.TargetState", false);
			context.startService(i);
			//this will cause security to be turned back on
			

			
			//start up the user present to wait for next unlock.
			//when it starts it closes the non-secure KG and then re-enables with the secure one
			Intent u = new Intent();
		    u.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.UserPresentService");
		    context.startService(u);
			}
			else {
				//turn off the timeout, we don't want it to go off again while security isn't on
				Log.v("idle lock","timeout reached, security was off- disabling timer");
				SharedPreferences.Editor e = prefs.edit();
				e.putInt("idletime", 0);
			}
		}
	}
}