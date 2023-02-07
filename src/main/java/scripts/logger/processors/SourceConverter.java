package scripts.logger.processors;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import scripts.logger.model.MyList;
import scripts.logger.model.ProcessingUnit;
import scripts.logger.processors.appender.AppenderProcessor;
import scripts.logger.processors.appender.impl.ConsoleAppenderProcessor;
import scripts.logger.processors.appender.impl.FileAppenderProcessor;

public class SourceConverter {

    private final Path path;
    private MyList<String> linesOfCode;
    public static boolean isWrapperImportNeeded;

    private static final Pattern CONSOLE_LOGGER_EXISTS_PATTERN = Pattern.compile("System\\.(\\w+)\\.print(ln)?");
    private static final Pattern CLASS_DECLARATION = Pattern.compile("class\\s(\\w+)\\s?\\{?");
    private static final Pattern FILE_LOGGER_EXISTS_PATTERN = Pattern.compile("FileLogger\\.log");
    private static final Pattern STACK_TRACE_PATTERN = Pattern.compile("(ex?)\\.printStackTrace\\(\\);");
    private static final Pattern CONSOLE_LOGGING_STATEMENT = Pattern.compile("System\\.(\\w+)\\.print(ln)?\\((.+)\\);");
    private static final Pattern LOGGING_STATEMENT_PATTERN = Pattern.compile("FileLogger\\.log\\(\\s?(.+),\\s*FileLogger\\.(\\w+)\\s*(,.+)?\\);");

    public SourceConverter(Path path) {
        this.path = path;
        isWrapperImportNeeded = false;
    }

