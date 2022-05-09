package com.assignment.otherapproaches.apriori.concurrent;

import org.openjdk.jmh.annotations.*;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.stream.Collectors;

@State(Scope.Benchmark)
public class ApAlgorithm {

//	@Benchmark
//	@BenchmarkMode(Mode.Throughput)
//	@Measurement(iterations = 1)
//	@Fork(value = 3, warmups = 2)
//	@Timeout(time = 30)
//	@OutputTimeUnit(TimeUnit.SECONDS)
	public void test() throws Exception {
	//public static void main(String[] args) throws Exception {
		// start timer
		long start = System.currentTimeMillis();
		ApAlgorithm ap = new ApAlgorithm();
		ap.configure();
		ap.go();
		long end = System.currentTimeMillis();
		System.out.println("Execution time is: " + ((double) (end - start) / 1000) + " seconds.");
	}
	/** starts the algorithm after configuration */
	/** the list of current itemsets */
	private List<int[]> currentItemsets;
	/** the name of the transcation file */
	private String transactionFileName;
	/** number of different items in the dataset */
	private int numItems;
	/** total number of transactions in transaFile */
	private int numTransactions;
	/** minimum support for a frequent itemset in percentage, e.g. 0.8 */
	private double minSup;
	private static double minConf;

	/** Stores The values from the frequent itemsets */
	private static List<String> itemsetValues = new ArrayList<>();

	/** by default, Apriori is used with the command line interface */
	private boolean usedAsLibrary = false;
	// dataset to be stored inside this variable for future use
	private List<int[]> dataset;
	private ExecutorService executors;
	private int numOfProcessors;
	/**
	 * generates the apriori itemsets from a file
	 * @throws Exception
	 */

	public void go() throws Exception {

		// first we generate the candidates of size 1
		createItemsetsOfSize1();
		int itemsetNumber = 1; // the current itemset being looked at
		int nbFrequentSets = 0;

		while (currentItemsets.size() > 0) {
			// the frequent itemsets need to produce something
			// in order to let create new itemsets from previous ones
			// to consume
			// therefore we can introduce some asynchronous methodology
			// and inside those methods we can split the iteration tasks
			// into several processors

			calculateFrequentItemsets();

			if (currentItemsets.size() != 0) {
				nbFrequentSets += currentItemsets.size();
				log("Found " + currentItemsets.size() + " frequent itemsets of size " + itemsetNumber + " (with support "
						+ (minSup * 100) + "%)");

				createNewItemsetsFromPreviousOnes();
			}
			itemsetNumber++;
		}
		log("Found " + nbFrequentSets + " frequents sets for support " + (minSup * 100) + "% (absolute "
				+ Math.round(numTransactions * minSup) + ")");
		log("Done");
		executors.shutdown();
		//Lister();
	}

	/** triggers actions if a frequent item set has been found */
	private void foundFrequentItemSet(int[] itemset, int support) {
		String New, New1;

		New = Arrays.toString(itemset);
		New1 = New.substring(0, New.length() - 1) + ", " + support + "]";
		itemsetValues.add(New1);
		System.out.println(New + "  (" + ((support / (double) numTransactions)) + " " + support + ")");

	}

	/** outputs a message in Sys.err if not used as library */
	private void log(String message) {
		if (!usedAsLibrary) {
			System.err.println(message);
		}
	}

