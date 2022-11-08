package slp.core.example;

import static java.util.stream.Collectors.toList;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Stream;

import slp.core.lexing.Lexer;
import slp.core.lexing.runners.LexerRunner;
import slp.core.lexing.simple.PunctuationLexer;
import slp.core.translating.Vocabulary;
import slp.core.translating.VocabularyRunner;

public class GetGTJava {
	public static final File VOCABFILE = new File("G:\\MASTER\\MODELS\\SLP-Core\\ast_small_vocab.txt");
	public static final String type = "variables";
	public static final File expFile = new File("G:\\MASTER\\Evaluation\\gt\\"+ type + "_ast_SLP.txt");
	public static final File GROUND_TRUTH = new File("G:\\MASTER\\Evaluation\\gt\\variables.txt");
	public static final boolean loadVocab = true;
	
	private static Vocabulary setupVocabulary() {
		Vocabulary vocabulary;
		if (loadVocab)
			vocabulary = VocabularyRunner.read(VOCABFILE);
		else
			vocabulary = new Vocabulary();
		return vocabulary;
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
			List<String> ground_truths = read_gt(GROUND_TRUTH);
			for(String gt : ground_truths) {
				List<String> toked_gt = lexer.lexLine(gt).collect(toList());
				String res = "";
				for(String l : toked_gt)
					res += " " + l;
				writeToFile(res);
				
			}
		}	
}
