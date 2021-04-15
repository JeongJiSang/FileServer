package com.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Map;

//1개만 존재하는 서버, 모든 클라이언트의 연결 및 클라이언트 1:1 맞춤 쓰레드 할당 담당
//접속중인 쓰레드 관리도 담당
public class ChatServer extends ServerSocket implements Runnable{
	private Thread thread = null;
	
	/************************
	 * Key : 접속된 유저 아이디
	 * Value : 접속된 유저의 소켓
	 ************************/
	public Map<String, ChatSocket> onlineUser = null; 
	
	
	
	
	/**
	 * 생성자
	 */
	public ChatServer(int port) throws IOException {
		super(port);
		this.start();
		
	}
	
	/**
	 * 서버 개시
	 */
	public void start() {
		thread = new Thread(this);
		thread.start();
	}
	/**
	 * 클라이언트가 접속을 할 때 실행되는 메소드
	 */
	public ChatSocket accpet() throws IOException {
		ChatSocket chat = new ChatSocket();
		implAccept(chat);
		chat.serverStart();
		return chat;
	}
	@Override
	public void run() { // 클라이언트 접속을 계속 받아야 하기때문에 쓰레드 구성
		boolean isStop = false;
		while (!isStop) {
			try {
				ChatSocket chat = this.accpet();
				
			} catch (Exception e) {
				
			}
		}
	}

}
