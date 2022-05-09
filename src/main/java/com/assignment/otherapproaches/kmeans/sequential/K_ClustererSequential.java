package com.assignment.otherapproaches.kmeans.sequential;/*
 * Programmed by Shephalika Shekhar
 * Class for Kmeans Clustering implemetation
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class K_ClustererSequential {
//	@Benchmark
//	@BenchmarkMode(Mode.Throughput)
//	@Measurement(iterations = 1)
//	@Fork(value = 3, warmups = 2)
//	@Timeout(time = 30)
//	@OutputTimeUnit(TimeUnit.SECONDS)
	public void test() throws Exception {
	//public static void main(String args[]) throws IOException {
		ReadDatasetSeq readDataSetObj = new ReadDatasetSeq();
		List<double[]> dataObjects = readDataSetObj.getFeatures();
		dataObjects.clear();

		String file= "Iris.txt";
		readDataSetObj.read(file); //load data

		int ex= 1;
		int k = 5;
		int iterationsCount = 100;
		int distance = 1;

		// Hashmap to store centroids with index
		Map<Integer, double[]> clusterCentroidMap = new HashMap<>();
		// calculating initial centroids
		double[] x1;
		int r =0;
		for (int i = 0; i < k; i++) {
			x1=dataObjects.get(r = r + 1);
			clusterCentroidMap.put(i, x1);
		}
		// initial cluster print
		/*	for (double[] key : clusters.keySet()) {
			for (int i = 0; i < key.length; i++) {
				System.out.print(key[i] + ", ");
			}
			System.out.print(clusters.get(key) + "\n");
		}
		*/
		double[] centroid;
		// reassigning to new clusters
		// potential of parallel k-means clustering
		// split tasks to different threads
		/*
		this processes consist of two major processes
		0. first iteration will be based on randomly-assigned centroids
		1. calculate the centroids based on the clustering results
		2. reiterate the clustering process based on the centroids,
		to see which centroids are the nearest, in order to assign data object
		with the clusters that have the nearest centroids
		 */
		Map<double[], Integer> prevClustersResult = new HashMap<>();
		Map<double[], Integer> clusters = kmeans(dataObjects, distance, clusterCentroidMap, k);
		int numOfIteration = 0;
		while(!clusters.equals(prevClustersResult)) {
			// loop through k = 0,1,2,3,4
			for (int j = 0; j < k; j++) {
				// instantiate a new ArrayList that stores double array
				// that contained in different clusters (defined by user - K)
				List<double[]> dataObjectsInTheirCluster = new ArrayList<>();
				for (double[] dataObject : dataObjects) {
					// Returns the value to which the specified data object is mapped
					// add the data object into an ArrayList if the value (which cluster the feature belongs to)
					// is equal to j.

					// Therefore, we can calculate all the clusters' centroids
					// by categorizing large amount of features into different clusters
					// and calculate each of the centroids.
					if (clusters.get(dataObject) == j) {
						dataObjectsInTheirCluster.add(dataObject);
					}
				}
				// calculate centroid for different cluster.
				centroid = centroidCalculator(dataObjectsInTheirCluster, readDataSetObj);
				// map cluster (K = 0,1,2,3,4) with its respective centroids
				clusterCentroidMap.put(j, centroid);
			}
			prevClustersResult.putAll(clusters);
			clusters.clear();
			// conduct next clustering with newly determined centroids
			clusters = kmeans(dataObjects, distance, clusterCentroidMap, k);
			numOfIteration ++;
		}
		
		//final cluster print
		System.out.println("\nFinal Clustering of Data");
		System.out.println("Feature1\tFeature2\tFeature3\tFeature4\tCluster");
		for (double[] key : clusters.keySet()) {
			for (int i = 0; i < key.length; i++) {
				System.out.print(key[i] + "\t \t");
			}
			System.out.print(clusters.get(key) + "\n");
		}
		
		//Calculate WCSS
		double wcss=0;
		
		for(int i=0;i<k;i++){
			double sse=0;
			for (double[] key : clusters.keySet()) {
				if (clusters.get(key)==i) {
					sse+=Math.pow(DistanceSeq.eucledianDistance(key, clusterCentroidMap.get(i)),2);
				}
				}
			wcss+=sse;
		}
		String dis="";
		if(distance ==1)
			 dis="Euclidean";
		else
			dis="Manhattan";
		System.out.println("\n*********Programmed by Shephalika Shekhar************\n*********Results************\nDistance Metric: "+dis);
		System.out.println("Iterations: "+numOfIteration);
		System.out.println("Number of Clusters: "+k);
		System.out.println("WCSS: "+wcss);
	}
	
	//method to calculate centroids
	public static double[] centroidCalculator(List<double[]> listOfDataObjectsInDiffCluster,
											  ReadDatasetSeq ReadDataSetObj) {

		int count = 0;
		//double x[] = new double[ReadDataset.numberOfFeatures];
		double sum=0.0;
		double[] centroids = new double[ReadDataSetObj.getNumberOfFeatures()];
		for (int i = 0; i < ReadDataSetObj.getNumberOfFeatures(); i++) {
			sum=0.0;
			count = 0;
			for(double[] dataObject : listOfDataObjectsInDiffCluster){
				// the calculation of centroids = summation of feature value in DO / count of data object
				count++;
				sum = sum + dataObject[i];
			}
			centroids[i] = sum / count;
		}
		// return the calculated centroids for all the features (in the specified different clusters)
		return centroids;

	}

	//method for putting features to clusters and reassignment of clusters.
	public static Map<double[], Integer> kmeans(List<double[]> features,int distance, Map<Integer, double[]> centroids, int k) {
		Map<double[], Integer> clusters = new HashMap<>();
		int k1 = 0;
		double dist=0.0;
		for(double[] x:features) {
			double minimum = 999999.0;
			for (int j = 0; j < k; j++) {
				if(distance==1){
				 dist = DistanceSeq.eucledianDistance(centroids.get(j), x);
				}
				else if(distance==2){
					dist = DistanceSeq.manhattanDistance(centroids.get(j), x);
				}
				if (dist < minimum) {
					minimum = dist;
					k1 = j;
				}
			
			}
			clusters.put(x, k1);
		}
		
		return clusters;

	}

}

class ConcurrentClusteringTask implements Runnable {
	List<double[]> dataObjects;
	// map that maps cluster with its respective centroids
	Map<Integer, double[]> clusterCentroidMap;

	@Override
	public void run() {

	}
}
