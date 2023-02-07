package scripts.logger.processors;

import java.util.ArrayList;
import java.util.List;

public class ParamProcessor {

    /**
     * Разбивает строку указанным символом если данный символ находится на первом уровне вложения
     * по скобкам и не в тексте.
     *
     * @param line строка без переводов строки
     * @param div разделитель
     * @return части [not null]
     */
    public static List<String> split(final String line, final char div) {
        int deep = 0;
        final List<String> result = new ArrayList<>();
        int start = 0;
        int i = 0;
        while (i < line.length()) {
            final char c = line.charAt(i);
            if (c == '"' || c == '\'') {
                i = findEndOfTheText(line, c, i + 1);
            } else if (c == '(' || c == '[') {
                deep++;
            } else if (c == ')' || c == ']') {
                deep--;
            } else if (c == div && deep == 0) {
                result.add(line.substring(start, i).trim());
                start = i + 1;
            }
            i++;
        }
        if (start < line.length()) {
            result.add(line.substring(start).trim());
        }
        return result;
    }

    private static int findEndOfTheText(final String line, final char end, final int start) {
        int i = start;
        while (i < line.length()) {
            final char c = line.charAt(i);
            if (c == '\\') {
                i++;
            }
            if (c == end) {
                return i;
            }
            i++;
        }
        throw new IllegalStateException("No '" + end + "' found, starting from " + start);
    }

    /**
     * Преобразует слагаемые в выражение через формат SLF4J
     * @param parts слагаемые
     * @return строка содержащая формат и параметры.
     */
    public static String transformTerms(final List<String> parts) {
        final StringBuilder sb = new StringBuilder();
        sb.append('"');
        final StringBuilder params = new StringBuilder();
        for (final String part : parts) {
            final String trim = part.trim();
            if (trim.startsWith("\"") && trim.endsWith("\"")) {
                sb.append(trim, 1, trim.length() - 1);
            } else if (trim.startsWith("'") && trim.endsWith("'")) {
                sb.append(trim, 1, trim.length() - 1);
            } else {
                sb.append("{}");
                params.append(", ").append(part.trim());
            }
        }
        sb.append('"');
        sb.append(params);
        return sb.toString();
    }


    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    private static void trial(String t) {
        t = t.trim();
        final List<String> params = split(t, ',');
        final List<String> terms = split(params.get(0), '+');
        final String join = transformTerms(terms);
        System.out.println(t);
        System.out.println("  params: " + params);
        System.out.println("  terms: " + terms);
        System.out.println("  join: " + join);
    }

    public static void main(final String[] args) {
        trial("\"text text\", 5654+7608+\"897\",e\n");
        trial("\"text text\\\"\"+(true ? u(5,78)+'':ooo)+\" kkk=\"+reload, 5654+(7608+,\"897\"),e\n");
        trial("indent +\" @\"+ name +\": @\"+ this._type.getAttribute(name).getName() +\" (\"+ this._type.getAttribute(name).getReference() +\")\"\n");
    }

}
