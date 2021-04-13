package com.server;

import java.io.File;

import com.file.FileServer;

public class ServerMain {
	public static void main(String[] args) {
		try {
			FileServer server = new FileServer(9999, new File("D:\\FileServer"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
