package slp.core.example;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.Map.Entry;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;

import slp.core.counting.Counter;
import slp.core.counting.giga.GigaCounter;
import slp.core.counting.io.CounterIO;
import slp.core.counting.trie.AbstractTrie;
import slp.core.lexing.Lexer;
import slp.core.lexing.runners.LexerRunner;
import slp.core.lexing.simple.PunctuationLexer;
import slp.core.modeling.Model;
import slp.core.modeling.ngram.ADMModel;
import slp.core.modeling.ngram.NGramModel;
import slp.core.modeling.runners.ModelRunner;
import slp.core.translating.Vocabulary;
import slp.core.translating.VocabularyRunner;
import slp.core.util.Pair;

import static java.util.stream.Collectors.toList;
import com.sun.management.OperatingSystemMXBean;

/**
 * This example shows a typical use-case for natural language of this tool with detailed comments.
 * We setup a {@link LexerRunner} with a {@link Lexer}, train a {@link Model} using a {@link ModelRunner}
 * and print the overall result. This is a good starting point for understanding the tool's API.
 * See also the {@link BasicJavaRunner} for an equivalent example with typical settings for source code modeling tasks.
 * 
 * @author Vincent Hellendoorn
 */
public class BasicNLRunner {
	public static final File COUNTERFILE = new File("G:\\MASTER\\MODELS\\SLP-Core\\model_ast_small.txt");
	public static final File VOCABFILE = new File("G:\\MASTER\\MODELS\\SLP-Core\\ast_small_vocab.txt");
	public static final File TRAIN = new File("G:\\MASTER\\scenario\\ast\\train\\");
	public static final File TEST = new File("G:\\MASTER\\raw_files\\AST\\small\\eval\\sicherungskopie\\temp\\");
	public static final File OUT = new File("G:\\MASTER\\MODELS\\SLP-Core\\");
	public static final boolean loadCounter = true;
	public static final boolean loadVocab = true;
	public static final boolean doTest = true;
	public static final boolean doTrain = false;
	public static final String modelName = "ast_global_2";
	public static final String language = "simple";
	public static final int maxSamples = 100000;
	public static final File expFile = new File("G:\\MASTER\\Evaluation\\"+"SLP_"+ modelName+".txt");
	
	private static NGramModel getNGramModel() {
		Counter counter;
		if(loadCounter)
			counter = getCounter();
		else
			counter = new GigaCounter();
		int order = 4;
		NGramModel model;
		model = new ADMModel(order, counter);
		// This will speed up and reduce memory of trie counting for the simplest classes of models
		AbstractTrie.COUNT_OF_COUNTS_CUTOFF = 1;
		return model;
	}

	private static Model wrapModel(Model m) {
		return m;
	}

