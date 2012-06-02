package i4nc4mp.myLock;

import i4nc4mp.myLock.ManageKeyguard.LaunchOnKeyguardExit;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;


//For this lifecycle, we go dormant for any outside event
//such as incoming call ringing, alarm, handcent popup, etc.
//we detect going dormant by losing focus while already paused.
//if focus loss occurs while not paused, it means the user is actively navigating out of the woken lockscreen
//FIXME integrate the new stop without focus loss that is being caused by handcent latest ver


public class BasicGuardActivity extends Activity {
	//import from the guard activity from Complete revision
	Handler serviceHandler;
    Task myTask = new Task();


/* Lifecycle flags */
public boolean starting = true;//flag off after we successfully gain focus. flag on when we send task to back
public boolean finishing = false;//flag on when an event causes unlock, back off when onStart comes in again (relocked)

public boolean paused = false;

public boolean dormant = false;
//special lifecycle phase- we are waiting in the background for outside event to return focus to us
//an example of this is while a call is ringing. we have to force the state
//because the call prompt acts like a user notification panel nav

public boolean pendingExit = false;
//special lifecycle phase- when we lose focus and aren't paused, we launch a KG pause
//two outcomes, either we securely exit if a pause comes in meaning user is navigating out
//or else we are going to get focus back and re-enable keyguard

public boolean slideWakeup = false;
//we will set this when we detect slideopen, only used with instant unlock (replacement for 2c ver)

public boolean slideGuard = false;
//user pref whether to have the slide wake show lockscreen. default is still auto exit

public boolean pendingDismiss = false;
//will be set true when we launch the dismiss window for auto and user requested exits
//this ensures focus changes and pause/resume will be ignored to allow dismiss activity to finish

public boolean resurrected = false;
//just to handle return from dormant, avoid treating it same as a user initiated wake

    
    
    //very very complicated business.
    @Override
protected void onCreate(Bundle icicle) {
    super.onCreate(icicle);

    requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
    
    updateLayout();
    
    
    IntentFilter onfilter = new IntentFilter (Intent.ACTION_SCREEN_ON);
            registerReceiver(screenon, onfilter);
    
    IntentFilter callbegin = new IntentFilter ("i4nc4mp.myLock.lifecycle.CALL_START");
    registerReceiver(callStarted, callbegin);  
    
    IntentFilter callpend = new IntentFilter ("i4nc4mp.myLock.lifecycle.CALL_PENDING");
    registerReceiver(callPending, callpend);
    
  
    
    IntentFilter userunlock = new IntentFilter (Intent.ACTION_USER_PRESENT);
    registerReceiver(unlockdone, userunlock);
    
    
            
    serviceHandler = new Handler();
    
    SharedPreferences settings = getSharedPreferences("myLock", 0);
    slideGuard = settings.getBoolean("slideGuard", false);
}
    
    
    
           
protected View inflateView(LayoutInflater inflater) {
    return inflater.inflate(R.layout.guardlayout, null);
}

private void updateLayout() {
    LayoutInflater inflater = LayoutInflater.from(this);

    setContentView(inflateView(inflater));
}
    
@Override
public void onBackPressed() {
    //Back will cause unlock
    
    StartDismiss(getApplicationContext());
    finishing=true;
}


BroadcastReceiver screenon = new BroadcastReceiver() {
            
    public static final String Screenon = "android.intent.action.SCREEN_ON";

    @Override
    public void onReceive(Context context, Intent intent) {
            if (!intent.getAction().equals(Screenon) || finishing) return;
            Log.v("guard is handling wakeup","deciding whether to dismiss");
            
            
            if (hasWindowFocus() && !resurrected && !pendingDismiss) {             	
            	
            	if (slideWakeup && slideGuard){
            		finishing = true;
            	    finish();
            	}//this screen will exit, yielding to the regular unlock screen
            	//the callback ensures that it will be recreated if user fails to complete unlock
            	//TODO add a haptic notif so user might know if this pocket wake happened
            	else {
            		StartDismiss(context);
            	} 
            }
            else {
            	if (resurrected) {
                	//ignore this wake as we do not actually want instant exit
                	resurrected = false;
                	Log.v("guard resurrected","ignoring invalid screen on");
                }
            	
            	//TODO possible alternative to this:
            	//Always exit when slide wakeup detected
            	//mediator also detect slide wakeup and fire auto dismiss if not slide guarded
            	//result is user still sees lockscreen on slider despite having wallpaper enabled
            	if (slideWakeup && !slideGuard && pendingDismiss) {
            		ManageKeyguard.exitKeyguardSecurely(new LaunchOnKeyguardExit() {
            	        public void LaunchOnKeyguardExitSuccess() {
            	           Log.v("slide wake exit", "closing the guard window");
            	           pendingDismiss = false;
            	           finishing = true;
            	           finish();
            	        	}
            			});
            		}
            }
            
    return;//avoid unresponsive receiver error outcome
         
}};

BroadcastReceiver callStarted = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
    if (!intent.getAction().equals("i4nc4mp.myLock.lifecycle.CALL_START")) return;
    
