package ufrj.dcc.agil.test;

import ufrj.dcc.agil.MoveGestureActivity;
import android.test.ActivityInstrumentationTestCase2;

public class TesteClasse extends ActivityInstrumentationTestCase2<MoveGestureActivity> {

	
	MoveGestureActivity demo;
	
	public TesteClasse() {
		super("ufrj.dcc.agil", MoveGestureActivity.class);
	}
	
	@Override
	protected void setUp() throws Exception {
		demo = this.getActivity();
		
	}
	
	public void testNomeAplicacao() {
		assertEquals(demo.a(),0);
	}

	
}