	private static Model getModel() {
		return wrapModel(getNGramModel());
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
    		        text = text.replace("CLS", "");
    		        text = text.replace("[SEP]", "");
    		        text = text.trim();
    		        List<String> lex = lexer.lexLine(text).collect(toList());
    		        if(lex.size() >= 2)
	    				line.add(text);
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
    	/**
    	int j = 0;
    	for (String l : line) {
    		if(j >= maxSamples)
    			break;
    		if(l.startsWith("[") || l.endsWith("[SEP]") || l.startsWith("*") || l.startsWith("/") || l.startsWith("import") || l.startsWith("package") || l.startsWith("@") || l.contains("CLS") || l.contains("SEP")) {
    			;
    		}
    		else{
    			//List<String> lex = lexer.lexLine(l).collect(toList());
    			if(l.length() > 2) {
    				result.add(l);
    				j++;
    			}
    		}

    	}
    	  */
    	return line;
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
	public static void main(String[] args) {
		long t = System.currentTimeMillis();
		// 1. Lexing
		//   a. Set up lexer using a PunctuationLexer (splits on whitespace, and separates out punctuation).
		//	    - You can also use WhitespaceLexer (just splits on whitespace), for instance if you already lexed your text.
		//		- The second parameter informs it that we want to treat each line in the file in isolation.
		//		  This is often true for natural language tasks, but change it if applicable for you.
		Lexer lexer = new PunctuationLexer();
		LexerRunner lexerRunner = new LexerRunner(lexer, true);
		//   b. If your data does not contain sentence markers (for the start and end of each file), add these here;
		//		- The model will assume that these markers are present and always skip the first token when modeling
		lexerRunner.setSentenceMarkers(true);
		Vocabulary vocabulary = setupVocabulary();
		// 3. Model
		//	  a. We will use an n-gram model with Modified Absolute Discounting (works well for NLP)
		//		 - The n-gram order is set to 4, which works better for NLP than the code standard (6)
		Model model = getModel();
		//	  b. We create a ModelRunner with this model and ask it to learn the train directory
		//		 - This invokes Model.learn for each file, which is fine for n-gram models since these are count-based;
		//         other model implementations may prefer to train in their own way.
		ModelRunner modelRunner = new ModelRunner(model, lexerRunner, vocabulary);
		System.out.println("Model loaded in " + (System.currentTimeMillis() - t)/1000 + "s");

		FileExtractor javafileExtractor = new FileExtractor("raw");
		List<File> files = javafileExtractor.getFilesFromFolder(TEST.getAbsolutePath());
		System.out.println(files.size());
		List<String> examples = getExamples(files, lexer);
		System.out.println(examples.size());
		if(doTrain) {
			// 2. Vocabulary
			//    a. Ignore any words seen less than twice (i.e. one time) in training data, replacing these with "<unk>"
			//       (other values may be better, esp. for very larger corpora)
			VocabularyRunner.cutOff(2);
			//	  b. Build the vocabulary on the training data with convenience function provided by VocabularyRunner
			//       - You can use VocabularyRunner.write to write it for future use (VocabularyRunner.read to read it back in)
			vocabulary = VocabularyRunner.build(lexerRunner, TRAIN);
			//    c. Close the resulting vocabulary (i.e. treat new words as "<unk>") now that it is complete.
			//		 - Note: this is typical for natural language, but less applicable to source code.
			vocabulary.close();
			modelRunner = new ModelRunner(model, lexerRunner, vocabulary);
			modelRunner.learnDirectory(TRAIN);
			System.out.println("Model counted in " + (System.currentTimeMillis() - t)/1000 + "s");
			Counter counter = ((NGramModel) model).getCounter();
			// Force GigaCounter.resolve() (if applicable), just for accurate timings below
			counter.getCount();
			System.out.println("Writing counter to file");
			File outFile = new File(OUT, "model_" + modelName + ".txt");
			CounterIO.writeCounter(counter, outFile);

			System.out.println("Writing vocabulary to file");
			File vocabFile = new File(OUT, modelName + "_vocab.txt");
			VocabularyRunner.write(vocabulary, vocabFile);

			//    d. We assume you are self-testing if the train and test directory are equal.
			//		 This will make it temporarily forget any sequence to model, effectively letting you train and test on all your data
			modelRunner.setSelfTesting(TRAIN.equals(TEST));
			System.out.println("Model saved in " + (System.currentTimeMillis() - t)/1000 + "s");
		}



		
		
		// 4. Running
		//    a. Model each file in 'test' recursively
		/**
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
			vocL.set(index, vocabulary.toIndex("mask"));
			String gt = vocabulary.toWord(ground_truth);
			
			//Prediction
			Map<Integer, Pair<Double, Double>> preds = model.predictToken(vocL, index);
			HashMap<Integer, Pair<Double, Double>> topk = sortByValue(preds);
			
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
			writeToFile(example+"\t ground truth: " + gt + "\t Top5: " +  predTop5.toString());
			//System.out.println(predTop5);
		
		}
		writeToFile("Top1 accuracy: " + (float) (acc/len));
		writeToFile("Top5 accuracy: " + (float) (acc5/len));
		writeToFile("average prediction time: " + (float)(pred_time/len)/1000.0f);
		writeToFile("MRR: " + (float) (mrr/len));
		// Print best prediction
		//System.out.println(predWords);
	}
}
