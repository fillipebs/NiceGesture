package i4nc4mp.myLock;
//thanks geekyouup battery widget. I want our lockscreen to display the battery % also.
//this receiver gets registered by the mediator which as a remote service will always get these updates
//then place the level into shared prefs for any lock activity to read from

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
//import android.os.BatteryManager;

public class BatteryInfo extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
        try
        {
                        String action = intent.getAction();
                if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {                     
                    SharedPreferences settings = context.getSharedPreferences("myLock", 0);
                    if(settings !=null)
                    {
                       SharedPreferences.Editor editor = settings.edit();
                       editor.putInt("BattLevel", intent.getIntExtra("level", 0));
                       //editor.putInt("IsCharging",intent.getIntExtra("status", BatteryManager.BATTERY_STATUS_UNKNOWN));
                       //editor.putInt("BattVoltage", intent.getIntExtra("voltage", 0));
                       editor.commit();
                    }
                    
                        
                }
        }catch(Exception e){}
        }
}