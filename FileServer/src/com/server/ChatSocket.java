package com.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
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
		oos.writeObject(msg);
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
	 *  채팅방에 해당하는 유저에게 메세지 전송
	 *  @param server.onlineUser
	 */
	private void sendMSG(String roomName, String id, String msg) {
		try {
			List<ChatSocket> roomMember = new Vector<>();
			roomMember.addAll(server.chatRoom.get(roomName));
			for(ChatSocket user: roomMember) {
				user.oos.writeObject(Protocol.sendMessage+Protocol.seperator
						+roomName+Protocol.seperator
						+id+Protocol.seperator
						+msg);
			}
		} catch (Exception e) {

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
			//채팅방에 참여중인 user들의 ChatSocket을 넣기.
			chatMemRef.add(server.onlineUser.get(member));
		}
		//채팅방 이름과 채팅방에 참여중인 모든 유저들을 Map으로 관리.
		server.chatRoom.put(roomName, chatMemRef);
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
							}
						}
						else { //로그인 실패
							send(Protocol.checkLogin, result);//로그인실패메세지
						}
					}break;
					case Protocol.addUser:{ //110#
						MyBatisServerDao serDao = new MyBatisServerDao();
					}break;
					case Protocol.addUserView:{ //111
						send(Protocol.addUserView);
					}break;
					case Protocol.showUser:{ //120#
						MyBatisServerDao serDao = new MyBatisServerDao();
					}break;
					case Protocol.createRoomView:{ //201#myID
						String myID = st.nextToken();
						List<String> chatMember = new Vector<>();
						for(String id : server.onlineUser.keySet()) {
							//List chatMember에 onlineUser들 다시 넣어주기.
							chatMember.add(id);
						}
						// onlineUser들 중에서 현재 접속한 myID제거.
						chatMember.remove(myID);
						send(Protocol.createRoomView, chatMember.toString());
						
					}break;
					case Protocol.createRoom:{ //200#roomName#id#chatMember
						String roomName = st.nextToken();
						String id = st.nextToken();
						List<String> chatMember = decompose(st.nextToken());
						createRoom(roomName, id, chatMember);
						send(Protocol.createRoom, roomName, id, chatMember.toString());
					}break;
					case Protocol.closeRoom:{ //210#roomName#id
						String roomName = st.nextToken();
		                String id = st.nextToken();
		                  List<ChatSocket> chatMemberRef = new Vector<ChatSocket>();
		                  //채팅방에 있는 user들의 ChatSocket을 chatMemberRef에 새로 주입. 
		                  chatMemberRef.addAll(server.chatRoom.get(roomName));
		                  //서버에 있는 onlineUser리스트에서, 나가는 유저의 ChatSocket을 closeUser에 주입. 
		                  ChatSocket closeUser = server.onlineUser.get(id);
		                  //chatMemberRef에서 나가는 user의 ChatSocket을 제거.
		                  chatMemberRef.remove(closeUser);
		                  //퇴장한 user를 제외한, 채팅방에 있는 유저들에게 oos 발송. 
		                  server.chatRoom.replace(roomName, chatMemberRef);
		                  
		                  for(ChatSocket user:chatMemberRef) {
		                     user.oos.writeObject(Protocol.closeRoom+Protocol.seperator
		                                    +roomName+Protocol.seperator
		                                    +id);
		                  }
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
						System.out.println(roomName+"#"+filePath+"#"+fileName+"#"+id);
						try {
							List<ChatSocket> roomMember = new Vector<>();
							roomMember.addAll(server.chatRoom.get(roomName));
							for(ChatSocket user: roomMember) {
								user.oos.writeObject(Protocol.sendFile+Protocol.seperator
										+roomName+Protocol.seperator
										+id+Protocol.seperator
										+fileName+Protocol.seperator);
							}
							System.out.println("msg: "+Protocol.sendFile+Protocol.seperator
									+roomName+Protocol.seperator
									+id+Protocol.seperator
									+fileName);
						} catch (Exception e) {

						}
					}break;
					}
				}
		} catch(Exception e) {

		}
	}

}
