package com.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.Vector;

import com.common.MyBatisServerDao;
import com.common.Protocol;

//소켓과 쓰레드를 하나의 클래스로 제작, 수신받은 오브젝트를 처리 후 송신하는 역할 담당.
//모든 기능은 메소드로 대체
public class ChatSocket extends Socket implements Runnable{
	private ChatServer server = null;
	protected ObjectOutputStream oos = null;
	protected ObjectInputStream ois = null;
	private Stack<Exception> errorList = null;
	private Thread thread = null;

	protected ChatSocket(ChatServer server) {
		this.server=server;
	}
	protected void serverStart() throws IOException {
		thread = new Thread(this);
		ois = new ObjectInputStream(getInputStream());
		oos = new ObjectOutputStream(getOutputStream());
		errorList = new Stack<Exception>();
		thread.start();
	}
	/**
	 *  요청 전송 메소드 - 단일
	 *  @param ProtocolNumber, String 입력 시 자동 전송
	 */
	public void send(String... str) throws IOException {
		String msg = "";
		for(int i=0;i<str.length;i++) {
			if(i==str.length-1) 
				msg = msg+str[i];
			else 
				msg = msg+str[i]+Protocol.seperator;
		}
		synchronized (this) {
			oos.writeObject(msg);
		}
	}
	/**
	 *  요청 전송 메소드 - 전체
	 *  @param ProtocolNumber, String 입력 시 자동 전송
	 */
	private void broadcasting(String... str) throws IOException {
		String msg = "";
		for(int i=0;i<str.length;i++) {
			if(i==str.length-1) 
				msg = msg+str[i];
			else 
				msg = msg+str[i]+Protocol.seperator;				
		}
		synchronized (this) {
			for(String key:server.onlineUser.keySet()) {
				server.onlineUser.get(key).oos.writeObject(msg);
			}
		}
	}
	/**
	 *  온라인 유저목록, 오프라인 유저목록 전송
	 *  @param server.onlineUser
	 */
	private void showUser(Map<String, ChatSocket> user) {
		try {
			List<String> onlineUser = new Vector<String>();
			List<String> offlineUser = new Vector<String>();
			for(String p_id:user.keySet()) {
				onlineUser.add(p_id);
			}
			MyBatisServerDao serDao = new MyBatisServerDao();
			offlineUser = serDao.showUser(onlineUser);
			broadcasting(Protocol.showUser, onlineUser.toString(), offlineUser.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 * 현재 생성된 채팅방 목록 클라이언트에게 전송
	 *  
	 */
	private void showRoom(Map<String, List<ChatSocket>> chatRoom) {
		//채팅방 인원 한명도 없으면 리스트에서 없애기 기능 추가해야함.
		try {
			List<ChatSocket> chatMemberRef = new Vector<>();
			for(String room:server.chatRoom.keySet()) { 
				chatMemberRef = server.chatRoom.get(room);
				if(chatMemberRef.size()==0) {
					server.chatRoom.remove(room);
				}
			}
			List<String> serverRoomList = new Vector<>();
			serverRoomList.addAll(server.chatRoom.keySet());//현재 서버에 저장되어있는(생성된) 채팅방 이름 가져오기
			broadcasting(Protocol.showRoom,serverRoomList.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	/**
	 *  채팅방에 해당하는 유저에게 메세지 전송
	 *  @param server.onlineUser
	 */
	private void sendMSG(String roomName, String id, String msg) {//300#roomName#id#msg
		try {
			List<ChatSocket> roomMember = new Vector<>();
			roomMember.addAll(server.chatRoom.get(roomName));
			synchronized (this) {
				for(ChatSocket user: roomMember) {
					user.send(Protocol.sendMessage,roomName,id,msg);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	/**
	 *  채팅방 생성
	 *  @param server.onlineUser
	 */
	private void createRoom(String roomName, String id, List<String> chatMember) {
		List<ChatSocket> chatMemRef = new Vector<ChatSocket>();
		chatMemRef.add(server.onlineUser.get(id));//채팅방을 만든 user의 ChatSocket 넣기. 
		for(String member:chatMember) {
			chatMemRef.add(server.onlineUser.get(member));
		}
		server.chatRoom.put(roomName, chatMemRef);
	}
	
	
	/**
	 * 로그아웃 프로토콜, 메시지 전송
	 * chatRoom의 채팅방마다 해당 유저 소켓 제거
	 * List<String> roomNames - 제거된 유저가 있던 채팅방 이름들을 각 클라이언트에게 전송.
	 * onlineUser에서 해당 유저 제거후 showUser로 갱신
	 * 
	 * @param id
	 */
	private void LogoutMSG(String id) {
		//기존에 오픈된 채팅방에 있다면 퇴장메시지, 주소번지 빼주기
		try {
			List<String> roomNames = new Vector<>(); //클라로 보낼 (로그아웃 할 유저가 속해있는 방)방이름
			List<ChatSocket> chatMemberRef = new Vector<>();
			for(String room : server.chatRoom.keySet()) {
				chatMemberRef = server.chatRoom.get(room); //각 방에 참여하는 소켓리스트
				
				for(int i=0; i<chatMemberRef.size(); i++) {
					if(chatMemberRef.contains(this)) {
						chatMemberRef.remove(this); //채팅방에 있는 유저리스트에서 제거
						server.chatRoom.replace(room, chatMemberRef);
						roomNames.add(room); //해당 채팅방이름을 roomNames에 추가
					}
				}
			}
			for(String key : server.onlineUser.keySet()) { //온라인 유저들에게 각각 쏴주기
				ChatSocket user = server.onlineUser.get(key);
				user.send(Protocol.logout,id,roomNames.toString());
			}
			server.onlineUser.remove(id,this);
			showUser(server.onlineUser);//로그아웃한 dtm갱신
			showRoom(server.chatRoom);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public List<String> getKey(String roomName) {
		List<ChatSocket> user = server.chatRoom.get(roomName);
		List<String> userName = new Vector<String>();
		for(ChatSocket cs:user) {
			for(String id:server.onlineUser.keySet()) {
				if(cs.equals(server.onlineUser.get(id))) {
					userName.add(id);
				}
			}
		}
		List<String> users = new Vector<String>();
		users.addAll(server.onlineUser.keySet());
		for(String s:userName) {
			users.remove(s);
		}
        return users;
    }
	
	/**
	 * String으로 들어온 list 변환 메소드
	 */
	private List<String> decompose(String result){
		List<String> list = new Vector<>();
		String[] values = result.replaceAll("\\p{Punct}", "").split(" ");
		for(String str:values) {
			list.add(str);
		}
		return list;
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
					String msg = ois.readObject().toString();
					System.out.println("msg === : "+msg);
					StringTokenizer st = new StringTokenizer(msg, "#");
					switch(st.nextToken()) {
					case Protocol.checkLogin:{ //100#id#pw
						MyBatisServerDao serDao = new MyBatisServerDao();
						String id = st.nextToken();
						String result = serDao.checkLogin(id, st.nextToken());
						if(id.equals(result)) {
							boolean seccess = true;
							Iterator<String> keys = server.onlineUser.keySet().iterator();
							while(keys.hasNext()) {
								if(result.equals(keys.next())) {//중복로그인
									String overlap = "overlap";
									send(Protocol.checkLogin, overlap);
									seccess = false;
									break;
								}
							}
							if(seccess) {//로그인 성공
								send(Protocol.checkLogin, result);
								server.onlineUser.put(result, this);
								showUser(server.onlineUser);
								showRoom(server.chatRoom);
							}
						}
						else { //로그인 실패
							send(Protocol.checkLogin, result);//로그인실패메세지
						}
					}break;
					case Protocol.addUser:{ //110#id#pw#name
						MyBatisServerDao serDao = new MyBatisServerDao();
						String id = st.nextToken();
						String pw = st.nextToken();
						String name = st.nextToken();
						String result = serDao.addUser(id, pw, name);
						if("fail".equals(result)) {
							send(Protocol.addUser,result);
						}else if("success".equals(result)) {
							send(Protocol.addUser,result);
						}
					}break;
					case Protocol.addUserView:{ //111#
						send(Protocol.addUserView);
					}break;
					case Protocol.showUser:{ //120#

					}break;
					case Protocol.logout:{ //130#myID
						//온라인 유저에서 내 아이디를 뺀 후 다시 showuser해야함.
						String myID = st.nextToken();
						LogoutMSG(myID);
					}break;
					case Protocol.createRoomView:{//201#myID
						//나 자신을 제외한 id들 배열or벡터로 보내주기
						String myID = st.nextToken();
						List<String> chatMember = new Vector<>(); // 온라인 유저 넣어주기
						chatMember.addAll(server.onlineUser.keySet());
						chatMember.remove(myID); //나 자신 제외
						
						//채팅방 이름 중복체크를 위해 서버에 저장된 chatRoom을 클라이언트로 전송.
						String serverRooms = server.chatRoom.keySet().toString();
						send(Protocol.createRoomView,chatMember.toString(),serverRooms);
					}break;
					case Protocol.createRoom:{ //200#roomName#id#chatMember
						String roomName = st.nextToken();
						String id = st.nextToken();
						List<String> chatMember = decompose(st.nextToken());
						createRoom(roomName, id, chatMember);
						showRoom(server.chatRoom);
						send(Protocol.createRoom,roomName);
					}break;
					case Protocol.inviteUserView:{//204#roomName#myID
						String roomName = st.nextToken();
						String myID = st.nextToken();
						List<String> userName = getKey(roomName);
						send(Protocol.inviteUserView,roomName,userName.toString());
						
					}break;
					case Protocol.inviteUser:{//205#roomName#newUserList
						String roomName = st.nextToken();
						List<ChatSocket> chatUser = server.chatRoom.get(roomName);
						List<String> chatMember = decompose(st.nextToken());
						for(String u:chatMember) {
							chatUser.add(server.onlineUser.get(u));
						}
						server.chatRoom.put(roomName, chatUser);
					}break;
					case Protocol.closeRoom:{ //210#roomName#id
						String roomName = st.nextToken();
						String id = st.nextToken();
						List<ChatSocket> chatMemberRef = new Vector<ChatSocket>();
						chatMemberRef.addAll(server.chatRoom.get(roomName));
						for(ChatSocket user:chatMemberRef) {
							user.send(Protocol.closeRoom,roomName,id);
						}
						ChatSocket closeUser = server.onlineUser.get(id);
						chatMemberRef.remove(closeUser);
						server.chatRoom.replace(roomName, chatMemberRef);
						showRoom(server.chatRoom);
					}break;
					case Protocol.sendMessage:{ //300#roomName#id#msg
						sendMSG(st.nextToken(), st.nextToken(), st.nextToken());
						
					}break;
					case Protocol.sendEmoticon:{ //310#
						
					}break;
					case Protocol.sendFile:{ //320#roomName#filePath#filName#myID
						String roomName = st.nextToken();
						String filePath = st.nextToken();
						String fileName = st.nextToken();
						String id = st.nextToken();
						List<ChatSocket> roomMember = new Vector<>();
						roomMember.addAll(server.chatRoom.get(roomName));
						for(ChatSocket user: roomMember) {
							user.send(Protocol.sendFile, roomName, id, fileName);
						}
					}break;
					}
				}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

}
