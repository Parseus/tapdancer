package co.kica.tapdancer;

import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;

import androidx.appcompat.app.AppCompatActivity;

public class HelpActivity extends AppCompatActivity {

	private WebView wv;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
       
        this.setContentView(R.layout.activity_help);
        
        wv = this.findViewById(R.id.webView1);
        wv.loadUrl("file:///android_asset/html/index.html");
        
        wv.clearHistory();
    }
    
    public void clickCloseHelp( View view ) {
    	finish();
    }
	
}
