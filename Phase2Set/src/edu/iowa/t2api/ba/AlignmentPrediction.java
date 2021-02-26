package edu.iowa.t2api.ba;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import config.Config;
import edu.berkeley.nlp.wa.basic.Pair;
import utils.FileUtils;

public class AlignmentPrediction {

	public static final String BA_OUTPUT_FILE = "/home/dharani/concordia/thesis/CodeToTask/berkeleyaligner/distribution/output/training.ts-cd.align";

	public static void main(String[] args) throws IOException {
		AlignmentPrediction predictor = new AlignmentPrediction();
		predictor.readTrainedAlignments();
		// predictor.predict();
	}

	public void readTrainedAlignments() throws IOException {
		// Initialize data structure for calculation
		Map<Pair<String, String>, Double> mappings = new HashMap<Pair<String, String>, Double>(); // all mappings and
																									// weights <term,
																									// element>
		Map<Pair<String, String>, Integer> mappingNumb = new HashMap<Pair<String, String>, Integer>(); // all mappings
																										// and weights
																										// <term,
																										// element>
		Map<String, Set<String>> mappingsByTerm = new HashMap<String, Set<String>>(); // get set of aligned code
																						// elements given terms
		Map<String, Integer> mappingsByCode = new HashMap<String, Integer>(); // count mappings that contains element
																				// <element>

		/// TODO: This is where you put your output file obtained from Berkeley Aligner
		// FileInputStream inStream = new FileInputStream(
		// Config.alignDirPath + "output_" + Config.fileExtension + "_soft/" +
		// "train.j-l." + Config.alignSuffix);

		FileInputStream inStream = new FileInputStream(BA_OUTPUT_FILE);

		// FileInputStream inStream = new FileInputStream(Config.alignDirPath +"output_"
		// + Config.fileExtension +
		// "_soft/" + "train.j-l." + Config.softAlignSuffix);
		InputStreamReader inStrReader = new InputStreamReader(inStream);
		BufferedReader bufReader = new BufferedReader(inStrReader);

		String curLine;
		int lineCount = 0;
		while ((curLine = bufReader.readLine()) != null) {
			/* For debugging */
			lineCount++;
			if (lineCount % 1000 == 0) {
				System.out.print(lineCount + " ");
				if (lineCount % 10000 == 0)
					System.out.println();
			}

			String[] alignments = curLine.trim().split("\\s");
			for (String alignment : alignments) {
				String[] split = alignment.split("-");
				/* If length = 4 and weight ~ 10E-x, throw it away */
				if (split.length == 2 /* || split.length == 4 */) {
					String englishTerm = split[0];
					String codeElement = split[1];
					double strength = 0;
					// strength = Double.parseDouble(split[2]);
					Double accumStrength = mappings.get(new Pair<String, String>(englishTerm, codeElement));
					Integer count = mappingNumb.get(new Pair<String, String>(englishTerm, codeElement));
					if (accumStrength == null) {
						accumStrength = 0.0;
						count = 0;
					}
					mappings.put(new Pair<String, String>(englishTerm, codeElement), accumStrength + strength);
					mappingNumb.put(new Pair<String, String>(englishTerm, codeElement), count + 1);

					/** store set of code elements for each text */
					Set<String> resCodeSet = mappingsByTerm.get(englishTerm); // respective set of code elements aligned
																				// with this term
					if (resCodeSet == null) {
						resCodeSet = new HashSet<String>();
						mappingsByTerm.put(englishTerm, resCodeSet);
					}
					resCodeSet.add(codeElement);

					/** count mappings for each code element */
					Integer mappingCountPerCode = mappingsByCode.get(codeElement);
					if (mappingCountPerCode == null)
						mappingCountPerCode = 0;
					mappingsByCode.put(codeElement, mappingCountPerCode + 1);
				}
			}
		}
		bufReader.close();

		new File(Config.mappingDirPath).mkdir();

		// FileUtils.writeObjectFile(mappings, Config.mappingDirPath +
		// "mappingsWithIBMWeights_" + Config.fileExtension + ".dat");
		FileUtils.writeObjectFile(mappingNumb,
				Config.mappingDirPath + "aligmentFrequency_" + Config.fileExtension + ".dat");
		FileUtils.writeObjectFile(mappingsByTerm,
				Config.mappingDirPath + "alignedCodeByTerm_" + Config.fileExtension + ".dat");
		FileUtils.writeObjectFile(mappingsByCode,
				Config.mappingDirPath + "mappingCountByCode_" + Config.fileExtension + ".dat");
	}

