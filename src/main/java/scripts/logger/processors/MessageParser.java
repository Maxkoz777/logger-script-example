package scripts.logger.processors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;
import lombok.Getter;

@Getter
public class MessageParser {

    List<String> nonMessageArgs;
    StringBuilder messageBuilder;

    private static final List<Character> ALLOWED_COMMENT_SURROUNDINGS = List.of('?', ':', '+');

    public void processLine(String line) {
        messageBuilder = new StringBuilder("\"");
        List<String> mixedArguments = Arrays.stream(line.split("\\+"))
            .map(String::trim).collect(Collectors.toCollection(ArrayList::new));
        normalizeArguments(mixedArguments);
        nonMessageArgs = new ArrayList<>();
        for (String argument : mixedArguments) {
            if (argument.startsWith("\"")) {
                messageBuilder.append(argument, 1, argument.length() - 1);
            } else {
                messageBuilder.append("{}");
                nonMessageArgs.add(argument);
            }
        }
    }

    public String finalizedMessage() {
        messageBuilder.append("\"");
        nonMessageArgs.stream()
            .filter(arg -> !arg.isBlank())
            .forEach(arg -> messageBuilder.append(", ").append(arg));
        String innerMessage = messageBuilder.toString();
        if (innerMessage.startsWith("\" - ")) {
            innerMessage = innerMessage.replaceFirst("\\s?-\\s", "");
        }
        return innerMessage;
    }

    private void normalizeArguments(List<String> args) {
        ListIterator<String> iterator = args.listIterator();
        while (iterator.hasNext()) {
            String line = iterator.next();
            if (LoggerTransformationUtils.checkParenthesis(line) || line.trim().startsWith("\"")) {
                continue;
            }
            StringBuilder stringBuilder = new StringBuilder(line);
            iterator.remove();
            while (iterator.hasNext()) {
                String nextLine = iterator.next();
                stringBuilder.append(nextLine);
                if (LoggerTransformationUtils.checkParenthesis(stringBuilder.toString())) {
                    break;
                }
                iterator.remove();
            }
            iterator.remove();
            String finalizedArg = finalizeComplexArgument(stringBuilder.toString());
            iterator.add(finalizedArg);
        }
    }

    private String finalizeComplexArgument(String arg) {
        int areStringArgsEven = arg.trim().startsWith("\"") ? 1 : 0;
        List<String> argParts = Arrays.stream(arg.split("\"")).collect(Collectors.toCollection(LinkedList::new));
        ListIterator<String> listIterator = argParts.listIterator();
        while (listIterator.hasNext()) {
            String line = listIterator.next().trim();
            StringBuilder stringBuilder = new StringBuilder(line);
            if (listIterator.nextIndex() % 2 == areStringArgsEven) {
                listIterator.set("\"" + line + "\"");
                continue;
            }
            if (listIterator.nextIndex() > 2) {
                updateArgStart(stringBuilder);
            }
            if (listIterator.hasNext()) {
                updateArgTail(stringBuilder);
            }
            listIterator.set(stringBuilder.toString());
        }
        return String.join("", argParts);
    }

    private void updateArgStart(StringBuilder stringBuilder) {
        String line = stringBuilder.toString();
        if (!ALLOWED_COMMENT_SURROUNDINGS.contains(line.charAt(0))) {
            stringBuilder.insert(0, " + ");
        }
    }

    private void updateArgTail(StringBuilder stringBuilder) {
        String line = stringBuilder.toString();
        if (!ALLOWED_COMMENT_SURROUNDINGS.contains(line.charAt(line.length() - 1))) {
            stringBuilder.append(" + ");
        }
    }

}
