package com.common;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;


public class Protocol implements Serializable{
	//유저 아이디 저장 할 필요가 있을까? 
	public static String myID = "";
	//온라인 유저 리스트
	public static Vector<String> onlineUser = new Vector<String>();
	//오프라인 유저 리스트
	public static Vector<String> offlineUser = new Vector<String>();
	
	
	//유저 로그인 관련 프로토콜
	public static final String checkLogin = "100"; //로그인
	public static final String addUser = "110"; //회원가입
	public static final String addUserView = "111";//회원가입 화면 오픈
	public static final String showUser = "120"; //유저리스트
	public static final String logout = "130"; //로그아웃
	
	//채팅방 생성 관련 프로토콜
	public static final String createRoom = "200"; //채팅방 생성
	public static final String createRoomView = "201"; //채팅방 생성
	public static final String closeRoom = "210"; // 채팅방 퇴장
	
	//메세지 전송 관련 프로토콜
	public static final String sendMessage = "300"; //메세지 전송
	public static final String sendEmoticon = "310"; //이모티콘 전송
	public static final String sendFile = "320"; // 파일 전송
	
	//토큰 구분 값
	public static final String seperator= "#"; 
	
}
