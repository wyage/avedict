package com.yage.dict;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.yage.dict.R.id;
import com.yage.dict.db.DatabaseHelper;
import com.yage.dict.entity.FavoriteWord;
import org.yage.dict.star.WordPosition;

import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 用来显示【历史】和【收藏】
 * @author voyage
 * @since 2014-1-11 23:02:12
 */
public class FavoriteActivity extends Activity {

	//显示单词的类型是否是【收藏】，否则是【历史】
	public static final String TYPE_IS_FAVORITE="is_type_favorite";
	
	//是否要显示收藏，还是历史
	private boolean isFavorite;
	
	//列表控件
	private ListView wordList;

	//绑定列表
	private SimpleAdapter adapter;

	//数据库访问工具
	private DatabaseHelper databaseHelper;
	
	//在单词上长按时显示的对话框
	private AlertDialog wordActionMenuDialog;
	
	//上一个点击的单词的序号
	private int lastClickedIndex;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_favorite);
		this.isFavorite=this.getIntent().getExtras().getBoolean(FavoriteActivity.TYPE_IS_FAVORITE);
		Log.d("oncreate", "isfavorite:"+this.isFavorite);
		if(!this.isFavorite){
			this.setTitle(R.string.title_activity_history);
		}
		
		Log.d("oncreate", "will initialize databasehelper via:"+this.getApplicationContext());
		this.databaseHelper=new DatabaseHelper(this.getApplicationContext());
		
		List<FavoriteWord> favs=null;
		if(this.isFavorite){
			favs=this.databaseHelper.getAllFavoriteWord();
		}else{
			favs=this.databaseHelper.getAllHistoryWord();
		}
		
		List<Map<String,Object>> data=new ArrayList<Map<String,Object>>();
		for(FavoriteWord fw:favs){
			Log.d("favoriteactivity oncreate", fw.toString());
			Map<String,Object> item=new HashMap<String,Object>();
			item.put("tword", fw.getWord());
			item.put("id", fw.getId());
			item.put("addtime", DatabaseHelper.dateFormat.format(fw.getAddTime()));
			data.add(item);
		}
		this.wordList=(ListView)this.findViewById(R.id.favorite_list_words);
		this.adapter=new SimpleAdapter(this,data,R.layout.word_item,new String[]{"tword","addtime"},new int[]{R.id.tview_word,R.id.tview_length});
		
		this.wordList.setClickable(true);
		this.wordList.setFocusable(true);
		this.wordList.setItemsCanFocus(true);
		this.wordList.setFocusableInTouchMode(true);
		this.wordList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		this.wordList.setAdapter(adapter);
		
		//下面绑定列表的响应事件
		//点击列表事件
		this.wordList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long rowid) {
				handleListItemClick(parent,view,false);
			}
		});
		
		//使其本身也可响应长按
		this.wordList.setLongClickable(true);
		
		//长按列表事件
		this.wordList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long rowid) {
				lastClickedIndex=position;
				handleListItemClick(parent,view,true);
				//必须返回true以使不至于触发itemclick
				return true;
			}
		});
		
		this.wordActionMenuDialog=new AlertDialog.Builder(this).setTitle(R.string.action_long_label).setItems(R.array.favorite_long_press_actions,
				new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch(which){
				case 0:
					//代表删除项
					@SuppressWarnings("unchecked")
					Map<String,Object> it=(Map<String, Object>) adapter.getItem(lastClickedIndex);
					String xword=it.get("tword").toString();
					long id=(Long) it.get("id");
					int res=-1;
					if(isFavorite){
						res=databaseHelper.deleteFavoriteWord(id);
					}else{
						res=databaseHelper.deleteHistoryRecord(id);
					}
					Log.d("action clicked", "delete"+xword+", id:"+id+",res:"+res);
					Toast.makeText(getApplicationContext(), "删除"+(isFavorite?"收藏":"历史")+":"+(res>0?"成功":"失败")+":"+xword, Toast.LENGTH_SHORT).show();
					if(res>0){
						//成功之后显示列表
						fillWordList();
					}
					break;
				}
			}
		}).create();
	}
	
	/**
	 * 显示列表
	 */
	private void fillWordList(){
		List<FavoriteWord> favs=null;
		if(this.isFavorite){
			favs=this.databaseHelper.getAllFavoriteWord();
		}else{
			favs=this.databaseHelper.getAllHistoryWord();
		}
		
		List<Map<String,Object>> data=new ArrayList<Map<String,Object>>();
		for(FavoriteWord fw:favs){
			Log.d("favoriteactivity oncreate", fw.toString());
			Map<String,Object> item=new HashMap<String,Object>();
			item.put("tword", fw.getWord());
			item.put("id", fw.getId());
			item.put("addtime", DatabaseHelper.dateFormat.format(fw.getAddTime()));
			data.add(item);
		}
		this.adapter=new SimpleAdapter(this,data,R.layout.word_item,new String[]{"tword","addtime"},new int[]{R.id.tview_word,R.id.tview_length});
		this.wordList.setAdapter(adapter);
	}
	/**
	 * 把长按和短按的处理函数写在一起
	 * @param parent
	 * @param view 当前被点击对象
	 * @param islong 是否是长按
	 */
	private void handleListItemClick(AdapterView<?> parent,View view,boolean islong){
		TextView wordview=null;
		if(view instanceof TextView){
			//在模拟器上4.x版本，事件的对象总是TextView之一
			wordview=(TextView)view;
			if(wordview.getId()!=id.tview_word){
				View v=parent.findViewById(id.tview_word);
				if(v instanceof TextView){
					wordview=(TextView) v;
				}
				Log.d("list long click", "get by parent:"+v.getId()+","+wordview.getText().toString());
			}else{
				Log.d("list long click", "is tview_word.id "+wordview.getText().toString()+", id:"+wordview.getId());
			}
		}else if(view instanceof LinearLayout){
			//在实际手机2.3版本上，事件对象总是LinearLayout
			wordview=(TextView)view.findViewById(id.tview_word);
		}else{
			Toast.makeText(getApplicationContext(), "unknownview", Toast.LENGTH_SHORT).show();
		}
		
		String tword=wordview.getText().toString();
		WordPosition wp=MainActivity.readDict.getWords().get(tword);
		if(wp==null){
			Toast.makeText(this, "未预料的情况，没有找到单词:"+tword, Toast.LENGTH_SHORT).show();
			return;
		}
		String strexp=MainActivity.readDict.getWordExplanation(wp.getStartPos(), wp.getLength());
		if(islong){
			//长按显示菜单
			this.wordActionMenuDialog.show();
		}else{
			//显示释义
			ShowUtil.showWord(this, false, tword, strexp);
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.favorite, menu);
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

}
