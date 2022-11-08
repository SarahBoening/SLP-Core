package slp.core.example;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileExtractor {
	
	private static String targetFileEnding;
	
	public FileExtractor(String fileEnding) {
		targetFileEnding = fileEnding;
	}

	public List<File> getFilesFromFolder(String folderPath) {
        List<File> result = new ArrayList<>();
        walk(folderPath, result);
        return result;
    }
	
	private static void walk(String path, List<File> targetListReference) {
        File root = new File(path);
        File[] files = root.listFiles();
        if(files == null) return;
        for(File f : files) {
            if(f.isDirectory()) {
                walk(f.getAbsolutePath(), targetListReference);
            }
            else {
                if(f.getName().endsWith(targetFileEnding)) {
                    targetListReference.add(f);
                }
            }
        }
    }

}
