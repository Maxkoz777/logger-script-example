package scripts.logger.model;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import scripts.logger.LoggerTransformationUtils;
import scripts.logger.PatternUtils;

@Slf4j
@AllArgsConstructor
public class ScriptProcessingUnit {

    private String directoryPath;

    public void process() {

        log.info("\n\nFiles that will be changed:\n");
        getAllFilesForDirectory(directoryPath).stream()
            .filter(filterClassesToRefactor)
            .peek(fileNameLogger)
            .map(ProcessingFile::new)
            .map(ProcessingFile::inlineLoggingStatements)
            .map(ProcessingFile::getFile)
            .forEach(this::reformatClassLogger);

    }

    private final Predicate<File> filterClassesToRefactor = file -> {
        try {
            String text = Files.readString(file.toPath());
            return PatternUtils.LOGGER_EXISTS_PATTERN.matcher(text).find();
        } catch (IOException e) {
            log.error("Unable to parse file {}", file.getName());
            return false;
        }
    };

    private final Consumer<File> fileNameLogger = file -> {
        try {
            log.info(file.getCanonicalPath());
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }
    };

    private void reformatClassLogger(File file) {
        LoggerTransformationUtils.isWrapperImportNeeded = false;
        String initialText;
        try {
            initialText = Files.readString(file.toPath());
        } catch (IOException e) {
            log.error(e.getMessage());
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

    private String createNewClassContent(String text, List<String> reformattedStrings) {
        StringBuilder updatedContent = new StringBuilder();
        boolean importsAdded = false;
        boolean loggerDeclarationAdded = false;
        String[] lines = text.split("\n");
        int index = 0;
        while (index < lines.length) {
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

    private void updateJavaClass(String content, File file) {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private List<File> getAllFilesForDirectory(String directoryPath) {
        try {
            return Files.walk(Paths.get(directoryPath))
                .filter(Files::isRegularFile)
                .map(Path::toFile)
                .toList();
        } catch (IOException e) {
            log.error("Unable to process all files inside {}", directoryPath, e);
            throw new RuntimeException(e);
        }
    }

    private boolean isPartOfComment(String line) {
        return Stream.of("//", "*", "/*").anyMatch(symbol -> line.trim().startsWith(symbol));
    }

}
