package i4nc4mp.myLock;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;


//Forces itself into foreground mode, as a low mem death is likely during boot
//many processes are all doing init and demanding resources.

public class BootHandler extends Service {
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(getClass().getSimpleName(),"BootHandler - setting foreground");
		           
            int icon = R.drawable.icon;
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
		SharedPreferences settings = getSharedPreferences("myLock", 0);
		
		boolean secure = settings.getBoolean("security", false);

		boolean active = settings.getBoolean("enabled", false);
		
		boolean waitforuser = true;
		
		Intent u = new Intent();
		u.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.UserPresentService");
		
		Intent i = new Intent();
		i.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.Toggler");
		i.putExtra("i4nc4mp.myLock.TargetState", true);
		
		//security mode requires that we force security on at boot and launch user present
		//so, only when system's security is off, we launch user present
		//it will handle if user unlocked the non-secure KG immediately at startup
		
		if (secure) {
			
			int patternsetting = 0;

	        try {
	                patternsetting = android.provider.Settings.System.getInt(getContentResolver(), android.provider.Settings.System.LOCK_PATTERN_ENABLED);
	        } catch (SettingNotFoundException e) {
	                // TODO Auto-generated catch block
	                e.printStackTrace();
	        }
	        
	        boolean s = (patternsetting == 1);
			
			if (!s) {
				Log.v("secure boot","re-enabling pattern");
				android.provider.Settings.System.putInt(getContentResolver(), 
						android.provider.Settings.System.LOCK_PATTERN_ENABLED, 1);
			}
			else {
				Log.v("secure boot","system pattern already active");
				ManageKeyguard.initialize(getApplicationContext());
				if (!ManageKeyguard.inKeyguardRestrictedInputMode()) waitforuser = false;
					
				//security was already on and phone has been unlocked
				//so no need to wait
				}
		}
		else waitforuser = false;
		
		//Don't wait if not aware of security or we already started system secure and got are already unlocked
		
		Log.v("Startup result","wait for user flag is " + waitforuser);
		
		if (waitforuser) startService(u);//start user present
		else if (secure || active) startService(i);//start toggler
		
		stopForeground(true);
		stopSelf();
		
		return START_NOT_STICKY;//ensure it won't be restarted
	}
}