package com.download.services;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.http.HttpStatus;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.download.entities.FileInfo;
/**
 * 下载服务
 *
 */
public class DownloadService extends Service{

	//下载路径
	public static final String DOWNLOAD_DIR =
			Environment.getExternalStorageDirectory().getAbsolutePath() +
					"/downloads/";
	//开始下载命令
	public static final String ACTION_START = "ACTION_START";
	//停止下载命令
	public static final String ACTION_STOP = "ACTION_STOP";
	//结束下载命令
	public static final String ACTION_FINISH = "ACTION_FINISH";
	//更新UI命令
	public static final String ACTION_UPDATE = "ACTION_UPDATE";
	//初始化标识
	public static final int MSG_INIT = 0x1;
	public static final int MSG_BIND = 0x2;
	public static final int MSG_START = 0x3;
	public static final int MSG_STOP = 0x4;
	public static final int MSG_FINISH = 0x5;
	public static final int MSG_UPDATE = 0x6;
	private InitThread mInitThread = null;
	//下载任务的集合
	private Map<Integer,DownloadTask> mTasks =
			new LinkedHashMap<Integer,DownloadTask>();
	private Messenger mActivityMessenger = null;

	@Override
	public IBinder onBind(Intent intent) {
		//创建Messenger传递Service中的Handler
		return new Messenger(mHandler).getBinder();
	}

//	@Override
//	public int onStartCommand(Intent intent, int flags, int startId) {
//		Log.i("test",intent.getAction());
//		if(ACTION_START.equals(intent.getAction())){
//			FileInfo fileInfo = (FileInfo) intent.getSerializableExtra("fileInfo");
//			//接到下载命令，启动初始化线程
//			mInitThread = new InitThread(fileInfo);
//			//用线程池启动线程
//			DownloadTask.sExecutorService.execute(mInitThread);
//		}else if(ACTION_STOP.equals(intent.getAction())){
//			//暂停下载
//			FileInfo fileInfo = (FileInfo) intent.getSerializableExtra("fileInfo");
//			//从集合中取出下载任务
//			DownloadTask task = mTasks.get(fileInfo.getId());
//			if(task != null){
//				//停止下载任务
//				task.isPause = true;
//			}
//		}
//		return super.onStartCommand(intent, flags, startId);
//	}

	Handler mHandler = new Handler(){
		public void handleMessage(Message msg){
			DownloadTask task = null;
			FileInfo fileInfo = null;
			switch(msg.what){
				case MSG_INIT:
					//获得初始化的结果
					fileInfo = (FileInfo) msg.obj;
					//启动下载任务 3代表启动3个线程下载
					task = new DownloadTask(DownloadService.this,mActivityMessenger,fileInfo,3);
					task.download();
					//把下载任务添加到集合中
					mTasks.put(fileInfo.getId(), task);
					//通知Activity任务已经启动
					Message msg1 = new Message();
					msg1.what = DownloadService.MSG_START;
					msg1.obj = fileInfo;
					try {
						mActivityMessenger.send(msg1);
					} catch (RemoteException e) {
						e.printStackTrace();
					}
					break;
				case MSG_BIND:
					//接受到Activity的消息
					mActivityMessenger = msg.replyTo;
					break;
				case MSG_START:
					fileInfo = (FileInfo) msg.obj;
					//接到下载命令，启动初始化线程
					mInitThread = new InitThread(fileInfo);
					//用线程池启动线程
					DownloadTask.sExecutorService.execute(mInitThread);
					break;
				case MSG_STOP:
					//暂停下载
					fileInfo = (FileInfo) msg.obj;
					//从集合中取出下载任务
					task = mTasks.get(fileInfo.getId());
					if(task != null){
						//停止下载任务
						task.isPause = true;
					}
					break;
			}
		}
	};

	/**
	 * 初始化线程
	 *
	 */
	class InitThread extends Thread{
		private FileInfo mFileInfo = null;
		public InitThread(FileInfo fileInfo){
			mFileInfo = fileInfo;
		}
		public void run(){
			HttpURLConnection conn = null;
			RandomAccessFile raf = null;
			try {
				//获取文件长度
				URL url = new URL(mFileInfo.getUrl());
				conn = (HttpURLConnection) url.openConnection();
				conn.setConnectTimeout(3000);
				conn.setRequestMethod("GET");
				if(conn.getResponseCode() == HttpStatus.SC_OK){
					int length = conn.getContentLength();
					//设置本地文件长度
					File dir = new File(DOWNLOAD_DIR);
					if(!dir.exists()){
						dir.mkdir();
					}
					File file = new File(dir,mFileInfo.getName());
					raf = new RandomAccessFile(file,"rwd");
					raf.setLength(length);
					//将初始化信息回传Service
					mFileInfo.setLength(length);
					mHandler.obtainMessage(MSG_INIT,length,0,mFileInfo).sendToTarget();
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally{
				try {
					conn.disconnect();
					raf.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
}
