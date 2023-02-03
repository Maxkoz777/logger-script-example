package scripts.logger.processors.appender.impl;

import static scripts.logger.processors.LoggerTransformationUtils.LOGGER_VAR_NAME;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import scripts.logger.model.ProcessingUnit;
import scripts.logger.processors.appender.AppenderProcessor;
import scripts.logger.processors.args.OptionalArgsProcessor;
import scripts.logger.processors.args.impl.MultipleArgsProcessor;
import scripts.logger.processors.args.impl.ObjectClassProcessor;
import scripts.logger.processors.args.impl.ThrowableArgsProcessor;
import scripts.logger.processors.LoggerTransformationUtils;

public class FileAppenderProcessor extends AppenderProcessor {

    public FileAppenderProcessor(ProcessingUnit processingUnit) {
        super(processingUnit);
        messageGroupNumber = 1;
    }

    @Override
    public String getUpdatedLoggerStatement() {
        String loggerMessage = getLoggerMessageWithArgs();
        String additionalObject = processingUnit.getMatcher().group(3);
        String message = generateLoggerMessage(loggerMessage, Optional.ofNullable(additionalObject));
        processingUnit.setLoggingStatement(message);
        return processingUnit.getUpdatedString();
    }

    @Override
    public String getLoggingLevel() {
        OldLoggingLevels oldLoggingLevel = OldLoggingLevels.valueOf(processingUnit.getMatcher().group(2));
        return switch (oldLoggingLevel) {
            case EXCEPTION, ERROR -> "error";
            case DEBUG -> "debug";
            case TRACE -> "trace";
            case NOTICE -> "info";
            case WARNING -> "warn";
        };
    }

    private OptionalArgsProcessor initializeArgsProcessor(List<String> args) {
        if (args.size() == 2) {
            return new MultipleArgsProcessor();
        } else if (isThrowable()) {
            return new ThrowableArgsProcessor();
        } else {
            return new ObjectClassProcessor();
        }
    }

    private boolean isThrowable() {
        int index = processingUnit.getCurrentIndex();
        for (int i = index; i > index - 10; i--) {
            if (processingUnit.getLinesOfCode().get(i).contains("catch")) {
                return true;
            }
        }
        return false;
    }

    private String generateLoggerMessage(String line, Optional<String> optionalObject) {
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
        OptionalArgsProcessor optionalArgsProcessor = null;
        if (optionalObject.isPresent()) {
            LoggerTransformationUtils.isWrapperImportNeeded = true;
            String additionalArgs = optionalObject.get().replaceFirst(",", "").trim();
            List<String> compliantArgsList = getCompliantAdditionalArguments(additionalArgs);
            optionalArgsProcessor = initializeArgsProcessor(compliantArgsList);
            optionalArgsProcessor.propagateLoggerMessage(message, nonMessageArgs, compliantArgsList);
        }
        message.append("\"");
        nonMessageArgs.stream()
            .filter(arg -> !arg.isBlank())
            .forEach(arg -> message.append(", ").append(arg));
        String innerMessage = message.toString();
        if (innerMessage.startsWith("\" - ")) {
            innerMessage = innerMessage.replaceFirst("\\s?-\\s", "");
        }
        return constructFinalLoggerVersion(getLoggingLevel(), innerMessage, optionalArgsProcessor);
    }

    private List<String> getCompliantAdditionalArguments(String additionalArgs) {
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

    private static String constructFinalLoggerVersion(String level, String message, OptionalArgsProcessor optionalArgsProcessor) {
        String secondLogger = Objects.nonNull(optionalArgsProcessor)
            ? optionalArgsProcessor.getSecondLogger(level)
            : "";
        String finalLoggerVersion = LOGGER_VAR_NAME + "." + level + "(" + message + ");";
        if (!secondLogger.isBlank()) {
            finalLoggerVersion += "\n" + secondLogger;
        }
        return finalLoggerVersion;
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

    private enum OldLoggingLevels {
        TRACE, DEBUG, NOTICE, WARNING, EXCEPTION, ERROR
    }
}
