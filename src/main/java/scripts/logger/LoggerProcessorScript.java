package scripts.logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import scripts.logger.processors.SourceConverter;

public class LoggerProcessorScript {

    public static void main(String[] args) throws IOException {

        AtomicInteger filesCount = new AtomicInteger();
        int sum = 0;

        try (Stream<Path> filesStream = Files.walk(Paths.get(args[0]))) {
            sum = filesStream.filter(Files::isRegularFile)
                .map(SourceConverter::new)
                .map(SourceConverter::process)
                .mapToInt(x -> x)
                .filter(x -> x != 0)
                .peek(x -> filesCount.getAndIncrement())
                .sum();
        }

        System.out.println("Number of changed java classes: " + filesCount.get());
        System.out.println("Number of changed lines: " + sum);

    }

}