	@SuppressWarnings("unchecked")
	public void predict() throws IOException {
		/* Loading data */
		Map<Pair<String, String>, Double> mappingsWithIBMWeights = (HashMap<Pair<String, String>, Double>) FileUtils
				.readObjectFile(Config.mappingDirPath + "mappingsWithIBMWeights_" + Config.fileExtension + ".dat");
		Map<Pair<String, String>, Integer> mappingNumb = (Map<Pair<String, String>, Integer>) FileUtils
				.readObjectFile(Config.mappingDirPath + "aligmentFrequency_" + Config.fileExtension + ".dat");
		Map<String, Set<String>> mappingsByTerm = (Map<String, Set<String>>) FileUtils
				.readObjectFile(Config.mappingDirPath + "alignedCodeByTerm_" + Config.fileExtension + ".dat");
		Map<String, Integer> mappingsByCode = (HashMap<String, Integer>) FileUtils
				.readObjectFile(Config.mappingDirPath + "mappingCountByCode_" + Config.fileExtension + ".dat");

		ArrayList<String> testPairs = readTestSamples();

		/**
		 * Prepare for output. To write to file, the best solution is to create
		 * alignments and using Pharaoh output
		 */
		String unionName = "test_max_" + (Config.fileExtension) + ".align";
		final FileWriter unionPharaohOut = new FileWriter(Config.alignEvalDirPath + "posterior/" + unionName);

		for (String sentence : testPairs) {
			StringBuffer sbuf = new StringBuffer();
			String[] terms = sentence.split("\\s");

			/* Debugging */
			for (String term : terms) {
				Set<String> relElements = mappingsByTerm.get(term); // get relevant code elements
				/** There is no code element aligned with this <term> */
				if (relElements == null)
					continue;
				HashMap<String, Double> scores = new HashMap<String, Double>();
				for (String element : relElements) {
					/** Compute #mapping <term, element> */
					int totalCount = mappingNumb.get(new Pair<String, String>(term, element));
					/** Number of mappings containing c */
					int prob_element = mappingsByCode.get(element);
					/** Compute score for the possible alignment <term, code>: p(t,c)/p(c) */
					double score = totalCount / (double) prob_element;
					/*
					 * Empirically, trivial counting works very well. Skip the following line for
					 * testing
					 */
					score = totalCount;
					score = mappingsWithIBMWeights.get(new Pair<String, String>(term, element));
					scores.put(element, score);
				}
				/* Sorting and select K elements with highest ranking score */
				Map<String, Double> sortedList = sortByComparator(scores, false);
				int count = 0;
				StringBuilder printer = new StringBuilder();
				printer.append(term + ":");
				for (String candidate : sortedList.keySet()) {
					// if(JavaStopElements.isStopElement(candidate))
					// continue;
					if (count++ >= 20)
						break;
					sbuf.append(term + "-" + candidate + " ");
				}
			}
			unionPharaohOut.append(sbuf.toString() + System.lineSeparator());
		}
		unionPharaohOut.close();
	}

	public static ArrayList<String> readTestSamples() throws IOException {
		ArrayList<String> testPairs = new ArrayList<String>();

		FileInputStream inStream = new FileInputStream(Config.alignTestDirPath + "test_251." + Config.nlpPostfix);
		InputStreamReader inStrReader = new InputStreamReader(inStream);
		BufferedReader bufReader = new BufferedReader(inStrReader);

		String curLine;
		while ((curLine = bufReader.readLine()) != null) {
			testPairs.add(curLine);
		}

		bufReader.close();
		return testPairs;
	}

	public static Map<String, Double> sortByComparator(Map<String, Double> unsortMap, final boolean order) {

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
		Map<String, Double> sortedMap = new LinkedHashMap<String, Double>();
		for (Entry<String, Double> entry : list) {
			sortedMap.put(entry.getKey(), entry.getValue());
		}

		return sortedMap;
	}
}
