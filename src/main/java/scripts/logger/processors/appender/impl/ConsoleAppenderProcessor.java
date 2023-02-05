package scripts.logger.processors.appender.impl;

import static scripts.logger.processors.LoggerTransformationUtils.LOGGER_VAR_NAME;

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
        messageParser.processLine(line);
        return LOGGER_VAR_NAME + "." + getLoggingLevel() + "(" + messageParser.finalizedMessage() + ");";
    }


}
