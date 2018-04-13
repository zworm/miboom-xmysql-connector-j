package com.miboom.xmysql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.dbcp2.BasicDataSourceFactory;

/**
 * 
 * @author zzf
 *
 */
public class TestCachePrepStmts {
	private String db = "test";
	private String table = "t_pstmt";

	public void testCachePrepStmts() throws Exception {
		BasicDataSource ds = BasicDataSourceFactory.createDataSource(createProperties());
		try (Connection con = ds.getConnection()) {
			int cols = ensureCreatedTable(con);

			testPreparedStatement(con);
			// 修改列
			addColumn(con, "f" + cols);
			/**
			 * 原驱动，数据库表结构更改后， 缓存的select * ...
			 * 的preparedStatement列结构对不上，merge列时ArrayIndexOutOfBoundsException异常。 fix后没问题。
			 */
			testPreparedStatement(con);
		}
		ds.close();
	}

	private void testPreparedStatement(Connection con) throws Exception {
		/**
		 * 用select *测试。
		 */
		PreparedStatement ps = con.prepareStatement("select * from " + table + " where id>?");
		ps.setInt(1, 1);
		System.out.println(ps);
		ResultSet rs = ps.executeQuery();
		int col = rs.getMetaData().getColumnCount();
		System.out.println("列数=" + col);
		while (rs.next()) {
			for (int i = 0; i < col; i++) {
				System.out.print(rs.getObject(i + 1) + "\t");
			}
			System.out.println();
		}
		System.out.println();
		rs.close();
		ps.close();
	}

	private void addColumn(Connection con, String columnName) throws Exception {
		Statement stmt = con.createStatement();
		stmt.executeUpdate("alter table " + table + " add column " + columnName + " bigint");
		stmt.close();
	}

	/**
	 * @return 列数
	 */
	private int ensureCreatedTable(Connection con) throws Exception {
		Statement stmt = con.createStatement();
		ResultSet rs = stmt.executeQuery("SELECT COUNT(1) FROM information_schema.TABLES WHERE TABLE_SCHEMA='" + db
				+ "' and TABLE_NAME='" + table + "'");
		rs.next();
		if (rs.getInt(1) == 0) {
			stmt.executeUpdate("create table " + table + "(id bigint)");
			stmt.executeUpdate("insert into " + table + "(id) values(1)");
			stmt.executeUpdate("insert into " + table + "(id) values(2)");
		}
		rs.close();
		rs = stmt.executeQuery("SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='" + db
				+ "' and TABLE_NAME='" + table + "'");
		rs.next();
		int cols = rs.getInt(1);
		stmt.close();
		return cols;
	}

	private Properties createProperties() {
		String baseUrl = "jdbc:mysql://localhost:3306/" + db + "?serverTimezone=Asia/Shanghai&useCursorFetch=true";
		// 设置useServerPrepStmts和cachePrepStmts为true
		baseUrl += "&useServerPrepStmts=true&cachePrepStmts=true";
		Properties pp = new Properties();
		pp.setProperty("url", baseUrl);
		pp.setProperty("username", "root");
		pp.setProperty("password", "111");
		pp.setProperty("initialSize", "0");
		return pp;
	}

	public static void main(String[] args) throws Exception {
		new TestCachePrepStmts().testCachePrepStmts();
	}
}
