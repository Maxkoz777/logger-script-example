package scripts.logger.model;

import java.util.List;
import java.util.regex.Matcher;
import lombok.Data;

@Data
public class ProcessingUnit {
    private String prefix;
    private String postfix;
    private String initialLine;
    private String loggingStatement;
    private Matcher matcher;
    private List<String> linesOfCode;
    private int currentIndex;

    public ProcessingUnit(List<String> lines, int index) {
        this.linesOfCode = lines;
        this.currentIndex = index;
        this.initialLine = lines.get(index);
    }

    public String getUpdatedString() {
        return String.join("", prefix, loggingStatement, postfix);
    }

    public void initializeSurroundings() {
        prefix = initialLine.substring(0, matcher.start());
        postfix = initialLine.trim().endsWith(");") ?
            "" :
            initialLine.substring(matcher.end());
    }

}
