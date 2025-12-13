package word_statistics.core;


import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class FileSearch {
    public static List<Path> searchTextFiles(String directoryPath, boolean includeSubdirs) throws IOException {
        List<Path> txtFiles = new ArrayList<>();
        Path startDir = Paths.get(directoryPath);

        if (!Files.exists(startDir) || !Files.isDirectory(startDir)) {
            throw new IllegalArgumentException("Invalid directory selected!");
        }
        int depth = includeSubdirs ? Integer.MAX_VALUE : 1;

        try (var stream = Files.walk(startDir, depth)) {
            stream.filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".txt"))
                    .forEach(txtFiles::add);
        } catch (

        UncheckedIOException e) {
            // Skip AccessDenied Folders or files
        }

        return txtFiles;
    }

    // for testing
    // public static void main(String[] args) {
    // try {
    // List<Path> files = searchTextFiles("C:\\Users\\Racoon\\Desktop\\oz testing",
    // false);
    // System.out.println("Found text files: " + files.size());
    // for (Path p : files) {
    // System.out.println(p.toString());
    // }
    // } catch (Exception e) {
    // e.printStackTrace();
    // }
    // }
}
