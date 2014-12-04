package dataPoints;

import javax.sound.midi.SysexMessage;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
		
		/*Pick any k random centroids from the given data */
		Point[] centroids = randomCentroids(points,k);

		/*Time the execution */
		long startTime = System.currentTimeMillis();
		Point[] results = sequentialKMeans(points, centroids, k);
		long stopTime = System.currentTimeMillis();
	    long elapsedTime = stopTime - startTime;
        System.out.print("Elapsed Time: " + elapsedTime);
	}
	
	public static Point[] sequentialKMeans(List<Point> points, Point[] centroids,int k){
		List<List<Point>> clusters = new ArrayList<List<Point>>();
		/*Create k clusters- one for each centroid */
		for (int i = 0; i < k; i++){
			clusters.add(new ArrayList<Point>());
		}
        int iterations = 0;
		while(true){
			/*For all points, find the centroid closest to it, and add that point to that cluster. */
			for (Point p : points){
                int index = p.closetPointIndex(centroids);
				clusters.get(index).add(p);
			}
			Point[] new_centroids = new Point[k];
			int count = 0;
			
			/*Re calculate all the centroids by determining the mean of all the clusters */
			for (List<Point> list : clusters){
				new_centroids[count] = Point.mean(list);
                count++;
			}
			
			/*If there are no changes to the centroids we are done */
			if (Arrays.equals(centroids, new_centroids)){
                System.out.println("Total Iterations: " + iterations + 1);
				return new_centroids;
			}
			centroids = Arrays.copyOf(new_centroids, new_centroids.length);
            iterations++;
		}
	}

	/*From a list of points get a list of n random points */
	public static Point[] randomCentroids(List<Point> points, int n){
		List<Point> linked_list_points = new LinkedList<Point>(points);
		Collections.shuffle(linked_list_points);
		return linked_list_points.subList(0, n).toArray(new Point[0]);

	}
	
	
	
	
	
	
	
	
}
