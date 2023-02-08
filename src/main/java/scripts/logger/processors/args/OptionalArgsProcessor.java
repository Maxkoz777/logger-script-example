package scripts.logger.processors.args;

import java.util.List;
import scripts.logger.processors.SourceConverter;

public interface OptionalArgsProcessor {

    void propagateLoggerMessage(StringBuilder message, List<String> nonMessageArgs, List<String> optionalArgs);

    String getSecondLogger(String level);

    default String wrapIntoProperStringFormat(String arg) {
        SourceConverter.isWrapperImportNeeded = true;
        return "LoggerObjectWrapper.wrap(" + arg + ")";
    }

}
