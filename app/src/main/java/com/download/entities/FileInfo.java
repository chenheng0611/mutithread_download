package com.download.entities;

import java.io.Serializable;


/**
 * 文件信息
 */
public class FileInfo implements Serializable{
	private int id;
	private String url;
	private String name;
	private int length;
	private int finished;
	public FileInfo(int id, String url, String name, int length, int finished) {
		super();
		this.id = id;
		this.url = url;
		this.name = name;
		this.length = length;
		this.finished = finished;
	}
	public FileInfo() {
		super();
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public int getLength() {
		return length;
	}
	public void setLength(int length) {
		this.length = length;
	}
	public int getFinished() {
		return finished;
	}
	public void setFinished(int finished) {
		this.finished = finished;
	}
	@Override
	public String toString() {
		return "FileInfo [id=" + id + ", url=" + url + ", name=" + name
				+ ", length=" + length + ", finished=" + finished + "]";
	}


}
