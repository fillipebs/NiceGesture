package i4nc4mp.myLock;


import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.Toast;

//getSharedPreferences("myLockAutoUnlockprefs", Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE);
//deprecated - we now have a preference screen to make this more intuitive for end user


public class SettingsActivity extends Activity {
    
    
    private boolean security = false;
    
    private boolean enabled = false;
    
    private boolean active = false;	
    

    public CheckBox toggle;
    public CheckBox secured;
    	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settingsactivity);
        
     toggle = (CheckBox)findViewById(R.id.activeBox);
        
      toggle.setOnClickListener(new OnClickListener() {
    	  public void onClick(View v){
        		enabled = toggle.isChecked();
        		ManageMediator.invokeToggler(getApplicationContext(), enabled);
        		}
          });    
    
        
    secured = (CheckBox)findViewById(R.id.secureBox);       
    
    secured.setOnClickListener(new OnClickListener() {

                public void onClick(View v) {
             	   //hint to the observant user
                	Toast.makeText(SettingsActivity.this, "Please change security from system prefs", Toast.LENGTH_LONG).show();
                	secured.setChecked(!secured.isChecked());
                	//undo the change
                }
            });
 }
    
    public void getStatus() {
    	SharedPreferences settings = getSharedPreferences("myLock", 0);
    	
    	enabled = settings.getBoolean("enabled", false);//only set by toggler
    	
    	active = ManageMediator.bind(getApplicationContext());
        
        if (enabled && !active) {
        	//toggleService(true);
        	//active = true;
        	Log.e("pref getStatus","mediator bind failed");
        	 Toast.makeText(SettingsActivity.this, "error: myLock service not found", Toast.LENGTH_LONG).show();
        }
        
        //if service is not active, force security setting based on system
        //if it is active we are going to rely on the pref setting
                
        if (!enabled) {
        	
        security = getPatternSetting();
        
        SharedPreferences.Editor e = settings.edit();
        e.putBoolean("security", security);
        e.commit();
        }
        else security = settings.getBoolean("security", false);	
    }
    
    public boolean getPatternSetting() {
    	int patternsetting = 0;

        try {
                patternsetting = android.provider.Settings.System.getInt(getContentResolver(), android.provider.Settings.System.LOCK_PATTERN_ENABLED);
        } catch (SettingNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
        }
        
        boolean s = (patternsetting == 1);
        
        return s;
    }
    
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        return true;
    }
    
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.idlesetup:
        	Intent setup = new Intent();
        	setup.setClassName("i4nc4mp.myLock.idleLock", "i4nc4mp.myLock.idleLock.IdleSetup");
        	try {
        		startActivity(setup);
        	}
        	catch (ActivityNotFoundException e) {
        		Toast.makeText(SettingsActivity.this, "Please download Idle Lock addon", Toast.LENGTH_LONG).show();
        	}
            return true;
        }
        return false;
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	
    	getStatus();
    	
    	toggle.setChecked(enabled);
    	secured.setChecked(security);
    }
    
    @Override
    public void onBackPressed() {
    	finish();
    }
}

