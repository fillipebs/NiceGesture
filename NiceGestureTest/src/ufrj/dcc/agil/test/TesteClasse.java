package ufrj.dcc.agil.test;

import java.util.List;

import ufrj.dcc.agil.MoveGestureActivity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.test.ActivityInstrumentationTestCase2;

public class TesteClasse extends
		ActivityInstrumentationTestCase2<MoveGestureActivity> {

	MoveGestureActivity demo;
	ActivityManager manager;

	public TesteClasse() {
		super("ufrj.dcc.agil", MoveGestureActivity.class);
	}

	@Override
	protected void setUp() throws Exception {
		demo = this.getActivity();
		manager = (ActivityManager) demo
				.getSystemService(Context.ACTIVITY_SERVICE);

	}

	public void testNomeNossaAplicacao() throws NameNotFoundException {
		PackageManager packageManager = demo.getPackageManager();
		RunningAppProcessInfo process = getProcessoVisivel();
		

		String processLabel = (packageManager
				.getApplicationLabel(packageManager.getApplicationInfo(
						process.processName, PackageManager.GET_META_DATA)))
				.toString();
		assertEquals("Calculator", processLabel);
	}

	private RunningAppProcessInfo getProcessoVisivel() {
		List<ActivityManager.RunningAppProcessInfo> processes = manager
				.getRunningAppProcesses();

		for (ActivityManager.RunningAppProcessInfo process : processes) {
			// Take a look at the IMPORTANCE_VISIBLE property as well in the
			// link provided at the bottom
			if (process.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
				return process;
			}
		}

		return null;
	}

}
