package DBACCESS;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

//reference: https://www.linkedin.com/learning/java-ee-concurrency-and-multithrea

public class DBConnection {

	public static Connection getConnection() {

		String dbUrl = System.getenv("DB_URL");
		String dbUser = System.getenv("DB_USER");
		String dbPassword = System.getenv("DB_PASSWORD");
		String dbClass = System.getenv("DB_CLASS");
		System.out.println("dbClass: " + dbClass);
		System.out.println("dbUrl: " + dbUrl);
		System.out.println("dbUser: " + dbUser);
		System.out.println("dbPassword: " + dbPassword);
		
		
		Connection connection = null;
		try {
			Class.forName(dbClass);
			System.out.println("We are here in DBConnection");
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return connection;
	}
}