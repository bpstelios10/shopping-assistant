package org.learnings.ai.shoppingassistant.rag.loaddocs;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.nio.file.Path;

@Slf4j
@SpringBootApplication
public class RagIngestionRunner {

    static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.err.println("Usage: RagIngestionRunner <folder>");
            System.exit(1);
        }

        log.info("Working Directory: [{}]", System.getProperty("user.dir"));
        log.info("folder requested: [{}]", args[0]);

        try (ConfigurableApplicationContext ctx = new SpringApplicationBuilder(RagIngestionRunner.class)
                .web(WebApplicationType.NONE)   // no web server
                .run(args)) {

            DocumentImporter importer = ctx.getBean(DocumentImporter.class);
            importer.reimport(Path.of(args[0]));
        }
    }
}
