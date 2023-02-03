package scripts.logger;

import lombok.extern.slf4j.Slf4j;
import scripts.logger.model.ScriptProcessingUnit;

@Slf4j
public class LoggerProcessorScript {

    private static final String DIRECTORY_PATH = "/Users/user/Documents/WaveAcess/server/server/src/main/java/com/cellpointdigital/mesb";

    public static void main(String[] args) {

        ScriptProcessingUnit unit = new ScriptProcessingUnit(DIRECTORY_PATH);
        unit.process();

    }

}
