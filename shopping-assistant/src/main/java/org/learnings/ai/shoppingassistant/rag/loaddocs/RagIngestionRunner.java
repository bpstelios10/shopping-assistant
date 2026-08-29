package org.learnings.ai.shoppingassistant.rag.loaddocs;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@Slf4j
@Component
@Profile("ingest")
public class RagIngestionRunner implements ApplicationRunner {

    private final DocumentImporter importer;

    public RagIngestionRunner(DocumentImporter importer) {
        this.importer = importer;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        List<String> folders = args.getNonOptionArgs();
        if (folders.isEmpty()) {
            throw new IllegalArgumentException("Usage: --spring.profiles.active=ingest <folder>");
        }
        importer.reimport(Path.of(folders.getFirst()));
    }
}
