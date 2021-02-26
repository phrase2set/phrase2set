package edu.iowa.t2api.ba;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import utils.FileUtils;
import config.Config;

public class CooccurredCodePairs {
	public enum DataType {
		DT_nlp,
		DT_Java,
	}
	
	private DataType processingType = DataType.DT_nlp;
	
	public static void main(String[] args) throws IOException {
		CooccurredCodePairs counter = new CooccurredCodePairs();
		counter.countElementPairs();
	}
	
	public void countElementPairs() throws IOException {
		/* Count each pair <code1, code2> appearing in each bag of a training pair */
		HashMap<String, HashMap<String, Integer>> codeElementPairs = new HashMap<String, HashMap<String,Integer>>();
		/* Each time encountering a code element <code>, count it */
		HashMap<String, Integer> codeFrequencies = new HashMap<String, Integer>();
		/* Weights of each pairs <code1, code2> after normalization. Sort by weights and write to file */
		HashMap<String, HashMap<String, Double>> allPairsByWeights = new HashMap<String, HashMap<String,Double>>();
		/* Counting the number of occurrences of each code elements */
		HashMap<String, Integer> CodeOccurrences = new HashMap<String, Integer>();
		
		/* Using code frequencies to punish those who appear so frequently, e.g. String.String, etc */
		
		/* Read from code elements from each pair of training data, better to use LinkedHashSet to remove duplicate */
		FileInputStream inStream;
		if(processingType == DataType.DT_Java)
			inStream = new FileInputStream("/home/dharani/concordia/thesis/CodeToTask/berkeleyaligner/distribution/title/train/train.cd");
		else
			inStream = new FileInputStream("/home/dharani/concordia/thesis/CodeToTask/berkeleyaligner/distribution/title/train/train.ts");
		
		InputStreamReader inReader = new InputStreamReader(inStream);
		BufferedReader bufReader = new BufferedReader(inReader);
		
		String currentLine = "";
		int lineCount = 0;
		while((currentLine = bufReader.readLine()) != null) {
			/* For debug only */
			if(++lineCount % 1000 == 0) {
				System.out.print(lineCount + " ");
				if(lineCount % 10000 == 0)
					System.out.println();
			}
			
			/* Read each line into a string and parse it into set of code elements */
			List<String> setCodeElements = new ArrayList<String>();
			String[] dupCodeElements = currentLine.split("\\s");
			for(String code : dupCodeElements) {
				setCodeElements.add(code);
				Integer frequency = CodeOccurrences.get(code);
				if(frequency == null)
					frequency = 0;
				CodeOccurrences.put(code, frequency + 1);
			}
			
			/* Count co-occurrence of each pair of 2 code elements */
			int I = setCodeElements.size();
			for(int i = 0; i < I; i ++) {
				String codeElement1 = setCodeElements.get(i);
				HashMap<String, Integer> corresBag = codeElementPairs.get(codeElement1);
				for(int j = 0; j < I; j ++) {
					if(j == i)
						continue;
					String codeElement2 = setCodeElements.get(j);
					if(corresBag == null) {
						corresBag = new HashMap<String, Integer>();
						corresBag.put(codeElement2, 1);
						codeElementPairs.put(codeElement1, corresBag);
					}
					else {
						Integer frequency = corresBag.get(codeElement2);
						if(frequency == null)
							frequency = 0;
						corresBag.put(codeElement2, frequency + 1);
					}
				}
				Integer freqByElement = codeFrequencies.get(codeElement1);
				if(freqByElement == null)
					freqByElement = 0;
				codeFrequencies.put(codeElement1, freqByElement + 1);
			}
		}
		bufReader.close();
		
		/* Re-calculate weights and sort. Should divide by max frequency of two code elements, not max. Reasoning again */
		for(String codeElement : codeElementPairs.keySet()) {
			HashMap<String, Double> weightedCandList = new HashMap<String, Double>();
			
			HashMap<String, Integer> cooccurredElements = codeElementPairs.get(codeElement);
			for(String corresElement : cooccurredElements.keySet()) {
				int pairFreq = cooccurredElements.get(corresElement);
				int elementFreq = codeFrequencies.get(codeElement);
				int corElementFreq = codeFrequencies.get(corresElement);
				int max = Math.max(elementFreq, corElementFreq);
				/* Weight of pair in terms of occurrence of the second element. E.g, if Intent.getAction and Abc.Xyz appear just one time, 
				 * we also encounter Abc.Xyz one time, => weight = 1.0. It works so bad, if Abc.Xyz is a noisy extracted element */
				Double weight = pairFreq / (double) max;
				weightedCandList.put(corresElement, weight);
			}
			
			/* Sorting */
			HashMap<String, Integer> sortedMapByFreq = sortByFrequency(cooccurredElements, false); // false indicates descending order
			HashMap<String, Double> sortedMapByWeight = sortByWeight(weightedCandList, false);
			codeElementPairs.put(codeElement, sortedMapByFreq);
			allPairsByWeights.put(codeElement, sortedMapByWeight);
			
			/* Debugging. Take a look at an example */
			if(codeElement.equals("WebView.WebView")) {
				int count = 0;
				System.out.format("Candidates for expansion algorithm based on counting \"%s\": ", codeElement);
				for(String sample : sortedMapByFreq.keySet()) {
					if(count ++ < 10) {
						System.out.print(sample + " ");
					}
				}
				System.out.println();
				/* reset counter */
				count = 0;
				System.out.format("Candidates for expansion algorithm based on weighting \"%s\": ", codeElement);
				for(String sample : sortedMapByWeight.keySet()) {
					if(count ++ < 10) {
						System.out.print(sample + " ");
					}
				}
				System.out.println();
			}
		}
		
		new File(Config.codeTermStatsPath).mkdir();
		
		/* Output 2 files separately (counting and weighting) for future comparison and reference */
		if(processingType == DataType.DT_Java) {
			FileUtils.writeObjectFile(codeElementPairs, Config.codeTermStatsPath + "CodePairByFrequency_" + Config.fileExtension + ".dat");
			FileUtils.writeObjectFile(allPairsByWeights, Config.codeTermStatsPath + "CodePairByWeight_" + Config.fileExtension + ".dat");
			FileUtils.writeObjectFile(CodeOccurrences, Config.codeTermStatsPath + "CodeOccurrences_" + Config.fileExtension + ".dat");
		}
		else {
			FileUtils.writeObjectFile(codeElementPairs, Config.codeTermStatsPath + "TermPairByFrequency_" + Config.fileExtension + ".dat");
			FileUtils.writeObjectFile(allPairsByWeights, Config.codeTermStatsPath + "TermPairByWeight_" + Config.fileExtension + ".dat");
			FileUtils.writeObjectFile(CodeOccurrences, Config.codeTermStatsPath + "TermOccurrences_" + Config.fileExtension + ".dat");
		}
	}
	