	/** computes numItems, numTransactions, and sets minSup */
//	@Benchmark
//	@BenchmarkMode(Mode.Throughput)
//	@Measurement(iterations = 1)
//	@Fork(value = 3, warmups = 2)
//	@Timeout(time = 30)
//	@OutputTimeUnit(TimeUnit.SECONDS)
	/*
	JingHoong: this code can be modified to store the value obtained from reader file.
	Hence, all the readings of values in the future will be obtained from a variable
	stored. (using List<int[]> to store arraylist of integers)
	 */
	public void configure() throws Exception {

		transactionFileName = "mushroom.dat"; // default

		minSup = 0.5;// by default
		if (minSup > 1 || minSup < 0)
			throw new Exception("minSup: bad value");

		minConf = 0.5;
		if (minConf > 1 || minConf < 0)
			throw new Exception("minConf: bad value");

		// going through the file to compute numItems and numTransactions
		// going through the file to compute numItems and numTransactions
		AtomicInteger atomicNumItems = new AtomicInteger(0);
		AtomicInteger atomicNumTransactions = new AtomicInteger(0);
		dataset = new ArrayList<>();
		dataset = Files.readAllLines(new File(transactionFileName).toPath(), Charset.defaultCharset())
				.parallelStream()
				.filter(s -> !s.matches("\\s*"))
				.map(s -> s.split(" "))
				.map(stringArr -> {
					int[] intArray = new int[stringArr.length];
					for(int i = 0; i < stringArr.length; i++) {
						intArray[i] = Integer.parseInt(stringArr[i]);
						if (intArray[i] + 1 > atomicNumItems.get()) {
							atomicNumItems.set(intArray[i] + 1);
						}
					}
					atomicNumTransactions.incrementAndGet();
					return intArray;
				})
				.collect(Collectors.toList());
		numTransactions = atomicNumTransactions.get();
		numItems = atomicNumItems.get();

		// Jing Hoong: refactor into converting them into ArrayList using
		// stream

//		for(String line : lines) {
//			if (line.matches("\\s*"))
//				continue; // be friendly with empty lines
//			String[] strArray = line.split(" ");
//			int[] intArray = new int[strArray.length];
//			for(int i = 0; i < strArray.length; i++) {
//				intArray[i] = Integer.parseInt(strArray[i]);
//			}
//			dataset.add(intArray);
//			numTransactions++;
//			// the old method is to use StringTokenizer to tokenize the string (occurrence of " ")
//			// and calculate the distinct number of items using while loop to iterate the string tokenizer.
//			// however, the process can be improved using parallel stream to forEach
//			StringTokenizer t = new StringTokenizer(line, " ");
//			// we can convert this into using parallel stream
//			// IntStream.of(numbers) distinct count;
//			while (t.hasMoreTokens()) {
//				int x = Integer.parseInt(t.nextToken());
//				// log(x);
//				if (x + 1 > numItems)
//					numItems = x + 1;
//			}
//		}

		// instantiate an ExecutorServices for future use
		numOfProcessors = Runtime.getRuntime().availableProcessors();
		// several types of executorservices can be examined to
		// choose the best performance
		executors = Executors.newFixedThreadPool(numOfProcessors);

//		BufferedReader data_in = new BufferedReader(new FileReader(transactionFileName));
//		while (data_in.ready()) {
//			String line = data_in.readLine();
//			if (line.matches("\\s*"))
//				continue; // be friendly with empty lines
//			numTransactions++;
//			StringTokenizer t = new StringTokenizer(line, " ");
//			while (t.hasMoreTokens()) {
//				int x = Integer.parseInt(t.nextToken());
//				// log(x);
//				if (x + 1 > numItems)
//					numItems = x + 1;
//			}
//		}

		outputConfig();

	}

	/**
	 * outputs the current configuration
	 */
	private void outputConfig() {
		// output config info to the user
		log("Input configuration: " + numItems + " items, " + numTransactions + " transactions, ");
		log("minsup = " + minSup + "%");
	}

	/**
	 * puts in itemsets all sets of size 1, i.e. all possibles items of the datasets
	 */
	public void createItemsetsOfSize1() {
		// current itemsets are created one time here.
		// In this case, all the itemsets with size one has been created.
		// complexity of O(n)
		currentItemsets = new ArrayList<>();
		for (int i = 0; i < numItems; i++) {
			int[] cand = { i };
			currentItemsets.add(cand);
		}
	}

	/**
	 * if m is the size of the current itemsets, generate all possible itemsets of
	 * size n+1 from pairs of current itemsets replaces the itemsets of itemsets by
	 * the new ones
	 */
	private void createNewItemsetsFromPreviousOnes() {
		// by construction, all existing itemsets have the same size
		int currentSizeOfItemsets = currentItemsets.get(0).length;
		log("Creating itemsets of size " + (currentSizeOfItemsets + 1) + " based on " + currentItemsets.size()
				+ " itemsets of size " + currentSizeOfItemsets);

		HashMap<String, int[]> tempCandidates = new HashMap<String, int[]>(); // temporary candidates

		// compare each pair of itemsets of size n-1
		for (int i = 0; i < currentItemsets.size(); i++) {
			for (int j = i + 1; j < currentItemsets.size(); j++) {
				int[] X = currentItemsets.get(i);
				int[] Y = currentItemsets.get(j);

				assert (X.length == Y.length);

				// make a string of the first n-2 tokens of the strings
				int[] newCand = new int[currentSizeOfItemsets + 1];
				for (int s = 0; s < newCand.length - 1; s++) {
					newCand[s] = X[s];
				}

				int ndifferent = 0;
				// then we find the missing value
				for (int s1 = 0; s1 < Y.length; s1++) {
					boolean found = false;
					// is Y[s1] in X?
					for (int s2 = 0; s2 < X.length; s2++) {
						if (X[s2] == Y[s1]) {
							found = true;
							break;
						}
					}
					if (!found) { // Y[s1] is not in X
						ndifferent++;
						// we put the missing value at the end of newCand
						newCand[newCand.length - 1] = Y[s1];
					}

				}

				// we have to find at least 1 different, otherwise it means that we have two
				// times the same set in the existing candidates
				assert (ndifferent > 0);

				if (ndifferent == 1) {
					// HashMap does not have the correct "equals" for int[] :-(
					// I have to create the hash myself using a String :-(
					// I use Arrays.toString to reuse equals and hashcode of String
					Arrays.sort(newCand);
					tempCandidates.put(Arrays.toString(newCand), newCand);
				}
			}
		}

		// set the new itemsets
		currentItemsets = new ArrayList<int[]>(tempCandidates.values());
		log("Created " + currentItemsets.size() + " unique itemsets of size " + (currentSizeOfItemsets + 1));

	}

