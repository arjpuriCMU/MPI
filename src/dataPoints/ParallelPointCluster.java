package dataPoints;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import mpi.*;



public class ParallelPointCluster {
	private MPI jmpi;
	
	public ParallelPointCluster(String[] args){
		MPI.Init(args);
		
		int rank = MPI.COMM_WORLD.Rank();
		/* If rank is 0 then we are running our first system so it should be a server */
		if (rank == 0){
			runServer(args);
		}
		
	}
	
	private void runServer(String[] args) {
		/* TODO Argument length error check */
		
		Integer k = null;
		String file_path = null;
		try{
			k = Integer.valueOf(args[0]); /*TODO Change this. don't think it will be args[0] */
			file_path = args[1];
		}
		
		catch (Exception e){
			System.out.println("Please input correct arguments");
		}
		BufferedReader br = null;
		String line;
		List<Point> points = new ArrayList<Point>();
		String[] temp_point;
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
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		/*Pick any k random centroids from the given data */
		Point[] centroids = randomCentroids(points,k);
		/*Time the execution */
		long startTime = System.currentTimeMillis();
		Point[] results = parallelKMeans(points, centroids, k);
		long stopTime = System.currentTimeMillis();
	    long elapsedTime = stopTime - startTime;
	}

	
	private Point[] parallelKMeans(List<Point> points, Point[] centroids,
			Integer k) {

	
	}

	/*From a list of points get a list of n random points */
	public static Point[] randomCentroids(List<Point> points, int n){
		List<Point> linked_list_points = new LinkedList<Point>(points);
		Collections.shuffle(linked_list_points);
		return (Point[]) linked_list_points.subList(0, n).toArray();
	}
	
	public static void main(String[] args){
		ParallelPointCluster ppc = new ParallelPointCluster(args);
	}
	
	
}
