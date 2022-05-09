package com.assignment.kmeansalgorithm.runnable.originalcode;/*
 * Programmed by Shephalika Shekhar
 * Class for Kmeans Clustering implemetation
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class K_ClustererOriginal {
//	@Benchmark
//	@BenchmarkMode(Mode.Throughput)
//	@Measurement(iterations = 1)
//	@Fork(value = 3, warmups = 2)
//	@Timeout(time = 30)
//	@OutputTimeUnit(TimeUnit.SECONDS)
	public void test() throws Exception {
	//public static void main(String args[]) throws IOException {
		ReadDatasetSeq r1 = new ReadDatasetSeq();
		List<double[]> features = r1.getFeatures();
		features.clear();

		String file= "Iris.txt";
		r1.read(file); //load data

		int ex=1;
		int k = 5;
		int max_iterations = 100;
		int distance = 1;

		// Hashmap to store centroids with index
		Map<Integer, double[]> centroids = new HashMap<>();
		// calculating initial centroids
		double[] x1;
		int r =0;
		for (int i = 0; i < k; i++) {
			x1=features.get(r = r + 1);
			centroids.put(i, x1);
		}
		//Hashmap for finding cluster indexes
		Map<double[], Integer> clusters = new HashMap<>();
		clusters = kmeans(features, distance, centroids, k);
		// initial cluster print
		/*	for (double[] key : clusters.keySet()) {
			for (int i = 0; i < key.length; i++) {
				System.out.print(key[i] + ", ");
			}
			System.out.print(clusters.get(key) + "\n");
		}
		*/
		double db[];

		// reassigning to new clusters
		// potential of parallel k-means clustering
		// split tasks to different threads
		for (int i = 0; i < max_iterations; i++) {
			for (int j = 0; j < k; j++) {
				List<double[]> list = new ArrayList<>();
				for (double[] key : clusters.keySet()) {
					if (clusters.get(key)==j) {
						list.add(key);
					}
			}
				db = centroidCalculator(list, r1);
				centroids.put(j, db);
			}
			clusters.clear();
			clusters = kmeans(features,distance, centroids, k);
			
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
					sse+=Math.pow(DistanceSeq.eucledianDistance(key, centroids.get(i)),2);
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
		System.out.println("Iterations: "+max_iterations);
		System.out.println("Number of Clusters: "+k);
		System.out.println("WCSS: "+wcss);
	}
	
	//method to calculate centroids
	public static double[] centroidCalculator(List<double[]> a, ReadDatasetSeq r1) {

		int count = 0;
		//double x[] = new double[ReadDataset.numberOfFeatures];
		double sum=0.0;
		double[] centroids = new double[r1.getNumberOfFeatures()];
		for (int i = 0; i < r1.getNumberOfFeatures(); i++) {
			sum=0.0;
			count = 0;
			for(double[] x:a){
				count++;
				sum = sum + x[i];
			}
			centroids[i] = sum / count;
		}
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
