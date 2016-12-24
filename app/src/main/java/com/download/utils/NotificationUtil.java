package com.download.utils;

import java.util.HashMap;
import java.util.Map;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.download.app.MainActivity;
import com.download.app.R;
import com.download.entities.FileInfo;
import com.download.services.DownloadService;

/**
 * 通知工具类
 */
public class NotificationUtil {

	private NotificationManager mNotificationManager = null;
	private Map<Integer,Notification> mNotifications = null; //保存已经显示的通知 key是文件ID
	private Context mContext = null;

	public NotificationUtil(Context context){
		mContext = context;
		//获得通知系统服务
		mNotificationManager =
				(NotificationManager) context.getSystemService(context.NOTIFICATION_SERVICE);
		//创建通知的集合
		mNotifications = new HashMap<Integer,Notification>();
	}

	/**
	 * 显示通知
	 * @param fileInfo
	 */
	public void showNotification(FileInfo fileInfo){
		//判断通知是否已经显示了
		if(!mNotifications.containsKey(fileInfo.getId())){
			//创建通知对象
			Notification notification = new Notification();
			//设置滚动文字
			notification.tickerText = fileInfo.getName() + "开始下载";
			//设置显示时间
			notification.when = System.currentTimeMillis();
			//设置图标
			notification.icon = R.drawable.ic_launcher;
			//设置通知特性
			notification.flags = Notification.FLAG_AUTO_CANCEL;
			//设置点击通知栏的操作
			Intent intent = new Intent(mContext,MainActivity.class);
			PendingIntent pintent = PendingIntent.getActivity(mContext, 0, intent, 0);
			notification.contentIntent = pintent;
			//创建RemoteViews对象
			RemoteViews remoteViews = new RemoteViews(mContext.getPackageName(),R.layout.notification);
			//设置开始按钮操作
			Intent intentStart = new Intent(mContext,DownloadService.class);
			intentStart.setAction(DownloadService.ACTION_START);
			intentStart.putExtra("fileInfo", fileInfo);
			PendingIntent piStart = PendingIntent.getService(mContext, 0, intentStart, 0);
			remoteViews.setOnClickPendingIntent(R.id.btStart, piStart);
			//设置结束按钮操作
			Intent intentStop = new Intent(mContext,DownloadService.class);
			intentStop.setAction(DownloadService.ACTION_STOP);
			intentStop.putExtra("fileInfo", fileInfo);
			PendingIntent piStop = PendingIntent.getService(mContext, 0, intentStop, 0);
			remoteViews.setOnClickPendingIntent(R.id.btStop, piStop);
			//设置TextView
			remoteViews.setTextViewText(R.id.tvFile, fileInfo.getName());
			//设置Notification的视图
			notification.contentView = remoteViews;
			//发出通知
			mNotificationManager.notify(fileInfo.getId(), notification);
			//把通知加到集合中
			mNotifications.put(fileInfo.getId(), notification);
		}
	}

	/**
	 * 取消通知
	 * @param id
	 */
	public void cancelNotification(int id){
		mNotificationManager.cancel(id);
		mNotifications.remove(id);
	}

	/**
	 * 更新进度条
	 * @param id
	 * @param progress
	 */
	public void updateNotification(int id,int progress){
		//获得通知
		Notification notification = mNotifications.get(id);
		if(notification != null){
			//修改进度条
			notification.contentView.setProgressBar(R.id.pbFile, 100, progress, false);
			//重新发出通知
			mNotificationManager.notify(id,notification);
		}
	}
}
