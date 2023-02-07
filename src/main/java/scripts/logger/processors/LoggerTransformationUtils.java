package scripts.logger.processors;

public class LoggerTransformationUtils {

    private LoggerTransformationUtils(){}

    public static boolean checkParenthesis(String str) {
        int counter = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == '(') {
                counter++;
            } else if (str.charAt(i) == ')') {
                counter--;
            }
            if (counter < 0) {
                return false;
            }
        }
        return counter == 0;
    }

    public static final String LOGGER_VAR_NAME = "log";

}
