package scripts.logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;

public class LoggerTransformationUtils {

    public static String reformatLogger(Matcher matcher) {
        String loggerLevel = getLoggingLevel(
            OldLoggingLevels.valueOf(matcher.group(2))
        );
        String loggerMessage = matcher.group(1)
            .replaceAll(PatternUtils.LOGGING_DEBUG_MARK_PATTERN.pattern(), "");
        String additionalObject = matcher.group(3);
        String message = getLoggerMessage(loggerMessage, Optional.ofNullable(additionalObject));
        return constructFinalLoggerVersion(loggerLevel, message);
    }

    public static String getLoggerImports() {
        return "import org.slf4j.Logger;\n"
            + "import org.slf4j.LoggerFactory;\n";
    }

    public static String getLoggerVarDeclaration(String className) {
        return "private static final Logger " + LOGGER_VAR_NAME + " = LoggerFactory.getLogger(" + className + ".class);\n";
    }

    private static final String LOGGER_VAR_NAME = "loggerLongUniqueName";

    private static String getLoggingLevel(OldLoggingLevels oldLevel) {
        return switch (oldLevel) {
            case EXCEPTION, ERROR -> "error";
            case DEBUG -> "debug";
            case TRACE -> "trace";
            case NOTICE -> "info";
            case WARNING -> "warn";
        };
    }

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
            message.append(" {}");
            nonMessageArgs.add(
                optionalObject.get().replaceFirst(",", "").trim()
            );
        }
        message.append("\"");
        nonMessageArgs.forEach(arg -> message.append(", ").append(arg));
        return message.toString();
    }

    private static String constructFinalLoggerVersion(String level, String message) {
        return LOGGER_VAR_NAME + "." + level + "(" + message + ");\n";
    }

    private enum OldLoggingLevels {
        TRACE, DEBUG, NOTICE, WARNING, EXCEPTION, ERROR
    }

}
