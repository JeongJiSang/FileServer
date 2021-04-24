package com.server;

import java.io.File;

public class ServerMain {
	public static void main(String[] args) {
		try {
			ChatServer chat = new ChatServer(9100);
			File serverStorage = new File("C:\\FileServerStorage");
			if(!serverStorage.exists()) {
				serverStorage.mkdirs();
			}
			FileServer server = new FileServer(9101, serverStorage);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
