package scripts.logger;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggerProcessorScript {

    private static Logger logger = LoggerFactory.getLogger(LoggerProcessorScript.class);
    private static final String TEST_DIRECTORY_PATH = "/Users/user/Documents/Projects/files";
    private static final String DIRECTORY_PATH = "/Users/user/Downloads/mesb/server/src/main/java/com/cellpointdigital";

    public static void main(String[] args) {

        logger.info("\n\nFiles that will be changed:\n");
        getAllFilesForDirectory(DIRECTORY_PATH).stream()
            .filter(filterClassesToRefactor)
            .peek(fileNameLogger)
            .map(LoggerProcessorScript::removeLoggingLineSeparator)
            .forEach(LoggerProcessorScript::reformatClassLogger);
    }

    private static File removeLoggingLineSeparator(File file) {
        List<String> lines = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            int index = 0;
            while (index < lines.size()) {
                StringBuilder line = new StringBuilder(lines.get(index));
                if (line.toString().contains("FileLogger.log")) {
                    while (!lines.get(index).trim().contains(");")) {
                        line.append(" ")
                            .append(lines.get(++index).trim()
                        );
                    }
                }
                bw.write(line.toString());
                bw.newLine();
                index++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;

    }

    private static final Predicate<File> filterClassesToRefactor = file -> {
        try {
            String text = Files.readString(file.toPath());
            return PatternUtils.LOGGER_EXISTS_PATTERN.matcher(text).find();
        } catch (IOException e) {
            logger.error("Unable to parse file {}", file.getName());
            return false;
        }
    };

    private static final Consumer<File> fileNameLogger = file -> {
        try {
            logger.info(file.getCanonicalPath());
        } catch (IOException e) {
            logger.error(e.getMessage());
            throw new RuntimeException(e);
        }
    };

    private static void reformatClassLogger(File file) {
        String initialText;
        try {
            initialText = Files.readString(file.toPath());
        } catch (IOException e) {
            logger.error(e.getMessage());
            throw new RuntimeException(e);
        }
        List<String> reformattedStrings = new LinkedList<>();
        Matcher matcher = PatternUtils.LOGGING_STATEMENT_PATTERN.matcher(initialText);
        while (matcher.find()) {
            reformattedStrings.add(
                LoggerTransformationUtils.reformatLogger(matcher)
            );
        }
        String newContent = createNewClassContent(initialText.trim(), reformattedStrings);
        updateJavaClass(newContent, file);
    }

    private static String createNewClassContent(String text, List<String> reformattedStrings) {
        StringBuilder updatedContent = new StringBuilder();
        boolean importsAdded = false;
        boolean loggerDeclarationAdded = false;
        String[] lines = text.split("\n");
        int index = 0;
        while(index < lines.length) {
            String line = lines[index];
            if (!importsAdded) {
                if (!line.contains("package")) {
                    updatedContent.append(line).append("\n");
                    index++;
                    continue;
                }
                updatedContent.append(line).append("\n\n");
                updatedContent.append(LoggerTransformationUtils.getLoggerImports());
                importsAdded = true;
                index++;
                continue;
            }
            if (!loggerDeclarationAdded) {
                Matcher matcher = PatternUtils.CLASS_DECLARATION.matcher(line);
                if (matcher.find() && !isPartOfComment(line)) {
                    updatedContent.append(line).append("\n");
                    if (lines[index + 1].contains("{") && !lines[index].contains("{")) {
                        index++;
                        updatedContent.append(lines[index]).append("\n");
                    }
                    updatedContent.append(LoggerTransformationUtils.getLoggerVarDeclaration(matcher.group(1)));
                    loggerDeclarationAdded = true;
                    index++;
                    continue;
                }
            }
            Matcher matcher = PatternUtils.LOGGING_STATEMENT_PATTERN.matcher(line);
            if (matcher.find()) {
                int loggingIndexStart = line.indexOf("FileLogger.log");
                if (loggingIndexStart != 0) {
                    updatedContent.append(line, 0, loggingIndexStart);
                }
                updatedContent.append(
                    reformattedStrings.remove(0)
                );
                if (matcher.end() != line.length()) {
                    updatedContent.append(line.substring(matcher.end())).append("\n");
                }
            } else {
                updatedContent.append(line).append("\n");
            }
            index++;
        }
        return updatedContent.toString();
    }

    private static void updateJavaClass(String content, File file) {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }


    private static List<File> getAllFilesForDirectory(String directoryPath) {
        List<File> files = new ArrayList<>();
        File directory = new File(directoryPath);
        if (directory.exists() && directory.isDirectory()) {
            getAllFiles(directory, files);
        }
        return files;
    }

    private static void getAllFiles(File dir, List<File> files) {
        for (File file : dir.listFiles()) {
            if (file.isFile()) {
                files.add(file);
            } else if (file.isDirectory()) {
                getAllFiles(file, files);
            }
        }
    }

    private static boolean isPartOfComment(String line) {
        return Stream.of("//", "*", "/*").anyMatch(symbol -> line.trim().startsWith(symbol));
    }

}