    public int process() {

        try {
            readFile();
            int initialModCount = linesOfCode.getModCount();
            if (legacyLoggerIncluded()) {
                updateFile();
            }
            int resultModCount = linesOfCode.getModCount();

            int difference = resultModCount - initialModCount;
            if (difference != 0) {
                rewriteJavaClass(String.join("\n", linesOfCode), path.toFile());
            }
            return difference;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private void readFile() throws IOException {
        linesOfCode = new MyList<>(Files.readAllLines(path));
    }

    private boolean legacyLoggerIncluded() {
        for (String line : linesOfCode) {
            if (isOutdatedLoggingStatement(line)) {
                return true;
            }
        }
        return false;
    }

    private void updateFile() {
        inlineLoggerStatements();
        updateLoggerStatements();
        addImports();
        addLoggerAnnotation();
        removeUnusedImports("import com.cellpointdigitail.basic.Debug;",
                            "import com.cellpointdigital.mesb.log.FileLogger;");
    }

    private void removeUnusedImports(String... imports) {
        int unusedImportsLeft = imports.length;
        ListIterator<String> listIterator = linesOfCode.listIterator();
        while (listIterator.hasNext() && unusedImportsLeft > 0) {
            String line = listIterator.next();
            boolean isLineForDeletion = Arrays.stream(imports).anyMatch(line::contains);
            if (isLineForDeletion) {
                listIterator.remove();
                unusedImportsLeft--;
            }
        }

    }

    private void addLoggerAnnotation() {
        ListIterator<String> listIterator = linesOfCode.listIterator();
        while (listIterator.hasNext()) {
            String line = listIterator.next();
            if (line.contains("@Slf4j")) {
                break;
            }
            Matcher matcher = CLASS_DECLARATION.matcher(line);
            if (matcher.find() && !isPartOfComment(line)) {
                listIterator.set("@Slf4j");
                listIterator.add(line);
                break;
            }
        }
    }

    private String getLoggerImports() {
        String defaultImports = "\nimport lombok.extern.slf4j.Slf4j;\n";
        if (isWrapperImportNeeded) {
            defaultImports += "import com.cellpointdigital.mesb.log.LoggerObjectWrapper;";
        }
        return defaultImports;
    }

    private void addImports() {
        ListIterator<String> listIterator = linesOfCode.listIterator();
        while (listIterator.hasNext()) {
            String line = listIterator.next();
            if (line.contains("package")) {
                listIterator.add(getLoggerImports());
                break;
            }
        }
    }

    private void inlineLoggerStatements() {
        ListIterator<String> listIterator = linesOfCode.listIterator();
        while (listIterator.hasNext()) {
            StringBuilder line = new StringBuilder(listIterator.next());
            if (isOutdatedLoggingStatement(line.toString()) && !line.toString().contains(");") && listIterator.hasNext()) {
                String followingLine = listIterator.next();
                while (!followingLine.contains(");")) {
                    line.append(" ").append(followingLine.trim());
                    listIterator.remove();
                    followingLine = listIterator.next();
                }
                line.append(" ").append(followingLine.trim());
                listIterator.remove();
                if (listIterator.hasPrevious()) {
                    listIterator.previous();
                    listIterator.remove();
                }
                listIterator.add(line.toString());
            }
        }
    }

    private boolean isOutdatedLoggingStatement(String line) {
        Matcher fileLoggerMatcher = FILE_LOGGER_EXISTS_PATTERN.matcher(line);
        Matcher consoleLoggerMatcher = CONSOLE_LOGGER_EXISTS_PATTERN.matcher(line);
        Matcher stackTraceMatcher = STACK_TRACE_PATTERN.matcher(line);
        return fileLoggerMatcher.find() || consoleLoggerMatcher.find() || stackTraceMatcher.find();
    }

    private void updateLoggerStatements() {
        isWrapperImportNeeded = false;
        ListIterator<String> listIterator = linesOfCode.listIterator();
        while (listIterator.hasNext()) {
            String line = listIterator.next();
            if (line.contains("Debug.printJDOM(buffer, System.err)")) {
                listIterator.set(
                    line.replaceAll(
                        "Debug.printJDOM\\(buffer, System.err\\)",
                        "log.error(\"{}\", LoggerObjectWrapper.wrap(buffer))"
                    )
                );
                continue;
            }
            Matcher stackTraceMatcher = STACK_TRACE_PATTERN.matcher(line);
            if (stackTraceMatcher.find()) {
                listIterator.set(stackTraceMatcher.replaceFirst(
                    "log.error(\"\", " + stackTraceMatcher.group(1) + ");")
                );
                continue;
            }
            int index = listIterator.nextIndex();
            if (isOutdatedLoggingStatement(line)) {
                listIterator.set(reformatLogger(index - 1));
            }
        }
    }

    private String reformatLogger(int index) {
        ProcessingUnit processingUnit = new ProcessingUnit(linesOfCode, index);
        prepareProcessingUnit(processingUnit);
        AppenderProcessor appenderProcessor = initializeAppenderProcessor(processingUnit);
        return appenderProcessor.getUpdatedLoggerStatement();
    }

    private static void prepareProcessingUnit(ProcessingUnit processingUnit) {
        Matcher fileLoggerMatcher = LOGGING_STATEMENT_PATTERN.matcher(processingUnit.getInitialLine());
        Matcher consoleLoggerMatcher = CONSOLE_LOGGING_STATEMENT.matcher(processingUnit.getInitialLine());
        if (fileLoggerMatcher.find()) {
            processingUnit.setMatcher(fileLoggerMatcher);
        } else {
            consoleLoggerMatcher.find();
            processingUnit.setMatcher(consoleLoggerMatcher);
        }
        processingUnit.setLoggingStatement(processingUnit.getMatcher().group());
        processingUnit.initializeSurroundings();
    }

    private static AppenderProcessor initializeAppenderProcessor(ProcessingUnit processingUnit) {
        return processingUnit.getInitialLine().contains("FileLogger")
            ? new FileAppenderProcessor(processingUnit)
            : new ConsoleAppenderProcessor(processingUnit);
    }

    private void rewriteJavaClass(String content, File file) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }
    }

    private boolean isPartOfComment(String line) {
        return Stream.of("//", "*", "/*").anyMatch(symbol -> line.trim().startsWith(symbol));
    }

}
