package com.yage.dict.entity;

import java.util.Date;

/**
 * 代表收藏的一个单词
 * @since 2014-1-4 23:16:52
 * @author voyage
 */
public class FavoriteWord {
	
	//键
	private long id;
	
	//单词
	private String word;
	
	//加入时间
	private Date addTime;
	
	//重要等级
	private int importantClass;

	public String getWord() {
		return word;
	}

	public void setWord(String word) {
		this.word = word;
	}

	public Date getAddTime() {
		return addTime;
	}

	public void setAddTime(Date addTime) {
		this.addTime = addTime;
	}

	public int getImportantClass() {
		return importantClass;
	}

	public void setImportantClass(int importantClass) {
		this.importantClass = importantClass;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}
	
	public String toString(){
		return id+" "+this.word+" added at:"+this.addTime;
	}
}
