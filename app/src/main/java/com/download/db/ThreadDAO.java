package com.download.db;

import java.util.List;

import com.download.entities.ThreadInfo;
/**
 * 线程信息数据访问接口
 *
 */
public interface ThreadDAO {

	/**
	 * 插入线程信息
	 * @param thread_id
	 * @param url
	 * @param start
	 * @param end
	 * @param finished
	 */
	public void insertThreadInfo(ThreadInfo threadInfo);

	/**
	 * 更新线程下载进度
	 * @param thread_id
	 * @param url
	 * @param finished
	 */
	public void updateThreadInfo(int thread_id, String url,
								 int finished);

	/**
	 * 删除下载信息
	 * @param url
	 */
	public void deleteThreadInfo(String url);

	/**
	 * 获取线程信息
	 */
	public List<ThreadInfo> getThreadInfos(String url);

	/**
	 * 线程是否存在
	 */
	public boolean isExists(String url, int thread_id);
}