	public static LinkedHashMap<String, Double> sortByWeight(Map<String, Double> unsortMap, final boolean order) {

        List<Entry<String, Double>> list = new LinkedList<Entry<String, Double>>(unsortMap.entrySet());

        // Sorting the list based on values
        Collections.sort(list, new Comparator<Entry<String, Double>>() {
            public int compare(Entry<String, Double> o1, Entry<String, Double> o2) {
                if (order)
                    return o1.getValue().compareTo(o2.getValue());
                else
                    return o2.getValue().compareTo(o1.getValue());
            }
        });

        // Maintaining insertion order with the help of LinkedList
        LinkedHashMap<String, Double> sortedMap = new LinkedHashMap<String, Double>();
        for (Entry<String, Double> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }
	
	public static LinkedHashMap<String, Integer> sortByFrequency(Map<String, Integer> unsortMap, final boolean order) {

        List<Entry<String, Integer>> list = new LinkedList<Entry<String, Integer>>(unsortMap.entrySet());

        // Sorting the list based on values
        Collections.sort(list, new Comparator<Entry<String, Integer>>() {
            public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
                if (order)
                    return o1.getValue().compareTo(o2.getValue());
                else
                    return o2.getValue().compareTo(o1.getValue());
            }
        });

        // Maintaining insertion order with the help of LinkedList and LinkedHashMap
        LinkedHashMap<String, Integer> sortedMap = new LinkedHashMap<String, Integer>();
        for (Entry<String, Integer> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }

}
