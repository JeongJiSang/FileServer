package com.server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.Stack;

import com.common.FileBitConverter;
import com.common.FileListener;


public class FileSocket extends Socket implements Runnable{
	private Thread thread = null;
	private InputStream receiver = null;
	private ObjectInputStream ois = null;
	private OutputStream sender = null;
	private Stack<Exception> errorList = null;
	private File savefile = null;
	private FileListener listener = null;
	
	
	public FileSocket(File savePath, int processSize) throws IOException {
		this.savefile = savePath;
	}
	
	protected void serverStart() throws IOException {
		thread = new Thread(this);
		receiver = getInputStream();
		sender = getOutputStream();
		ois = new ObjectInputStream(getInputStream());
		errorList = new Stack<Exception>();
		thread.start();
	}

	@Override
	public void run() {
		byte[] lengthData = null;
		int length = 0;
		String filename = "";
		FileOutputStream out = null;
		try {
			String savePath = ois.readObject().toString();
			lengthData = new byte[FileBitConverter.INTBITSIZE];
			File Path = new File(savefile.getPath() + "\\" +savePath);
			if(!Path.exists()) {
				Path.mkdirs();
			}
//파일이름 사이즈를 받는다.
			receiver.read(lengthData, 0, lengthData.length);
			length = FileBitConverter.toInt32(lengthData, 0);
//파일 사이즈가 없으면 종료한다.
			if (length == 0) {
				return;
			}
// 다운로드 시작 리스너 호출(이벤트 형식)
			if (listener != null) {
				listener.downloadStart();
			}
// 파일 이름 설정
			byte[] filenamebyte = new byte[length];
			receiver.read(filenamebyte, 0, filenamebyte.length);
			filename = new String(filenamebyte);
			//만들어진 폴더경로에 client에서 보낸 파일 저장.
			File file = new File(savefile.getPath() + "\\" +savePath+"\\"+ filename);
			System.out.println("saved filePath: "+file);
//파일이 있으면 삭제
			if (file.exists())
				file.delete();
			out = new FileOutputStream(file);
			System.out.println("fileSocket outStream: "+out);
//파일 사이즈를 받는다.
			receiver.read(lengthData, 0, lengthData.length);
			length = FileBitConverter.toInt32(lengthData, 0);
//파일 사이즈가 없으면 종료
			if (length == 0) {
				return;
			}
//파일 받기 시작
			receiveWrite(out, length, listener);
// 다운로드 종료 리스너 호출(이벤트 형식)
			if (listener != null) {
				listener.downloadComplate();
				listener.fileSaveComplate(savefile.getPath() + "\\" + filename);
			}
		} catch (Exception e) {
// 에러가 발생하면 에러 리스너 호출
			if (listener != null) {
				listener.receiveError(e);
			}
			errorList.push(e);
		} finally {
			try {
				if (isConnected()) {
					close();
				}
				if (out != null) {
					out.close();
				}
			} catch (Exception ex) {
				if (listener != null) {
					listener.receiveError(ex);
				}
				errorList.push(ex);
			}
		}
	}
	/**
	 * 파일 수신 메소드
	 */
	private void receiveWrite(FileOutputStream out, int length, FileListener listener) throws Exception {
		//커넥션 체크
		if (isClosed()) {
			throw new SocketException("socket closed");
		}
		if (!isConnected()) {
			throw new SocketException("socket diconnection");
		}
		byte[] buffer = new byte[4096];
		int progressCount = 0;
		while (progressCount < length) {
			int bufferSize = 0;
			while ((bufferSize = receiver.read(buffer)) > 0) {
				out.write(buffer, 0, bufferSize);
				progressCount += bufferSize;
				// 리스너 파일 수신 진행율 호출
				if (listener != null) {
					listener.progressFileSizeAction(progressCount, length);
				}
				if (progressCount >= length) {
					break;
				}
			}
		}
	}
}
