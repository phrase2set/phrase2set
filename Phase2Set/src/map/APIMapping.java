package map;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import utils.FileUtils;
import utils.Sortings;
import align.JavaStopElements;
import config.Config;
import edu.berkeley.nlp.wa.basic.Pair;
import eval.FormatMappingOuput;

public class APIMapping {
	
	private boolean pivotUtilized = true;
	
	
	public static void main(String[] args) throws Exception {
		JavaStopElements.loadData();
		APIMapping selector = new APIMapping();
		
		/** Do mappings */
		String testInput = Config.alignTestDirPath + "test.l";
		String output = Config.alignEvalDirPath + "output.txt";
		selector.doMapping(testInput, output);
		
		/** Produce output with format usable by Gralan */
		FormatMappingOuput processor = new FormatMappingOuput();
		processor.format();
	}
	
	@SuppressWarnings("unchecked")
	public void doMapping(String inputText, String output) throws IOException {
		
		/*** Load data */
//		Map<Pair<String, String>, Double> mappingsWithIBMWeights = (HashMap<Pair<String, String>, Double>)
//				FileUtils.readObjectFile(Config.mappingDirPath + "mappingsWithIBMWeights_236919.dat");
		Map<Pair<String, String>, Integer> mappingNumb = (Map<Pair<String, String>, Integer>) 
				FileUtils.readObjectFile(Config.mappingDirPath + "aligmentFrequency_236919.dat");
		Map<String, Set<String>> mappingsByTerm = (Map<String, Set<String>>) 
				FileUtils.readObjectFile(Config.mappingDirPath + "alignedCodeByTerm_236919.dat");
		Map<String, Integer> mappingsByCode = (HashMap<String, Integer>) 
				FileUtils.readObjectFile(Config.mappingDirPath + "mappingCountByCode_236919.dat");
		
		/* Frequency of all pairs of code elements*/
		HashMap<String, HashMap<String, Integer>> codeElementPairs = (HashMap<String, HashMap<String, Integer>>) 
				FileUtils.readObjectFile(Config.codeTermStatsPath + "CodePairByFrequency_236919.dat");
		HashMap<String, HashMap<String, Double>> allPairsByWeights = (HashMap<String, HashMap<String, Double>>) 
				FileUtils.readObjectFile(Config.codeTermStatsPath + "CodePairByWeight_236919.dat");
		HashMap<String, Integer> CodeOccurrences = (HashMap<String, Integer>) 
				FileUtils.readObjectFile(Config.codeTermStatsPath + "CodeOccurrences_236919.dat");
		HashMap<String, HashMap<String, Integer>> wordPairs = (HashMap<String, HashMap<String, Integer>>) 
				FileUtils.readObjectFile(Config.codeTermStatsPath + "TermPairByFrequency_236919.dat");
		HashMap<String, Integer> docFrequency = (HashMap<String, Integer>) 
				FileUtils.readObjectFile(Config.codeTermStatsPath + "TermOccurrences_" + Config.fileExtension + ".dat");
		HashMap<String, Integer> descOrderDocFrequency = Sortings.sortByFrequency(docFrequency, false);
		
		/* List candidate pivots from camel-case detection */
		HashMap<Integer,LinkedHashSet<String>> codeBlendedInText = (HashMap<Integer, LinkedHashSet<String>>)
				FileUtils.readObjectFile(Config.mappingDirPath + "CodeBlendedInTest.dat");
		HashMap<Integer, LinkedHashSet<String>> removedTerms = (HashMap<Integer, LinkedHashSet<String>>)
				FileUtils.readObjectFile(Config.stackOverflowTest + "removedTerms.dat");
						
		/* Output alignment */
		final FileWriter outputWriter = new FileWriter(output);
		
		/* Read from test data and get terms for each sentence with their descending tf-idf */
		FileInputStream inStream = new FileInputStream(inputText);
		InputStreamReader inStrReader = new InputStreamReader(inStream);
		BufferedReader bufReader = new BufferedReader(inStrReader);
		
		String currentLine = "";
		int lineCount = 0;
		while ((currentLine = bufReader.readLine()) != null) {
			LinkedHashSet<String> pivotList = codeBlendedInText.get(++lineCount);
			String[] sequences = currentLine.split("\\s");
			
			/** Prepare data structure for pivot selection */
			/* Count # terms that this code element maps to */
			HashMap<String, Integer> commonCodeElements = new HashMap<String, Integer>();
			/* Store weights of mappings <term, code> to rank candidate and choose the best as pivot */
			HashMap<String, HashMap<String, Double>> pivotCandidateWeights = new HashMap<String, HashMap<String,Double>>();
			/* Candidates for mappings */
			HashMap<String, HashMap<String, Double>> mappingCandidates = new HashMap<String, HashMap<String,Double>>();
			/* Store all distinct terms */
			LinkedHashSet<String> allWords = new LinkedHashSet<String>();
			
			/// TODO: Be careful with duplicate terms. Not sure whether it could impact on pivot selection???
			for(String term : sequences) {
				if(!descOrderDocFrequency.containsKey(term) || descOrderDocFrequency.get(term) > 30000 || descOrderDocFrequency.get(term) < 15)
					continue;
				Set<String> relElements = mappingsByTerm.get(term); // get relevant code elements
				/** There is no code element aligned with this <term> */
				if(relElements == null || (removedTerms.get(lineCount) != null && removedTerms.get(lineCount).contains(term)))
					continue;
				
				/* Store for determining textual pivot */
				allWords.add(term);
				/* This score for selecting pivot */
				HashMap<String, Double> scores = new HashMap<String, Double>();
				/* Score using counting that works effectively in finding mappings */
				HashMap<String, Double> scores2 = new HashMap<String, Double>();
				for(String element : relElements) {
					/** Compute #mapping <term, element> */
					int totalCount = mappingNumb.get(new Pair<String, String>(term, element));
					/* #mappings with element */
					int prob_element = mappingsByCode.get(element);
					double score = totalCount / (double) prob_element;
					scores.put(element, score);
					scores2.put(element, (double) totalCount);
				}
				
				/* Sorting and select elements with highest ranking score */
				Map<String, Double> sortedList = Sortings.sortByComparator(scores, false);
				LinkedHashMap<String, Double> sortedList2 = Sortings.sortByWeight(scores2, false);
				mappingCandidates.put(term, sortedList2);
				
				int elementCount = 0;
				for(String candidate : sortedList.keySet()) {
					if(elementCount ++ >= 100)
						break;
					Integer occurrenceNo = commonCodeElements.get(candidate);
					if(occurrenceNo == null)
						occurrenceNo = 0;
					commonCodeElements.put(candidate, occurrenceNo + 1);
					
					HashMap<String, Double> correspMappedTerms = pivotCandidateWeights.get(candidate);
					if(correspMappedTerms == null) {
						correspMappedTerms = new HashMap<String, Double>();
						pivotCandidateWeights.put(candidate, correspMappedTerms);
					}
					correspMappedTerms.put(term, sortedList.get(candidate));
				}
			}
			
			HashMap<String, Integer> sortedCommonCodeElements = Sortings.sortByFrequency(commonCodeElements, false);
			
			/* Should expand based on textual context */
			LinkedHashSet<String> textualPivots = new LinkedHashSet<String>();
			
			/*Done with pivot selection, now expand association from the pivot */
			StringBuffer sbuf = new StringBuffer();
			if(pivotUtilized) {
				String pivot = "";
				if(!sortedCommonCodeElements.isEmpty()) {
					if(pivotList == null)
						pivotList = new LinkedHashSet<String>();
					
					/* Remove frequent items from pivot list */
					Iterator<String> iterator = pivotList.iterator();
					for(; iterator.hasNext(); ) {
						String removedPivot = iterator.next();
						if(JavaStopElements.isStopElement(removedPivot))
							iterator.remove();
					}
					int maximumCoOccurrence = (int) sortedCommonCodeElements.values().toArray()[0];
					pivot = choosePivot(sortedCommonCodeElements, pivotCandidateWeights, maximumCoOccurrence);
					System.out.println(currentLine + ": " + pivot);
					if(!pivot.isEmpty())
						pivotList.add(pivot);
				}
				textualPivots = selectTextualPivot(pivotList, pivotCandidateWeights, allWords);
				orderTermsForExpansion(textualPivots, allWords, wordPairs);
				expandAssociations(mappingCandidates, codeElementPairs, allPairsByWeights, CodeOccurrences, pivotList, textualPivots, sbuf);
			}
			else 
				expandAssociations(mappingCandidates, codeElementPairs, allPairsByWeights, CodeOccurrences, pivotList, textualPivots, sbuf);
			outputWriter.append(sbuf.toString() + System.lineSeparator());
		}
		
		outputWriter.close();
		bufReader.close();
	}
	
