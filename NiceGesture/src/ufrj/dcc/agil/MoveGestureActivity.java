package ufrj.dcc.agil;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class MoveGestureActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        abreAplicativo("com.android.calculator2", "com.android.calculator2.Calculator");
		// final Button button = (Button) findViewById(R.id.button1);
		// button.setOnClickListener(new View.OnClickListener() {
		// public void onClick(View v) {
		//
		// }
		// });
    }
    
    public void abreAplicativo(String caminho, String caminhoNome) {
		Intent i = new Intent();
         i.setClassName(caminho,caminhoNome);
         startActivity(i);
	}
}