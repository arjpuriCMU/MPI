package DNA;

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

import javax.sound.midi.SysexMessage;


public class ParallelDNACluster {

    public ParallelDNACluster(String[] args) throws MPIException{
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
        Strand[] results = parallelKMeans(strands, centroids, k);
        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        System.out.println("Elapsed Time: " + elapsedTime);
        MPI.Finalize();
    }

    private void runSlave() throws MPIException {
        Intracomm comm = MPI.COMM_WORLD;
		/*Get the k value and the points sent from the master server */
        int[] k_val_arr = new int[1];
        int[] strand_size_arr = new int[1];
        comm.Recv(k_val_arr, 0, 1 , MPI.INT, 0, 91);
        comm.Recv(strand_size_arr, 0, 1 , MPI.INT, 0, 92); /*Need size of array first */
        int k = k_val_arr[0];
        int strand_size = strand_size_arr[0];

        Strand[] strands = new Strand[strand_size];
        comm.Recv(strands, 0,strand_size , MPI.OBJECT, 0, 93); /*Gets the points */

        Strand[] centroids = new Strand[k];
        int[] strandCluster = new int[strand_size];
        while(true){
			/*Get the centroid broadcast from the master server */
            comm.Recv(centroids, 0, k, MPI.OBJECT, 0, 94);

			/*create clusters */
            for (int i = 0; i < strands.length; i++){
                Strand s = strands[i];
                strandCluster[i] = s.closetPointIndex(centroids);
            }
			/*Send info back to master server */
            comm.Send(strandCluster, 0, strand_size, MPI.INT, 0, 95);

            boolean[] isDone = new boolean[1];
            comm.Recv(isDone, 0, 1, MPI.BOOLEAN, 0, 96);

            if(isDone[0])
                break;
        }
        MPI.Finalize();
    }

    private Strand[] parallelKMeans(List<Strand> strands, Strand[] centroids,
                                   Integer k) throws MPIException{
        int iterations = 0;
        Intracomm comm = MPI.COMM_WORLD;
        int num_slave_nodes = comm.Size() -1; /*1 node is the server node */

        List<List<Strand>> slave_strands = new ArrayList<List<Strand>>(); /*original points to distribute */
        List<List<Integer>> slave_strand_ids = new ArrayList<List<Integer>>();
        boolean[] isDone = new boolean[1];
		
		/*Create empty array list of points for all slave nodes  */
        for (int i = 0; i < num_slave_nodes ; i++){
            slave_strands.add(new ArrayList<Strand>());
            slave_strand_ids.add(new ArrayList<Integer>());
        }
		/*Distribute the points amongst all the slave nodes */
        for (int i = 0; i < strands.size(); i++){
            slave_strands.get(i % num_slave_nodes).add(strands.get(i));
            slave_strand_ids.get(i % num_slave_nodes).add(i);
        }
        for (int i = 0; i < num_slave_nodes; i++){
			/*Send the k value to all slaves. Has to be packaged in array as per MPI requirements */
            comm.Send(new int[]{k}, 0, 1, MPI.INT, i+1, 91);

			/*Send the size of the array of points being sent so slave can instantiate array of that size */
            comm.Send(new int[]{slave_strands.get(i).size()},0 , 1, MPI.INT, i+1, 92);
			
			/*Send the distributed points to the corresponding slave */
            comm.Send(convertToArray(slave_strands.get(i)), 0, slave_strands.get(i).size(), MPI.OBJECT, i+1, 93);
        }

        Strand[] new_centroids = new Strand[k];
        List<List<Strand>> clusters = new ArrayList<List<Strand>>();

        while(true)
        {


            /* Wipe Clusters */
            for(int i = 0; i < k; i++)
                clusters.add(new ArrayList<Strand>());

            for (int i = 0; i < num_slave_nodes; i++) {
				/*Broadcast the centroids to all the slave nodes so they have knowledge of them */
                comm.Send(centroids, 0, centroids.length, MPI.OBJECT, i + 1, 94);

                /* Receive centroid index for each of the strands on the slave */
                int[] strandClusters = new int[slave_strands.get(i).size()];
                comm.Recv(strandClusters, 0, slave_strands.get(i).size(), MPI.INT, i + 1, 95);

                for (int j = 0; j < slave_strands.get(i).size(); j++) {
                    Strand s = slave_strands.get(i).get(j);
                    clusters.get(strandClusters[j]).add(s);
                }
            }

            for (int i = 0; i < k; i++)
                new_centroids[i] = Strand.getCentroid(clusters.get(i));


            iterations++;
            //System.out.println(iterations);

            // Centroids Converged
            if (Arrays.equals(centroids, new_centroids)){
                isDone[0] = true;
                for(int i = 0; i < num_slave_nodes; i++)
                    comm.Send(isDone, 0, 1, MPI.BOOLEAN, i+1, 96);
                System.out.println("Total Iterations: " + iterations);
                return new_centroids;
            }

			/* Centroids Not Yet Converged */
            for(int i = 0; i < num_slave_nodes; i++)
                comm.Send(isDone, 0, 1, MPI.BOOLEAN, i+1, 96);
            centroids = Arrays.copyOf(new_centroids, new_centroids.length);
        }

    }

    private Strand[] convertToArray(List<Strand> list) {
        Strand[] arr = new Strand[list.size()];
        for (int i = 0; i< list.size(); i++){
            arr[i] = list.get(i);
        }
        return arr;
    }


    /*From a list of points get a list of n random points */
    public static Strand[] randomCentroids(List<Strand> points, int n){
        List<Strand> linked_list_points = new LinkedList<Strand>(points);
        Collections.shuffle(linked_list_points);
        return linked_list_points.subList(0, n).toArray(new Strand[0]);
    }

    public static void main(String[] args) throws MPIException{
        ParallelDNACluster ppc = new ParallelDNACluster(args);
    }


}
