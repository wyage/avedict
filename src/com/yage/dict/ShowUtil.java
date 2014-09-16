package com.yage.dict;

import android.app.AlertDialog;
import android.content.Context;
import android.widget.Toast;

/**
 * 用来显示释义
 * @author Administrator
 * @since 2014-7-27 19:32:28
 */
public class ShowUtil {
	//在对话框中显示单词释义
	public static void showWord(Context ctx,boolean opennew,String word,String exp){
		if(!opennew){
			new AlertDialog.Builder(ctx).setMessage(exp).setTitle(word).show();
		}else{
			Toast.makeText(ctx, "will open in new activity, not implemented yet", Toast.LENGTH_LONG).show();
		}
	}
}
