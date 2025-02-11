package slp.core.example;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Scanner;
import java.util.stream.Stream;

import slp.core.counting.Counter;
import slp.core.counting.giga.GigaCounter;
import slp.core.counting.io.CounterIO;
import slp.core.counting.trie.AbstractTrie;
import slp.core.lexing.Lexer;
import slp.core.lexing.code.JavaLexer;
import slp.core.lexing.runners.LexerRunner;
import slp.core.modeling.Model;
import slp.core.modeling.dynamic.CacheModel;
import slp.core.modeling.dynamic.NestedModel;
import slp.core.modeling.mix.MixModel;
import slp.core.modeling.ngram.JMModel;
import slp.core.modeling.ngram.NGramModel;
import slp.core.modeling.runners.ModelRunner;
import slp.core.translating.Vocabulary;
import slp.core.translating.VocabularyRunner;
import slp.core.util.Pair;

import static java.util.stream.Collectors.toList;

import org.apache.commons.io.FileUtils;
import com.sun.management.OperatingSystemMXBean;

/**
 * This example shows a typical use-case for (Java) source code of this tool with detailed comments.
 * We setup a {@link LexerRunner} with a {@link Lexer}, train a {@link Model} using a {@link ModelRunner}
 * and print the overall result. This is a good starting point for understanding the tool's API.
 * See also the {@link BasicNLRunner} for an equivalent example with typical settings for natural language modeling tasks.
 * <br /><br />
 * More complex use-cases can be found in the other examples, such finding entropy for each token and line,
 * using a bi-directional model (in parallel with others), .
 * 
 * @author Vincent Hellendoorn
 */
public class BasicJavaRunner {
	public static final File COUNTERFILE = new File("G:\\MASTER\\MODELS\\SLP-Core\\model_java_small.txt");
	public static final File VOCABFILE = new File("G:\\MASTER\\MODELS\\SLP-Core\\java_small_vocab.txt");
	public static final File TRAIN = new File("G:\\MASTER\\scenario\\rub_rotterdam\\");
	public static final File TEST = new File("G:\\MASTER\\raw_files\\Java\\small\\eval\\temp\\");
	public static final File OUT = new File("G:\\MASTER\\MODELS\\SLP-Core\\");
	public static final boolean loadCounter = true;
	public static final boolean loadVocab = true;
	public static final boolean doTest = true;
	public static final boolean doTrain = false;
	public static final boolean doFinetune = false;
	public static final String modelName = "java_glob_2";
	public static final String language = "java";
	public static final int maxSamples = 10;
	public static final File expFile = new File("G:\\MASTER\\Evaluation\\"+"SLP_"+ modelName+".txt");

	public <K, V extends Comparable<V>> V maxUsingIteration(Map<K, V> map) {
		Map.Entry<K, V> maxEntry = null;
		for (Map.Entry<K, V> entry : map.entrySet()) {
			if (maxEntry == null || entry.getValue()
					.compareTo(maxEntry.getValue()) > 0) {
				maxEntry = entry;
			}
		}
		return maxEntry.getValue();
	}

	private static NGramModel getNGramModel() {
		Counter counter;
		if(loadCounter)
			counter = getCounter();
		else
			counter = new GigaCounter();
		int order = 6;
		NGramModel model;
		model = new JMModel(order, counter);
		// This will speed up and reduce memory of trie counting for the simplest classes of models
		AbstractTrie.COUNT_OF_COUNTS_CUTOFF = 1;
		return model;
	}

	private static Model wrapModel(Model m,LexerRunner lexerRunner, Vocabulary vocabulary) {
		if(doTest){
			m = new NestedModel(m, lexerRunner, vocabulary, TEST);
			m = MixModel.standard(m, new CacheModel());
			m.setDynamic(true);
		}
		return m;
	}

	private static Model getModel(LexerRunner lexerRunner, Vocabulary vocabulary) {
		return wrapModel(getNGramModel(), lexerRunner, vocabulary);
	}

	private static Counter getCounter() {
		long t = System.currentTimeMillis();
		System.out.println("Retrieving counter from file");
		Counter counter = CounterIO.readCounter(COUNTERFILE);
		System.out.println("Counter retrieved in " + (System.currentTimeMillis() - t)/1000 + "s");
		return counter;
	}

	private static Vocabulary setupVocabulary() {
		Vocabulary vocabulary;
		if (loadVocab)
			vocabulary = VocabularyRunner.read(VOCABFILE);
		else
			vocabulary = new Vocabulary();
		return vocabulary;
	}

