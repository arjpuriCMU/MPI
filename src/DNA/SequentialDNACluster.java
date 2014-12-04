package DNA;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class SequentialDNACluster {

	public static void main(String[] args){
		if (args.length != 2){
			System.out.println("There must be 2 arguments in this order: k, file. Where k is the number of clusters, and file is the .csv of dna strands");
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
		List<Strand> strands = new ArrayList<Strand>();
		String[] temp_point;
		
		/*Parse the .csv file and converts all data to the Point abstraction we have defined */
		try {
			br = new BufferedReader(new FileReader(file_path));
			while ( (line = br.readLine()) != null){
				strands.add(new Strand(line));
			}
			
		} catch (FileNotFoundException e) {
			System.out.println("Incorrect file path!");
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		/*Pick any k random centroids from the given data */
		Strand[] centroids = randomCentroids(strands,k);
		/*Time the execution */
		long startTime = System.currentTimeMillis();
		Strand[] results = sequentialKMeans(strands, centroids, k);
		long stopTime = System.currentTimeMillis();
	    long elapsedTime = stopTime - startTime;
        System.out.print("Elapsed Time: " + elapsedTime);
	}
	
	public static Strand[] sequentialKMeans(List<Strand> points, Strand[] centroids,int k){
		List<List<Strand>> clusters = new ArrayList<List<Strand>>();
		/*Create k clusters- one for each centroid */
		for (int i = 0; i < k; i++){
			clusters.add(new ArrayList<Strand>());
		}

        int iterations = 0;
		while (true){
			/*For all points, find the centroid closest to it, and add that point to that cluster. */
			for (Strand p : points){
				int index = p.closetPointIndex(centroids);
				clusters.get(index).add(p);
			}
			Strand[] new_centroids = new Strand[k];
			int count = 0;
			
			/*Re calculate all the centroids by determining the mean of all the clusters */
			for (List<Strand> list : clusters){
				new_centroids[count] = Strand.getCentroid(list);
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
	public static Strand[] randomCentroids(List<Strand> points, int n){
		List<Strand> linked_list_points = new LinkedList<Strand>(points);
		Collections.shuffle(linked_list_points);
		return linked_list_points.subList(0, n).toArray(new Strand[0]);
	}
	
	
	
	
	
	
	
	
}
