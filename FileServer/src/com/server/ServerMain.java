package com.server;

import java.io.File;

public class ServerMain {
	public static void main(String[] args) {
		try {
			ChatServer chat = new ChatServer(9100);
			//FileServer server = new FileServer(9999, new File("D:\\FileServer"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