    //we are going to be dormant while this happens, therefore we need to force finish
    Log.v("guard received broadcast","completing callback and finish");
    
    StopCallback();
    finish();
    
    return;
    }};
    
BroadcastReceiver callPending = new BroadcastReceiver() {
    @Override
       public void onReceive(Context context, Intent intent) {
    if (!intent.getAction().equals("i4nc4mp.myLock.lifecycle.CALL_PENDING")) return;
            //incoming call does not steal focus till user grabs a tab
            //lifecycle treats this like a home key exit
            //forcing dormant state here will allow us to only exit if call is answered
            dormant = true;
            return;                 
    }};

    
    BroadcastReceiver unlockdone = new BroadcastReceiver() {
	    
	    public static final String present = "android.intent.action.USER_PRESENT";

	    @Override
	    public void onReceive(Context context, Intent intent) {
	    	if (!intent.getAction().equals(present)) return;
	    	Log.v("user present","Keyguard is now fully dismissed");
	    	//if we try to hide or request close before this moment it causes a frozen lockscreen
	    	
	    	if (pendingDismiss) {
	    		pendingDismiss = false;//prevent duplicate handling
	    		//system sometimes calls this several times
	    		StopCallback();
	    		moveTaskToBack(true);//minimize self
	    		finish();//request destruction, or else we just sit minimized in the background
	    	}
	    	else Log.v("unexpected user present","there might be an external kg disable event occurring");
	    	
	    }
    };
    
    
    class Task implements Runnable {
    public void run() {
            
            ManageKeyguard.exitKeyguardSecurely(new LaunchOnKeyguardExit() {
        public void LaunchOnKeyguardExitSuccess() {
           Log.v("doExit", "This is the exit callback");
           StopCallback();
           finish();
            }});
    }}
    
    
//here, if slide guarded mode is active in prefs, we will hide self to reveal normal lockscreen
//when slider wakes device TODO not yet implemented
    @Override
public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    if (newConfig.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO) {
            //this means that a config change happened and the keyboard is open.
            if (starting) Log.v("slide-open lock","aborting handling, slide was opened before this lock");
            else {
            	Log.v("slide-open wake","setting state flag");
            	slideWakeup = true;
            	if (!slideGuard) {
            		//we will do a KM secure exit here for performance reasons
            		pendingDismiss = true;
            		ManageKeyguard.disableKeyguard(getApplicationContext());
            	}
            	
            }           
    }
    else if (newConfig.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_YES) {
    	Log.v("slide closed","lockscreen activity got the config change from background");
    }
                  
}

    @Override
