package scripts.logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import lombok.extern.slf4j.Slf4j;
import scripts.logger.model.ProcessingFile;
import scripts.logger.processors.OptionalArgsProcessor;
import scripts.logger.processors.impl.MultipleArgsProcessor;
import scripts.logger.processors.impl.ObjectClassProcessor;
import scripts.logger.processors.impl.ThrowableArgsProcessor;

@Slf4j
public class LoggerTransformationUtils {

    public static boolean isWrapperImportNeeded = false;
    public static OptionalArgsProcessor optionalArgsProcessor = new ThrowableArgsProcessor();
    private static ProcessingFile currentProcessingFile = null;

    public static void reformatLogger(Matcher matcher, ProcessingFile processingFile) {
        currentProcessingFile = processingFile;
        String loggerLevel = getLoggingLevel(
            OldLoggingLevels.valueOf(matcher.group(2))
        );
        String loggerMessage = matcher.group(1)
            .replaceAll(PatternUtils.LOGGING_DEBUG_MARK_PATTERN.pattern(), "");
        String additionalObject = matcher.group(3);
        String message = getLoggerMessage(loggerMessage, Optional.ofNullable(additionalObject));
        processingFile.getUpdatedLoggerLines().add(constructFinalLoggerVersion(loggerLevel, message));
    }

    public static String getLoggerImports() {
        String defaultImports = "import org.slf4j.Logger;\n"
            + "import org.slf4j.LoggerFactory;\n";
        if (isWrapperImportNeeded) {
            defaultImports += "import com.cellpointdigital.mesb.log.LoggerObjectWrapper;\n";
        }
        return defaultImports;
    }

    public static String getLoggerVarDeclaration(String className) {
        return "private static final Logger " + LOGGER_VAR_NAME + " = LoggerFactory.getLogger(" + className + ".class);\n";
    }

    public static final String LOGGER_VAR_NAME = "loggerLongUniqueName";

    private static String getLoggingLevel(OldLoggingLevels oldLevel) {
        return switch (oldLevel) {
            case EXCEPTION, ERROR -> "error";
            case DEBUG -> "debug";
            case TRACE -> "trace";
            case NOTICE -> "info";
            case WARNING -> "warn";
        };
    }

    // todo: delete " - " if exist
    private static String getLoggerMessage(String line, Optional<String> optionalObject) {
        StringBuilder message = new StringBuilder("\"");
        List<String> mixedArguments = Arrays.stream(line.split("\\+"))
            .map(String::trim).toList();
        List<String> nonMessageArgs = new ArrayList<>();
        for (String argument : mixedArguments) {
            if (argument.startsWith("\"")) {
                message.append(argument, 1, argument.length() - 1);
            } else {
                message.append("{}");
                nonMessageArgs.add(argument);
            }
        }
        if (optionalObject.isPresent()) {
            isWrapperImportNeeded = true;
            String additionalArgs = optionalObject.get().replaceFirst(",", "").trim();
            List<String> compliantArgsList = getCompliantAdditionalArguments(additionalArgs);
            initializeArgsProcessor(compliantArgsList);
            optionalArgsProcessor.propagateLoggerMessage(message, nonMessageArgs, compliantArgsList);
        }
        message.append("\"");
        nonMessageArgs.forEach(arg -> message.append(", ").append(arg));
        return message.toString();
    }

    private static void initializeArgsProcessor(List<String> args) {
        if (args.size() == 2) {
            optionalArgsProcessor = new MultipleArgsProcessor();
        } else if (isThrowable()) {
            optionalArgsProcessor = new ThrowableArgsProcessor();
        } else {
            optionalArgsProcessor = new ObjectClassProcessor();
        }
    }

    private static boolean isThrowable() {
        int currentLoggerIndex = currentProcessingFile.getCurrentProcessingLineIndex();
        if (currentLoggerIndex == -1) {
            log.error("File {} can't be processed", currentProcessingFile.getPath().getFileName());
        }
        for (int i = currentLoggerIndex; i > currentLoggerIndex - 10; i--) {
            if (currentProcessingFile.getInitialTextLines().get(i).contains("catch")) {
                return true;
            }
        }
        return false;
    }

    private static List<String> getCompliantAdditionalArguments(String additionalArgs) {
        String[] separatedLines = additionalArgs.split(",");
        List<String> compliantArgs = new ArrayList<>();
        StringBuilder compliantStringArgument = new StringBuilder();
        for (String arg : separatedLines) {
            compliantStringArgument.append(arg);
            if (checkParenthesis(compliantStringArgument.toString())) {
                compliantArgs.add(compliantStringArgument.toString());
                compliantStringArgument = new StringBuilder();
            } else {
                compliantStringArgument.append(",");
            }
        }
        if (!compliantStringArgument.isEmpty()) {
            compliantArgs.add(compliantStringArgument.toString());
        }
        return compliantArgs;
    }

    private static String constructFinalLoggerVersion(String level, String message) {
        String secondLogger = optionalArgsProcessor.getSecondLogger(level);
        String finalLoggerVersion = LOGGER_VAR_NAME + "." + level + "(" + message + ");\n";
        if (!secondLogger.isBlank()) {
            finalLoggerVersion += secondLogger;
        }
        return finalLoggerVersion;
    }

    private enum OldLoggingLevels {
        TRACE, DEBUG, NOTICE, WARNING, EXCEPTION, ERROR
    }

    private static boolean checkParenthesis(String str) {
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

}
