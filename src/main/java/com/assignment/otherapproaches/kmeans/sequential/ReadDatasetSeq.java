package com.assignment.otherapproaches.kmeans.sequential;/*
 * Programmed by Shephalika Shekhar
 * Class to extract features from file
 */

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ReadDatasetSeq {
	
	private List<double[]> features;

	private List<String> label;

	private int numberOfFeatures;
	
	public List<double[]> getFeatures() {
		return features;
	}
	
	public List<String> getLabel() {
		return label;
	}
	
	public void read(String s) throws NumberFormatException, IOException {
		
		File file=new File(s);
		
		try {
			BufferedReader readFile=new BufferedReader(new FileReader(file));
			String line;
			while((line=readFile.readLine()) != null)
				{

				 String[] split = line.split(",");
						 double[] feature = new double[split.length - 1];
						numberOfFeatures = split.length-1;
						for (int i = 0; i < split.length - 1; i++)
							 feature[i] = Double.parseDouble(split[i]);
						features.add(feature);
						String labels = split[feature.length];
						label.add(labels);
				}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
	}

	public ReadDatasetSeq() {
		features = new ArrayList<>();
		label = new ArrayList<>();
	}

	public void display() {
		Iterator<double[]> itr=features.iterator();
		Iterator<String> sitr=label.iterator();
		while(itr.hasNext())
		{
			double db[]=itr.next();
			for(int i=0; i<4;i++)
		{
			System.out.print(db[i]+" ");
		}
			String s=sitr.next() ;
			System.out.println(s);
			//System.out.println();
		}

	}

	public int getNumberOfFeatures() {
		return numberOfFeatures;
	}

	public void setNumberOfFeatures(int numberOfFeatures) {
		this.numberOfFeatures = numberOfFeatures;
	}
}
