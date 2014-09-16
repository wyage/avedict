package com.yage.dict;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.yage.dict.db.DatabaseHelper;
import com.yage.dict.entity.FavoriteWord;
import com.yage.first.dict.ReadDictWord;
import com.yage.first.dict.WordPosition;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.text.ClipboardManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

/**
 * 字典程序，主程序入口
 * modify 2014-7-27 18:57:28
 * @since 2014-1-4 16:58:12
 * @author voyage
 */
@SuppressWarnings("deprecation")
@SuppressLint("HandlerLeak")
public class MainActivity extends Activity {

	//全局变量，保存每次搜索的结果
	private List<Map.Entry<String,WordPosition>> words;
	
	//显示单词列表
	private ListView listView;
	
	//首次加载时显示加载等待
	private ProgressBar progressWait;
	
	//用户输入单词，暂时没用自动建议的功能
	private AutoCompleteTextView txtInput;
	
	//与单词列表绑定的适配器，用来对单词列表进行修改
	private ArrayAdapter<String> adapter;
	
	//词典类，用来加载词典，搜索单词，显示单词等。也供收藏界面调用
	public static ReadDictWord readDict;
	
	//消息常数，指明要显示【单词列表】，并隐藏【等待进度条】
	private static final int SHOW_WORD_LIST=1;
	
	//消息常数，指明加载字典的尝试失败，并隐藏【等待进度条】
	private static final int SHOW_LOAD_DICT_ERROR=2;
	
	private static final int FILE_SELECT_CODE = 0;
	
	//最近一次选择的单词的序号
	private int LAST_SELECTED_WORD_INDEX=-1;
	
	//在单词上长按时显示的对话框
	private AlertDialog wordActionMenuDialog;
	
	//数据库操作工具
	private DatabaseHelper databaseHelper;
	
	//设置值读取
	private SharedPreferences sharedPreference;
	
	//字典文件存放位置，扫描顺序
	private static String[] DICT_LOCATIONS={"dict/oxford","oxford"};
	
	//以下面为键分别保存idx文件和dict文件的路径
	private static final String SETTING_IDX_FILE_PATH_KEY="idx_file_path";
	private static final String SETTING_DICT_FILE_PATH_KEY="dict_file_path";
	
	//消息处理辅助类
	private Handler mHandler=new Handler(){
		public void handleMessage(Message msg){
			switch(msg.what){
				case SHOW_WORD_LIST:
					//已经数据加载完毕，此时显示单词列表，隐去进度条
					progressWait.setVisibility(View.INVISIBLE);
					listView.setVisibility(View.VISIBLE);
					pickUpRandomWord();
					refreshWordList();
					break;
				case SHOW_LOAD_DICT_ERROR:
					//字典加载失败
					progressWait.setVisibility(View.INVISIBLE);
					listView.setVisibility(View.VISIBLE);
					//下面手动选择字典
					showFileChooser();
					break;
				default:
					break;
			}
			super.handleMessage(msg);
		}
	};
	
	private void showFileChooser(){
    	Intent intent=new Intent(Intent.ACTION_GET_CONTENT);
    	intent.setType("*/*");
    	intent.addCategory(Intent.CATEGORY_OPENABLE);
    	try{
    		this.startActivityForResult(Intent.createChooser(intent, "请选择idx或dict文件"), FILE_SELECT_CODE);
    	}catch(ActivityNotFoundException ane){
    		Toast.makeText(getApplicationContext(), "你竟然没有装个文件管理器，程序彻底没法用了，太不像话了！", Toast.LENGTH_LONG).show();
    	}
    }
	
