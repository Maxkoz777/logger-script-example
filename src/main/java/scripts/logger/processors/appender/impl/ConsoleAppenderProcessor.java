package scripts.logger.processors.appender.impl;

import static scripts.logger.LoggerTransformationUtils.LOGGER_VAR_NAME;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import scripts.logger.model.ProcessingUnit;
import scripts.logger.processors.appender.AppenderProcessor;

public class ConsoleAppenderProcessor extends AppenderProcessor {

    public ConsoleAppenderProcessor(ProcessingUnit processingUnit) {
        super(processingUnit);
        messageGroupNumber = 3;
    }

    @Override
    public String getUpdatedLoggerStatement() {
        String loggerMessage = getLoggerMessageWithArgs();
        String message = generateLoggerMessage(loggerMessage);
        processingUnit.setLoggingStatement(message);
        return processingUnit.getUpdatedString();
    }

    @Override
    public String getLoggingLevel() {
        String oldLoggingLevel = processingUnit.getMatcher().group(1);
        if (oldLoggingLevel.equals("err")) {
            return "error";
        } else {
            return "info";
        }
    }

    private String generateLoggerMessage(String line) {
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
        message.append("\"");
        nonMessageArgs.stream()
            .filter(arg -> !arg.isBlank())
            .forEach(arg -> message.append(", ").append(arg));
        String innerMessage = message.toString();
        if (innerMessage.startsWith("\" - ")) {
            innerMessage = innerMessage.replaceFirst("\\s?-\\s", "");
        }
        return LOGGER_VAR_NAME + "." + getLoggingLevel() + "(" + innerMessage + ");";
    }


}
