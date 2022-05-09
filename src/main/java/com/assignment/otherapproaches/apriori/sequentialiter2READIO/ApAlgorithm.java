package com.assignment.otherapproaches.apriori.sequentialiter2READIO;

import org.openjdk.jmh.annotations.*;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

@State(Scope.Benchmark)
public class ApAlgorithm {
	/** the list of current itemsets */
	private List<int[]> itemsets;
	/** the name of the transcation file */
	private String transaFile;
	/** number of different items in the dataset */
	private int numItems;
	/** total number of transactions in transaFile */
	private int numTransactions;
	/** minimum support for a frequent itemset in percentage, e.g. 0.8 */
	private double minSup;
	/**
	 * minimum confidence for a generating association rules in percentage, e.g. 0.8
	 */
	private static double minConf;
	/** Stores The values from the frequent itemsets */
	private static List<String> tupples = new ArrayList<String>();

	/** by default, Apriori is used with the command line interface */
	private boolean usedAsLibrary = false;

	// Jing Hoong: refactoring
	// dataset to be stored inside this variable for future use
	private List<int[]> dataset;

//	@Benchmark
//	@BenchmarkMode(Mode.Throughput)
//	@Measurement(iterations = 1)
//	@Fork(value = 3, warmups = 2)
//	@Timeout(time = 30)
//	@OutputTimeUnit(TimeUnit.SECONDS)
	public void test() {
	//public static void main(String[] args) {
		ApAlgorithm ap = new ApAlgorithm();
	}

	/**
	 * generates the apriori itemsets from a file
	 * 
	 * param args configuration parameters: args[0] is a filename, args[1] the min
	 *             support (e.g. 0.8 for 80%)
	 * @throws Exception
	 */
	public ApAlgorithm() {
		try {
			long start = System.currentTimeMillis();
			configure();
			go();
			long end = System.currentTimeMillis();
			log("Execution time is: " + ((double) (end - start) / 1000) + " seconds.");
			log("Done");
		} catch (Exception e) {}
	}
	/** starts the algorithm after configuration */
	private void go() throws Exception {
		// start timer

		// first we generate the candidates of size 1
		createItemsetsOfSize1();
		int itemsetNumber = 1; // the current itemset being looked at
		int nbFrequentSets = 0;

		while (itemsets.size() > 0) {

			calculateFrequentItemsets();

			if (itemsets.size() != 0) {
				nbFrequentSets += itemsets.size();
				log("Found " + itemsets.size() + " frequent itemsets of size " + itemsetNumber + " (with support "
						+ (minSup * 100) + "%)");

				createNewItemsetsFromPreviousOnes();
			}
			itemsetNumber++;
		}

		// display the execution time
		log("Found " + nbFrequentSets + " frequents sets for support " + (minSup * 100) + "% (absolute "
				+ Math.round(numTransactions * minSup) + ")");
		//Lister();
	}

