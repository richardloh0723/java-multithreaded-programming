package com.assignment.otherapproaches.kmeans.parallelcallableandrunnable;/*
 * Programmed by Shephalika Shekhar
 * Class for Kmeans Clustering implemetation
 */

import org.openjdk.jmh.annotations.*;

import java.util.*;
import java.util.concurrent.*;

public class K_ClustererCallableAndRunnable {
	@Benchmark
	@BenchmarkMode(Mode.Throughput)
	@Measurement(iterations = 1)
	@Fork(value = 3, warmups = 2)
	@Timeout(time = 30)
	@OutputTimeUnit(TimeUnit.SECONDS)
	public void test() throws Exception {
	//public static void main(String args[]) throws IOException, ExecutionException, InterruptedException {
		ReadDatasetSeq r1 = new ReadDatasetSeq();
		List<double[]> features = r1.getFeatures();
		features.clear();

		String file= "Iris.txt";
		r1.read(file); //load data

		int ex=1;
		int k = 5;
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
		Map<double[], Integer> clusters;
		Map<double[], Integer> previousClusters = new HashMap<>();
		//initialize the first cluster, can't use concurrency
		clusters = kmeans(features, distance, centroids, k);

		// reassigning to new clusters
		// potential of parallel k-means clustering
		// split tasks to different threads
		int numOfCores = Runtime.getRuntime().availableProcessors();
		ExecutorService executors = Executors.newFixedThreadPool(numOfCores);
		CountDownLatch controller;
		boolean clusterNotChanged = false;
		int iterationCount = 0;
		List<Future<Map<Integer,double[]>>> results;
		while(!clusterNotChanged) {
			results = new ArrayList<>(k);
			for (int j = 0; j < k; j++) {
				AsyncCentroidsCalculation task = new AsyncCentroidsCalculation(r1,j,clusters);
				Future<Map<Integer, double[]>> result = executors.submit(task);
				results.add(result);
			}
			for(Future<Map<Integer,double[]>> result : results) {
				centroids.putAll(result.get());
			}
			previousClusters.putAll(clusters);
			clusters = new ConcurrentHashMap<>();
			controller = new CountDownLatch(numOfCores);
			for (int coreId = 0; coreId < numOfCores; coreId++) {
				int[] startEndArr = calculateStartEndIndex(numOfCores, coreId, features);
				executors.execute(new SyncDistanceCalculation(features, centroids, k,
						startEndArr[0], startEndArr[1], clusters, controller));
			}
			// possible bottleneck
			controller.await();
			//clusters = futureResult.get();
			if(clusters.equals(previousClusters)) {
				clusterNotChanged = true;
			}
			previousClusters.clear();
			iterationCount++;
		}

		executors.shutdown();
		
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
		System.out.println("Iterations: "+ iterationCount);
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
		// need to introduce atomicity
		Map<double[], Integer> clusters = new ConcurrentHashMap<>();
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

	public static int[] calculateStartEndIndex(int numOfCores, int corePosition,
											   List<double[]> features) {
		// return int[0] start index, int[1] end index
		int startIndex;
		int endIndex;
		int featuresPerCore;
		int remainingFeatures = 0;
		// scope: the number of cores must be in even number
		featuresPerCore = features.size() / numOfCores;

		startIndex = corePosition * featuresPerCore;
		if(corePosition == numOfCores - 1) {
			endIndex = features.size() - 1;
		} else {
			endIndex = (corePosition + 1) * featuresPerCore - 1;
		}
		return new int[] {startIndex, endIndex};
	}
}

class SyncDistanceCalculation implements Runnable {
	List<double[]> features;
	Map<Integer, double[]> centroids;
	int k;
	int start;
	int end;
	CountDownLatch controller;
	// put the result inside this variable, needs to be atomic
	Map<double[], Integer> clusters;
	@Override
	public void run() {
//		Map<double[], Integer> clusters = new HashMap<>();
//		int k1 = 0;
//		double dist=0.0;
//		for(double[] x:features) {
//			double minimum = 999999.0;
//			for (int j = 0; j < k; j++) {
//				dist = DistanceSeq.eucledianDistance(centroids.get(j), x);
//				if (dist < minimum) {
//					minimum = dist;
//					k1 = j;
//				}
//
//			}
//			clusters.put(x, k1);
//		}
//		return clusters;
		int k1 = 0;
		double distance = 0.0;
		for(int i = start; i <= end; i++) {
			double minimum = 999999.0;
			for(int j = 0; j < k; j++) {
				distance = DistanceSeq.eucledianDistance(centroids.get(j), features.get(i));
				if(distance < minimum) {
					minimum = distance;
					k1 = j;
				}
			}
			clusters.put(features.get(i),k1);
		}
		controller.countDown();
	}

	public SyncDistanceCalculation(List<double[]> features, Map<Integer, double[]> centroids, int k,
								   int start, int end, Map<double[], Integer> clusters, CountDownLatch controller) {
		this.features = features;
		this.centroids = centroids;
		this.k = k;
		this.start = start;
		this.end = end;
		this.clusters = clusters;
		this.controller = controller;
	}
}

class AsyncCentroidsCalculation implements Callable<Map<Integer, double[]>> {
	ReadDatasetSeq r1;
	int k;
	Map<double[], Integer> clusters;

	Map<Integer, double[]> centroids = new HashMap<>();
	double[] centroidPosition;
	@Override
	public Map<Integer, double[]> call() {
		List<double[]> list = new ArrayList<>();
		for (double[] key : clusters.keySet()) {
			if (clusters.get(key) == k) {
				list.add(key);
			}
		}
		centroidPosition = centroidCalculator(list, r1);
		centroids.put(k, centroidPosition);
		return centroids;
	}

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

	public AsyncCentroidsCalculation(ReadDatasetSeq r1, int k, Map<double[], Integer> clusters) {
		this.r1 = r1;
		this.k = k;
		this.clusters = clusters;
	}
}
