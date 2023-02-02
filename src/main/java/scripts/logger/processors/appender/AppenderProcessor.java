package scripts.logger.processors.appender;

import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import scripts.logger.model.ProcessingUnit;

@Slf4j
public abstract class AppenderProcessor {

    private static final Pattern LOGGING_DEBUG_MARK_PATTERN = Pattern.compile("Debug\\.printDebugMark\\(\\)(\\s*\\+)?\\s?,?");

    protected int messageGroupNumber;

    protected ProcessingUnit processingUnit;

    protected AppenderProcessor(ProcessingUnit processingUnit) {
        this.processingUnit = processingUnit;
    }

    public abstract String getUpdatedLoggerStatement();

    public abstract String getLoggingLevel();

    protected String getLoggerMessageWithArgs() {
        return processingUnit.getMatcher().group(messageGroupNumber)
            .replaceAll(LOGGING_DEBUG_MARK_PATTERN.pattern(), "");
    }

}
