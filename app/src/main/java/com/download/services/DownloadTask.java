package com.download.services;

import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.http.HttpStatus;

import android.content.Context;
import android.content.Intent;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import com.download.db.ThreadDAO;
import com.download.db.ThreadDAOImpl;
import com.download.entities.FileInfo;
import com.download.entities.ThreadInfo;
/**
 * 下载任务类
 *
 */
public class DownloadTask {

	private Context mContext = null;
	private FileInfo mFileInfo = null;
	private ThreadDAO mDao = null;
	public boolean isPause = false;
	private int mFinished = 0;
	private int mThreadCount = 1; //线程数量
	private List<DownloadThread> mThreadList = null;//线程集合
	public static ExecutorService sExecutorService =
			Executors.newCachedThreadPool(); //线程池
	private Timer mTimer = new Timer(); //定时器 用于发送进度广播
	private Messenger mMessenger = null;

	public DownloadTask(Context mContext,Messenger mMessenger, FileInfo mFileInfo, int mThreadCount) {
		this.mContext = mContext;
		this.mMessenger = mMessenger;
		this.mFileInfo = mFileInfo;
		this.mThreadCount = mThreadCount;
		mDao = new ThreadDAOImpl(mContext);
	}

	public void download(){
		this.isPause = false;
		//从数据库获得下载进度
		List<ThreadInfo> threads = mDao.getThreadInfos(mFileInfo.getUrl());
		if(threads.size() == 0){
			//获得每个线程下载长度
			int length = mFileInfo.getLength() / mThreadCount;
			for(int i = 0; i < mThreadCount;i++){
				//创建线程信息
				ThreadInfo threadInfo = new ThreadInfo(i,mFileInfo.getUrl(),
						length * i,(i + 1) * length - 1,0);
				if(i == mThreadCount - 1){
					//设置最后一个线程下载到文件末尾
					threadInfo.setEnd(mFileInfo.getLength());
				}
				//添加到线程信息集合中
				threads.add(threadInfo);
				//插入下载线程信息
				mDao.insertThreadInfo(threadInfo);
			}
		}
		//创建线程集合
		mThreadList = new ArrayList<DownloadThread>();
		//启动多个线程进行下载
		for(ThreadInfo info : threads){
			DownloadThread thread = new DownloadThread(info);
//			thread.start();
			DownloadTask.sExecutorService.execute(thread);
			//添加线程到集合中
			mThreadList.add(thread);
		}
		//启动定时任务发送文件进度，延时1000毫秒 ，周期是1500毫秒
		mTimer.schedule(new TimerTask(){
			@Override
			public void run() {
				if(!isPause){
					//发送进度到Activity
//					Intent intent = new Intent(DownloadService.ACTION_UPDATE);
//					intent.putExtra("finished", mFinished * 100 / mFileInfo.getLength());
//					intent.putExtra("id", mFileInfo.getId());
//					mContext.sendBroadcast(intent);
					Message msg = new Message();
					msg.what = DownloadService.MSG_UPDATE;
					msg.arg1 = mFinished * 100 / mFileInfo.getLength();
					msg.arg2 = mFileInfo.getId();
					try {
						mMessenger.send(msg);
					} catch (RemoteException e) {
						e.printStackTrace();
					}
				}
			}
		}, 1000, 1500);
	}

	/**
	 * 判断是否所有线程都执行完毕
	 */
	private synchronized void checkAllThreadsFinshed(){
		boolean allFinished = true;
		//遍历线程集合，判断线程是否都执行完毕
		for(DownloadThread thread : mThreadList){
			if(!thread.isFinished){
				allFinished = false;
				break;
			}
		}
		if(allFinished){
			//取消定时器
			mTimer.cancel();
			//删除下载记录
			mDao.deleteThreadInfo(mFileInfo.getUrl());
			//发送广播通知UI下载任务结束
//			Intent intent = new Intent(DownloadService.ACTION_FINISH);
//			intent.putExtra("fileInfo", mFileInfo);
//			mContext.sendBroadcast(intent);
			Message msg = new Message();
			msg.what = DownloadService.MSG_FINISH;
			msg.obj = mFileInfo;
			try {
				mMessenger.send(msg);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 数据下载线程
	 */
	class DownloadThread extends Thread{

		private ThreadInfo mThreadInfo = null;
		public boolean isFinished = false; //线程是否执行完毕

		public DownloadThread(ThreadInfo threadInfo){
			mThreadInfo = threadInfo;
		}

		public void run(){
			HttpURLConnection conn = null;
			RandomAccessFile raf = null;
			InputStream input = null;
			try {
				//打开连接
				URL url = new URL(mThreadInfo.getUrl());
				conn = (HttpURLConnection) url.openConnection();
				conn.setConnectTimeout(3000);
				conn.setRequestMethod("GET");
				//设置下载位置
				int start = mThreadInfo.getStart()+mThreadInfo.getFinished();
				conn.setRequestProperty("Range", "bytes="+start+"-"+mThreadInfo.getEnd());
				//设置本地文件写入位置
				File file = new File(DownloadService.DOWNLOAD_DIR, mFileInfo.getName());
				raf = new RandomAccessFile(file,"rwd");
				raf.seek(start);
				//累加完成进度
				mFinished += mThreadInfo.getFinished();
				if(conn.getResponseCode() == HttpStatus.SC_PARTIAL_CONTENT){
					//下载文件
					int len = 0;
					byte[] buffer = new byte[1024];
					input = conn.getInputStream();
					//读取文件
					while((len = input.read(buffer))!=-1){
						//写入文件
						raf.write(buffer,0,len);
						//累加整个文件完成进度
						mFinished += len;
						//累加每个线程完成的进度
						mThreadInfo.setFinished(mThreadInfo.getFinished() + len);
						if(isPause){
							//保存进度到数据库
							mDao.updateThreadInfo(mThreadInfo.getId(),
									mThreadInfo.getUrl(),
									mThreadInfo.getFinished());
							return;
						}
					}
				}
				//标识线程执行完毕
				isFinished = true;
				//检查下载任务是否执行完毕
				checkAllThreadsFinshed();
			} catch (Exception e) {
				e.printStackTrace();
			} finally{
				try {
					conn.disconnect();
					raf.close();
					input.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
}