	private String choosePivot(HashMap<String, Integer> sortedCommonCodeElements, HashMap<String, 
			HashMap<String, Double>> pivotCandidateWeights, int maximumCoOccurrence) {
		if(maximumCoOccurrence == 0) {
			return "";
		}

		String elementWithHighestScore = "";
		HashMap<String, Double> pivotalCandidates = new HashMap<String, Double>();
		
		for(String mostFreqOcc : sortedCommonCodeElements.keySet()) {
			int freq = sortedCommonCodeElements.get(mostFreqOcc);
			double combinedWeight = 0;
			if(freq == maximumCoOccurrence) {
				System.out.print(mostFreqOcc + " ");
				HashMap<String, Double> termList = pivotCandidateWeights.get(mostFreqOcc);
				for(String term : termList.keySet()) {
					combinedWeight += Math.log(termList.get(term)); // using negative for maximum
				}
				pivotalCandidates.put(mostFreqOcc, combinedWeight);
			}
		}
		
		HashMap<String, Double> descOrderCandidates = Sortings.sortByWeight(pivotalCandidates, false);
		boolean pivotFound = false;
		for(String candidate : descOrderCandidates.keySet()) {
			if(candidate.isEmpty() || JavaStopElements.isStopElement(candidate) || candidate.split("\\.").length > 2)
				continue;
			else {
				pivotFound = true;
				elementWithHighestScore = candidate;
				break;
			}
		}
		
		System.out.println();
		if(!pivotFound) {
			maximumCoOccurrence -= 1;
			elementWithHighestScore = choosePivot(sortedCommonCodeElements, pivotCandidateWeights, maximumCoOccurrence);
		}
		return elementWithHighestScore;
	}
	