	/** triggers actions if a frequent item set has been found */
	private void foundFrequentItemSet(int[] itemset, int support) {
		String New, New1;

		New = Arrays.toString(itemset);
		New1 = New.substring(0, New.length() - 1) + ", " + support + "]";
		tupples.add(New1);
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
	public void configure() throws Exception {
		// setting transafile
//		if (args.length != 0)
//			transaFile = args[0];

		transaFile = "mushroom.dat"; // default

		// setting minsupport
//		if (args.length >= 2)
//			minSup = (Double.valueOf(args[1]).doubleValue());
		minSup = 0.5;// by default
		if (minSup > 1 || minSup < 0)
			throw new Exception("minSup: bad value");

		minConf = 0.5;
		if (minConf > 1 || minConf < 0)
			throw new Exception("minConf: bad value");

		// going through the file to compute numItems and numTransactions
		numItems = 0;
		numTransactions = 0;
		numItems = 0;
		numTransactions = 0;
		dataset = new ArrayList<>();
		List<String> lines = Files.readAllLines(new File(transaFile).toPath(), Charset.defaultCharset());
		for(String line : lines) {
			if (line.matches("\\s*"))
				continue; // be friendly with empty lines
			String[] strArray = line.split(" ");
			int[] intArray = new int[strArray.length];
			for(int i = 0; i < strArray.length; i++) {
				intArray[i] = Integer.parseInt(strArray[i]);
			}
			dataset.add(intArray);
			numTransactions++;
			StringTokenizer t = new StringTokenizer(line, " ");
			while (t.hasMoreTokens()) {
				int x = Integer.parseInt(t.nextToken());
				// log(x);
				if (x + 1 > numItems)
					numItems = x + 1;
			}
		}
		System.out.println("RCL: " + numItems + numTransactions);

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
	private void createItemsetsOfSize1() {
		itemsets = new ArrayList<int[]>();
		for (int i = 0; i < numItems; i++) {
			int[] cand = { i };
			itemsets.add(cand);
		}
	}

	/**
	 * if m is the size of the current itemsets, generate all possible itemsets of
	 * size n+1 from pairs of current itemsets replaces the itemsets of itemsets by
	 * the new ones
	 */
	private void createNewItemsetsFromPreviousOnes() {
		// by construction, all existing itemsets have the same size
		int currentSizeOfItemsets = itemsets.get(0).length;
		log("Creating itemsets of size " + (currentSizeOfItemsets + 1) + " based on " + itemsets.size()
				+ " itemsets of size " + currentSizeOfItemsets);

		HashMap<String, int[]> tempCandidates = new HashMap<String, int[]>(); // temporary candidates

		// compare each pair of itemsets of size n-1
		for (int i = 0; i < itemsets.size(); i++) {
			for (int j = i + 1; j < itemsets.size(); j++) {
				int[] X = itemsets.get(i);
				int[] Y = itemsets.get(j);

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
		itemsets = new ArrayList<int[]>(tempCandidates.values());
		log("Created " + itemsets.size() + " unique itemsets of size " + (currentSizeOfItemsets + 1));

	}

	/** put "true" in trans[i] if the integer i is in line */
	private void line2booleanArray(int[] line, boolean[] trans) {
		Arrays.fill(trans, false);
		// put the contents of that line into the transaction array
		for(int token : line) {
			trans[token] = true; // if it is not a 0, assign the value to true
		}
	}

	/**
	 * passes through the data to measure the frequency of sets in @link itemsets,
	 * then filters thoses who are under the minimum support (minSup)
	 */
	private void calculateFrequentItemsets() throws Exception {

		log("Passing through the data to compute the frequency of " + itemsets.size() + " itemsets of size "
				+ itemsets.get(0).length);

		List<int[]> frequentCandidates = new ArrayList<int[]>(); // the frequent candidates for the current itemset

		boolean match; // whether the transaction has all the items in an itemset
		int count[] = new int[itemsets.size()]; // the number of successful matches, initialized by zeros

		// load the transaction file
		//BufferedReader data_in = new BufferedReader(new InputStreamReader(new FileInputStream(transaFile)));

		boolean[] trans = new boolean[numItems];

		// for each transaction
		for (int i = 0; i < numTransactions; i++) {

			// boolean[] trans = extractEncoding1(data_in.readLine());
			// String line = data_in.readLine();
			line2booleanArray(dataset.get(i), trans);

			// check each candidate
			for (int c = 0; c < itemsets.size(); c++) {
				match = true; // reset match to false
				// tokenize the candidate so that we know what items need to be
				// present for a match
				int[] cand = itemsets.get(c);
				// int[] cand = candidatesOptimized[c];
				// check each item in the itemset to see if it is present in the
				// transaction
				for (int xx : cand) {
					if (trans[xx] == false) {
						match = false;
						break;
					}
				}
				if (match) { // if at this point it is a match, increase the count
					count[c]++;
					// log(Arrays.toString(cand)+" is contained in trans "+i+" ("+line+")");
				}
			}

		}

		// data_in.close();

		for (int i = 0; i < itemsets.size(); i++) {
			// if the count% is larger than the minSup%, add to the candidate to
			// the frequent candidates
			if ((count[i] / (double) (numTransactions)) >= minSup) {
				foundFrequentItemSet(itemsets.get(i), count[i]);
				frequentCandidates.add(itemsets.get(i));
			}
			// else log("-- Remove candidate: "+ Arrays.toString(candidates.get(i)) + " is:
			// "+ ((count[i] / (double) numTransactions)));
		}

		// new candidates are only the frequent candidates
		itemsets = frequentCandidates;
	}

	public static void Lister() {
		int b = tupples.size();
		if (b == 0) {
			System.exit(0);
		}
		int i, j, k = 0, m = 0;
		String newb = tupples.get(b - 1);
		int a = ((newb.substring(1, newb.length() - 1).split(", ")).length);
		int[][] lol = new int[b][a - 1];
		int[] lols = new int[b];
		for (i = 0; i < b; i++) {
			newb = tupples.get(i);
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
