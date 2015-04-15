package com.triptailor.setup;

import java.io.BufferedReader;
import java.io.BufferedWriter;
//import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
//import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
//import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
//import java.util.TreeMap;

import com.triptailor.database.DatabaseHelper;
import com.triptailor.nlp.Analyser;

public class TripTailorSetup {
	
	public static final int CFREQ = 0;
	public static final int FREQ = 1;
	public static final int RATING = 2;
	
	private static final String CITIES_FILE = "new_cities.txt";
	private static final String DATA_PATH = "data/";
	private static final String URLS_FILE = "hostel_urls.txt";
	private static final String COUNTER_FILE = "counter.txt";
	private static final String STOP_FILE = "stop.txt";
	
	public static Map<String, Object> stop = new HashMap<String, Object>();

	public static void main(String[] args) throws IOException, ClassNotFoundException, SQLException {
		BufferedReader counterReader = new BufferedReader(new FileReader(COUNTER_FILE));
		BufferedReader reader = new BufferedReader(new FileReader(CITIES_FILE));
		BufferedReader stopReader = new BufferedReader(new FileReader(STOP_FILE));
		BufferedWriter counterWriter;
		
		String[] counters = counterReader.readLine().split(",");
		int lastCity = Integer.parseInt(counters[0]);
		int lastHostel = Integer.parseInt(counters[1]);
		counterReader.close();
		
		int cityCounter = 0;
		for(; cityCounter < lastCity; cityCounter++)
			reader.readLine();
		
		DatabaseHelper databaseHelper = new DatabaseHelper();
		
		String line;
		while((line = stopReader.readLine()) != null)
			stop.put(line, null);
		stopReader.close();
		
		counterWriter = new BufferedWriter(new FileWriter(COUNTER_FILE));
		counterWriter.write(lastCity + "," + lastHostel);
		counterWriter.flush();
		
		while((line = reader.readLine()) != null) {
			String[] locations = line.split(",");
			String country = locations[0];
			String city = locations[1];
			
			int locationId;
			if(lastHostel == 0) {
				System.out.println("Inserting " + city);
				locationId = databaseHelper.insertLocation(city, country);
			}
			else {
				System.out.println("Going over " + city);
				locationId = databaseHelper.getLocationId(city);
			}
			
			int hostelCounter = 1;
			// Go over all files of the data
			for(File file : new File(DATA_PATH + country + "/" + city).listFiles()) {
				if(hostelCounter > lastHostel) {
					String filePath = file.getAbsolutePath();
					
					if(filePath.contains("_reviews.txt")) {
						BufferedReader infoReader = new BufferedReader(new FileReader(filePath.replace("_reviews.txt", "_general.txt")));
						
						// Hostel general info
						String hostelName = infoReader.readLine().replace(";", "").replace("_", " ");
						String info = infoReader.readLine();
						double price = info.length() > 3 ? Double.parseDouble(info.substring(3)) / 14 : 0;
						int noReviews = Integer.parseInt(infoReader.readLine());
						infoReader.readLine();
						infoReader.readLine();
						int hoscars = Integer.parseInt(infoReader.readLine());
						for(int i = 0; i< hoscars; i ++)
							infoReader.readLine();
						int awards = Integer.parseInt(infoReader.readLine());
						for(int i = 0; i < awards; i++)
							infoReader.readLine();
						int noServices = Integer.parseInt(infoReader.readLine());
						List<String> services = new ArrayList<String>();
						for(int i = 0; i < noServices; i++)
							services.add(infoReader.readLine().toLowerCase());
						
						infoReader.close();
						
						// NLP analysis
						Analyser analyser = new Analyser();
						System.out.print("Analysing NLP for " + hostelName + " ");
						analyser.nlpAnalyse(filePath, hostelName);
						System.out.println();
						
						System.out.println("Inserting hostel info for " + hostelName);
						databaseHelper.insertHostelInfo(hostelName, price, noReviews, locationId);
						
						System.out.print("Inserting dependencies for " + hostelName + " ");
						databaseHelper.insertHostelDependencies(hostelName, analyser.getVector(), services);
						System.out.println();
						
						counterWriter = new BufferedWriter(new FileWriter(COUNTER_FILE));
						counterWriter.write(cityCounter + "," + hostelCounter);
						counterWriter.flush();
					}
				}
				hostelCounter++;
			}
			cityCounter++;
			lastHostel = 0;
		}
		reader.close();
		counterWriter.close();
		
		System.out.println("Inserting hostel urls");
		databaseHelper.insertHostelUrls(URLS_FILE);
	}
}
