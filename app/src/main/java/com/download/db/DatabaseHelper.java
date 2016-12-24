package com.download.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * 数据库帮助类
 */
public class DatabaseHelper extends SQLiteOpenHelper{

	private static final String DB_NAME = "download.db";
	
	//创建下载线程表
	private static final String CREATE_SQL = 
			"create table thread_info(" +
			"_id integer primary key autoincrement," +
			"url text," +
			"thread_id integer," +
			"start integer," +
			"end integer," +
			"finished integer" +
			")";

	private DatabaseHelper(Context context) {
		super(context, DB_NAME, null, 1);
	}

	private static DatabaseHelper sHelper = null; 
	/**
	 * 实现单例模式
	 */
	public static DatabaseHelper getInstance(Context context){
		if(sHelper == null){
			sHelper = new DatabaseHelper(context);
		}
		return sHelper;
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(CREATE_SQL);
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	}
	
}