	private LinkedHashSet<String> selectTextualPivot(LinkedHashSet<String> codePivots, HashMap<String, HashMap<String, Double>> pivotCandidateWeights,
			LinkedHashSet<String> allWords) {
		LinkedHashSet<String> textualPivots = new LinkedHashSet<String>();
		if(codePivots.isEmpty())
			return textualPivots;
		
		for(String codePivot : codePivots) {
			String tPivot = "";
			String[] partOfCode = codePivot.split("\\.");
			if(partOfCode.length == 2)
				tPivot = partOfCode[1].toLowerCase();
			if(allWords.contains(tPivot)) {
				textualPivots.add(tPivot);
				continue;
			}
			
			HashMap<String, Double> corresMappedTerms = pivotCandidateWeights.get(codePivot);
			if(corresMappedTerms == null) {
				continue;
			}
			/* In the case, the pivot is selected based on Camel case, it might be not existed in <pivotCandidateWeights> */
			HashMap<String, Double> descOrderMappedTerms = Sortings.sortByWeight(corresMappedTerms, false);
			
//			for(String candidate : descOrderMappedTerms.keySet()) {
//				if(!textualPivots.contains(candidate))
//					textualPivots.add(candidate);
//			}
			tPivot = (String) descOrderMappedTerms.keySet().toArray()[0];
			textualPivots.add(tPivot);
		}
		return textualPivots;
	}
	
