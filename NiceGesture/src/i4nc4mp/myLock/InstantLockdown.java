package i4nc4mp.myLock;

import android.os.Bundle;

public class InstantLockdown extends Lockdown {
	 @Override
	    protected void onCreate(Bundle icicle) {
	        super.onCreate(icicle);
	        
	        sleep = true;
	 }
}