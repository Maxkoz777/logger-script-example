package scripts.logger.processors;

import java.util.List;

public interface OptionalArgsProcessor {

    void propagateLoggerMessage(StringBuilder message, List<String> nonMessageArgs, List<String> optionalArgs);

    String getSecondLogger(String level);

    default String wrapIntoProperStringFormat(String arg) {
        return "LoggerObjectWrapper.wrap(" + arg + ")";
    }

}
