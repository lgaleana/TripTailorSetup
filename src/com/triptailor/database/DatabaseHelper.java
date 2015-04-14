package com.triptailor.database;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import com.triptailor.setup.TripTailorSetup;

public class DatabaseHelper {
	private final String DATABASE = "triptailor";
	private final String USER = "triptailor";
	private final String PASSWORD = "hostels";
	
	Connection connect;
	
	public DatabaseHelper() throws ClassNotFoundException, SQLException {
		Class.forName("com.mysql.jdbc.Driver");
		connect = DriverManager.getConnection("jdbc:mysql://localhost/" + DATABASE + "?" +
				"user=" + USER + "&password=" + PASSWORD);
	}
	
	public int insertLocation(String city, String country) throws SQLException {
		PreparedStatement statement = connect.prepareStatement("INSERT INTO location (city, country) VALUES (?, ?)",
				Statement.RETURN_GENERATED_KEYS);
		statement.setString(1, city);
		statement.setString(2, country);
		statement.executeUpdate();
		
		ResultSet keys = statement.getGeneratedKeys();
		keys.next();
		int id = keys.getInt(1);
		
		keys.close();
		statement.close();
		
		return id;
	}
	
	public void insertHostelInfo(String name, double price, int noReviews, int locationId) throws SQLException {
		PreparedStatement statement = connect.prepareStatement("INSERT INTO hostel (name, price, no_reviews, location_id) VALUES "
				+ "(?, ?, ?, ?)");
		statement.setString(1, name);
		statement.setDouble(2, price);
		statement.setInt(3, noReviews);
		statement.setInt(4, locationId);
		statement.executeUpdate();
		
		statement.close();
	}
	
	public void insertHostelDependencies(String hostelName, Map<String, double[]> vector, List<String> services) throws SQLException {
		PreparedStatement statement = connect.prepareStatement("SELECT id FROM hostel WHERE name = ?");
		statement.setString(1, hostelName);
		ResultSet keys = statement.executeQuery();
		keys.next();
		int hostelId = keys.getInt(1);

		for (Map.Entry<String, double[]> attribute : vector.entrySet()) {
			try {
				double[] values = attribute.getValue();
				int attributeId = getDependencyId("attribute", attribute.getKey());

				statement = connect.prepareStatement("INSERT INTO hostel_attribute VALUES (?, ?, ?, ?, ?)");
				statement.setInt(1, hostelId);
				statement.setInt(2, attributeId);
				statement.setDouble(3, values[TripTailorSetup.FREQ]);
				statement.setDouble(4, values[TripTailorSetup.CFREQ]);
				statement.setDouble(5, values[TripTailorSetup.RATING]);
				statement.executeUpdate();
				
				System.out.print(".");
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		for (String service : services) {
			try {
				int serviceId = getDependencyId("service", service);

				statement = connect.prepareStatement("INSERT INTO hostel_service VALUES (?, ?)");
				statement.setInt(1, hostelId);
				statement.setInt(2, serviceId);
				statement.executeUpdate();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		try {
			statement.close();
			keys.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void insertHostelUrls(String urlsFile) throws SQLException, IOException {
		BufferedReader reader = new BufferedReader(new FileReader(urlsFile));
		
		String line;
		while((line = reader.readLine()) != null) {
			String[] terms = line.split(",");
			String hostelName = terms[0];
			String url = terms[1];
			
			PreparedStatement statement = connect.prepareStatement("UPDATE hostel SET url = ? WHERE name = ?");
			statement.setString(1, url);
			statement.setString(2, hostelName);
			statement.executeUpdate();
			
			statement.close();
		}
		reader.close();
	}
	
	public int getLocationId(String city) throws SQLException {
		int locationId;
		
		PreparedStatement statement = connect.prepareStatement("SELECT id FROM location WHERE city = ?");
		statement.setString(1, city);
		ResultSet result = statement.executeQuery();
		
		if(result.next())
			locationId = result.getInt(1);
		else
			throw new SQLException();
		
		result.close();
		statement.close();
		
		return locationId;
	}
	
	private int getDependencyId(String dependency, String name) throws SQLException {
		int dependencyId;
		
		PreparedStatement statement = connect.prepareStatement("SELECT id FROM " + dependency + " WHERE name = ?");
		statement.setString(1, name);
		ResultSet result = statement.executeQuery();
		
		if(result.next())
			dependencyId = result.getInt(1);
		else {
			statement = connect.prepareStatement("INSERT INTO " + dependency + " (name) VALUES(?)",
					Statement.RETURN_GENERATED_KEYS);
			statement.setString(1, name);
			statement.executeUpdate();
			
			result = statement.getGeneratedKeys();
			result.next();
			dependencyId = result.getInt(1);
		}
		
		result.close();
		statement.close();
		
		return dependencyId;
	}
}
