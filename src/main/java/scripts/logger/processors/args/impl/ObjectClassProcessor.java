package scripts.logger.processors.args.impl;

import java.util.List;
import scripts.logger.processors.args.OptionalArgsProcessor;

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
