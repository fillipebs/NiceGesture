package i4nc4mp.myLock;

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

//not effective if pattern is already on.
//this process would need to begin with a non-secure KG, then turn on security and call re-enable
//on receiving user present
public class PatternRestoreActivity extends Activity {
      public boolean done = false;
      
      protected void onCreate(Bundle icicle) {
      super.onCreate(icicle);
      
      //ManageKeyguard.disableKeyguard(getApplicationContext());
      
      requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
      getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
  
      setContentView(R.layout.dismisslayout);
      
      //we don't actually get a user present when this is launched while the pattern is on
      //interestingly, the translucency works and I see whatever was there.
      //this is only because we actually called disable

}
  
  @Override
  public void onWindowFocusChanged(boolean hasFocus) {
	  if (!done && hasFocus) {
		  finish();
	  }
  }
  
  
  @Override
  public void onDestroy() {
      super.onDestroy();
      
      Log.v("destroy_dismiss","Destroying");
  }
}