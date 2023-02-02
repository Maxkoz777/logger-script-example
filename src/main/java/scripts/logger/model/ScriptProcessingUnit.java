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
import java.util.regex.Pattern;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import scripts.logger.LoggerTransformationUtils;

@Slf4j
public class ScriptProcessingUnit {

    private final String directoryPath;
    private List<String> linesOfCode = new LinkedList<>();

    private static final Pattern CONSOLE_LOGGER_EXISTS_PATTERN = Pattern.compile("System\\.(\\w+)\\.print(ln)?");
    private static final Pattern CLASS_DECLARATION = Pattern.compile("class\\s(\\w+)\\s?\\{?");
    public static final Pattern LOGGING_STATEMENT_PATTERN = Pattern.compile("FileLogger\\.log\\(\\s?(.+),\\s*FileLogger\\.(\\w+)\\s*(,.+)?\\);");

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
            Matcher matcher = CLASS_DECLARATION.matcher(line);
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
        Matcher fileLoggerMatcher = LOGGING_STATEMENT_PATTERN.matcher(line);
        Matcher consoleLoggerMatcher = CONSOLE_LOGGER_EXISTS_PATTERN.matcher(line);
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
