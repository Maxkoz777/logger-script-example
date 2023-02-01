package scripts.logger.processors.impl;

import java.util.List;
import java.util.Optional;
import scripts.logger.processors.OptionalArgsProcessor;

public class ObjectClassProcessor implements OptionalArgsProcessor {

    @Override
    public void propagateLoggerMessage(StringBuilder message, List<String> nonMessageArgs, List<String> optionalArgs) {
        nonMessageArgs.add(this.wrapIntoProperStringFormat(optionalArgs.get(0).trim()));
        message.append(" {}");
    }

    @Override
    public String getSecondLogger(String level) {
        return "";
    }

}
