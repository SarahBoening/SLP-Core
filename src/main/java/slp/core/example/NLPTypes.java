package slp.core.example;

import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

public class NLPTypes {
	public static final File COUNTERFILE = new File("G:\\MASTER\\MODELS\\SLP-Core\\model_ast_small.txt");
	public static final File VOCABFILE = new File("G:\\MASTER\\MODELS\\SLP-Core\\ast_small_vocab.txt");
	public static final File TRAIN = new File("G:\\MASTER\\scenario\\ast\\train\\");
	public static final File TEST = new File("G:\\MASTER\\Small_Corp\\small\\Pred_categories\\methodbody\\ast\\");
	public static final File GROUND_TRUTH = new File("G:\\MASTER\\Evaluation\\gt\\body_ast.txt");
	public static final File OUT = new File("G:\\MASTER\\Evaluation\\types\\");
	public static final String type = "methodbody";
	public static final boolean loadCounter = true;
	public static final boolean loadVocab = true;
	public static final boolean doTest = true;
	public static final boolean doTrain = false;
	public static final String modelName = "ast_global";
	public static final String language = "simple";
	public static final int maxSamples = 50;
	public static final File expFile = new File("G:\\MASTER\\Evaluation\\types\\"+"SLP_"+ modelName+ "_"+ type +".txt");
	
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
    
    public static List<List<String>> getExamples(List<File> files, Lexer lexer){
    	List<List<String>> result = new ArrayList<List<String>>();
    	boolean done = false;
    	String text = null;
    	for(File file : files) {
    		try {
    				String content = "";
    				content = FileUtils.readFileToString(file, "UTF-8");
    				content = content.split("\\[MASK\\]", 2)[0];
    				List<String> lex = lexer.lexLine(content).collect(toList());
    				if(type.equals("line") || type.equals("methodbody") || type.equals("parameterlist") ||  type.equals("righthandAssignment") ) {
    					lex.remove(lex.size() -1);
    					lex.remove(lex.size() -1);
    				}
    				result.add(lex);
    		} catch(IOException e) {
    			System.err.println("Error processing " + file.getName().split("\\.")[0]);
    		}
    	}
    	
    	return result;
    }
    
    public static List<String> read_gt(File file){
    	List<String> result = new ArrayList<String>();
    	String text = null;
    	try {
    		Scanner myReader = new Scanner(file);
			while (myReader.hasNextLine()) { 
		        text = myReader.nextLine();
		        text = text.trim();
		        result.add(text);		
			}
    	} catch(IOException e) {
			System.err.println("Error processing " + file.getName().split("\\.")[0]);
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
			e.printStackTrace();
		}
    	
    }
	public static void main(String[] args) {
		
		Lexer lexer = new PunctuationLexer();
		LexerRunner lexerRunner = new LexerRunner(lexer, true);
		lexerRunner.setSentenceMarkers(true);
		Vocabulary vocabulary = setupVocabulary();
		
		Model model = getModel();
		
		ModelRunner modelRunner = new ModelRunner(model, lexerRunner, vocabulary);
		

		FileExtractor javafileExtractor = new FileExtractor("json");
		List<File> files = javafileExtractor.getFilesFromFolder(TEST.getAbsolutePath());
		List<List<String>> examples = getExamples(files, lexer);
		System.out.println("length examples: " + examples.size());
		int len = examples.size();
		
		List<String> ground_truths = read_gt(GROUND_TRUTH);
		int max_toks = 0;
		System.out.println("GTs: " + ground_truths.size());
		
		// 4. Running
		//    a. Model each file in 'test' recursively
		Random rand = new Random(66);
		float acc = 0.0f;
		float acc5 = 0.0f;
		int i = 0;
		int mask_id = vocabulary.toIndex("mask");
		for(List<String> example : examples) {
			// Tokenize test string
			//Stream<String> testtok = lexer.lexLine(example);
			List<Integer> vocL = vocabulary.toIndices(example);
			// grountruth tokenize and to List
			Stream<String> gt = lexer.lexLine(ground_truths.get(i));
			List<String >gt_list =  gt.collect(toList());
			List<Integer> gt_ints_list = vocabulary.toIndices(gt_list);
			max_toks += gt_ints_list.size();
			List<String> prediction = new ArrayList<>();
			int j = 0;
			for(String g : gt_list) {
				//vocL.add(mask_id);
				int index = vocL.size();
				//Prediction
				Map<Integer, Pair<Double, Double>> preds = model.predictToken(vocL, index);
				HashMap<Integer, Pair<Double, Double>> topk = sortByValue(preds);
				
				int c = 0;
				
				List<Integer> top5 = new ArrayList<Integer>();
				for(Entry<Integer, Pair<Double, Double>> m : topk.entrySet()) {
					if(c<5)
						top5.add(m.getKey());
					if(c == 0 && m.getKey() == gt_ints_list.get(j))
						acc++;
					if(c < 5 && m.getKey() == gt_ints_list.get(j))
						acc5++;
					c++;
				}
				String pred1 = vocabulary.toWord(top5.get(0));
				prediction.add(pred1);
				vocL.add(top5.get(0));
				j++;
				
				
			}

			
			writeToFile(prediction.toString());
			
			i++;
		}
		writeToFile("--------");
		writeToFile("Top1 accuracy: " + (float) (acc/max_toks));
		writeToFile("Top5 accuracy: " + (float) (acc5/max_toks));
		writeToFile("No of predictions: " + max_toks);
	}
}