	/** put "true" in trans[i] if the integer i is in line */
	private void line2booleanArray(int[] itemset, boolean[] trans) {
		Arrays.fill(trans, false);
		//StringTokenizer stFile = new StringTokenizer(line, " "); // read a line from the file to the tokenizer
		// put the contents of that line into the transaction array

//		while (stFile.hasMoreTokens()) {
//
//			int parsedVal = Integer.parseInt(stFile.nextToken());
//			trans[parsedVal] = true; // if it is not a 0, assign the value to true
//		}
		// the granularity should be
		// item > itemset > itemsets > string
		for(int item : itemset) {
			trans[item] = true;
		}
	}

	/**
	 * 1. passes through the data to measure the frequency of sets in the current itemsets
	 * 2. filters those who are under the minimum support (minSup)
	 */
	public void calculateFrequentItemsets() throws Exception {

		log("Passing through the data to compute the frequency of " + currentItemsets.size() + " itemsets of size "
				+ currentItemsets.get(0).length);

		List<int[]> frequentCandidates = new ArrayList<int[]>(); // the frequent candidates for the current itemset

		boolean transactionIsMatched; // whether the transaction has all the items in an itemset

		// jinghoong: the count should be modified into something thread-safe
		// to curb with multithreading assess
		// the number of successful matches, initialized by zeros
		//int[] count = new int[currentItemsets.size()];
		AtomicIntegerArray count = new AtomicIntegerArray(currentItemsets.size());

		// for each transaction
		// jing hoong: in order to pass through all the datasets
		// you need to iterate through all the dataset with one processor
		// which can be improved using ExecutorServices + async
		CountDownLatch controller = new CountDownLatch(numOfProcessors);
		for(int coreId = 0; coreId < numOfProcessors; coreId++) {
			int[] startEndArr = calculateStartEndIndex(numOfProcessors,
					coreId,numTransactions);
			executors.execute(new Task(startEndArr[0],startEndArr[1],
					dataset,currentItemsets,count, controller, numItems));
		}
		controller.await();
//		for (int i = 0; i < numTransactions; i++) {
//
//			// boolean[] trans = extractEncoding1(data_in.readLine());
//			line2booleanArray(dataset.get(i), trans);
//
//			// check each candidate
//			for (int c = 0; c < currentItemsets.size(); c++) {
//				transactionIsMatched = true; // reset match to true
//				// tokenize the candidate so that we know what items need to be
//				// present for a match
//				int[] candidate = currentItemsets.get(c);
//				// int[] cand = candidatesOptimized[c];
//				// check each item in the itemset to see if it is present in the
//				// transaction
//				for (int candidateElement : candidate) {
//					if (!trans[candidateElement]) {
//						transactionIsMatched = false;
//						break;
//					}
//				}
//				if (transactionIsMatched) { // if at this point it is a match, increase the count
//					count[c]++;
//					// log(Arrays.toString(cand)+" is contained in trans "+i+" ("+line+")");
//				}
//			}
//		}

		for (int i = 0; i < currentItemsets.size(); i++) {
			// if the count% is larger than the minSup%, add to the candidate to
			// the frequent candidates
			if ((count.get(i) / (double) (numTransactions)) >= minSup) {
				foundFrequentItemSet(currentItemsets.get(i), count.get(i));
				frequentCandidates.add(currentItemsets.get(i));
			}
			// else log("-- Remove candidate: "+ Arrays.toString(candidates.get(i)) + " is:
			// "+ ((count[i] / (double) numTransactions)));
		}

		// new candidates are only the frequent candidates
		currentItemsets = frequentCandidates;
	}