	//选择事件返回
	public void onActivityResult(int requestcode,int resultCode,Intent data){
    	if(resultCode==Activity.RESULT_OK){
    		String strerr="";
    		Uri uri=data.getData();
    		String url=uri.getPath();
    	    Log.d("file", "selected:"+url);
    	    Log.d("file", "selected:"+data.getDataString());
    		if(url.endsWith("idx")){
    			File fdict=new File(url.substring(0, url.length()-4)+".dict");
    			Log.d("file", "seek for:"+fdict.getAbsolutePath());
    			if(!fdict.exists()){
    				strerr="缺少对应的dict文件，请保证dict和idx文件同时存在，例如[oxford.dict]和[oxford.idx]";
    			}else{
    				readDict.loadIndexFile(url);
    				if(readDict.loadContentFile(fdict.getAbsolutePath())){
    					saveDictFileInfo(url,fdict.getAbsolutePath());
    				}
    			}
    		}else if(url.endsWith("dict")){
    			File fidx=new File(url.substring(0, url.length()-5)+".idx");
    			Log.d("file", "seek for:"+fidx.getAbsolutePath());
    			if(!fidx.exists()){
    				strerr="缺少对应的idx文件，请保证dict和idx文件同时存在，例如[oxford.dict]和[oxford.idx]";
    			}else{
    				readDict.loadIndexFile(fidx.getAbsolutePath());
    				if(readDict.loadContentFile(url)){
    					saveDictFileInfo(fidx.getAbsolutePath(),url);
    				}
    			}
    		}else{
    			strerr="所选文件既不是dict又不是idx文件，请保证dict和idx文件同时存在，例如[oxford.dict]和[oxford.idx]:"+url;
    		}
    		
    		if(strerr.length()>0){
    			new AlertDialog.Builder(MainActivity.this)
                .setTitle("请重新选择")
                .setMessage(strerr)
                .setPositiveButton("确定",
                    new DialogInterface.OnClickListener() {  
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        	showFileChooser();
                        }
                    }).setNegativeButton("取消", null).create().show();
    		}else{
    			pickUpRandomWord();
    			refreshWordList();
    		}
    	}
    	super.onActivityResult(requestcode, resultCode, data);
    }
	
	private void initWidget(){
		this.listView=(ListView)this.findViewById(R.id.listAllWords);
		this.progressWait=(ProgressBar)this.findViewById(R.id.progressBarShowLoading);
		this.txtInput=(AutoCompleteTextView)this.findViewById(R.id.autoCompleteTextViewInput);
		this.txtInput.addTextChangedListener(new TextWatcher(){
			@Override
			public void afterTextChanged(Editable arg0) {
				refreshWordList();
			}
			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}
			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}
		});
		
		//刚开始时隐藏列表
		this.listView.setVisibility(View.INVISIBLE);
		
		this.wordActionMenuDialog=new AlertDialog.Builder(this).setTitle(R.string.action_long_label).setItems(R.array.word_long_press_actions,
				new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch(which){
				case 0:
					Log.d("action clicked", "which: "+which+", will add to favorites.");
					addToFavorite();
					break;
				case 1:
					Log.d("action clicked", "which: "+which+", will copy word.");
					copyWord();
					break;
				case 2:
					Log.d("action clicked", "which: "+which+", will copy explanation of word.");
					copyWordExplanation();
					break;
				case 3:
					Log.d("action clicked", "which: "+which+", will show explanation of word.");
					showWordExplanation();
				}
			}
		}).create();
	}
	
	/**
	 * 刷新单词显示列表
	 */
	private void refreshWordList(){
		this.words=readDict.searchWord(this.txtInput.getText().toString());
		this.adapter.clear();
		for(Map.Entry<String,WordPosition> en:words){
			Log.d("word:",en.getKey());
			this.adapter.add(en.getKey());
		}
	}
	
	/**
	 * 第一次加载时，随机找出一个单词并在输入框中显示
	 */
	private void pickUpRandomWord(){
		int len=readDict.getWords().size();
		Set<Integer> targets=new HashSet<Integer>();
		for(int i=0;i<20;i++){
			targets.add((int)(Math.random()*len));
		}
		
		String tword="declare";
		Iterator<Entry<String, WordPosition>> it=readDict.getWords().entrySet().iterator();
		int i=0;
		while(it.hasNext()){
			Entry<String,WordPosition> en=it.next();
			if(targets.contains(Integer.valueOf(i))){
				String xword=en.getKey();
				int j=xword.indexOf(" ");
				Log.d("pickword", "get:"+xword);
				if(j>-1){
					xword=xword.substring(0,j);
				}
				if(xword.length()>10){
					tword=xword;
					Log.d("pickword", "new word:"+tword);
				}
			}
			i++;
		}
		//只取最多4个字，这样可以多搜出来一点，好看一些
		if(tword.length()>4){
			tword=tword.substring(0,4);
		}
		this.txtInput.setText(tword);
	}
	
	/**
	 * 随机找出20个单词
	 */
	private void showSomeRandom(){
		Set<String> swords=readDict.getWords().keySet();
		int len=swords.size();
		this.adapter.clear();
		List<Integer> nums=new ArrayList<Integer>();
		for(int i=0;i<20;){
			Integer ti=Integer.valueOf((int)(Math.random()*len));
			if(!nums.contains(ti)){
				//如果两次都取到同一个词的情况，虽然这种情况出现的几率是数十万分之一，但是也要考虑进去
				nums.add(ti);
				i++;
			}
			Log.d("gotrandom", ti.toString());
		}
		Collections.sort(nums);
		
		Iterator<Entry<String, WordPosition>> it=readDict.getWords().entrySet().iterator();
		
		int i=0;
		long cnt=0;
		this.words.clear();
		while(i<20 && it.hasNext() && nums.size()>0){
			if(nums.get(0)==cnt){
				Entry<String, WordPosition> st=it.next();
				this.adapter.add(st.getKey());
				this.words.add(st);
				Log.d("showrandom", nums.get(0)+" - "+st.getKey());
				nums.remove(0);
				i++;
			}else{
				it.next();
			}
			cnt++;
		}
	}
	
	private void initData(){
		List<String> twords=new ArrayList<String>();
		this.adapter=new ArrayAdapter<String>(this,R.layout.simple_word_item,R.id.worditem,twords);
		this.listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		this.listView.setAdapter(this.adapter);
		this.databaseHelper=new DatabaseHelper(this.getApplicationContext());
		this.sharedPreference=PreferenceManager.getDefaultSharedPreferences(this);
		
		Log.d("initdata", "databaseHelper ini complete.");
		this.listView.setClickable(true);
		//this.listView.setItemsCanFocus(false);
		//点击列表事件
		this.listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long rowid) {
				Log.d("list click", view.getClass()+", position:"+position+", rowid:"+rowid);
				showWord(position);
			}
		});
		
		
		//在小米手机上无法响应单击事件，将list item的focusable,clickable事件置false即可
		this.listView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				Log.d("list onItemSelected", view.getClass()+", position:"+position+", rowid:"+id);
				showWord(position);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});

		//长按列表事件
		this.listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long rowid) {
				Log.d("list long click", view.getClass()+", position:"+position+", rowid:"+rowid);
				LAST_SELECTED_WORD_INDEX=position;
				wordActionMenuDialog.show();
				//必须返回true以使不至于触发itemclick
				return true;
			}
		});
		
		readDict=new ReadDictWord();
		
		//开辟新进程来加载字典的索引数据
		new Thread(new Runnable(){
			@Override
			public void run() {
				String exStoragePath=Environment.getExternalStorageDirectory().getPath();
				Log.d("thread","external storage path:"+exStoragePath);
				
				//下面作为标记，指示是否加载字典成功
				boolean res=false;
				
				//首先从设置信息里读取
				res=readDictInfoFromSetting();
				Log.d("thread","read dict info from setting:"+res);
				if(!res){
					for(int i=0;i<MainActivity.DICT_LOCATIONS.length;i++){
						File fidx=new File(exStoragePath,MainActivity.DICT_LOCATIONS[i]+".idx");
						File fdict=new File(exStoragePath,MainActivity.DICT_LOCATIONS[i]+".dict");
						Log.d("thread", "try load dict at:"+fidx.getAbsolutePath()+"...");
						if(fidx.exists() && fidx.canRead() && fdict.exists() && fdict.canRead()){
							readDict.loadIndexFile(fidx.getAbsolutePath());
							res=readDict.loadContentFile(fdict.getAbsolutePath());
							
							Log.d("thread","load dict index file ok at:"+fidx.getAbsolutePath());
							Log.d("thread","load dict content file ok at:"+fdict.getAbsolutePath());
							
							if(res){
								saveDictFileInfo(fidx.getAbsolutePath(),fdict.getAbsolutePath());
							}
							break;
						}
					}
				}
				
				if(!res){
					Log.d("thread","Can not load oxford.idx at both / and /dict, try find any idx file at /dict...");
					File fd=new File(exStoragePath,"dict");
					Log.d("thread", "dict file:"+fd.getAbsolutePath());
					FilenameFilter ff=new FilenameFilter(){
						public boolean accept(File dir,String fname){
							if(fname.endsWith(".idx")){
								String barename=fname.substring(0,fname.length()-4);
								if(new File(dir,barename+".dict").exists()){
									return true;
								}
							}
							return false;
						}
					};
					
					if(!fd.exists()){
						//如果不存在dict目录，则往上找根
						fd=fd.getParentFile();
						Log.d("thread", "dict dir not exists, parent dir:"+fd.getAbsolutePath());
					}
					
					File[] fs=fd.listFiles(ff);
					Log.d("thread", "fs:"+fs);
					//sd卡不存在的时候Fs会返回null
					if(fd.exists() && fs!=null && fs.length>0){
						readDict.loadIndexFile(fs[0].getAbsolutePath());
						String x=fs[0].getName();
						File dictfile=new File(fd,x.substring(0, x.length()-4)+".dict");
						res=readDict.loadContentFile(dictfile.getAbsolutePath());
						
						Log.d("thread", "Load dict index file success at:"+fs[0].getAbsolutePath());
						Log.d("thread", "Load dict content file success at:"+dictfile.getAbsolutePath());
						
						if(res){
							saveDictFileInfo(fs[0].getAbsolutePath(),dictfile.getAbsolutePath());
						}
					}
				}
				
				Message msg=new Message();
				msg.what=SHOW_WORD_LIST;
				if(!res){
					Log.d("read_dict_thread","在"+exStoragePath+"目录下查找字典失败");
					msg.what=SHOW_LOAD_DICT_ERROR;
				}
				mHandler.sendMessage(msg);
			}
		}).start();
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		initWidget();
		initData();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		//图标的设置已经移动到资源文件中,android.R.drawable.ic_menu_search
		return true;
	}
	
	/**
	 * 主菜单响应过程
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		switch(item.getItemId()){
		case R.id.action_settings:
			startActivity(new Intent(this,DictPreferences.class));
			return true;
		case R.id.action_exit:
			this.finish();
			return true;
		case R.id.action_doaction:
			this.showActionMenu();
			return true;
		case R.id.action_favorite:
			this.showFavorite();
			return true;
		case R.id.action_history:
			this.showHistory();
			return true;
		case R.id.action_wonder:
			this.showSomeRandom();
			return true;
		}
		return false;
	}
	
	/**
	 * 用户点击了主菜单中的【动作】菜单项，显示一个可供选择的动作列表，并响应之
	 * @since 2014-1-4 14:47:44
	 */
	private void showActionMenu(){
		new AlertDialog.Builder(this).setTitle(R.string.action_select_action).setItems(R.array.main_menu_actions,
			new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					switch(which){
					case 0:
						Log.d("action clicked", "which: "+which+", will clear favorites.");
						clearFavorites();
						break;
					case 1:
						Log.d("action clicked", "which: "+which+", will clear favorites.");
						clearHistory();
						break;
					case 2:
						Log.d("action clicked", "which: "+which+", will clear dict info setting.");
						saveDictFileInfo("","");
						break;
					case 3:
						Log.d("action clicked", "which: "+which+", will goto read help.");
						readHelp();
						break;
					case 4:
						Log.d("action clicked", "which: "+which+", will goto read about.");
						readAbout();
						break;
					}
				}
			}).show();
	}
	
	/**
	 * 清除我的收藏
	 */
	private void clearFavorites(){
		int res=databaseHelper.deleteAllFavoriteWord();
		Toast.makeText(this, "共删除的收藏记录个数:"+res, Toast.LENGTH_LONG).show();
	}
	
	/**
	 * 清除我的查询历史记录
	 */
	private void clearHistory(){
		int res=databaseHelper.deleteAllHistoryRecord();
		Toast.makeText(this, "共删除的历史记录个数:"+res, Toast.LENGTH_LONG).show();
	}
	
	/**
	 * 在新activity中显示历史单词
	 */
	private void showHistory(){
		Intent in=new Intent(this,FavoriteActivity.class);
		in.putExtra(FavoriteActivity.TYPE_IS_FAVORITE, false);
		this.startActivity(in);
	}
	
	/**
	 * 在新activity中显示收藏的单词
	 */
	private void showFavorite(){
		Intent in=new Intent(this,FavoriteActivity.class);
		in.putExtra(FavoriteActivity.TYPE_IS_FAVORITE, true);
		this.startActivity(in);
	}
	
	/**
	 * 阅读帮助文档
	 */
	private void readHelp(){
		Intent in=new Intent(this,AboutActivity.class);
		in.putExtra(AboutActivity.TYPE_IS_ABOUT, false);
		this.startActivity(in);
	}
	
	/**
	 * 阅读关于
	 */
	private void readAbout(){
		Intent it=new Intent(this,AboutActivity.class);
		it.putExtra(AboutActivity.TYPE_IS_ABOUT, true);
		this.startActivity(it);
	}
	
	/**
	 * 显示某个单词的释义
	 * @param position 此单词在列表words中的位置，几乎不可能在列表之外，所以不做检查
	 */
	private void showWord(int position){
		WordPosition wp=words.get(position).getValue();
		String tword=words.get(position).getKey();
		//如果记录查看历史
		if(this.sharedPreference.getBoolean("if_record_history", true)){
			this.databaseHelper.createHistoryRecord(tword);
		}
		String wordexp=readDict.getWordExplanation(wp.getStartPos(), wp.getLength());
		boolean opennew=this.sharedPreference.getBoolean("if_show_in_new_activity", false);
		Log.d("if_show_in_new_activity", opennew+"");
		ShowUtil.showWord(this, opennew, tword, wordexp);
	}
	
	/**
	 * 长按菜单项，【加入收藏】
	 */
	private void addToFavorite(){
		String tword=this.words.get(LAST_SELECTED_WORD_INDEX).getKey();
		if(databaseHelper.isWordFavorited(tword)){
			Toast.makeText(this, "此单词已经被收藏，不再重复收藏:"+tword, Toast.LENGTH_LONG).show();
			return;
		}
		FavoriteWord fw=new FavoriteWord();
		fw.setImportantClass(1);
		fw.setWord(tword);
		long res=databaseHelper.createFavoriteWord(fw);
		Toast.makeText(this, "单词收藏:"+(res==-1?"失败":"成功")+":"+tword, Toast.LENGTH_LONG).show();
		Log.d("addtofavorite","add return id:"+res);
	}
	
	/**
	 * 长按菜单项，【复制单词】
	 * 新的操作剪贴板的写法只在3.0之后可用，为了兼容2.3等版本，只能用这个已经过时的方法。
	 */
	private void copyWord(){
		String tword=this.words.get(LAST_SELECTED_WORD_INDEX).getKey();
		ClipboardManager clipboard = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
		clipboard.setText(tword);
		Toast.makeText(this, "已成功复制"+tword.length()+"个字!", Toast.LENGTH_SHORT).show();
	}
	
	/**
	 * 长按菜单项，【复制释义】
	 * 新的操作剪贴板的写法只在3.0之后可用，为了兼容2.3等版本，只能用这个已经过时的方法。
	 */
	private void copyWordExplanation(){
		WordPosition wp=this.words.get(LAST_SELECTED_WORD_INDEX).getValue();
		String wexp=readDict.getWordExplanation(wp.getStartPos(), wp.getLength());
		ClipboardManager clipboard = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
		clipboard.setText(wexp);
		Toast.makeText(this, "已成功复制"+wexp.length()+"个字!", Toast.LENGTH_SHORT).show();
	}
	
	/**
	 * 长按菜单项，【显示释义】
	 */
	private void showWordExplanation(){
		this.showWord(this.LAST_SELECTED_WORD_INDEX);
	}
	
	/**
	 * 保存字典路径信息
	 * @param sidx
	 * @param sdict
	 */
	private void saveDictFileInfo(String sidx,String sdict){
		Log.d("savedictinfo", "idx file:"+sidx);
		Log.d("savedictinfo", "dict file:"+sdict);
		this.sharedPreference.edit().putString(MainActivity.SETTING_IDX_FILE_PATH_KEY, sidx).commit();
		this.sharedPreference.edit().putString(MainActivity.SETTING_DICT_FILE_PATH_KEY, sdict).commit();
	}
	
	private boolean readDictInfoFromSetting(){
		String sidx=this.sharedPreference.getString(SETTING_IDX_FILE_PATH_KEY, "");
		String sdict=this.sharedPreference.getString(SETTING_DICT_FILE_PATH_KEY, "");
		Log.d("read_dict_info", "sidx file:"+sidx);
		Log.d("read_dict_info", "sdict file:"+sdict);
		if(sidx.length()<1){
			this.sharedPreference.edit().putString(MainActivity.SETTING_DICT_FILE_PATH_KEY, "").commit();
			return false;
		}
		if(sdict.length()<1){
			this.sharedPreference.edit().putString(MainActivity.SETTING_IDX_FILE_PATH_KEY, "").commit();
			return false;
		}
		if(!new File(sidx).exists() || !new File(sdict).exists()){
			this.sharedPreference.edit().putString(MainActivity.SETTING_IDX_FILE_PATH_KEY, "").commit();
			this.sharedPreference.edit().putString(MainActivity.SETTING_DICT_FILE_PATH_KEY, "").commit();
			return false;
		}
		boolean res=true;
		readDict.loadIndexFile(sidx);
		res&=readDict.loadContentFile(sdict);
		Log.d("read_dict_info", "load res:"+res+", num:"+readDict.getWords().size());
		return res;
	}
}
