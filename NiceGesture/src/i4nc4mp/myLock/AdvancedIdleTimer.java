package i4nc4mp.myLock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class AdvancedIdleTimer {
	private static final String myIntent = "i4nc4mp.myLock.KILL_ADVANCED";
	private static final int REQUEST_ID = 0;


    private static PendingIntent buildIntent(Context ctx) {
        Intent intent = new Intent(myIntent);
        PendingIntent sender = PendingIntent.getBroadcast(ctx, REQUEST_ID, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        return sender;
    }

    public static void start(Context ctx) {
    	SharedPreferences settings = ctx.getSharedPreferences("myLock", 0);
    	//Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE);
    	int minutes = settings.getInt("idletime", 30);
    	
    	long timeout = minutes * 60000;
    	
        long triggerTime = System.currentTimeMillis() + timeout;

        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);

        am.set(AlarmManager.RTC_WAKEUP, triggerTime, buildIntent(ctx));
    }

    public static void cancel(Context ctx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);

        am.cancel(buildIntent(ctx));
    }    
}