package i4nc4mp.myLock;

import i4nc4mp.myLock.ManageKeyguard.LaunchOnKeyguardExit;
import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

//simple dismiss activity that will exit self at wakeup
//this mode is for users who wish for all buttons to wake and unlock
//they want no guarding... probably those belt clip holster touting jerks

public class UnguardService extends MediatorService {
	private boolean persistent = false;
    private boolean timeoutenabled = false;
    
    private boolean security = false;
    
/* Life-Cycle Flags */
    private boolean shouldLock = true;
    //Flagged true upon Lock Activity exit callback, remains true until StartLock intent is fired.
            
    private boolean PendingLock = false;
    //Flagged true upon sleep, remains true until StartLock sends first callback indicating Create success.
    
    
    private static Handler myHandler;
    private boolean closing = false;
    
    private int waited = 0;
    
    //The mediator service INSTANCE sets up the shared handler
    protected void initHandler() {
    	myHandler = new Handler() {
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				
				switch (msg.what) {
					case 0:
						handleLockEvent(true);
						break;
					case 1:
						handleLockEvent(false);
						break;
					default:
						break;
					}
				}
		};
    }
    
    //to be used in the case of a lockdown
    public static void closeLockscreen(Context c) {
    	Intent intent = new Intent("i4nc4mp.myLock.lifecycle.CALL_START");
        c.sendBroadcast(intent);
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
            
        SharedPreferences settings = getSharedPreferences("myLock", 0);
            
        //toggle security back on
        	if (security) ManageSecurity.enableSecurity(getApplicationContext());
        	        	
                myHandler = null;
                
                unregisterReceiver(goidle);
                
                settings.unregisterOnSharedPreferenceChangeListener(prefslisten);
                
                ManageWakeLock.releasePartial();
                
                
              //when we get closed, it might be due to locale toggle
              //if our lock activity is alive, send broadcast to close it
            	if (!shouldLock) closeLockscreen(getApplicationContext());
}
    
    @Override
    public void onFirstStart() {
            SharedPreferences settings = getSharedPreferences("myLock", 0);
            
            persistent = settings.getBoolean("FG", false);
            security = settings.getBoolean("security", false);
                        
            if (persistent) doFGstart();
            
            timeoutenabled = (settings.getInt("idletime", 0) != 0);
            
            //register a listener to update this if pref is changed to 0
            settings.registerOnSharedPreferenceChangeListener(prefslisten);
            
            
            //toggle out of security
            if (security) ManageSecurity.disableSecurity(getApplicationContext());
            
                            
           initHandler();
            
           
           IntentFilter kill = new IntentFilter("i4nc4mp.myLock.KILL_ADVANCED");
           registerReceiver (goidle, kill);
           
            
            ManageWakeLock.acquirePartial(getApplicationContext());
            //if not always holding partial we would only acquire at Lock activity exit callback
            //we found we always need it to ensure key events will not occasionally drop on the floor from idle state wakeup
            
            
    }
    
    @Override
    public void onRestartCommand() {
    	timeoutenabled = (getSharedPreferences("myLock", 0).getInt("idletime", 0) != 0);
    }
    
    SharedPreferences.OnSharedPreferenceChangeListener prefslisten = new OnSharedPreferenceChangeListener () {
    	@Override
    	public void onSharedPreferenceChanged (SharedPreferences sharedPreference, String key) {
    		Log.v("pref change","the changed key is " + key);
    		
      		if ("FG".equals(key)) {
    			boolean fgpref = sharedPreference.getBoolean(key, false);
    			if(!fgpref && persistent) {
    				stopForeground(true);//kills the ongoing notif
    			    persistent = false;
    			}
    			else if (fgpref && !persistent) doFGstart();//so FG mode is started again
      		}
    		}
    	};
    	
    BroadcastReceiver goidle = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			if (!intent.getAction().equals("i4nc4mp.myLock.KILL_ADVANCED")) return;
			//custom handling of the idle close
			//we would also need to execute exactly this if being closed by toggler plugin interface
			closing = true;
			closeLockscreen(context);
		}
    	
    };
    
    private void handleLockEvent(boolean newstate) {
    	if (newstate) {
    		if (!PendingLock) Log.v("lock start callback","did not expect this call");
    		else PendingLock = false;
        
    		Log.v("lock start callback","Lock Activity is primed");                
                            
    		if (timeoutenabled) {
    			Log.v("idle lock","starting timer");
    			AdvancedIdleTimer.start(getApplicationContext());
        		}
    		}
    	else {
    		if (shouldLock) Log.v("lock exit callback","did not expect this call"); 
            else shouldLock = true;
                            
            Log.v("lock exit callback","Lock Activity is finished");
                                                                            
            if (timeoutenabled) {
            	AdvancedIdleTimer.cancel(getApplicationContext());
            	
            	if (closing) {
            		//this also needs to be used to toggle off via background condition
            		//FIXME
            		//TODO
            		ManageSecurity.enableSecurity(getApplicationContext());
            		ManageKeyguard.reenableKeyguard();
            		stopSelf();
            		UserPresentService.launch(getApplicationContext()); 
            		
            	}
            	else {

            		ManageKeyguard.exitKeyguardSecurely(new LaunchOnKeyguardExit() {
          		  
        	        public void LaunchOnKeyguardExitSuccess() {
        	           ManageKeyguard.reenableKeyguard();
        	        	}
        			});
            	}
            }
    	}
    }
    
    protected void tryLock() {
       	
    	//our thread will essentially wait for the start of lock activity
    	//there is a chance the KG is never detected due to sleep within dock app
    	//so we wait 10 seconds before assuming that is the case
    	new Thread() {

    	public void run() {
    		Context mCon = getApplicationContext();
    		do {	
    		try {
        			Thread.sleep(500);} catch (InterruptedException e) {
        			}
        			if (waited == 0) Log.v("tryLock thread","beginning KG check cycle");
                    if (!PendingLock) {
                    	Log.v("startLock user abort","detected wakeup before lock started");
                    	waited = 0;
                    	return;
                    //ensures break the attempt cycle if user has aborted the lock
                    //on incredible there is no grace period on timeout sleep, this case doesn't occur
                    }
                    
                    if (waited == 20) {
                    	Log.v("startLock abort","system or app seems to be suppressing lockdown");
                    	waited = 0;
                    	PendingLock = false;
                    	return;
                    }
                                        
                    //see if any keyguard exists yet
                            ManageKeyguard.initialize(mCon);
                            if (ManageKeyguard.inKeyguardRestrictedInputMode()) {                            	
                                    shouldLock = false;//set the state of waiting for lock start success
                                    waited = 0;
                                    StartLock(mCon);//take over the lock
                            }
                            else waited++;
                            	
                            
        		//myHandler.sendMessage(Message.obtain(myHandler, 2));
    		} while (shouldLock && PendingLock);
    	}
    	       }.start();
    	    }
    
    @Override
    public void onScreenWakeup() {
    	if (!shouldLock) {
        	//lock activity is active so let's populate the screen wake awareness
        	SharedPreferences settings = getSharedPreferences("myLock", 0);
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean("screen", true);
            editor.commit();
        }    
    	
    	
    	if (!PendingLock) return;
            //we only handle this when we get a screen on that's happening while we are waiting for a lockscreen start callback
                    
                            
            //This case comes in two scenarios
            //Known bug (seems to be fixed)--- the start of LockActivity was delayed to screen on due to CPU load
            //This was fixed by always holding a partial wake lock. cpu decides to sleep sometimes, causing havoc
            
            //other possible case is just User-Abort of timeout sleep by any key during 5 sec unguarded interim
                                                                                            
                    PendingLock = false;
                    if (!shouldLock) {
                            //this is the case that the lockscreen still hasn't sent us a start callback at time of this screen on
                            shouldLock = true;
                    }
                            
                    
            return;
    }
    
    
    @Override
    public void onScreenSleep() {
            //when sleep after an unlock, start the lockscreen again
    	SharedPreferences settings = getSharedPreferences("myLock", 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("screen", false);
        editor.commit();
        //always populate our screen state ref for lock activity to check
        //we only switch the flag on during the lock activity life cycle    
    	
    	
            if (receivingcall || placingcall) {
                    Log.v("mediator screen off","call flag in progress, aborting handling");
                    return;//don't handle during calls at all
            }
            
            if (shouldLock) {
            PendingLock = true;
            //means trying to start lock (waiting for start callback from activity)
    
                                   
            Log.v("mediator screen off","sleep - starting check for keyguard");

            //serviceHandler.postDelayed(myTask, 500L);
            tryLock();
            }
    
            
            return;//prevents unresponsive broadcast error
    }
    
    private void StartLock(Context context) {
            
            Intent closeDialogs = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
                    context.sendBroadcast(closeDialogs);
            
                    
            if (timeoutenabled) ManageKeyguard.disableKeyguard(getApplicationContext());
 
                   

            /* launch UI, explicitly stating that this is not due to user action
                     * so that the current app's notification management is not disturbed */
                    Intent lockscreen = new Intent(context, UnguardActivity.class);
                    
                    
                  //new task required for our service activity start to succeed. exception otherwise
                    lockscreen.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
                   //without this flag my alarm clock only buzzes once.
                           
                    
                            //| Intent.FLAG_ACTIVITY_NO_HISTORY
                            //this flag will tell OS to always finish the activity when user leaves it
                            //when this was on, it was exiting every time it got created. interesting unexpected behavior
                            //even happening when i wait 4 seconds to create it.
                            //| Intent.FLAG_ACTIVITY_NO_ANIMATION)
                            //because we don't need to animate... O_o doesn't really seem to be for this
                    
                    context.startActivity(lockscreen);
            }
    
    public static void StartDismiss(Context context) {
            
    	PowerManager myPM = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        myPM.userActivity(SystemClock.uptimeMillis(), false);
    	
    Class w = DismissActivity.class; 
                  
    Intent dismiss = new Intent(context, w);
    dismiss.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK//required for a service to launch activity
                    | Intent.FLAG_ACTIVITY_NO_USER_ACTION//Just helps avoid conflicting with other important notifications
                    | Intent.FLAG_ACTIVITY_NO_HISTORY//Ensures the activity WILL be finished after the one time use
                    | Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    
    context.startActivity(dismiss);
}
    
//============Phone call case handling
    
    //we have many cases where the phone reloads the lockscreen even while screen is awake at call end
    //my testing shows it actually comes back after any timeout sleep plus 5 sec grace period
    //then phone is doing a KM disable command at re-wake. and restoring at call end
    //that restore is what we intercept in these events as well as certain treatment based on lock activity lifecycle
    
    @Override
    public void onCallStart() {
            
            if (!shouldLock) {
            //if our lock activity is alive, send broadcast to close it
            
            //this case we will also flag to restart lock at call end
            //callWake = true;
            
            Intent intent = new Intent("i4nc4mp.myLock.lifecycle.CALL_START");
            getApplicationContext().sendBroadcast(intent);
            //FIXME is there a way to do this with a class method now with inner class activity?
            }
            else shouldLock = false;
    }
    
    @Override
    public void onCallEnd() {
            //all timeout sleep causes KG to visibly restore after the 5 sec grace period
            //the phone appears to be doing a KM disable to pause it should user wake up again, and then re-enables at call end
            
            //if call ends while asleep and not in the KG-restored mode (watching for prox wake)
            //then KG is still restored, and we can't catch it due to timing
                        
            Context mCon = getApplicationContext();
            
            Log.v("call end","checking if we need to exit KG");
            
            ManageKeyguard.initialize(mCon);
            
            boolean KG = ManageKeyguard.inKeyguardRestrictedInputMode();
            //this will tell us if the phone ever restored the keyguard
            //phone occasionally brings it back to life but suppresses it
            
            boolean screen = isScreenOn();
            
            if (!screen) {
            	//asleep case, only detected on 2.1+
            	
            	Log.v("asleep call end","restarting lock activity.");
                PendingLock = true;
                StartLock(mCon);
            }
            else {
            	//awake or pre-2.1 (causing wakeup if asleep & locked)
            	shouldLock = true;
            	if (KG) StartDismiss(mCon);
            }
    }
    
    @Override
    public void onCallRing() {  	
    	Intent intent = new Intent("i4nc4mp.myLock.lifecycle.CALL_PENDING");
        getApplicationContext().sendBroadcast(intent);
        //lets the activity know it should not treat focus loss as a navigation exit
        //this will keep activity alive, only stopping it at call start
    }
    
    public boolean isScreenOn() {
    	//Allows us to tap into the 2.1 screen check if available
    	
    	if(Integer.parseInt(Build.VERSION.SDK) < 7) { 
    		
    		return true;
    		//our own isAwake doesn't work sometimes when prox sensor shut screen off
    		//better to treat as awake then to think we are awake when actually asleep
    		
    	}
    	else {
    		PowerManager myPM = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
    		return myPM.isScreenOn();
    	}
    }
    
//============================
    
    void doFGstart() {
            //putting ongoing notif together for start foreground
            
            //String ns = Context.NOTIFICATION_SERVICE;
            //NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
            //No need to get the mgr, since we aren't manually sending this for FG mode.
            
            int icon = R.drawable.icone;
            CharSequence tickerText = "myLock is starting up";
            
            long when = System.currentTimeMillis();

            Notification notification = new Notification(icon, tickerText, when);
            
            Context context = getApplicationContext();
            CharSequence contentTitle = "myLock - click to open settings";
            CharSequence contentText = "lockscreen is disabled";

            Intent notificationIntent = new Intent(this, SettingsActivity.class);
            PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

            notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
            
            final int SVC_ID = 1;
            
            //don't need to pass notif because startForeground will do it
            //mNotificationManager.notify(SVC_ID, notification);
            persistent = true;
            
            startForeground(SVC_ID, notification);
    }
    
    //The activity is wrapped by the mediator so we can interact with a static handler instance
    //The activity can be dismissed via back button
    //User configures what keys if any will fully auto unlock
    public static class UnguardActivity extends Activity {

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

    	public boolean resurrected = false;
    	//just to handle return from dormant, avoid treating it same as a user initiated wake

    	private int lastkeycode = 0;
    	    
    	protected void onCreate(Bundle icicle) {
            super.onCreate(icicle);
            
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
                    
            setContentView(R.layout.unguard);
            //cool translucent overlay that shows what's behind
            
            IntentFilter onfilter = new IntentFilter (Intent.ACTION_SCREEN_ON);
            registerReceiver(screenon, onfilter);
    
    IntentFilter callbegin = new IntentFilter ("i4nc4mp.myLock.lifecycle.CALL_START");
    registerReceiver(callStarted, callbegin);  
    
    IntentFilter callpend = new IntentFilter ("i4nc4mp.myLock.lifecycle.CALL_PENDING");
    registerReceiver(callPending, callpend);
    
    SharedPreferences settings = getSharedPreferences("myLock", 0);
    slideGuard = settings.getBoolean("slideGuard", false);
           
            
        
            }    

    	BroadcastReceiver screenon = new BroadcastReceiver() {
    	            
    	    public static final String Screenon = "android.intent.action.SCREEN_ON";

    	    @Override
    	    public void onReceive(Context context, Intent intent) {
    	            if (!intent.getAction().equals(Screenon) || finishing) return;
    	            Log.v("guard is handling wakeup","deciding whether to dismiss");
    	            
    	            if (resurrected) {
	                	//ignore this wake as we do not actually want instant exit
	                	resurrected = false;
	                	Log.v("guard resurrected","ignoring invalid screen on");
	                }
    	            else if (hasWindowFocus()) {             	
    	            	
    	            	if (slideWakeup && slideGuard){
    	            		//HERE is where we need to call re-enable.. but should do it after exiting
    	            		Log.v("slide wake lockdown", "re enabling kg");
    	            	    //exit on its own just leads to unlocked state
    	            	}
    	            	//TODO add a haptic notif so user might know if this pocket wake happened
    	            	
    	            	//we don't call finish via wake like the show when locked moed
    	            	//we use the key events and other events to decide if unlock or lockdown should occur
    	            	
    	            	if (slideWakeup && !slideGuard) {
    	            		
	            	           Log.v("slide wake exit", "closing the guard window");
	            	           
	            	           finishing = true;
	            	           finish();
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
    	    
    	    
    	    @Override
    	public void onConfigurationChanged(Configuration newConfig) {
    	    super.onConfigurationChanged(newConfig);
    	    if (newConfig.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO) {
    	            //this means that a config change happened and the keyboard is open.
    	            if (starting) Log.v("slide-open lock","aborting handling, slide was opened before this lock");
    	            else {
    	            	Log.v("slide-open wake","setting state flag");
    	            	slideWakeup = true;    	            	
    	            }           
    	    }
    	    else if (newConfig.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_YES) {
    	    	Log.v("slide closed","lockscreen activity got the config change from background");
    	    }
    	                  
    	}

    	    @Override
    	protected void onStop() {
    	    super.onStop();
    	    
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
    	        else {
    	            //we got paused, lost focus, then finally stopped
    	            //this only happens if user is navigating out via notif, popup, or home key shortcuts
    	            Log.v("lock stop","onStop is telling mediator we have been unlocked by user navigation");
    	            finishing = true;
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
    	    
    	    if (!starting && !hasWindowFocus()) {
    	            //case: we yielded focus to something but didn't pause. Example: notif panel
    	            //pause in this instance means something else is launching, that is about to try to stop us
    	            //so we need to exit now, as it is a user nav, not a dormancy event
    	            Log.v("navigation exit","got paused without focus, starting dismiss sequence");
    	            
    	               StopCallback();
    	               finish();
    	                  	            
    	    }
    	    else {
    	    	if (hasWindowFocus()) Log.v("lock paused","normal pause - we still have focus");
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
    	      	lastkeycode = 0;
    	      	//don't remember the last key through sleep
    	      	//we only care from resume to pause
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
    	                	   
    	   unregisterReceiver(screenon);
    	   unregisterReceiver(callStarted);
    	   unregisterReceiver(callPending);
    	    
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
    	            }
    	    }
    	    else if (!finishing && paused) {
    	//Handcent popup issue-- we haven't gotten resume & screen on yet
    	//Handcent is taking focus first thing
    	//So it is now behaving like an open of notif panel where we aren't stopped and aren't even getting paused
    	      	  
    	     //we really need to know we were just resumed and had screen come on to do this handoff
    	                        
    	                        if (dormant) Log.v("dormant handoff complete","the external event now has focus");
    	                        else if (checkScreen()) {
    	                      		  //Here's the handcent case
    	                      		  //if you then exit via a link on pop,
    	                      		  //we do get the user nav handling on onStop
    	                      		  Log.v("popup event","focus handoff before screen on, nav exit possible");
    	                      		  dormant = true;                            
    	                      		  //we need to be dormant so we realize once the popup goes away
    	                      	  }
    	                        
    	        	}
    	            else if (!paused) {
    	                    //not paused, losing focus
    	                    Log.v("focus yielded while active","about to exit through notif nav");
    	                    pendingExit = true;
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
    		if (myHandler != null) myHandler.sendMessage(Message.obtain(myHandler, 0));
    	}

    	public void StopCallback() {
    		if (myHandler != null) myHandler.sendMessage(Message.obtain(myHandler, 1));
    		else {
    			Log.v("lock stop","no handler, assuming idle shutdown");
    			//ManageKeyguard.reenableKeyguard();
    		}
    	}

    	public boolean checkScreen() {
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
    	
    
    
  //here's where most of the magic happens
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
   	
        
    	//boolean mult = event.getAction() == KeyEvent.ACTION_MULTIPLE;
        //int repeat = event.getRepeatCount();
        //Log.v("key event", "repeat is " + repeat);
        //if (!mult && repeat != 0) mult = true;
        //multiple is not actually a double press, it is when a key is held
        
        boolean up = event.getAction() == KeyEvent.ACTION_UP;
        //flags to true if the event we are getting is the up (release)
        //when we are coming from sleep, the pwr down gets taken by power manager to cause wakeup
        //if we are awake already the power up might also get taken.
        //however even from sleep we get a down and an up for focus & cam keys with a full press
        
        int code = event.getKeyCode();
        //Log.v("dispatching a key event","Is this the up? -" + up);
        
       //TODO implement pref-checking method to see if any advanced power saved keys are set
        //and also let user define what keys auto unlock
        
               
      //if (code == KeyEvent.KEYCODE_FOCUS) reaction = 0; else//locked (advanced power save)
        //locked method we want doesn't work on OLED/ htc incredible/nexus

        int reaction = 0;//wakeup, the preferred behavior in advanced mode
        if (code == KeyEvent.KEYCODE_POWER
        		|| code == KeyEvent.KEYCODE_BACK
        		|| code == KeyEvent.KEYCODE_MENU
        		|| code == KeyEvent.KEYCODE_SEARCH
        		|| code == lastkeycode)//basic detection of 2nd press of same key
        	reaction = 1;
        //event.getFlags()==KeyEvent.FLAG_VIRTUAL_HARD_KEY
        //hard keys - these only come through while "Awake" since touchscreen is disabled otherwise
        //this isn't working on device.. i don't know why.
        //will have to actually specify the menu, back, and search codes
        
        //if (event.getFlags()==KeyEvent.FLAG_WOKE_HERE) return true;
        //we don't want to handle the wake as that always comes from down
        //doesn't seem to work as expected
        
        boolean unlock = (reaction == 1);
        if (up) lastkeycode = code;//at up take note of what key it is
        
        //following is replaced by a switch for other reactions once advanced features are implemented
        
    	   if (unlock && !finishing) {
    		   Log.v("unlock key","closing");
    		   finishing = true;
    	  	  	
    		   //setBright((float) 0.1);
    		       		       		  
    		   moveTaskToBack(true);
    		  return true;
    	   }
    	   
    	   return true;
    	   //we don't pass any keys along
    	   //we do not perform any action except unlock
    	   
    /* * advanced power save handling 
     * 
     *      * -- decide whether a regular wake or locked/screen off wake should happen
     * switch (reaction) {
        	case 2:
    	   if (up && !screenwake) {
                   //waking = true;
                  	Log.v("key event","wake key");
               	//wakeup();
                  	screenwake = true;
    	   }
    	   return true;
    	   //ensures that a wakeup will be handled properly if sleep occurs again
       
        * advanced power save handling
        case 0:    	   
    	   if (!screenwake && up) {
         	   timeleft=10;
         	//so that countdown is refreshed
            //countdown won't be running in screenwakes
         	if (!waking) {
            //start up the quiet wake timer    
             Log.v("key event","locked key timer starting");

             	waking = true;
             	serviceHandler.postDelayed(myTask, 500L);
             		}
            }
             
             
    	   return true;*/
       
    }
}
}
