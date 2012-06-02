package ufrj.dcc.agil;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

public class MoveGestureActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        final Button button = (Button) findViewById(R.id.button1);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	 abreAplicativo("com.android.calculator2", "com.android.calculator2.Calculator");
            	 
            }	
        });    
    }
    
    public void abreAplicativo(String caminho, String caminhoNome) {
		Intent i = new Intent();
         i.setClassName(caminho,caminhoNome);
         startActivity(i);
	}
}