package scripts.logger.processors.appender;

import lombok.extern.slf4j.Slf4j;
import scripts.logger.PatternUtils;
import scripts.logger.model.ProcessingUnit;

@Slf4j
public abstract class AppenderProcessor {

    protected int messageGroupNumber;

    protected ProcessingUnit processingUnit;

    protected AppenderProcessor(ProcessingUnit processingUnit) {
        this.processingUnit = processingUnit;
    }

    public abstract String getUpdatedLoggerStatement();

    public abstract String getLoggingLevel();

    protected String getLoggerMessageWithArgs() {
        return processingUnit.getMatcher().group(messageGroupNumber)
            .replaceAll(PatternUtils.LOGGING_DEBUG_MARK_PATTERN.pattern(), "");
    }

}
