package scripts.logger.model;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class ProcessingFile {

    private File file;
    private String content;
    private List<String> initialTextLines;
    private List<String> updatedLoggerLines;

    public ProcessingFile(File file) {
        this.file = file;
        Path path = file.toPath();
        try {
            content = Files.readString(path);
            initialTextLines = Files.readAllLines(path);
        } catch (IOException exception) {
            log.error("Unable to read file {} content", file.getName(), exception);
        }
    }

    public void inlineLoggingStatements() {

    }

}
