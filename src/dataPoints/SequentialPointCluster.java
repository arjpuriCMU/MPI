package dataPoints;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class SequentialPointCluster {

	public static void main(String[] args){
		if (args.length != 2){
			System.out.println("There must be 2 arguments in this order: k, file. Where k is the number of clusters, and file is the .csv of data points");
			return;
		}
		Integer k = null;
		String file_path = null;
		
		/*Retrieve the number of cluster points, and the file path of all the .csv */
		try{
			k = Integer.valueOf(args[0]);
			file_path = args[1];
		}
		catch (Exception e){
			System.out.println("Please input correct arguments");
		}
		BufferedReader br = null;
		String line;
		List<Point> points = new ArrayList<Point>();
		String[] temp_point;
		
		/*Parse the .csv file and converts all data to the Point abstraction we have defined */
		try {
			br = new BufferedReader(new FileReader(file_path));
			while ( (line = br.readLine()) != null){
				temp_point = line.split(",");
				Double x = new Double(temp_point[0]);
				Double y = new Double(temp_point[1]);
				points.add(new Point(x,y));
			}
			
		} catch (FileNotFoundException e) {
			System.out.println("Incorrect file path!");
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		List<Point> centroids = randomCentroids(points,k);
		
		
		
	}

	/*From a list of points get a list of n random points */
	public static List<Point> randomCentroids(List<Point> points, int n){
		List<Point> linked_list_points = new LinkedList<Point>(points);
		Collections.shuffle(linked_list_points);
		return linked_list_points.subList(0, n);
	}
	
	
	
}
