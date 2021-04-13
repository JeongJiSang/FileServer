package com.common;

public interface FileListener {
	public void progressFileSizeAction(long complateSize, long filesize);

	public void downloadStart();

	public void downloadComplate();

	public void uploadStart();

	public void uploadComplate();

	public void fileSaveComplate(String filepath);

	public void receiveError(Exception e);

	public void connectionClose();
}
