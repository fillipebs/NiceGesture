package i4nc4mp.myLock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ForceLock extends BroadcastReceiver {
	
	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		if("i4nc4mp.myLock.FORCE_LOCK".equals(intent.getAction())) {
			//this is the action we are registered for via manifest declaration
			Log.v("force lock request","launching instant lock");
			
			Intent f = new Intent();
	    	f.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.InstantLockdown");
	    	f.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
	    			| Intent.FLAG_FROM_BACKGROUND);
	    	context.startActivity(f);
		}
	}
}