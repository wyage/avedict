package com.yage.dict;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * ≥Ã–Úµƒ…Ë÷√
 * @author Administrator
 */
public class DictPreferences extends PreferenceActivity {
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		this.addPreferencesFromResource(R.xml.settings);
	}
}
