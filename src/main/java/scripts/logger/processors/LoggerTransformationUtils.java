package scripts.logger.processors;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import scripts.logger.model.ProcessingUnit;
import scripts.logger.processors.appender.AppenderProcessor;
import scripts.logger.processors.appender.impl.ConsoleAppenderProcessor;
import scripts.logger.processors.appender.impl.FileAppenderProcessor;

@Slf4j
public class LoggerTransformationUtils {

    private LoggerTransformationUtils(){}

    public static boolean isWrapperImportNeeded = false;
    public static AppenderProcessor appenderProcessor;
    private static final Pattern CONSOLE_LOGGING_STATEMENT = Pattern.compile("System\\.(\\w+)\\.print(ln)?\\((.+)\\);");
    private static final Pattern LOGGING_STATEMENT_PATTERN = Pattern.compile("FileLogger\\.log\\(\\s?(.+),\\s*FileLogger\\.(\\w+)\\s*(,.+)?\\);");

    public static String reformatLogger(List<String> lines, int index) {
        ProcessingUnit processingUnit = new ProcessingUnit(lines, index);
        prepareProcessingUnit(processingUnit);
        initializeAppenderProcessor(processingUnit);
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

    private static void initializeAppenderProcessor(ProcessingUnit processingUnit) {
        appenderProcessor = processingUnit.getInitialLine().contains("FileLogger")
            ? new FileAppenderProcessor(processingUnit)
            : new ConsoleAppenderProcessor(processingUnit);
    }

    public static boolean checkParenthesis(String str) {
        int counter = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == '(') {
                counter++;
            } else if (str.charAt(i) == ')') {
                counter--;
            }
            if (counter < 0) {
                return false;
            }
        }
        return counter == 0;
    }

    public static final String LOGGER_VAR_NAME = "log";

}
