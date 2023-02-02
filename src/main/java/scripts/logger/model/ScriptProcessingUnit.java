package scripts.logger.model;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import scripts.logger.LoggerTransformationUtils;
import scripts.logger.PatternUtils;

@Slf4j
public class ScriptProcessingUnit {

    private final String directoryPath;
    private List<String> linesOfCode = new LinkedList<>();

    public ScriptProcessingUnit(String directoryPath) {
        this.directoryPath = directoryPath;
    }

    public void process() {
        try (Stream<Path> filesStream = Files.walk(Paths.get(directoryPath))){
                filesStream.filter(Files::isRegularFile)
                .forEach(path -> {
                    readFile(path);
                    if (legacyLoggerIncluded()) {
                        updateFile(path);
                    }
                });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private void readFile(Path path) {
        try {
            linesOfCode = Files.readAllLines(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean legacyLoggerIncluded() {
        for (String line : linesOfCode) {
            if (isOutdatedLoggingStatement(line)) {
                return true;
            }
        }
        return false;
    }

    private void updateFile(Path path) {
        inlineLoggerStatements();
        updateLoggerStatements();
        addImports();
        addLoggerImplementation();
        removeUnusedImports();
        rewriteJavaClass(String.join("\n", linesOfCode), path.toFile());
    }

    private void removeUnusedImports() {
        ListIterator<String> listIterator = linesOfCode.listIterator();
        while (listIterator.hasNext()) {
            String line = listIterator.next();
            if (line.contains("import com.cellpointdigitail.basic.Debug;")) {
                listIterator.remove();
                break;
            }
        }

    }

    private void addLoggerImplementation() {
        ListIterator<String> listIterator = linesOfCode.listIterator();
        while (listIterator.hasNext()) {
            String line = listIterator.next();
            Matcher matcher = PatternUtils.CLASS_DECLARATION.matcher(line);
            if (matcher.find() && !isPartOfComment(line)) {
                String className = matcher.group(1);
                if (line.contains("{")) {
                    listIterator.add(LoggerTransformationUtils.getLoggerVarDeclaration(className));
                } else if (listIterator.hasNext()) {
                    String next = listIterator.next();
                    listIterator.add(LoggerTransformationUtils.getLoggerVarDeclaration(className));
                }
                break;
            }
        }
    }

    private void addImports() {
        ListIterator<String> listIterator = linesOfCode.listIterator();
        while (listIterator.hasNext()) {
            String line = listIterator.next();
            if (line.contains("package")) {
                listIterator.add(LoggerTransformationUtils.getLoggerImports());
                break;
            }
        }
    }

    private void inlineLoggerStatements() {
        ListIterator<String> listIterator = linesOfCode.listIterator();
        while (listIterator.hasNext()) {
            String line = listIterator.next().trim();
            if (isOutdatedLoggingStatement(line) && !line.contains(");") && listIterator.hasNext()) {
                String followingLine = listIterator.next();
                while (!followingLine.trim().contains(");")) {
                    line += " " + followingLine;
                    listIterator.remove();
                    followingLine = listIterator.next();
                }
                line += " " + followingLine;
                listIterator.remove();
                if (listIterator.hasPrevious()) {
                    listIterator.previous();
                    listIterator.remove();
                }
                listIterator.add(line);
            }
        }
    }

    private boolean isOutdatedLoggingStatement(String line) {
        Matcher fileLoggerMatcher = PatternUtils.LOGGING_STATEMENT_PATTERN.matcher(line);
        Matcher consoleLoggerMatcher = PatternUtils.CONSOLE_LOGGER_EXISTS_PATTERN.matcher(line);
        return fileLoggerMatcher.find() || consoleLoggerMatcher.find();
    }

    private void updateLoggerStatements() {
        LoggerTransformationUtils.isWrapperImportNeeded = false;
        ListIterator<String> listIterator = linesOfCode.listIterator();
        while (listIterator.hasNext()) {
            int index = listIterator.nextIndex();
            if (isOutdatedLoggingStatement(listIterator.next())) {
                listIterator.set(LoggerTransformationUtils.reformatLogger(linesOfCode, index));
            }
        }
    }

    private String createNewClassContent(ProcessingFile processingFile) {
        StringBuilder updatedContent = new StringBuilder();
        boolean importsAdded = false;
        boolean loggerDeclarationAdded = false;
        List<String> lines = processingFile.getInitialTextLines();
        int index = 0;
        while (index < lines.size()) {
            String line = lines.get(index);
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
                    if (lines.get(index + 1).contains("{") && !lines.get(index).contains("{")) {
                        index++;
                        updatedContent.append(lines.get(index)).append("\n");
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
                    processingFile.getUpdatedLoggerLines().remove(0)
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

    private void rewriteJavaClass(String content, File file) {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private boolean isPartOfComment(String line) {
        return Stream.of("//", "*", "/*").anyMatch(symbol -> line.trim().startsWith(symbol));
    }

}
