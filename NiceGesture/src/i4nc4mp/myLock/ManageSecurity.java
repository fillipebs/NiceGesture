package i4nc4mp.myLock;

import android.content.Context;
import android.provider.Settings.SettingNotFoundException;


public class ManageSecurity {
	//handles checking of system security state so that we can ensure the correct handling in mediator
	//the problem case we can't catch is dumb user changing security while myLock is running
	
	//important to check at start/stop
	//so we avoid turning on pattern when no pattern is set
	//or so we don't miss turning off the existent pattern, causing all our modes to break
	
	public static void enableSecurity(Context c) {
		if (!isPatternActive(c))
			android.provider.Settings.System.putInt(c.getContentResolver(),
					android.provider.Settings.System.LOCK_PATTERN_ENABLED, 1);
	}
	
	public static void disableSecurity(Context c) {
		if (isPatternActive(c))
			android.provider.Settings.System.putInt(c.getContentResolver(),
					android.provider.Settings.System.LOCK_PATTERN_ENABLED, 0);
	}
	
	public static boolean isPatternActive(Context c) {
    	int patternsetting = 0;

        try {
                patternsetting = android.provider.Settings.System.getInt(c.getContentResolver(), android.provider.Settings.System.LOCK_PATTERN_ENABLED);
        } catch (SettingNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
        }
        
        boolean s = (patternsetting == 1);
        
        return s;
    }
}