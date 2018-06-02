package com.coin.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import com.coin.vo.LocationVO;
import com.coin.vo.WorkVO;

public class WorkDAO {
	static final String table_name = "works";
	
	static final String[] columnNames = {
			"id",
			"pic_id",
			"hirer_id",
			"title",
			"description",
			"location_id"
	};
	
	public WorkVO getById(int id) {
		return getById_private(id);
	}
	public List<WorkVO> getListByTitle(String title){
		return getListBy("title",title,true); 
	}
	public List<WorkVO> getListByWorkerID(String workerID){
		return getListBy("worker_id",workerID,false); 
	}
	public List<WorkVO> getListByHirerID(int hiererID){
		return getListBy("hirer_id",hiererID); 
	}
	public List<WorkVO> getListByRegion(double latitude , double longitude , double radius){
		return getListByRegion_private(latitude , longitude , radius);
	}
	public WorkVO makeNew(String hirer_id,String title,String description, LocationVO loc) {
		return makeNew_private(hirer_id,title,description,loc);
	}
	public WorkVO makeNew(String hirer_id,String title,String description, double x, double y) {
		return makeNew_private(hirer_id,title,description,JDBCutil.locationDao.makeNew(x, y));
	}
	public int updatePicture(WorkVO vo,String pic_loc) {
		return updatePicture(vo.getId(),pic_loc);
	}
	public int updatePicture(int id,String pic_loc) {
		int pic_id = JDBCutil.pictureDao.makeNew(pic_loc).getId();
		return update(id,"pic_id",pic_id);
	}
	public int updateTitle(WorkVO vo,String title) {
		return update(vo.getId(), "title",title);
	}
	public int updateTitle(int id,String title) {
		return update(id, "title",title);
	}
	public int updateDescription(WorkVO vo,String description) {
		return update(vo.getId(), "description",description);
	}
	public int updateDescription(int id,String description) {
		return update(id, "description",description);
	}
	public int delete(WorkVO vo) {
		return delete_private(vo.getId());
	}
	public int delete(int id) {
		return delete_private(id);
	}
	
	
	
	
	static final String insert_values;
	static {
		StringBuilder sb = new StringBuilder();
		
		sb.append("(");
		for(int i = 0 ; i < columnNames.length ; i++) {
			sb.append(columnNames[i]);
			if(i != columnNames.length -1) {
				sb.append(",");
			}
		}
		sb.append(") values (");
		for(int i = 0 ; i < columnNames.length ; i++) {
			sb.append("?");
			if(i != columnNames.length -1) {
				sb.append(",");
			}
		}
		sb.append(")");
		
		insert_values = sb.toString();
	}
	
	
	public List<WorkVO> getListBy(String type, String val, boolean like){
		if(like) {
			return getListBy_private(type, val, like);
		}else {
			return getListBy_private(type,"'"+val+"'",like);
		}
	}
	public List<WorkVO> getListBy(String type, double val){
		return getListBy_private(type, Double.toString(val), false);
	}
	public List<WorkVO> getListBy(String type, int val){
		return getListBy_private(type, Integer.toString(val), false);
	}
	private List<WorkVO> getListBy_private(String type, String val , boolean like) {
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		//int result = 0;
		List<WorkVO> list  = new ArrayList<>();
		
		try {
			con = JDBCutil.getConnection();
			
			String likeOrEquals = null;
			if(like) {
				likeOrEquals = " like '%"+val+"%'";
			}else {
				likeOrEquals = "="+val;				
			}
			
			
			ps = con.prepareStatement("select * from "
										+table_name
										+" where " 
										+ type 
										+likeOrEquals);
			
			
										
			//result = ps.executeUpdate();
			rs = ps.executeQuery();
		
			
			while(rs.next()) {
				WorkVO vo = new WorkVO();
				vo.setId(rs.getInt(columnNames[0]));
				vo.setPic_id(rs.getInt(columnNames[1]));
				vo.setHirer_id(rs.getString(columnNames[2]));
				vo.setTitle(rs.getString(columnNames[3]));
				vo.setDescription(rs.getString(columnNames[4]));
				vo.setLocation_id(rs.getInt(columnNames[5]));
				list.add(vo);
			}
			
			
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally {
			JDBCutil.close(con, ps, rs);
		}
		return list;
	}
	

	private WorkVO makeNew_private(String hirer_id, String title, String description, LocationVO loc) {
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		int id = 1;
		//List<UserVO> list  = new ArrayList<>();
		
		try {
			con = JDBCutil.getConnection();
			
			ps = con.prepareStatement(" select (max(id)+1) as id from "+table_name);
			
			rs = ps.executeQuery();
			while(rs.next()) {
				id = rs.getInt("id");
			}
			
			ps = con.prepareStatement(" insert into works (id, hirer_id,title,description,location_id) " + 
					"values ( ? , ? , ? , ? ,?)");
			
			int count = 1;
			ps.setInt(count++,id);
			ps.setString(count++,hirer_id);
			ps.setString(count++,title);
			ps.setString(count++,description);
			ps.setInt(count++,loc.getId());
										
			ps.executeUpdate();
			//rs = ps.executeQuery();
			
			return getById(id);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally {
			JDBCutil.close(con, ps, rs);
		}
		return null;
	}
	
	private List<WorkVO> getListByRegion_private(double latitude , double longitude , double radius) {
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		//int result = 0;
		List<WorkVO> list  = new ArrayList<>();
		
		try {
			con = JDBCutil.getConnection();
			
			// there is some distortion , not an accurate way to calibrate 
			// x is longitude
			String sql = "select works.id,works.pic_id,works.hirer_id,works.title,works.description,works.location_id " + 
					" from works,locations " + 
					" where (works.location_id = locations.id) " +
				    " and ((locations.x-?)*(locations.x-?)+(locations.y-?)*(locations.y-?))< ?";
			
			ps = con.prepareStatement(sql);
			
			//ROK : 124~132 33~43 			
			
			int count = 1;
			ps.setDouble(count++, longitude);
			ps.setDouble(count++, longitude);
			ps.setDouble(count++, latitude);
			ps.setDouble(count++, latitude);
			ps.setDouble(count++, radius*radius);
			
										
			//result = ps.executeUpdate();
			rs = ps.executeQuery();
	
			
			while(rs.next()) {
				WorkVO vo = new WorkVO();
				vo.setId(rs.getInt(columnNames[0]));
				vo.setPic_id(rs.getInt(columnNames[1]));
				vo.setHirer_id(rs.getString(columnNames[2]));
				vo.setTitle(rs.getString(columnNames[3]));
				vo.setDescription(rs.getString(columnNames[4]));
				vo.setLocation_id(rs.getInt(columnNames[5]));
				list.add(vo);
			}
			
			
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally {
			JDBCutil.close(con, ps, rs);
		}
		return list;
	}

	private WorkVO getById_private(int id) {
		List<WorkVO> list = getListBy("id",Integer.toString(id),false);
		if(!list.isEmpty()) {
			return list.get(0);
		}else {
			return null;
		}
	}
	
	private int delete_private(int id) {
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		int result = 0;
		//List<UserVO> list  = new ArrayList<>();
		
		try {
			con = JDBCutil.getConnection();
			
			
			ps = con.prepareStatement("delete from "
										+table_name
										+" where id=?");
			
			int count = 1;
			ps.setInt(count++,id);
			
										
			result = ps.executeUpdate();
			//rs = ps.executeQuery();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally {
			JDBCutil.close(con, ps, rs);
		}
		return result;
	}
	
	
	
	public int update(int id,String type , String val) {
		return update_private(id,type, new StringBuilder().append("'").append(val).append("'").toString());
	}
	public int update(int id,String type , int val) {
		return update_private(id,type, Integer.toString(val));
	}
	private int update_private(int id,String type, String val) {
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		int result = 0;
		//List<UserVO> list  = new ArrayList<>();
		
		try {
			con = JDBCutil.getConnection();
			
			
			ps = con.prepareStatement("update "
										+table_name
										+" set "
										+ type
										+" = "
										+val
										+" where id=?");		
			int count = 1;
			ps.setInt(count++,id);
			
										
			result = ps.executeUpdate();
			//rs = ps.executeQuery();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally {
			JDBCutil.close(con, ps, rs);
		}
		return result;
	}
}
