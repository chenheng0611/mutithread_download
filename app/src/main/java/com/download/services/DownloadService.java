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
 * ���ط���
 *
 */
public class DownloadService extends Service{

	//����·��
	public static final String DOWNLOAD_DIR =
			Environment.getExternalStorageDirectory().getAbsolutePath() +
					"/downloads/";
	//��ʼ��������
	public static final String ACTION_START = "ACTION_START";
	//ֹͣ��������
	public static final String ACTION_STOP = "ACTION_STOP";
	//������������
	public static final String ACTION_FINISH = "ACTION_FINISH";
	//����UI����
	public static final String ACTION_UPDATE = "ACTION_UPDATE";
	//��ʼ����ʶ
	public static final int MSG_INIT = 0x1;
	public static final int MSG_BIND = 0x2;
	public static final int MSG_START = 0x3;
	public static final int MSG_STOP = 0x4;
	public static final int MSG_FINISH = 0x5;
	public static final int MSG_UPDATE = 0x6;
	private InitThread mInitThread = null;
	//��������ļ���
	private Map<Integer,DownloadTask> mTasks =
			new LinkedHashMap<Integer,DownloadTask>();
	private Messenger mActivityMessenger = null;

	@Override
	public IBinder onBind(Intent intent) {
		//����Messenger����Service�е�Handler
		return new Messenger(mHandler).getBinder();
	}

//	@Override
//	public int onStartCommand(Intent intent, int flags, int startId) {
//		Log.i("test",intent.getAction());
//		if(ACTION_START.equals(intent.getAction())){
//			FileInfo fileInfo = (FileInfo) intent.getSerializableExtra("fileInfo");
//			//�ӵ��������������ʼ���߳�
//			mInitThread = new InitThread(fileInfo);
//			//���̳߳������߳�
//			DownloadTask.sExecutorService.execute(mInitThread);
//		}else if(ACTION_STOP.equals(intent.getAction())){
//			//��ͣ����
//			FileInfo fileInfo = (FileInfo) intent.getSerializableExtra("fileInfo");
//			//�Ӽ�����ȡ����������
//			DownloadTask task = mTasks.get(fileInfo.getId());
//			if(task != null){
//				//ֹͣ��������
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
					//��ó�ʼ���Ľ��
					fileInfo = (FileInfo) msg.obj;
					//������������ 3��������3���߳�����
					task = new DownloadTask(DownloadService.this,mActivityMessenger,fileInfo,3);
					task.download();
					//������������ӵ�������
					mTasks.put(fileInfo.getId(), task);
					//֪ͨActivity�����Ѿ�����
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
					//���ܵ�Activity����Ϣ
					mActivityMessenger = msg.replyTo;
					break;
				case MSG_START:
					fileInfo = (FileInfo) msg.obj;
					//�ӵ��������������ʼ���߳�
					mInitThread = new InitThread(fileInfo);
					//���̳߳������߳�
					DownloadTask.sExecutorService.execute(mInitThread);
					break;
				case MSG_STOP:
					//��ͣ����
					fileInfo = (FileInfo) msg.obj;
					//�Ӽ�����ȡ����������
					task = mTasks.get(fileInfo.getId());
					if(task != null){
						//ֹͣ��������
						task.isPause = true;
					}
					break;
			}
		}
	};

	/**
	 * ��ʼ���߳�
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
				//��ȡ�ļ�����
				URL url = new URL(mFileInfo.getUrl());
				conn = (HttpURLConnection) url.openConnection();
				conn.setConnectTimeout(3000);
				conn.setRequestMethod("GET");
				if(conn.getResponseCode() == HttpStatus.SC_OK){
					int length = conn.getContentLength();
					//���ñ����ļ�����
					File dir = new File(DOWNLOAD_DIR);
					if(!dir.exists()){
						dir.mkdir();
					}
					File file = new File(dir,mFileInfo.getName());
					raf = new RandomAccessFile(file,"rwd");
					raf.setLength(length);
					//����ʼ����Ϣ�ش�Service
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
