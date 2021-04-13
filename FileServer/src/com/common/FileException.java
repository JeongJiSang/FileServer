package com.common;

import java.net.SocketException;

public class FileException extends SocketException {
	private static final long serialVersionUID = 1L;

	public FileException(){
	}

	public FileException(String arg){
		super(arg);
	}
}	