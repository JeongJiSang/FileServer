package com.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Stack;
import java.util.StringTokenizer;

import com.common.Protocol;

//소켓과 쓰레드를 하나의 클래스로 제작, 수신받은 오브젝트를 처리 후 송신하는 역할 담당.
//모든 기능은 메소드로 대체
public class ChatSocket extends Socket implements Runnable{
	ObjectOutputStream oos = null;
	ObjectInputStream ois = null;
	private Stack<Exception> errorList = null;
	Thread thread = null;

	protected void serverStart() throws IOException {
		thread = new Thread(this);
		ois = new ObjectInputStream(getInputStream());
		oos = new ObjectOutputStream(getOutputStream());
		errorList = new Stack<Exception>();
		thread.start();
	}

	@Override
	public void run() {
		boolean isStop = false;
		if(ois==null || this==null) { //무한루프 방지
			isStop = true;
		}
		try {
			run_start://while문같은 반복문 전체를 빠져 나가도록 처리할 때
				while(!isStop) {
					System.out.println("벨레레레레렐");
					String msg = ois.readObject().toString();
					StringTokenizer st = new StringTokenizer(msg, "#");
					switch(st.nextToken()) {
					case Protocol.checkLogin:{ //100#

					}break;
					case Protocol.addUser:{ //110#

					}break;
					case Protocol.showUser:{ //120#

					}break;
					case Protocol.createRoom:{ //200#

					}break;
					case Protocol.closeRoom:{ //210#

					}break;
					case Protocol.sendMessage:{ //300#

					}break;
					case Protocol.sendEmoticon:{ //310#

					}break;
					case Protocol.sendFile:{ //320#

					}break;
					}
				}
		} catch(Exception e) {

		}
	}

}
