package scripts.logger.processors.impl;

import java.util.List;
import java.util.Optional;
import scripts.logger.processors.OptionalArgsProcessor;

public class ThrowableArgsProcessor implements OptionalArgsProcessor {

    @Override
    public void propagateLoggerMessage(StringBuilder message, List<String> nonMessageArgs, List<String> optionalArgs) {
        nonMessageArgs.add(optionalArgs.get(0).trim());
    }

    @Override
    public Optional<String> getOptionalSecondLogger(String level) {
        return Optional.empty();
    }

}
