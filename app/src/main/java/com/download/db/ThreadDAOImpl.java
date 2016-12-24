package com.download.db;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.download.entities.ThreadInfo;

/**
 * 数据库访问接口实现
 *
 */
public class ThreadDAOImpl implements ThreadDAO {

	private DatabaseHelper helper = null;

	public ThreadDAOImpl(Context context){
		helper = DatabaseHelper.getInstance(context);
	}

	/* (non-Javadoc)
	 * @see com.download.db.ThreadDAO#insertThreadInfo(com.download.entities.ThreadInfo)
	 */
	@Override
	public synchronized void insertThreadInfo(ThreadInfo threadInfo){
		SQLiteDatabase db = helper.getWritableDatabase();
		String sql = "insert into thread_info(thread_id,url,start,end,finished) " +
				"values(?,?,?,?,?)";
		db.execSQL(sql,new Object[]{threadInfo.getId(),threadInfo.getUrl(),threadInfo.getStart(),threadInfo.getEnd(),threadInfo.getFinished()});
		db.close();
	}

	/* (non-Javadoc)
	 * @see com.download.db.ThreadDAO#updateThreadInfo(int, java.lang.String, int)
	 */
	@Override
	public synchronized void updateThreadInfo(int thread_id,String url,int finished){
		SQLiteDatabase db = helper.getWritableDatabase();
		String sql = "update thread_info set finished = ? where thread_id = ? and url = ?";
		db.execSQL(sql,new Object[]{finished,thread_id,url});
		db.close();
	}

	/* (non-Javadoc)
	 * @see com.download.db.ThreadDAO#deleteThreadInfo(java.lang.String, int)
	 */
	@Override
	public synchronized void deleteThreadInfo(String url){
		SQLiteDatabase db = helper.getWritableDatabase();
		String sql = "delete from thread_info where url = ?";
		db.execSQL(sql,new Object[]{url});
		db.close();
	}

	/* (non-Javadoc)
	 * @see com.download.db.ThreadDAO#getThreadInfos(java.lang.String)
	 */
	@Override
	public List<ThreadInfo> getThreadInfos(String url){
		SQLiteDatabase db = helper.getReadableDatabase();
		String sql = "select * from thread_info where url = ?";
		Cursor cur = db.rawQuery(sql, new String[]{url});
		List<ThreadInfo> threads = new ArrayList<ThreadInfo>();
		while(cur.moveToNext()){
			ThreadInfo info = new ThreadInfo();
			info.setId(cur.getInt(cur.getColumnIndex("thread_id")));
			info.setStart(cur.getInt(cur.getColumnIndex("start")));
			info.setEnd(cur.getInt(cur.getColumnIndex("end")));
			info.setFinished(cur.getInt(cur.getColumnIndex("finished")));
			info.setUrl(cur.getString(cur.getColumnIndex("url")));
			threads.add(info);
		}
		cur.close();
		db.close();
		return threads;
	}

	/* (non-Javadoc)
	 * @see com.download.db.ThreadDAO#isExists(java.lang.String, int)
	 */
	@Override
	public boolean isExists(String url,int thread_id){
		SQLiteDatabase db = helper.getReadableDatabase();
		String sql = "select * from thread_info where url = ? and thread_id = ?";
		Cursor cur = db.rawQuery(sql, new String[]{url,thread_id+""});
		boolean exists = cur.moveToNext();
		cur.close();
		db.close();
		return exists;
	}
}
