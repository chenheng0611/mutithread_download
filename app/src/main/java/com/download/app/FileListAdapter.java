package com.download.app;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.download.entities.FileInfo;
import com.download.services.DownloadService;
/**
 * 文件列表适配器
 */
public class FileListAdapter extends BaseAdapter {

	private Context mContext = null;
	private List<FileInfo> mFileList = null; //文件数据列表
	private Messenger mMessenger = null;

	public FileListAdapter(Context mContext, List<FileInfo> mFileList) {
		this.mContext = mContext;
		this.mFileList = mFileList;
	}

	public void setMessenger(Messenger mMessenger){
		this.mMessenger = mMessenger;
	}

	@Override
	public int getCount() {
		return mFileList.size();
	}

	@Override
	public Object getItem(int position) {
		return mFileList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View view, ViewGroup viewGroup) {
		ViewHolder holder = null;
		if(view == null){
			//加载视图
			view = LayoutInflater.from(mContext).inflate(R.layout.listitem, null);
			//获得布局中的控件
			holder = new ViewHolder();
			holder.tvFile = (TextView) view.findViewById(R.id.tvFile);
			holder.btStop = (Button) view.findViewById(R.id.btStop);
			holder.btStart = (Button) view.findViewById(R.id.btStart);
			holder.pbFile = (ProgressBar) view.findViewById(R.id.pbFile);
			view.setTag(holder);
		}else{
			holder = (ViewHolder) view.getTag();
		}
		final FileInfo fileInfo = mFileList.get(position);
		//显示进度
		holder.pbFile.setProgress(fileInfo.getFinished());
		//显示文件名
		holder.tvFile.setText(fileInfo.getName());
		//设置最大进度
		holder.pbFile.setMax(100);
		//设置开始按钮的监听
		holder.btStart.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				//通知Service开始下载
//				Intent intent = new Intent(mContext,DownloadService.class);
//				intent.setAction(DownloadService.ACTION_START);
//				intent.putExtra("fileInfo", fileInfo);
//				mContext.startService(intent);
				Message msg = new Message();
				msg.what = DownloadService.MSG_START;
				msg.obj = fileInfo;
				try {
					mMessenger.send(msg);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		});
		//设置停止按钮的监听
		holder.btStop.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				//通知Service停止下载
//				Intent intent = new Intent(mContext,DownloadService.class);
//				intent.setAction(DownloadService.ACTION_STOP);
//				intent.putExtra("fileInfo", fileInfo);
//				mContext.startService(intent);
				Message msg = new Message();
				msg.what = DownloadService.MSG_STOP;
				msg.obj = fileInfo;
				try {
					mMessenger.send(msg);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		});
		return view;
	}

	/**
	 * 更新列表项中的进度条
	 */
	public void updateProgress(int id,int progress){
		//修改视图的数据
		FileInfo fileInfo = mFileList.get(id);
		fileInfo.setFinished(progress);
		//通知刷新视图
		notifyDataSetChanged();
	}

	/**
	 * 用此类保存视图中的控件，这样不用每次重新获取控件
	 */
	static class ViewHolder{
		TextView tvFile;
		Button btStop,btStart;
		ProgressBar pbFile;
	}
}
