package i4nc4mp.myLock;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

//what we need is when this is launched, a short handler is spawned, and the checkbox for service status
//is replaced by a little "refreshing" spin-wheel until the status of mediator is determined

public class MainPreferenceActivity extends PreferenceActivity {
	private SharedPreferences myprefs;

	private boolean security = false;

	private boolean enabled = false;

	private boolean active = false;

	Handler serviceHandler;
	Task verifyBindTask = new Task();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.mainpref);
		// instead of a layout we set ourselves to a pref tree

		getPreferenceManager().setSharedPreferencesName("myLock");
		// tell the prefs persisted to go in the already existing pref file

		serviceHandler = new Handler();

		// Next, we have to set some things up just like we did in the old
		// settings activity
		// we use findPreference instead of findViewById

		final CheckBoxPreference toggle = (CheckBoxPreference) findPreference("enabled");
		if (toggle == null)
			Log.e("pref activity", "didn't find toggle item");
		else {
			toggle.setOnPreferenceClickListener(new OnPreferenceClickListener() {

				@Override
				public boolean onPreferenceClick(Preference preference) {
					if (preference.getKey().equals("enabled")) {
						Context mCon = getApplicationContext();

						// using enabled flag here caused a failure when turning
						// off then back on
						// within the same visible lifecycle of the pref screen
						// this is because we never update enabled except at
						// start
						if (toggle.isChecked()) {
							ManageMediator.startService(mCon);
							findPreference("enabled")
									.setTitle(R.string.enabled);
						} else {
							ManageMediator.stopService(mCon);
							findPreference("enabled").setTitle(
									R.string.disabled);
						}

						return true;
					} else
						return false;

				};
			});

			/*
			 * toggle.setOnPreferenceChangeListener(new
			 * OnPreferenceChangeListener() {
			 * 
			 * @Override public boolean onPreferenceChange(Preference
			 * preference, Object newValue) { Boolean v = (Boolean) newValue;
			 * Log.v("changed status","pref is now " + v);
			 * 
			 * CharSequence s = "myLock "; if (v) s = s + "Enabled"; else s = s
			 * + "Disabled";
			 * 
			 * 
			 * preference.setTitle(s); return true; } });
			 */

		}

		ListPreference mPref = (ListPreference) findPreference("mode");
		if (mPref == null)
			Log.e("pref activity", "didn't find mode item");
		else {
			mPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

				@Override
				public boolean onPreferenceChange(Preference preference,
						Object newValue) {
					String v = (String) newValue;
					Log.v("changed mode", "the new mode is " + v);

					int n = Integer.parseInt(v);

					updateMode(n);
					return true;
				}
			});
		}

	}

	@Override
	protected void onStart() {
		super.onStart();

		getPrefs();
	}

	private void getPrefs() {
		// we need to show the user's existing prefs and get status for service
		// & security
		myprefs = getSharedPreferences("myLock", 0);

		// These 2 and the mode will go by strict pref reading
		((CheckBoxPreference) findPreference("FG")).setChecked(myprefs
				.getBoolean("FG", false));
		((CheckBoxPreference) findPreference("slideGuard")).setChecked(myprefs
				.getBoolean("slideGuard", false));

		int m = 4;
		try {
			m = Integer.parseInt(myprefs.getString("mode", "0"));
		} catch (NumberFormatException x) {
			// I don't care
			m = 4;
		}

		if (m != 4)
			updateMode(m);

		// Security and Service statuses need extra handling
		enabled = myprefs.getBoolean("enabled", false);

		if (enabled) {
			// verify that the service is active, we will get true if we held
			// the bind as expected
			active = ManageMediator.bind(getApplicationContext());

			// necessary to show the pref state while active, since we suppress
			// system pref
			if (active)
				updateStatus(true);
			else
				serviceHandler.postDelayed(verifyBindTask, 100L);
			// we have to wait then doublecheck to allow time for bind to
			// execute
		} else
			updateStatus(false);
	}

	private boolean getPatternSetting() {
		int patternsetting = 0;

		try {
			patternsetting = android.provider.Settings.System.getInt(
					getContentResolver(),
					android.provider.Settings.System.LOCK_PATTERN_ENABLED);
		} catch (SettingNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		boolean s = (patternsetting == 1);

		return s;
	}

	// check security and post status results
	// param is so we can pass the determined service state
	// we have to base security handling on that state
	private void updateStatus(boolean on) {
		if (on) {
			enabled = true;
			security = myprefs.getBoolean("security", false);
		} else {
			enabled = false;
			security = getPatternSetting();
			// find actual system state while not enabled so we can store to
			// pref

			// The pref element only does persistent changes on user click
			// we're just using this pref to display, we set it ourselves here.
			if (security != myprefs.getBoolean("security", false)) {
				SharedPreferences.Editor e = myprefs.edit();
				e.putBoolean("security", security);
				e.commit();

				String hint = new String();

				if (security)
					hint = "Security detected";
				else
					hint = "Security disabled";

				Toast.makeText(MainPreferenceActivity.this, hint,
						Toast.LENGTH_SHORT).show();
			}
		}

		((CheckBoxPreference) findPreference("enabled")).setChecked(enabled);

		if (enabled)
			findPreference("enabled").setTitle(R.string.enabled);
		else
			findPreference("enabled").setTitle(R.string.disabled);

		((CheckBoxPreference) findPreference("security")).setChecked(security);
	}

	private void updateMode(int m) {
		String t = new String();
		String s = new String();

		switch (m) {
		case ManageMediator.MODE_BASIC:
			t = getResources().getString(R.string.basictitle);
			s = getResources().getString(R.string.basicsummary);
			break;
		case ManageMediator.MODE_HIDDEN:
			t = getResources().getString(R.string.hiddentitle);
			s = getResources().getString(R.string.hiddensummary);
			break;
		case ManageMediator.MODE_ADVANCED:
			t = getResources().getString(R.string.unguardtitle);
			s = getResources().getString(R.string.unguardsummary);
			break;
		}
		findPreference("mode").setTitle(t);
		findPreference("mode").setSummary(s);
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
			setup.setClassName("i4nc4mp.myLock", "i4nc4mp.myLock.IdleSetup");
			try {
				startActivity(setup);
			} catch (ActivityNotFoundException e) {
				// Toast.makeText(SettingsActivity.this,
				// "Please download Idle Lock addon", Toast.LENGTH_LONG).show();
				// we don't care anymore - idle setup now included in the final
				// pre-froyo package
			}
			return true;
		}
		return false;
	}

	class Task implements Runnable {
		public void run() {
			// doesn't handle fallout at all of incorrect enabled flag
			// the expected outcome here is that user will check the box,
			// causing start
			// that will correct the pref, but there is no hint given to user
			// widget should show correct state unless it is a newly added
			// widget
			// that might be best place to handle incorrect enabled flag

			updateStatus(ManageMediator.serviceActive(getApplicationContext()));
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		serviceHandler.removeCallbacks(verifyBindTask);

		serviceHandler = null;
	}

}