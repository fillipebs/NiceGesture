package ufrj.dcc.agil.test;

import java.util.Iterator;
import java.util.List;

import ufrj.dcc.agil.MoveGestureActivity;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

public class TesteClasse extends ActivityInstrumentationTestCase2<MoveGestureActivity> {

	
	MoveGestureActivity demo;
	
	public TesteClasse() {
		super("ufrj.dcc.agil", MoveGestureActivity.class);
	}
	
	@Override
	protected void setUp() throws Exception {
		demo = this.getActivity();
		
	}
	
	public void testNomeNossaAplicacao() {
		ActivityManager am = (ActivityManager)demo.getSystemService(demo.ACTIVITY_SERVICE);
		List l = am.getRunningAppProcesses();
		PackageManager pm = demo.getPackageManager();
		ActivityManager.RunningAppProcessInfo info2 = (ActivityManager.RunningAppProcessInfo)l.get(0);
		try {
			String nomeAplicativo = pm.getApplicationLabel(pm.getApplicationInfo(info2.processName, PackageManager.GET_META_DATA)).toString();
			assertEquals(nomeAplicativo,"NiceGesture");
		} catch (NameNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	public void testAbreAplicacao() {
		//demo.abreAplicativo("com.android.calculator2", "com.android.calculator2.Calculator");
		/*ActivityManager am = (ActivityManager)demo.getSystemService(demo.ACTIVITY_SERVICE);
		List l = am.getRunningAppProcesses();
		Iterator i = l.iterator();
		PackageManager pm = demo.getPackageManager();
		ActivityManager.RunningAppProcessInfo info2 = (ActivityManager.RunningAppProcessInfo)l.get(0);
		
		try {
			String nomeAplicativo = pm.getApplicationLabel(pm.getApplicationInfo(info2.processName, PackageManager.GET_META_DATA)).toString();
			Log.e("NOME", nomeAplicativo);
			//assertEquals(nomeAplicativo,"NiceGesture");
		} catch (NameNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		while(i.hasNext()){
			ActivityManager.RunningAppProcessInfo info3 = (ActivityManager.RunningAppProcessInfo)(i.next());
			try {
				String nomeAplicativo2 = pm.getApplicationLabel(pm.getApplicationInfo(info3.processName, PackageManager.GET_META_DATA)).toString();
				Log.e("lista", nomeAplicativo2);
			} catch (NameNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}*/
	}
	
}