	public static int[] calculateStartEndIndex(int numOfCores, int corePosition,
											   int numOfTransactions) {
		// return int[0] start index, int[1] end index
		int startIndex;
		int endIndex;
		int featuresPerCore;
		int remainingFeatures = 0;
		// scope: the number of cores must be in even number
		featuresPerCore = numOfTransactions / numOfCores;

		startIndex = corePosition * featuresPerCore;
		if(corePosition == numOfCores - 1) {
			endIndex = numOfTransactions - 1;
		} else {
			endIndex = (corePosition + 1) * featuresPerCore - 1;
		}
		return new int[] {startIndex, endIndex};
	}

	public static void Lister() {
		int b = itemsetValues.size();
		if (b == 0) {
			System.exit(0);
		}
		int i, j, k = 0, m = 0;
		String newb = itemsetValues.get(b - 1);
		int a = ((newb.substring(1, newb.length() - 1).split(", ")).length);
		int[][] lol = new int[b][a - 1];
		int[] lols = new int[b];
		for (i = 0; i < b; i++) {
			newb = itemsetValues.get(i);
			String[] poop = newb.substring(1, newb.length() - 1).split(", ");
			for (j = 0; j < poop.length - 1; j++) {
				lol[i][j] = Integer.parseInt(poop[j]);
			}
			lols[i] = Integer.parseInt(poop[j]);
			if ((j + 1) == a && k == 0) {
				k = i;
			}
			poop = null;
		}
		System.out.println("\nAssociation Rules: When Minimum Confidence=" + minConf * 100 + "%");
		for (i = k; i < b; i++) {
			for (j = 0; j < k; j++) {
				m += assoc_print(lol[i], lol[j], lols[i], lols[j]);
			}
		}
		if (m == 0) {
			System.out.println("No association rules passed the minimum confidence of " + minConf * 100 + "%");
		}
	}

	public static int assoc_print(int[] a, int[] b, int a1, int b1) {
		String win = "(", lose = "(";
		int i, j, k = 0;
		int[] loss = new int[a.length];
		for (i = 0; i < b.length && b[i] != 0; i++) {
			k = 1;
			win = win + b[i] + ",";
			for (j = 0; j < a.length; j++) {
				if (b[i] == a[j]) {
					k = 0;
					loss[j] = 1;
				}
			}
		}
		win = win.substring(0, win.length() - 1) + ")";
		for (i = 0; i < a.length; i++) {
			if (loss[i] == 0) {
				lose = lose + a[i] + ",";
			}
		}
		lose = lose.substring(0, lose.length() - 1) + ")";
		if (k == 0) {
			double Lol = (double) a1 / b1;
			if (Lol > minConf) {
				System.out.printf("%s ==> %s :	%.2f%c \n", win, lose, Lol * 100, 37);
				return 1;
			}
		}
		return 0;
	}
}

class Task implements Runnable {
	int startIndex;
	int endIndex;
	List<int[]> dataset;
	List<int[]> currentItemsets;
	AtomicIntegerArray count;
	CountDownLatch controller;
	int numItems;

	boolean transactionIsMatched;
	boolean[] trans;

	@Override
	public void run() {
		trans = new boolean[numItems];
		for (int i = startIndex; i <= endIndex; i++) {
			Arrays.fill(trans, false);
			for(int item : dataset.get(i)) {
				trans[item] = true;
			}
			// check each candidate
			for (int c = 0; c < currentItemsets.size(); c++) {
				transactionIsMatched = true; // reset match to true
				// tokenize the candidate so that we know what items need to be
				// present for a match
				int[] candidate = currentItemsets.get(c);
				// int[] cand = candidatesOptimized[c];
				// check each item in the itemset to see if it is present in the
				// transaction
				for (int candidateElement : candidate) {
					if (!trans[candidateElement]) {
						transactionIsMatched = false;
						break;
					}
				}
				if (transactionIsMatched) { // if at this point it is a match, increase the count
					count.incrementAndGet(c);
					// log(Arrays.toString(cand)+" is contained in trans "+i+" ("+line+")");
				}
			}
		}
		controller.countDown();
	}

	public Task(int startIndex, int endIndex,List<int[]> dataset,
				List<int[]> currentItemsets, AtomicIntegerArray count,
				CountDownLatch controller, int numItems) {
		this.startIndex = startIndex;
		this.endIndex = endIndex;
		this.dataset = dataset;
		this.currentItemsets = currentItemsets;
		this.count = count;
		this.controller = controller;
		this.numItems = numItems;
	}
}