protected void onStop() {
    super.onStop();
    
    if (pendingDismiss) return;
    
    if (finishing) {
    	//deliberate user action exit not caused by handoffs to other events
            Log.v("lock stop","we have been unlocked by a user exit request");
    }
    else if (paused) {
            if (hasWindowFocus()) {
    
            //stop is called, we were already paused, and still have focus
            //this means something is about to take focus, we should go dormant
            dormant = true;
            Log.v("lock stop","detected external event about to take focus, setting dormant");
            }
        else if (!hasWindowFocus()) {
            //we got paused, lost focus, then finally stopped
            //this only happens if user is navigating out via notif, popup, or home key shortcuts
            Log.v("lock stop","onStop is telling mediator we have been unlocked by user navigation");
            //if (dormant) finishing = true;//this would happen if user left via handcent dialog
        }
    }
    else Log.v("unexpected onStop","lockscreen was stopped for unknown reason");
    
    if (finishing) {
            StopCallback();
            finish();
    }
    
}

@Override
protected void onPause() {
    super.onPause();
    
    paused = true;
    
    if (!starting && !hasWindowFocus() && !pendingDismiss) {
            //case: we yielded focus to something but didn't pause. Example: notif panel
            //pause in this instance means something else is launching, that is about to try to stop us
            //so we need to exit now, as it is a user nav, not a dormancy event
            Log.v("navigation exit","got paused without focus, starting dismiss sequence");
            
            //anytime we lose focus before pause, we are calling disable
            //this will exit properly as we navigate out
            ManageKeyguard.exitKeyguardSecurely(new LaunchOnKeyguardExit() {
            public void LaunchOnKeyguardExitSuccess() {
               Log.v("doExit", "This is the exit callback");
               StopCallback();
               finish();
                }});
            
    }
    else {
    	if (pendingDismiss) {
      		Log.v("handoff to dismiss window","pausing, expecting user present soon");
      		//here we could queue something to recover from fallout of user present
      		//it seems to be caused by the handcent service.. it breaks the handoff
      		//causing lockscreen to come up which has to be unlocked before user present happens
      	}
    	else if (hasWindowFocus()) Log.v("lock paused","normal pause - we still have focus");
      	else Log.v("lock paused","exit pause - don't have focus");
      	
      	if (slideWakeup) {
      		Log.v("returning to sleep","toggling slide wakeup false");
      		slideWakeup = false;
      	}
      	if (resurrected) {
      		Log.v("returning to sleep","toggling resurrected false");
      		resurrected = false;
      		//sometimes the invalid screen on doesn't happen
      		//in that case we just turn off the flag at next pause
      		//FIXME need to ignore this in the autodismiss also
      		//without it the phone is unlocking after missed call
      	}
      }
    }

@Override
protected void onResume() {
    super.onResume();
    Log.v("lock resume","resuming, focus is " + hasWindowFocus());
   

    paused = false;
    
    
    //updateClock();
}

@Override
public void onDestroy() {
    super.onDestroy();
            
   serviceHandler.removeCallbacks(myTask);

   serviceHandler = null;
   
   unregisterReceiver(screenon);
   unregisterReceiver(callStarted);
   unregisterReceiver(callPending);
   unregisterReceiver(unlockdone);
    
   Log.v("destroy Guard","Destroying");
}