	// function to sort hashmap by values 
    public static HashMap<Integer, Pair<Double, Double>> sortByValue(Map<Integer, Pair<Double, Double>> hm) 
    { 
        // Create a list from elements of HashMap 
        List<Map.Entry<Integer, Pair<Double, Double>> > list = 
               new LinkedList<Map.Entry<Integer, Pair<Double, Double>> >(hm.entrySet()); 
  
        // Sort the list 
        Collections.sort(list, new Comparator<Map.Entry<Integer, Pair<Double, Double>> >() { 
            public int compare(Map.Entry<Integer, Pair<Double, Double>> o1,  
                               Map.Entry<Integer, Pair<Double, Double>> o2) 
            { 
            	// bestP.left.compareTo(bestEntry.getValue().left)
                return (o2.getValue().left).compareTo(o1.getValue().left); 
            } 
        }); 
          
        // put data from sorted list to hashmap  
        HashMap<Integer, Pair<Double, Double>> temp = new LinkedHashMap<Integer, Pair<Double, Double>>(); 
        for (Map.Entry<Integer, Pair<Double, Double>> aa : list) { 
            temp.put(aa.getKey(), aa.getValue()); 
        } 
        return temp; 
    } 
    
    
    public static List<String> getExamples(List<File> files, Lexer lexer){
    	List<String> result = new ArrayList<String>();
    	List<String> line = new ArrayList<String>();
    	boolean done = false;
    	String text = null;
    	for(File file : files) {
    		if(done)
    			break;
    		try {
    			System.out.println("Loading file: " + file.getName().split("\\.")[0]);
    			Scanner myReader = new Scanner(file);
    			while (myReader.hasNextLine()) { 
    		        text = myReader.nextLine();
    		        text = text.replace("\t", "");
    		        //text = text.replace("[CLS]", "");
    		        //text = text.replace("[SEP]", "");
    		        text = text.trim();
    		        if(text.startsWith("[") || text.endsWith("[SEP]") || text.startsWith("*") || text.startsWith("/") || text.startsWith("import") || text.startsWith("package") || text.startsWith("@") || text.contains("CLS") || text.contains("SEP")) {
    	    			;
    	    		}
    	    		else{
    		        List<String> lex = lexer.lexLine(text).collect(toList());
    		        if(lex.size() >= 2)
	    				line.add(text);
    	    		}
    		        if(line.size() >= maxSamples) {
	    				done = true;
	    				break;
	    			}
    			}
    			myReader.close(); 		     
	    		System.out.println("done");
    		} catch(IOException e) {
    			System.err.println("Error processing " + file.getName().split("\\.")[0]);
    		}
    	}
    	
    	Collections.shuffle(line, new Random(66));
    	int j = 0;
    	for (String l : line) {
    		if(j >= maxSamples)
    			break;
    		if(l.startsWith("[") || l.endsWith("[SEP]") || l.startsWith("*") || l.startsWith("/") || l.startsWith("import") || l.startsWith("package") || l.startsWith("@") || l.contains("CLS") || l.contains("SEP")) {
    			;
    		}
    		else{
    			List<String> lex = lexer.lexLine(l).collect(toList());
    			if(lex.size() > 2) {
    				result.add(l);
    				j++;
    			}
    		}
    	}
    	return result;
    }
    
    
    public static void writeToFile(String text) {
    	try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(expFile, true));
			bw.write(text);
			bw.newLine();
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    }
    
	@SuppressWarnings("restriction")
	public static void main(String[] args) {
		OperatingSystemMXBean bean = (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
		long t = System.currentTimeMillis();
		// 1. Lexing
		//   a. Set up lexer using a JavaLexer
		//		- The second parameter informs it that we want to files as a block, not line by line
		Lexer lexer = new JavaLexer();
		//true for text if 1 line equals 1 sentence
		LexerRunner lexerRunner = new LexerRunner(lexer, false);
		//   b. Since our data does not contain sentence markers (for the start and end of each file), add these here
		//		- The model will assume that these markers are present and always skip the first token when modeling
		lexerRunner.setSentenceMarkers(true);
		//   c. Only lex (and model) files that end with "java". See also 'setRegex'
		// for text = simple (?)
		lexerRunner.setExtension(language);
		// 2. Vocabulary:
		//    - For code, we typically make an empty vocabulary and let it be built while training.
		//    - Building it first using the default settings (no cut-off, don't close after building)
		//		should yield the same result, and may be useful if you want to write the vocabulary before training.
		//    - If interested, use: VocabularyRunner.build(lexerRunner, train);
		Vocabulary vocabulary = setupVocabulary();

		// 3. Model
		//	  a. We will use an n-gram model with simple Jelinek-Mercer smoothing (works well for code)
		//		 - The n-gram order of 6 is used, which is also the standard
		//       - Let's use a GigaCounter (useful for large corpora) here as well; the nested model later on will copy this behavior.
		Model model = getModel(lexerRunner, vocabulary);
		ModelRunner modelRunner = new ModelRunner(model, lexerRunner, vocabulary);
		
		FileExtractor javafileExtractor = new FileExtractor("raw");
		List<File> files = javafileExtractor.getFilesFromFolder(TEST.getAbsolutePath());
		System.out.println(files.size());
		List<String> examples = getExamples(files, lexer);
		System.out.println(examples.size());
		if(doTrain) {

			// TRAIN PROCEDURE
			modelRunner.learnDirectory(TRAIN);
			//    d. We assume you are self-testing if the train and test directory are equal.
			//		 This will make it temporarily forget any sequence to model, effectively letting you train and test on all your data
			System.out.println("Model counted in " + (System.currentTimeMillis() - t)/1000 + "s");
			Counter counter = ((NGramModel) model).getCounter();
			// Force GigaCounter.resolve() (if applicable), just for accurate timings below
			counter.getCount();
			System.out.println("Writing counter to file");
			File outFile = new File(OUT, "model_" + modelName + ".txt");
			CounterIO.writeCounter(counter, outFile);

			// store vocabulary to vocabFile
			System.out.println("Writing vocabulary to file");
			File vocabFile = new File(OUT, modelName + "_vocab.txt");
			VocabularyRunner.write(vocabulary, vocabFile);

			//		 - If you plan on using a NestedModel to self-test, you can also just remove the two above calls;
			//		   they teach the global model the same things that the nested model knows, which barely boosts performance.
			System.out.println("Model saved in " + (System.currentTimeMillis() - t)/1000 + "s");

		}
		
		// 4. Running
		/**
		modelRunner.setSelfTesting(TRAIN.equals(TEST));
		//    a. Finally, we model each file in 'test' recursively
		Stream<Pair<File, List<List<Double>>>> modeledFiles = modelRunner.modelDirectory(TEST);
		//	  b. Retrieve overall entropy statistics using ModelRunner's convenience method
		DoubleSummaryStatistics statistics = modelRunner.getStats(modeledFiles);
		Double entropy = statistics.getAverage();
		System.out.printf("Modeled %d tokens, average entropy:\t%.4f\n", statistics.getCount(), entropy);
		Double perplexity = Math.pow(2.0d, entropy);
		System.out.println("Perplexity: "+ perplexity.toString());
		*/
		Random rand = new Random(66);
		float mrr = 0.0f;
		float acc = 0.0f;
		float acc5 = 0.0f;
		int len = examples.size();
		long pred_time = 0L;
		for(String example : examples) {
			//System.out.println(example);
			// Test string for prediction, predict at last position (= mask)
			//String test_str = "public static void main(String[] args";
			// Tokenize test string
			long p1 = 	System.currentTimeMillis();
			Stream<String> testtok = lexer.lexLine(example);
			Stream<Integer> voc = vocabulary.toIndices(testtok);
			List<Integer> vocL = voc.collect(toList());
			int index = rand.nextInt(vocL.size() -1);
			int ground_truth = vocL.get(index);
			//vocL.set(index, vocabulary.toIndex("mask"));
			String gt = vocabulary.toWord(ground_truth);
			
			//Prediction
			Map<Integer, Pair<Double, Double>> preds = model.predictToken(vocL, index);
			HashMap<Integer, Pair<Double, Double>> topk = sortByValue(preds);
			System.out.println(bean.getProcessCpuLoad());
			System.out.println(bean.getCommittedVirtualMemorySize());
			//List<Integer> tt = new ArrayList<Integer>();
			//int j = 0;
			/*
			int best = -1;
			// Loop over all predictions to find best prediction (highest probability)
			// Predcition is map with Integer (vocabulary ID) and corresponding Pair (probability, confidence) (left = probability )
			Map.Entry<Integer, Pair<Double, Double>> bestEntry = null;
			for(Map.Entry<Integer, Pair<Double, Double>> entr : preds.entrySet()){
				Pair<Double, Double> bestP = entr.getValue();
				if(bestEntry == null || bestP.left.compareTo(bestEntry.getValue().left) > 0){
					bestEntry = entr;
				}
			}
			*/
			int c = 0;
			
			List<Integer> top5 = new ArrayList<Integer>();
			for(Entry<Integer, Pair<Double, Double>> m : topk.entrySet()) {
				if(c<5)
					top5.add(m.getKey());
				if(m.getKey() == ground_truth)
					mrr += ModelRunner.toMRR(c);
				if(c == 0 && m.getKey() == ground_truth)
					acc++;
				if(c < 4 && m.getKey() == ground_truth)
					acc5++;
				c++;
			}
			//List<Integer> tests = new ArrayList(preds.keySet());
			// save best ID
			//tt.add(bestEntry.getKey());
			//List<String> predWords = vocabulary.toWords(tests);
			
			// Map ID -> word
			//List<String> predWords = vocabulary.toWords(tt);
			List<String> predTop5 = vocabulary.toWords(top5);
			long p2 = System.currentTimeMillis();
			pred_time += p2-p1;
			//writeToFile(example+"\t ground truth: " + gt + "\t Top5: " +  predTop5.toString());
			//System.out.println(predTop5);
		
		}
		//writeToFile("Top1 accuracy: " + (float) (acc/len));
		//writeToFile("Top5 accuracy: " + (float) (acc5/len));
		//writeToFile("average prediction time: " + (float)(pred_time/len)/1000.0f);
		//writeToFile("MRR: " + (float) (mrr/len));
		// Print best prediction
		//System.out.println(predWords);
		
		
	}
}
