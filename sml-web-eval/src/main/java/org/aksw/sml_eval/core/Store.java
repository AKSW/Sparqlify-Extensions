package org.aksw.sml_eval.core;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.aksw.commons.util.jdbc.SqlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Store {
	
	private static final Logger logger = LoggerFactory.getLogger(Store.class);
	
	private DataSource ds;
	
	
	public Store(DataSource ds) {
		this.ds = ds;
	}
	
	
	
	/**
	 * TODO: We need to get a score sheet summary...
	 * TODO We need add information about the tasks that have not yet been completed.
	 * 
	 * @param userId
	 * @return
	 * @throws SQLException
	 */
	public EvalSummary getSummary(Integer userId) throws SQLException {
		// Fetch which tasks the user has completed.
		
		Connection conn = ds.getConnection();		
		
		Map<String, LangSummary> langSummaries = new HashMap<String, LangSummary>();
		
		List<String> evalOrder;
		String evalMode;
		try {
			evalOrder = getEvalOrder(userId);
			evalMode = getEvalMode(userId);
			
			
			String sql = "SELECT DISTINCT user_id, tool_id, task_id, is_solution FROM submission WHERE user_id = ? AND is_solution = true";
			ResultSet rs = SqlUtils.executeCore(conn, sql, userId);
	
			while(rs.next()) {
				Integer uid = rs.getInt("user_id");
				String langId = rs.getString("tool_id");
				String taskId = rs.getString("task_id");
				boolean isSolution = rs.getBoolean("is_solution");
			
				LangSummary langSummary = langSummaries.get(langId);
				if(langSummary == null) {
					langSummary = new LangSummary();
					
					langSummaries.put(langId, langSummary);
				}
				
				TaskSummary taskSummary = langSummary.getTaskSummaries().get(taskId);
				if(taskSummary == null) {
					taskSummary = new TaskSummary(taskId, isSolution);
					langSummary.getTaskSummaries().put(taskId, taskSummary);
				}
			}
		}
		finally {
			if(conn != null) {
				conn.close();
			}
		}
		
		EvalSummary result = new EvalSummary(userId, langSummaries, evalMode, evalOrder);

		
		
		return result;
	}
	
		
	public Long countLang(Connection conn, String name) throws SQLException {
		String sql = "SELECT COUNT(*) FROM eval_order WHERE sequence_id = 0 AND name = ?";
		Long result = SqlUtils.execute(conn, sql, Long.class, name);
		return result;
	}
	
	// Actually, language and tool are independent - one could want to e.g.  use sml with sparql map.
	public List<String> generateToolList() throws SQLException {
		List<String> ml = Arrays.asList("sml", "r2rml");

		Connection conn = null;
		try {
			conn = ds.getConnection();

			Long sml = countLang(conn, "sml");
			Long r2rml = countLang(conn, "r2rml");
			
			if(r2rml > sml) {
				Collections.reverse(ml);
			} else if(r2rml == sml) {
				Collections.shuffle(ml);				
			}
			
		} finally {
			close(conn);
		}
		

		return ml;
	}
	
	public String generateLimesToken(Connection conn, Integer userId, String lang) throws SQLException {
		String sql = "SELECT \"token\" FROM \"limes_token\" WHERE \"user_id\" IS NULL LIMIT 1";
		String token = SqlUtils.execute(conn, sql, String.class);
		
		if(token == null) {
			//throw
		}
		
		logger.debug("Limes Token is: " + token);
		
		String update = "UPDATE \"limes_token\" SET \"user_id\" = ?, \"lang\" = ? WHERE \"token\" = ?";
		SqlUtils.execute(conn, update, Void.class, userId, lang, token);
		
		
		return token;
	}
	
	public Map<String, String> fetchLimesToken(Connection conn, Integer userId) throws SQLException {
		Map<String, String> result = new HashMap<String, String>();
		String sql = "SELECT \"lang\", \"token\" FROM \"limes_token\" WHERE \"user_id\" = ?";

		ResultSet rs = SqlUtils.executeCore(conn, sql, userId);
		while(rs.next()) {
			String lang = rs.getString("lang");
			String token = rs.getString("token");
			
			result.put(lang, token);
		}
		
		
		//String token = SqlUtils.execute(conn, sql, String.class, userId, lang);
		
		return result;
	}
	

	public String getLastTaskSubmission(Integer userId, String taskId, boolean requireWorking)
			throws SQLException
	{
		Connection conn = ds.getConnection();
		
		String result;
		try {
			result = getLastTaskSubmission(conn, userId, taskId, requireWorking);
		}
		finally {
			if(conn != null) {
				conn.close();
			}
		}
		
		return result;
	}
	
	public String getLastTaskSubmission(Connection conn, Integer userId, String taskId, boolean requireWorking)
			throws SQLException
	{
		String sql = "SELECT \"mapping\" FROM \"submission\" WHERE user_id = ? AND task_id = ? AND is_working = TRUE OR is_working = ? ORDER BY ts LIMIT 1";
		
		
		String result = SqlUtils.execute(conn, sql, String.class, userId, taskId, requireWorking);
		return result;
		
	}
	
//	public String assignLimesToken(Connection conn, Integer userId) {
//		String token = generateLimesToken(conn, userId);
//		
//		
//		
//	}
	
	
	
	public void generateUserEvalOrder(Connection conn, Integer userId, List<String> toolList) throws SQLException {
		
		//ColumnsReference cr = new ColumnsReference("eval_order", "user_id", "sequence_id", "name");
		//Inserter inserter = new Inserter(target, schema);
		//inserter.add(userId, i, toolList);
		
		PreparedStatement stmt = conn.prepareStatement("INSERT INTO \"eval_order\"(\"user_id\", \"sequence_id\", \"name\") VALUES (?, ?, ?)");
		
		int i = 0;
		for(String item : toolList) {
			SqlUtils.execute(stmt, Void.class, userId, i, item);
			++i;
		}
		
	}
	
	
	public Integer getUserId(Connection conn, String username) throws SQLException {
		Integer result = null;
		String sql = "SELECT id FROM \"user\" WHERE name = ?";
	
		result = SqlUtils.execute(conn, sql, Integer.class, username);
		
		return result;		
	}
	
	// Returns the user id - null if that fails
	public Integer authenticate(String username, String password) throws SQLException
	{
		Integer result = null;
		Connection conn = null;
		try {
			conn = ds.getConnection();
			
			String sql = "SELECT id FROM \"user\" WHERE name = ? AND password = ?";
		
			result = SqlUtils.execute(conn, sql, Integer.class, username, password);

		} finally {
			if(conn != null) {
				conn.close();
			}
		}
		
		return result;
	}

	
	public String getEvalMode(Integer userId) throws SQLException {
		Connection conn = null;
		try {
			conn = ds.getConnection();
			String result = getEvalMode(conn, userId);
			
			return result;
		} finally {
			close(conn);
		}
	}

	
	public List<String> getEvalOrder(Integer userId) throws SQLException {
		Connection conn = null;
		try {
			conn = ds.getConnection();
			conn.setAutoCommit(false);
			
			List<String> result = getEvalOrder(conn, userId);
			conn.commit();

			return result;
		} finally {
			if(conn != null) {
				conn.close();
			}
		}
	}

	
	public List<String> getEvalOrder(Connection conn, Integer userId) throws SQLException {
		List<String> result = SqlUtils.executeList(conn, "SELECT name FROM eval_order WHERE user_id = ?", String.class, userId);
		return result;
	}
	
	/**
	 * Extend to getUserState
	 * 
	 * @param conn
	 * @param userId
	 * @return
	 * @throws SQLException 
	 */
	public String getEvalMode(Connection conn, Integer userId) throws SQLException {
		String sql = "SELECT MIN(sequence_id) FROM eval_order WHERE user_id = ? AND is_finished = FALSE";
		Integer seqId = SqlUtils.execute(conn, sql, Integer.class, userId);
		
		String sql2 = "SELECT name FROM eval_order WHERE user_id = ? AND sequence_id = ?";
		String result = SqlUtils.execute(conn, sql2, String.class, userId, seqId);

		return result;
	}

	public Integer writeMapping(Integer userId, String requestAddr, String toolId, String taskId, String mapping) throws SQLException {
		Connection conn = null;
		try {
			conn = ds.getConnection();
			conn.setAutoCommit(false);
			
			Integer submissionId = writeMapping(conn, userId, requestAddr, toolId, taskId, mapping);
			conn.commit();

			return submissionId;
		} finally {
			if(conn != null) {
				conn.close();
			}
		}
	}
	
	public boolean isTaskSolved(Integer userId, String taskId) throws SQLException {
		Connection conn = ds.getConnection();
		
		boolean result;
		try {
			result = isTaskSolved(conn, userId, taskId);
		}
		finally {
			if(conn != null) {
				conn.close();
			}
		}
		
		return result;
	}
	
	public boolean isTaskSolved(Connection conn, Integer userId, String taskId)
			throws SQLException
	{
		
		String lang = getEvalMode(userId);
		
		String sql = "SELECT id FROM \"submission\" WHERE user_id = ? AND task_id = ? AND tool_id = ? AND is_solution = TRUE";

		boolean result = false;
		
		ResultSet rs = null;
		try {
			rs = SqlUtils.executeCore(conn, sql, userId, taskId, lang);
			if(rs.next()) {
				result = true;
			}
		} finally {
			SqlUtils.close(rs);
		}

		conn.commit();
		return result;
		
	}
	
	public void setSolution(Integer submissionId, boolean producesOutput, boolean isSolution) throws SQLException {
		Connection conn = null;
		try {
			conn = ds.getConnection();
			
			setSolution(conn, submissionId, producesOutput, isSolution);
			conn.commit();
		} finally {
			if(conn != null) {
				conn.close();
			}
		}
	}
	

	
	public static void rollback(Connection conn) {
		if(conn != null) {
			try {
				conn.rollback();
			} catch(Exception e) {
				logger.error("Error rolling back a transaction: ", e);
			}
		}		
	}
	
	// TODO Move to SQL Utils
	public static void close(Connection conn, boolean rollback) {
		if(rollback) {
			rollback(conn);
		}
		
		close(conn);
	}
	
	public static void close(Connection conn) {

		if(conn != null) {
			try {
				conn.close();
			} catch (SQLException e) {
				logger.error("Error closing connection: ", e);
			}
		}

	}

	public void advance(Integer userId) {
		Connection conn = null;
		try {
			conn = ds.getConnection();
			advance(conn, userId);
		} catch(Exception e) {
			rollback(conn);
		}
		finally {
			close(conn);
		}
	}
	
	public void advance(Connection conn, Integer userId) throws SQLException {
		
		String sql = "SELECT MIN(sequence_id) FROM eval_order WHERE user_id = ? AND is_finished = FALSE";
		Integer seqId = SqlUtils.execute(conn, sql, Integer.class, userId);

		
		SqlUtils.execute(conn, "UPDATE eval_order SET is_finished = true WHERE user_id = ? AND sequence_id = ?", Void.class, userId, seqId);
		String lang = getEvalMode(conn, userId);
		
		// Generate a new limes token
		generateLimesToken(conn, userId, lang);

		//conn.commit();
	}
	
	public void setSolution(Connection conn, Integer submissionId, boolean producesOutput, boolean isSolution) throws SQLException {
		String sql = "UPDATE submission SET is_working = ?, is_solution = ? WHERE id = ?";
		SqlUtils.execute(conn, sql, Void.class, producesOutput, isSolution, submissionId);
	}
	
	
	public Integer writeMapping(Connection conn, Integer userId, String requestAddr, String toolId, String taskId, String mapping) throws SQLException {

		String sql = "INSERT INTO submission(user_id, request_addr, tool_id, task_id, mapping) VALUES(?, ?, ?, ?, ?)";
		SqlUtils.execute(conn, sql, Void.class, userId, requestAddr, toolId, taskId, mapping);
		
		Long tmp = SqlUtils.execute(conn, "SELECT LASTVAL()", Long.class);
		Integer submissionId = tmp.intValue();
		return submissionId;
	}
	

	public Integer registerUser(String username, String password, String email) throws SQLException {
		
		// The result indicates success
		Integer result = null;
		
		Connection conn = null;
		try {
			conn = ds.getConnection();
			conn.setAutoCommit(false);
			
			
			logger.debug("Authentication with: " + username + " " + password + " " + email);
			
			Integer userId = getUserId(conn, username);			
			if(userId != null) {
				throw new RuntimeException("User already registered");
			}

			
			String sql = "INSERT INTO \"user\"(name, email, password) VALUES (?, ?, ?)";
			
			SqlUtils.execute(conn, sql, Void.class, username, email, password);
			
			Integer uid = getUserId(conn, username);
			
			if(uid == null) {
				throw new RuntimeException("Internal error: User does not exist after insertion");
			}
			
			generateUserEvalOrder(conn, uid, generateToolList());
			
			String lang = getEvalMode(conn, uid);
			generateLimesToken(conn, uid, lang);
			
			conn.commit();
			
			result = uid;
		}
		finally {
			if(result == null) {
				conn.rollback();
			}
			
			if(conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					logger.error("Error closing connection: ", e);
				}
			}
		}
		
		return result;
	}



	public Map<String, String> getLimesToken(Integer userId) throws SQLException {
		
		Map<String, String> result = null;
		
		Connection conn = null;
		try {
			conn = ds.getConnection();
			conn.setAutoCommit(false);
			
			
			//String lang = getEvalMode(conn, userId);
			
			result = fetchLimesToken(conn, userId);
			conn.commit();
			
		}
		finally {
			if(result == null) {
				conn.rollback();
			}
			
			if(conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					logger.error("Error closing connection: ", e);
				}
			}
		}

		return result;
	}
}
