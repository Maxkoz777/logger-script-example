package scripts.logger;

import java.util.regex.Pattern;

public class PatternUtils {

    private PatternUtils(){}

    public static final Pattern LOGGER_EXISTS_PATTERN = Pattern.compile("FileLogger");

    public static final Pattern LOGGING_STATEMENT_PATTERN = Pattern.compile("FileLogger\\.log\\(\\s?(.+),\\s*FileLogger\\.(\\w+)\\s*(,.+)?\\);");

    public static final Pattern LOGGING_DEBUG_MARK_PATTERN = Pattern.compile("Debug\\.printDebugMark\\(\\)\\s*\\+\\s?");

    public static final Pattern CLASS_DECLARATION = Pattern.compile("class\\s(\\w+)");

    public static final Pattern LEFT_LOGGER_SPACE = Pattern.compile("\s*");

}
