package dataPoints;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import mpi.*;



public class ParallelPointCluster {

    public ParallelPointCluster(String[] args) throws MPIException{
        MPI.Init(args);

        int rank = MPI.COMM_WORLD.Rank();
		/* If rank is 0 then we are running our first system so it should be a server */
        if (rank == 0){
            runServer(args);
        }
        else{
            runSlave();
        }

    }



    private void runServer(String[] args) throws MPIException{
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
            e.printStackTrace();
        }
		/*Pick any k random centroids from the given data */
        Point[] centroids = randomCentroids(points,k);
		/*Time the execution */
        long startTime = System.currentTimeMillis();
        Point[] results = parallelKMeans(points, centroids, k);
        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        System.out.println("Elapsed Time: " + elapsedTime);
        MPI.Finalize();
    }

    private void runSlave() throws MPIException {
        Intracomm comm = MPI.COMM_WORLD;
		/*Get the k value and the points sent from the master server */
        int[] k_val_arr = new int[1];
        int[] points_size_arr = new int[1];
        comm.Recv(k_val_arr, 0, 1 , MPI.INT, 0, 91);
        comm.Recv(points_size_arr, 0, 1 , MPI.INT, 0, 92); /*Need size of array first */
        int k = k_val_arr[0];
        int points_size = points_size_arr[0];

        Point[] points = new Point[points_size];
        comm.Recv(points, 0,points_size , MPI.OBJECT, 0, 93); /*Gets the points */

        Point[] new_centroids = new Point[k];

        List<List<Point>> clusters = new ArrayList<List<Point>>();
		/*Create k clusters- one for each centroid */
        for (int i = 0; i < k; i++){
            clusters.add(new ArrayList<Point>());
        }

        Point[] centroids = new Point[k];
        int[] cluster_sizes = new int[k];
        while(true){
			/*Get the centroid broadcast from the master server */
            comm.Recv(centroids, 0, k, MPI.OBJECT, 0, 94);
			/* The master server has broadcasted an empty centroids array which means it is done */
            if (centroids.length == 0){
                break;
            }
			/*create clusters */
            for (Point p : points){
                int index = p.closetPointIndex(centroids);
                clusters.get(index).add(p);
            }
            int count = 0;
			/*Re calculate all the centroids by determining the mean of all the clusters */
            for (List<Point> list : clusters){
                new_centroids[count] = Point.mean(list);
                cluster_sizes[count] = list.size();
                count++;
            }
			/*Send info back to master server */
            comm.Send(cluster_sizes, 0, k, MPI.INT, 0, 95);
            comm.Send(new_centroids, 0, k, MPI.OBJECT, 0, 96);

            boolean[] isDone = new boolean[1];
            comm.Recv(isDone, 0, 1, MPI.BOOLEAN, 0, 97);

            if(isDone[0])
                break;

        }
        MPI.Finalize();

    }


    private Point[] parallelKMeans(List<Point> points, Point[] centroids,
                                   Integer k) throws MPIException{
        int iterations = 0;
        Intracomm comm = MPI.COMM_WORLD;
        int num_slave_nodes = comm.Size() -1; /*1 node is the server node */

        List<List<Point>> slave_points = new ArrayList<List<Point>>(); /*original points to distribute */
        List<int[]> all_cluster_sizes = new ArrayList<int[]>(); /*returned by the slave on every iteration */
        List<Point[]> all_centroids = new ArrayList<Point[]>(); /*returned by the slave on every iteration */
        boolean[] isDone = new boolean[1];
		
		/*Create empty array list of points for all slave nodes  */
        for (int i = 0; i < num_slave_nodes ; i++){
            slave_points.add(new ArrayList<Point>());
        }
		/*Distribute the points amongst all the slave nodes */
        for (int i = 0; i < points.size(); i++){
            slave_points.get(i % num_slave_nodes).add(points.get(i));
        }
        for (int i = 0; i < num_slave_nodes; i++){
			/*Send the k value to all slaves. Has to be packaged in array as per MPI requirements */
            comm.Send(new int[]{k}, 0, 1, MPI.INT, i+1, 91);

			/*Send the size of the array of points being sent so slave can instantiate array of that size */
            comm.Send(new int[]{slave_points.get(i).size()},0 , 1, MPI.INT, i+1, 92);
			
			/*Send the distributed points to the corresponding slave */
            comm.Send(convertToArray(slave_points.get(i)), 0, slave_points.get(i).size(), MPI.OBJECT, i+1, 93);
        }
        Point[] new_centroids = new Point[k];
        int[] cluster_sizes = new int[k];
        while(true){
            for (int i = 0; i < num_slave_nodes; i++){
				/*Broadcast the centroids to all the slave nodes so they have knowledge of them */
                comm.Send(centroids, 0, centroids.length, MPI.OBJECT, i+1, 94);
				/*After slave does computation, recieve cluster sizes */
                comm.Recv(cluster_sizes, 0, k , MPI.INT, i+1, 95);
                all_cluster_sizes.add(cluster_sizes); /*Add cluster size for this slave to a global store */
				/*recieves the centroids determined by the slave of rank determined by current iteration */
                comm.Recv(new_centroids,0,k,MPI.OBJECT,i+1,96);
                all_centroids.add(new_centroids);

            }
            new_centroids = generateCentroids(all_centroids,all_cluster_sizes,k);

            iterations++;
            //System.out.println(iterations);

            // Centroids Converged
            if (Arrays.equals(centroids, new_centroids)){
                isDone[0] = true;
                for(int i = 0; i < num_slave_nodes; i++)
                    comm.Send(isDone, 0, 1, MPI.BOOLEAN, i+1, 97);
                System.out.println("Total Iterations: " + iterations);
                return new_centroids;
            }
			
			/* Centroids Not Yet Converged */
            for(int i = 0; i < num_slave_nodes; i++)
                comm.Send(isDone, 0, 1, MPI.BOOLEAN, i+1, 97);
            centroids = Arrays.copyOf(new_centroids, new_centroids.length);


        }

    }

    private Point[] convertToArray(List<Point> list) {
        Point[] arr = new Point[list.size()];
        for (int i = 0; i< list.size(); i++){
            arr[i] = list.get(i);
        }
        return arr;
    }


    /* This will generate new centroids using a weighted average */
    private Point[] generateCentroids(List<Point[]> all_centroids,
                                      List<int[]> all_cluster_sizes, Integer k) {
		/*Calculates the combined size of the ith cluster on each machine to one single total sum */
        int[] total_sizes = new int[k];
        for (int i = 0; i < k; i++){
            for (int[] slave_cluster_sizes : all_cluster_sizes){
                total_sizes[i] += slave_cluster_sizes[i];
            }
        }
		
		/*Calculates a weighted average of the centroids for each cluster from each slave */
        Point[] final_centroids = new Point[k];
        //final_centroids[0] = new Point(0.0,0.0); /*initialize first centroid to (0,0) to use as identity for weighted average */
        Double scaled_x;
        Double scaled_y;
        Double prev_x;
        Double prev_y;
        for (int i = 0; i < k; i++){
            final_centroids[i] = new Point(0.0,0.0);
            for (int j = 0; j < all_centroids.size(); j++){
				/*Calculate the weight factor using the total size of the ith cluster across each machine, and 
				 * the individual size of each cluster on each machine
				 */
                if(all_cluster_sizes.get(j)[i] == 0.0)
                    continue;

                Double weight_factor = new Double(all_cluster_sizes.get(j)[i])/(new Double(total_sizes[i]));
                scaled_x = all_centroids.get(j)[i].x * weight_factor;
                scaled_y = all_centroids.get(j)[i].y * weight_factor;
                prev_x = final_centroids[i].x;
                prev_y = final_centroids[i].y;
				/*accumulate the weighted average in a Point object for each cluster */
                final_centroids[i] = new Point(scaled_x+prev_x,scaled_y+prev_y);
            }
        }
        return final_centroids;

    }

    /*From a list of points get a list of n random points */
    public static Point[] randomCentroids(List<Point> points, int n) throws MPIException{
        List<Point> linked_list_points = new LinkedList<Point>(points);
        Collections.shuffle(linked_list_points);
        return linked_list_points.subList(0, n).toArray(new Point[0]);
    }

    public static void main(String[] args) throws MPIException{
        ParallelPointCluster ppc = new ParallelPointCluster(args);
    }


}
