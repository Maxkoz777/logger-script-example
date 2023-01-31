package scripts.logger.processors;

import java.util.List;
import java.util.Optional;

public interface OptionalArgsProcessor {

    void propagateLoggerMessage(StringBuilder message, List<String> nonMessageArgs, List<String> optionalArgs);

    Optional<String> getOptionalSecondLogger(String level);

    default String wrapIntoProperStringFormat(String arg) {
        return "LoggerObjectWrapper.wrap(" + arg + ")";
    }

}
