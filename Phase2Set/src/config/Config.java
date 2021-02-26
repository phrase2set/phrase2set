package config;

public class Config {

	public static final String mainPath =  "/home/d_palan/Downloads/data/";
	public static final String mainDataPath = mainPath+ "data/";
	public static final String threadInfoPath = mainDataPath + "ThreadInfo.dat";

	public static final String mainDatabasePath = mainPath + "others/";
	public static final String mappingFilePath = mainDatabasePath + "clt.table";
	public static final String docsFilePath = mainDatabasePath + "docs.table";
	public static final String modMappingFilePath = mainDatabasePath + "clt_mod.table";
	public static final String mappingLineFilePath = Config.mainDatabasePath + "MappingLines.dat";
	public static final String globalInfoFilePath = Config.mainDatabasePath + "GlobalInfo.dat";
	public static final String testSampleFilePath = Config.mainDatabasePath + "TestSet.dat";
	public static final String interestPostPath = Config.mainDatabasePath + "interestPost.dat";
	public static final String interestThreadPath = Config.mainDatabasePath + "interestThread.dat";
	
	public static final String soAnswerPost = Config.mainDatabasePath + "soAnswerPost.dat";
	public static final String soQuestionPost = Config.mainDatabasePath + "soQuestionPost.dat";
	
	public static final String previousRunIdx = Config.mainDataPath + "previousRunIdx.dat";

	public static final int topDiscardCombinedAPI = 1000;
	public static final int topCombinedAPI = 1000;
	public static final int minNoCodeElemsInSnippet = 5;
	public static final int maxNoCodeElemsInSnippet = 15;
	public static final int noRecordsCLTTable = 40427828;
	public static final int noInterestColumnsCLTTable = 7;
	public static final int numberThreads = 16;
	public static final int alchemyNoThreadsProcessed = 1;
	/* Size of training data set: 181326; 236919, 289571, 331969 */
	public static final int fileExtension = 236919;

	public static String alignDirPath = "/home/d_palan/Downloads/data/berkeleyaligner/";
	public static String alignTrainDirPath = alignDirPath + "train/";
	public static String alignPrepTrainDirPath = alignDirPath + "pre_train/";
	public static String alignTestDirPath = alignDirPath + "test/";
	public static String stackOverflowTest = alignDirPath + "so/";
	public static String alignOutputDirPath = alignDirPath + "output/";
	public static String alignEvalDirPath = alignDirPath + "eval/";
	public static String myConfFilePath = alignDirPath + "align.conf";
	public static String alchemyKwDirPath = alignDirPath + "alchemy/";
	public static String codeTermStatsPath = alignDirPath + "stats/";
	public static String synthesisDataPath = alignDirPath + "synthesis/";
	public static String mappingDirPath = alignDirPath + "mappings/";
	
	public static String nlpPostfix = "l";
	public static String javaPostfix = "j";
	public static String alignSuffix = "align";
	public static String softAlignSuffix = "alignsoft";
}
