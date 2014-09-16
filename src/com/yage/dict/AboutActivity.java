package com.yage.dict;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;

public class AboutActivity extends Activity {

	private WebView webView;
	
	//显示类型是否是【关于】，否则是【帮助】
	public static final String TYPE_IS_ABOUT="is_type_about_program";
	
	//是否要显示【关于】，还是【帮助】
	private boolean isAbout;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_about);
		this.webView=(WebView)this.findViewById(R.id.webViewAbout);
		this.isAbout=this.getIntent().getExtras().getBoolean(AboutActivity.TYPE_IS_ABOUT);
		initData();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.about, menu);
		return true;
	}
	
	/**
	 * 主菜单响应过程
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		switch(item.getItemId()){
		case R.id.action_goback:
			this.finish();
			return true;
		}
		return false;
	}
	
	private void initData(){
		if(this.isAbout){
			webView.loadUrl("file:///android_asset/doc/about.html");
		}else{
			webView.loadUrl("file:///android_asset/doc/help.html");
			this.setTitle("帮助文档");
		}
	}

}
