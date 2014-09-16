package com.yage.dict.db;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.yage.dict.entity.FavoriteWord;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * 读写SQLite数据库的辅助类
 * @author voyage
 * @since 2014-1-4 23:18:43
 */
public class DatabaseHelper extends SQLiteOpenHelper{

	private static final String LOG=DatabaseHelper.class.getName();
	private static final int DATABASE_VERSION=1;
	
	private static final String DATABASE_NAME="AVEDict";
	private static final String TABLE_FAVORITE="dict_favorite";
	private static final String TABLE_HISTORY="dict_history";
	private static final String KEY_ID="id";
	private static final String KEY_CREATED_AT="created_at";
	private static final String KEY_WORD="word";
	private static final String KEY_IMPORTANCE_CLASS="important_class";
	
	private static final String CREATE_TABLE_FAVORITE="CREATE TABLE "+TABLE_FAVORITE+"("
			+KEY_ID+" INTEGER PRIMARY KEY,"+KEY_WORD+" TEXT,"+KEY_IMPORTANCE_CLASS+" INTEGER,"
			+KEY_CREATED_AT+" DATETIME)";
	
	private static final String CREATE_TABLE_HISTORY="CREATE TABLE "+TABLE_HISTORY+"("
			+KEY_ID+" INTEGER PRIMARY KEY,"+KEY_WORD+" TEXT,"
			+KEY_CREATED_AT+" DATETIME)";
	
	public static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
	
	public DatabaseHelper(Context context){
		super(context,DATABASE_NAME,null,DATABASE_VERSION);
	}
	
	public DatabaseHelper(Context context, String name, CursorFactory factory, int version) {
		super(context, name, factory, version);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(CREATE_TABLE_FAVORITE);
		db.execSQL(CREATE_TABLE_HISTORY);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS "+TABLE_FAVORITE);
		db.execSQL("DROP TABLE IF EXISTS "+TABLE_HISTORY);
		onCreate(db);
	}

	/**
	 * 新添加一条收藏
	 * @param fw
	 * @return
	 */
	public long createFavoriteWord(FavoriteWord fw){
		SQLiteDatabase db=this.getWritableDatabase();
		ContentValues values=new ContentValues();
		values.put(KEY_WORD, fw.getWord());
		values.put(KEY_CREATED_AT, getDateTime());
		values.put(KEY_IMPORTANCE_CLASS, fw.getImportantClass());
		long fwid=db.insert(TABLE_FAVORITE, null, values);
		return fwid;
	}
	
	/**
	 * 添加浏览历史记录
	 * @param word
	 * @return
	 */
	public long createHistoryRecord(String word){
		SQLiteDatabase db=this.getWritableDatabase();
		ContentValues values=new ContentValues();
		values.put(KEY_WORD, word);
		values.put(KEY_CREATED_AT, getDateTime());
		long fwid=db.insert(TABLE_HISTORY, null, values);
		return fwid;
	}
	
	/**
	 * 删除某个收藏的单词
	 * @param fwid
	 */
	public int deleteFavoriteWord(long fwid){
		SQLiteDatabase db=this.getWritableDatabase();
		return db.delete(TABLE_FAVORITE, KEY_ID+"=?", new String[]{String.valueOf(fwid)});
	}
	
	/**
	 * 删除某条历史记录
	 * @param hid 此记录的ID
	 * @return integer, 影响的行数
	 */
	public int deleteHistoryRecord(long hid){
		SQLiteDatabase db=this.getWritableDatabase();
		return db.delete(TABLE_HISTORY, KEY_ID+"=?", new String[]{String.valueOf(hid)});
	}
	
	/**
	 * 删除所有的收藏记录
	 * @return
	 */
	public int deleteAllFavoriteWord(){
		SQLiteDatabase db=this.getWritableDatabase();
		return db.delete(TABLE_FAVORITE, null, null);
	}
	
	/**
	 * 删除所有的历史记录
	 * @return
	 */
	public int deleteAllHistoryRecord(){
		SQLiteDatabase db=this.getWritableDatabase();
		return db.delete(TABLE_HISTORY, null, null);
	}
	
	/**
	 * 列出所有的收藏单词
	 * @return
	 */
	public List<FavoriteWord> getAllFavoriteWord(){
		SQLiteDatabase db=this.getWritableDatabase();
		List<FavoriteWord> res=new ArrayList<FavoriteWord>();
		String selectquery="SELECT * FROM "+TABLE_FAVORITE;
		Log.d(LOG, selectquery);
		Cursor c=db.rawQuery(selectquery, null);
		if(c.moveToFirst()){
			Log.d(LOG, "has movetofirst ok");
			do{
				Log.d(LOG, "movetonext ok.");
				FavoriteWord fw=new FavoriteWord();
				fw.setId(c.getLong(c.getColumnIndex(KEY_ID)));
				fw.setWord(c.getString(c.getColumnIndex(KEY_WORD)));
				fw.setAddTime(parseDateFromString(c.getString(c.getColumnIndex(KEY_CREATED_AT))));
				fw.setImportantClass(c.getInt(c.getColumnIndex(KEY_IMPORTANCE_CLASS)));
				res.add(fw);
			}while(c.moveToNext());
		}
		return res;
	}
	
	/**
	 * 得到历史记录
	 * @return
	 */
	public List<FavoriteWord> getAllHistoryWord(){
		SQLiteDatabase db=this.getWritableDatabase();
		List<FavoriteWord> res=new ArrayList<FavoriteWord>();
		String selectquery="SELECT * FROM "+TABLE_HISTORY;
		Log.d(LOG, selectquery);
		Cursor c=db.rawQuery(selectquery, null);
		if(c.moveToFirst()){
			Log.d(LOG, "has movetofirst ok");
			do{
				Log.d(LOG, "movetonext ok.");
				FavoriteWord fw=new FavoriteWord();
				fw.setId(c.getLong(c.getColumnIndex(KEY_ID)));
				fw.setWord(c.getString(c.getColumnIndex(KEY_WORD)));
				fw.setAddTime(parseDateFromString(c.getString(c.getColumnIndex(KEY_CREATED_AT))));
				res.add(fw);
			}while(c.moveToNext());
		}
		return res;
	}
	
	/**
	 * 查看此单词是否已经被收藏
	 * @param w
	 * @return true表示已经收藏过
	 */
	public boolean isWordFavorited(String w){
		SQLiteDatabase db=this.getWritableDatabase();
		String selectquery="SELECT * FROM "+TABLE_FAVORITE+" WHERE "+KEY_WORD+"=?";
		Log.d(LOG, selectquery);
		Cursor c=db.rawQuery(selectquery, new String[]{w});
		if(c.moveToFirst()){
			return true;
		}
		return false;
	}
	
	/**
	 * 从日期字符串转回日期对象
	 * @param s
	 * @return
	 */
	private Date parseDateFromString(String s){
		try {
			return dateFormat.parse(s);
		} catch (ParseException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private String getDateTime() {
        Date date = new Date();
        return dateFormat.format(date);
    }
}
