package scripts.logger.processors.impl;

import static scripts.logger.LoggerTransformationUtils.LOGGER_VAR_NAME;

import java.util.List;
import scripts.logger.processors.OptionalArgsProcessor;

public class MultipleArgsProcessor implements OptionalArgsProcessor {

    private String exception;
    private String object;

    @Override
    public void propagateLoggerMessage(StringBuilder message, List<String> nonMessageArgs, List<String> optionalArgs) {
        exception = optionalArgs.get(0).trim();
        object = optionalArgs.get(1).trim();
        nonMessageArgs.add(exception);
    }

    @Override
    public String getSecondLogger(String level) {
        String message = "\"{}\", " + wrapIntoProperStringFormat(object) + ", " + exception;
        return LOGGER_VAR_NAME + "." + level + "(" + message + ");\n";
    }


}