	private void expandAssociations(HashMap<String, HashMap<String, Double>> mappingCandidates,
			HashMap<String, HashMap<String, Integer>> codeElementPairs, 
			HashMap<String, HashMap<String, Double>> allPairsByWeights,
			HashMap<String, Integer> CodeOccurrences,
			LinkedHashSet<String> pivots, LinkedHashSet<String> textualPivots, StringBuffer sbuf) {

		HashMap<String, Integer> selectedMappings = new HashMap<String, Integer>();
		Integer dummy = 0;
		/* Write code elements extracted using Camel case as immediate output*/
		for(String pivot : pivots) {
			if(pivot.equals("findViewById"))
				pivot = "View.findViewById";
			else if(pivot.equals("getAssets"))
				pivot = "Context.getAssets";
			else if(pivot.equals("openConnection"))
				pivot = "URL.openConnection";
			selectedMappings.put(pivot, dummy);
			sbuf.append(pivot + " ");
		}
		
		/* The expansion algorithm follows 2 directions: text and code sides: which term goes first does matter */
		if(textualPivots.isEmpty()) {
			for(String term : mappingCandidates.keySet())
				textualPivots.add(term);
		}
		
		for(String term : textualPivots) {
			if(!mappingCandidates.containsKey(term))
				continue;
			LinkedHashSet<String> candByTextualContext = new LinkedHashSet<String>();
			int count = 0;
			for(String candidate : mappingCandidates.get(term).keySet()) {
				if(count ++ >= 20)
					break;
				candByTextualContext.add(candidate);
			}
			
			HashMap<String, Double> candByCodeContext = new HashMap<String, Double>();
			for(String candidate : candByTextualContext) {
				if(selectedMappings.containsKey(candidate))
					continue;
				double assoWeight = 0.0;
				int lowerThreshold = 0;
				double upperThres = 0.0;
				
				for(String eachPivot : pivots) {
					HashMap<String, Integer> freqCodesGoWithPivot = codeElementPairs.get(eachPivot);
					if(freqCodesGoWithPivot == null)
						continue;
					Integer count_c_p = 1;
					if(freqCodesGoWithPivot.containsKey(candidate))
						count_c_p = freqCodesGoWithPivot.get(candidate);
					int count_p = CodeOccurrences.get(eachPivot);
					assoWeight += Math.log(count_c_p);
					/* Check whether this candidate appears with one of the pivots */
					lowerThreshold += count_c_p;
					upperThres += Math.log(1/(double) count_p);
				}
				if(pivots.isEmpty()) {
					candByCodeContext.put(candidate, assoWeight);
					continue;
				}
				/* At least one element appearing with one of the pivots more than 3 times */
				upperThres += Math.log(1); // default 6 for SO
				if(lowerThreshold != pivots.size() && assoWeight >= upperThres)
					candByCodeContext.put(candidate, assoWeight);
			}
			
			HashMap<String, Double> sortedCandidates = Sortings.sortByWeight(candByCodeContext, false);
			
			count = 0;
			for(String candidate : sortedCandidates.keySet()) {
				if(count++ >= 5 /*|| statWeight < 0.5*/)
					break;
				if(candidate.equals("findViewById"))
					candidate = "View.findViewById";
				else if(candidate.equals("getAssets"))
					candidate = "Context.getAssets";
				else if(candidate.equals("openConnection"))
					candidate = "URL.openConnection";
				if(selectedMappings.containsKey(candidate) || candidate.split("\\.").length > 2 || candidate.split("\\.").length < 1
						|| JavaStopElements.isStopElement(candidate))
					continue;
				selectedMappings.put(candidate, dummy);
				pivots.add(candidate);
				sbuf.append(candidate + " ");
			}
		}
	}
	
	private void orderTermsForExpansion(LinkedHashSet<String> pivots, LinkedHashSet<String> allWords,
			HashMap<String, HashMap<String, Integer>> wordPairs) {
		allWords.removeAll(pivots);
		while(allWords.size() > 0) {
			double maximumScore = Double.NEGATIVE_INFINITY;
			String nextCandidate = "";
			for(String candidate : allWords) {
				HashMap<String, Integer> frequentTermsWithCandidate = wordPairs.get(candidate);
				if(frequentTermsWithCandidate == null) {
					/* Prepare for removal */
					nextCandidate = candidate;
					continue;
				}
				
				Double score = 0.0;
				for(String curPivot : pivots) {
					Integer frequency = frequentTermsWithCandidate.get(curPivot);
					if(frequency == null) /* Never go with pivot */
						frequency = 1;
					score += Math.log(frequency);
				}
				if(score > maximumScore) {
					maximumScore = score;
					nextCandidate = candidate;
				}
			}
			if(maximumScore > Math.log(3))
				pivots.add(nextCandidate);
			allWords.remove(nextCandidate);
		}
	}
}