@Override
public void onWindowFocusChanged (boolean hasFocus) {
    if (hasFocus) {
            Log.v("focus change","we have gained focus");
            //Catch first focus gain after onStart here.
            //this allows us to know if we actually got as far as having focus (expected but bug sometimes prevents
            if (starting) {
                    starting = false;
                    //set our own lifecycle reference now that we know we started and got focus properly
                    
                    //tell mediator it is no longer waiting for us to start up
                    StartCallback();
            }
            else if (dormant) {
                    Log.v("regained","we are no longer dormant");
                    dormant = false;
                    resurrected = true;
            }
            else if (pendingExit) {
                    Log.v("regained","we are no longer pending nav exit");
                    pendingExit = false;
                    ManageKeyguard.reenableKeyguard();
            }
    }
    else if (!pendingDismiss) {                                                  
        if (!finishing && paused) {
//Handcent popup issue-- we haven't gotten resume & screen on yet
//Handcent is taking focus first thing
//So it is now behaving like an open of notif panel where we aren't stopped and aren't even getting paused
      	  
     //we really need to know we were just resumed and had screen come on to do this exit
                        
                        if (dormant) Log.v("dormant handoff complete","the external event now has focus");
                        else {
                      	  if (isScreenOn()) {
                                Log.v("home key exit","launching full secure exit");
                                                                                
                                ManageKeyguard.disableKeyguard(getApplicationContext());
                                serviceHandler.postDelayed(myTask, 50);
                      	  }
                      	  else {
                      		  //Here's the handcent case
                      		  //if you then exit via a link on pop,
                      		  //we do get the user nav handling on onStop
                      		  Log.v("popup event","focus handoff before screen on, nav exit possible");
                      		  dormant = true;                            
                      		  //we need to be dormant so we realize once the popup goes away
                      	  }
                        }
        }
            else if (!paused) {
                    //not paused, losing focus, we are going to manually disable KG
                    Log.v("focus yielded while active","about to exit through notif nav");
                    pendingExit = true;
                    ManageKeyguard.disableKeyguard(getApplicationContext());
            }
    }
    
}

protected void onStart() {
    super.onStart();
    Log.v("lockscreen start success","setting flags");
    
    if (finishing) {
            finishing = false;
            Log.v("re-start","we got restarted while in Finishing phase, wtf");
            //since we are sometimes being brought back, safe to ensure flags are like at creation
    }
}

public void StartCallback() {
    Intent i = new Intent("i4nc4mp.myLock.lifecycle.LOCKSCREEN_PRIMED");
    getApplicationContext().sendBroadcast(i);
}

public void StopCallback() {
    Intent i = new Intent("i4nc4mp.myLock.lifecycle.LOCKSCREEN_EXITED");
    getApplicationContext().sendBroadcast(i);
}

public void StartDismiss(Context context) {
    
	PowerManager myPM = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
    myPM.userActivity(SystemClock.uptimeMillis(), false);
    //the KeyguardViewMediator poke doesn't have enough time to register before our handoff sometimes (rare)
    //this might impact the nexus more than droid. need to test further
    //result is the screen is off (as the code is successful)
    
    
    //but no keyguard, have to hit any key to wake it back up
    //TODO We solved this through a full wake lock acquired just before the start activity call.
	
    Class w = DismissActivity.class;
                  
    Intent dismiss = new Intent(context, w);
    dismiss.setFlags(//Intent.FLAG_ACTIVITY_NEW_TASK//For some reason it requires this even though we're already an activity
                    Intent.FLAG_ACTIVITY_NO_USER_ACTION//Just helps avoid conflicting with other important notifications
                    | Intent.FLAG_ACTIVITY_NO_HISTORY//Ensures the activity WILL be finished after the one time use
                    | Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    
    pendingDismiss = true;
    startActivity(dismiss);
}

public boolean isScreenOn() {
	//Allows us to tap into the 2.1 screen check if available
	
	boolean on = false;
	
	if(Integer.parseInt(Build.VERSION.SDK) < 7) { 
		//we will bind to mediator and ask for the isAwake, if on pre 2.1
		//for now we will just use a pref since we only need it during life cycle
		//so we don't have to also get a possibly unreliable screen on broadcast within activity
		Log.v("pre 2.1 screen check","grabbing screen state from prefs");
		SharedPreferences settings = getSharedPreferences("myLock", 0);
	   	on = settings.getBoolean("screen", false);
		
	}
	else {
		PowerManager myPM = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
		on = myPM.isScreenOn();
	}
	
	return on;
}
}