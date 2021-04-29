package com.server;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;

import com.common.FileException;
import com.common.FileServerListener;


public class FileServer extends ServerSocket implements Runnable {
	private Map<String, FileSocket> clients = null;
	private Thread thread = null;
	private Stack<Exception> errorList = null;
	private File savePath = null;
	private int processSize = 1;
	private FileServerListener listener = null;

	public FileServer(int port, File savePath) 
			throws IOException, FileException {
		this(port, savePath, 1);
	}
	public FileServer(int port, File savePath, int processSize) 
			throws IOException, FileException {
		super(port);
		this.savePath = savePath;
		if (!savePath.isDirectory() || !savePath.exists()) {
			throw new FileException("File Path wrong!");
		}
		errorList = new Stack<Exception>();
		clients = new Hashtable<String, FileSocket>();
		this.processSize = processSize;
		this.start();
	}

	/**
	 * 서버 개시
	 */
	public void start() throws FileException {
		thread = new Thread(this);
		thread.start();
	}

	public File getFile() {
		return this.savePath;
	}

	public void setFileTransferServerListener(FileServerListener listener) {
		this.listener = listener;
	}

	/**
	 * 클라이언트가 접속을 할 때 실행되는 메소드
	 */
	public FileSocket accept() throws FileException, IOException {
		if (isClosed())
			throw new FileException("Socket is closed");
		if (!isBound())
			throw new FileException("Socket is not bound yet");
		if (this.savePath == null)
			throw new FileException("file path wrong!");
		FileSocket s = new FileSocket(this.savePath, this.processSize);
		implAccept(s);
		s.serverStart();
		return s;
	}

	/**
	 * 멀티 스레드 환경
	 */
	@Override
	public void run() {
		boolean isStop = false;
		while (!isStop) {
			try {
				FileSocket client = this.accept();
				clients.put("a", client);
				if (this.listener != null) {
					this.listener.clientConnection(client);
				}
			} catch (IOException ie) {
				errorList.push(ie);
				if (this.listener != null) {
					this.listener.connectionError(ie);
				}
			} 
		}
	}
	public Exception getLastError() {
		if (errorList.size() > 0) {
			Exception e = errorList.pop();
			return e;
		} else {
			return null;
		}
	}
	/**
	 * 서버 종료
	 */
	public void close() throws IOException {
		for (Socket client : clients.values()) {
			client.close();
		}
		super.close();
		if (this.listener != null) {
			this.listener.connectionClose();
		}
	}
}
