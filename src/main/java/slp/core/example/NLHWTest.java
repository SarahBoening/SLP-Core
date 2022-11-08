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
public class NLHWTest {
	public static final File COUNTERFILE = new File("G:\\MASTER\\MODELS\\SLP-Core\\model_ast_small.txt");
	public static final File VOCABFILE = new File("G:\\MASTER\\MODELS\\SLP-Core\\ast_small_vocab.txt");
	public static final File TRAIN = new File("G:\\MASTER\\scenario\\ast\\train\\");
	public static final File TEST = new File("G:\\MASTER\\raw_files\\AST\\small\\eval\\sicherungskopie\\");
	public static final File OUT = new File("G:\\MASTER\\MODELS\\SLP-Core\\");
	public static final boolean loadCounter = true;
	public static final boolean loadVocab = true;
	public static final boolean doTest = true;
	public static final boolean doTrain = false;
	public static final String modelName = "ast_global_2";
	public static final String language = "simple";
	public static final int maxSamples = 10000;
	public static final File expFile = new File("G:\\MASTER\\Evaluation\\"+"SLP_"+ modelName+"_hw.txt");
	
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
			e.printStackTrace();
		}
    	
    }
	public static void main(String[] args) {
		OperatingSystemMXBean bean = (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
		
		Lexer lexer = new PunctuationLexer();
		LexerRunner lexerRunner = new LexerRunner(lexer, true);
		lexerRunner.setSentenceMarkers(true);
		Vocabulary vocabulary = setupVocabulary();
		
		Model model = getModel();
		
		ModelRunner modelRunner = new ModelRunner(model, lexerRunner, vocabulary);
		

		FileExtractor javafileExtractor = new FileExtractor("raw");
		List<File> files = javafileExtractor.getFilesFromFolder(TEST.getAbsolutePath());
		System.out.println(files.size());
		List<String> examples = getExamples(files, lexer);
		System.out.println(examples.size());
		int len = examples.size();
		
		// 4. Running
		//    a. Model each file in 'test' recursively
		Random rand = new Random(66);
		long mem = 0;
		double cpu = 0.0f;
		for(String example : examples) {
			// Tokenize test string
			Stream<String> testtok = lexer.lexLine(example);
			Stream<Integer> voc = vocabulary.toIndices(testtok);
			List<Integer> vocL = voc.collect(toList());
			int index = rand.nextInt(vocL.size() -1);
			int ground_truth = vocL.get(index);
			vocL.set(index, vocabulary.toIndex("mask"));
			String gt = vocabulary.toWord(ground_truth);
			
			//Prediction
			Map<Integer, Pair<Double, Double>> preds = model.predictToken(vocL, index);
			cpu += bean.getProcessCpuLoad();
			mem += bean.getCommittedVirtualMemorySize();
			HashMap<Integer, Pair<Double, Double>> topk = sortByValue(preds);
			
		
		}
		writeToFile("CPU Percentage of JVM: " + (float) (cpu/len) + " %");
		writeToFile("Virtual Memory: " + (float)(mem/len) / (float)(1e6) + " MB");
	}
}
