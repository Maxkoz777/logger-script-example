package scripts.logger.model;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class ProcessingFile {

    private Path path;
    private String content;
    private List<String> initialTextLines;
    private List<String> updatedLoggerLines = new LinkedList<>();
    private int currentProcessingLineIndex = -1;

    public ProcessingFile(Path path) {
        this.path = path;
        updateFileContent();
    }

    public ProcessingFile inlineLoggingStatements() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(path.toFile()))) {
            int index = 0;
            while (index < initialTextLines.size()) {
                StringBuilder line = new StringBuilder(initialTextLines.get(index));
                if (line.toString().contains("FileLogger.log")) {
                    while (!initialTextLines.get(index).trim().contains(");")) {
                        line.append(" ")
                            .append(initialTextLines.get(++index).trim());
                    }
                }
                bw.write(line.toString());
                bw.newLine();
                index++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        updateFileContent();
        return this;
    }

    public void computeCurrentProcessingLineIndex(String matchedLine) {
        for (int i = 0; i < initialTextLines.size(); i++) {
            if (initialTextLines.get(i).trim().contains(matchedLine)) {
                currentProcessingLineIndex = i;
                break;
            }
        }
    }

    private void updateFileContent() {
        try {
            content = Files.readString(path);
            initialTextLines = Files.readAllLines(path);
        } catch (IOException exception) {
            log.error("Unable to read file {} content", path.getFileName(), exception);
        }
    }

}
