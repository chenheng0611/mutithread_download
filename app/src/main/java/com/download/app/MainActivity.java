package com.download.app;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.widget.ListView;
import android.widget.Toast;

import com.download.entities.FileInfo;
import com.download.services.DownloadService;
import com.download.utils.NotificationUtil;

/**
 * 实现多线程断点续传下载的案例，技术点包括：
 1、使用线程池+Handler实现异步通信
 2、使用Service进行后台下载
 3、用Messenger进行Activity与Service的双向通信
 4、每个文件使用多个线程分批下载
 5、使用数据库实现断点续传
 6、使用通知进行下载进度的显示
 */
public class MainActivity extends Activity {

	private ListView mLvFile = null;
	private List<FileInfo> mFileList = null;
	private FileListAdapter mAdapter = null;
	private NotificationUtil mNotificationUtil = null;
	private Messenger mServiceMessenger = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mLvFile = (ListView) findViewById(R.id.lvFile);
		//创建文件集合
		mFileList = new ArrayList<FileInfo>();
		//创建文件对象
		FileInfo fileInfo1 = new FileInfo(0,"http://dlsw.baidu.com/sw-search-sp/soft/67/38406/mjtq1.0.1.15.1425465043.exe","mjtq1.0.1.15.1425465043.exe",0,0);
		FileInfo fileInfo2 = new FileInfo(1,"http://dlsw.baidu.com/sw-search-sp/soft/e0/13545/GooglePinyinInstaller.1419846448.exe","GooglePinyinInstaller.1419846448.exe",0,0);
		FileInfo fileInfo3 = new FileInfo(2,"http://dlsw.baidu.com/sw-search-sp/soft/1a/11798/kugou_V7.6.85.17344_setup.1427079848.exe","kugou_V7.6.85.17344_setup.1427079848.exe",0,0);

		mFileList.add(fileInfo1);
		mFileList.add(fileInfo2);
		mFileList.add(fileInfo3);
		//创建适配器
		mAdapter = new FileListAdapter(this,mFileList);
		//设置ListView
		mLvFile.setAdapter(mAdapter);
		//创建通知工具对象
		mNotificationUtil = new NotificationUtil(this);
		//绑定Service
		Intent intent = new Intent(this,DownloadService.class);
		bindService(intent,mServiceConnection,Service.BIND_AUTO_CREATE);
	}

	ServiceConnection mServiceConnection = new ServiceConnection(){
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			//获得Service中的Messenger
			mServiceMessenger = new Messenger(service);
			//把Hanlder引用保存在Messenger中
			Messenger messenger = new Messenger(mHandler);
			//传给Service中的Handler
			Message msg = new Message();
			msg.what = DownloadService.MSG_BIND;
			msg.replyTo = messenger;
			try {
				mServiceMessenger.send(msg);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
			mAdapter.setMessenger(mServiceMessenger);
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
		}
	};

	Handler mHandler = new Handler(){
		public void handleMessage(Message msg) {
			switch(msg.what){
				//更新进度
				case DownloadService.MSG_UPDATE:
					int finished = msg.arg1;
					int id = msg.arg2;
					mAdapter.updateProgress(id, finished);
					//更新通知里的进度
					mNotificationUtil.updateNotification(id, finished);
					break;
				case DownloadService.MSG_FINISH:
					//下载结束
					FileInfo fileInfo = (FileInfo) msg.obj;
					//更新进度为0
					mAdapter.updateProgress(fileInfo.getId(), 0);
					Toast.makeText(MainActivity.this,
							mFileList.get(fileInfo.getId()).getName()+"下载完毕",
							Toast.LENGTH_SHORT).show();
					//取消通知
					mNotificationUtil.cancelNotification(fileInfo.getId());
					break;
				case DownloadService.MSG_START:
					//显示通知
					mNotificationUtil.showNotification((FileInfo) msg.obj);
					break;
			}
		}
	};
}
