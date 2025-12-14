package word_statistics.model;

import java.io.File;

public class FileModel {

    private final String fileName;
    private final String filePath;
    private String longestWord;
    private String shortestWord;
    private FileStatus status;
    private int wordCount;
    private int isCount;
    private int areCount;
    private int youCount;

    public FileModel(String fileName, String filePath) {
        this(fileName, filePath, true);
    }

    public FileModel(String fileName, String filePath, boolean validate) {
        if (fileName == null || filePath == null) {
            throw new IllegalArgumentException(
                    "File name or path cannot be null");
        }

        if (validate) {
            File fileObj = new File(filePath);
            if (!fileObj.exists()) {
                throw new IllegalArgumentException("File path does not exist");
            }
            if (!fileObj.isFile()) {
                throw new IllegalArgumentException("Path is not a file");
            }
            if (!fileObj.canRead()) {
                throw new IllegalArgumentException("File is not readable");
            }
        }

        this.fileName = fileName;
        this.filePath = filePath;
        this.longestWord = "";
        this.shortestWord = "";
        this.wordCount = 0;
        this.isCount = 0;
        this.areCount = 0;
        this.youCount = 0;
        this.status = FileStatus.PENDING;
    }

    public FileStatus getStatus() {
        return status;
    }

    public void setStatus(FileStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
        if (status != this.status) {
            this.status = status;
        }
    }

    public String getFileName() {
        return fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getLongestWord() {
        return longestWord;
    }

    public void setLongestWord(String longestWord) {
        if (this.longestWord.equals("-") || this.longestWord.isEmpty() ||
                longestWord.length() >= this.longestWord.length()) {
            this.longestWord = longestWord;
        }
    }

    public String getShortestWord() {
        return shortestWord;
    }

    public void setShortestWord(String shortestWord) {
        if (this.shortestWord.isEmpty() ||
                shortestWord.length() <= this.shortestWord.length()) {
            this.shortestWord = shortestWord;
        }
    }

    public int getWordCount() {
        return wordCount;
    }

    public void setWordCount(int wordCount) {
        this.wordCount = wordCount;
    }

    public void incrementWordCount() {
        this.wordCount += 1;
    }

    public int getIsCount() {
        return isCount;
    }

    public void setIsCount(int isCount) {
        this.isCount = isCount;
    }

    public void incrementIsCount() {
        this.isCount += 1;
    }

    public int getAreCount() {
        return areCount;
    }

    public void setAreCount(int areCount) {
        this.areCount = areCount;
    }

    public void incrementAreCount() {
        this.areCount += 1;
    }

    public int getYouCount() {
        return youCount;
    }

    public void setYouCount(int youCount) {
        this.youCount = youCount;
    }

    public void incrementYouCount() {
        this.youCount += 1;
    }
}
