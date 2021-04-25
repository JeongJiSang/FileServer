package com.common;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import com.Mapper.MyBatisCommonFactory;

public class MyBatisServerDao {
	
	SqlSessionFactory sqlSessionFactory = null;
	public MyBatisServerDao() {
		sqlSessionFactory = MyBatisCommonFactory.getInstance();
	}
	public String gettest(){
		SqlSession sqlSession = null;
		String test = "";
	    try {
	    	sqlSession = sqlSessionFactory.openSession();
	    	test = sqlSession.selectOne("AllSearch");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			sqlSession.close();
		}
	    return test;
	}
	public String addUser(String id, String pw, String name) {

	      SqlSession sqlSession = null;
	      Map<String,String> user = new HashMap<String,String>();
	      user.put("id",id);
	      user.put("pw",pw);
	      user.put("name",name);
	      try {
	         sqlSession = sqlSessionFactory.openSession();
	         sqlSession.insert("addUser", user);         
	      }catch (Exception e) {
	         e.printStackTrace();
	      }finally {
	         sqlSession.close();
	      }
	      return user.get("msg");
	   }
	

	/*****************************
	 * 프로시저 호출 이용
	 * @param id
	 * @param pw
	 * @return difid or difpw or id
	 */
	public String checkLogin(String id, String pw) {
		SqlSession sqlSession = null;
		Map<String, String> user = new HashMap<String, String>();
		user.put("id", id);
		user.put("pw", pw);
		try {
			sqlSession = sqlSessionFactory.openSession();
			sqlSession.selectOne("checkLogin", user);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			sqlSession.close();
		}
		return user.get("msg");
	}
	/*****************************
	 * 프로시저 없이 실행해보려고 노력
	 * @param id
	 * @param pw
	 * @return difid or difpw or id
	 */
	public String checkLogin2(String id, String pw) {
		SqlSession sqlSession = null;
		int size = 0;
		Map<String, String> user = new HashMap<String, String>();
		user.put("id", id);
		user.put("pw", pw);
		List<String> result = new Vector<String>();	
		try {
			sqlSession = sqlSessionFactory.openSession();
			result = sqlSession.selectList("checkLogin", user);
			size = result.size();
			System.out.println(size);
			for(String m:result) {
				System.out.println(m);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			sqlSession.close();
		}
		return result.get(size-1);
	}
	
	/****************************
	 * @param onlineUser
	 * @return offlineUser
	 ****************************/
	public List<String> showUser(List<String> onlineUser) {
		List<String> offUser = new Vector<String>();
		System.out.println(onlineUser.toString());
		SqlSession sqlSession = null;
		try {
	    	sqlSession = sqlSessionFactory.openSession();
	    	offUser = sqlSession.selectList("showUser", onlineUser);
	    	
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			sqlSession.close();
		}
		return offUser;
	}
	
	
	public static void main(String args[]) {
		MyBatisServerDao serDao = new MyBatisServerDao();
		String msg = serDao.addUser("awnefjanhrthwejf", "tige", "eee");
		System.out.println(msg);
		
	}

}