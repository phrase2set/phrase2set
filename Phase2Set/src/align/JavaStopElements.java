package align;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Hashtable;

import config.Config;


public class JavaStopElements {
	
	private static Hashtable<String, Double> stopElements = null;
	public final static int threshold = 25000;
	
	static {
		if(stopElements == null) {
			stopElements = new Hashtable<String, Double>();
			Double dummy = new Double(0);
			
			///Hard code: 12 most frequent code elements
			stopElements.put("String.String", dummy);
			stopElements.put("int.int", dummy);
			stopElements.put("View.View", dummy);
			stopElements.put("Bundle.Bundle", dummy);
			stopElements.put("Bundle.saveInstanceState", dummy);
			stopElements.put("Intent.Intent", dummy);
			stopElements.put("Log.Log", dummy);
			stopElements.put("Context.Context", dummy);
			stopElements.put("AsyncTask.AsyncTask", dummy);
			stopElements.put("Activity.Activity", dummy);
			stopElements.put("AsyncTask.AsyncTask", dummy);
			stopElements.put("Context.context", dummy);
		}
	}
	
	public static void loadData() throws IOException {
		InputStreamReader isr = new InputStreamReader(new FileInputStream(Config.mainDatabasePath + "ce.table"));
		BufferedReader br = new BufferedReader(isr);
		
		String curLine;
		int lineCount = 0;
		while((curLine=br.readLine()) != null){
			lineCount++;
			if(lineCount%1000==0){
				System.out.print(lineCount + "  ");
				if(lineCount%100000==0)
					System.out.println();
				
			}
			String [] split = curLine.split(",");
			String pqn = split[0];
			String name = split[1];
			int docFrequency = Integer.parseInt(split[3]);
			if(docFrequency >= threshold)
				stopElements.put(pqn +"." + name, new Double(0.0));
		}
		System.out.format("Number of stop code elements %d", stopElements.size());
		br.close();
	}
	
	public static boolean isStopElement(String element) {
		return stopElements.containsKey(element);
	}

}
