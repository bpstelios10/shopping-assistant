package org.learnings.ai.shoppingassistant.rag.loaddocs;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Service
public class DocumentImporter {

    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;
    private final TokenTextSplitter splitter = TokenTextSplitter.builder().withChunkSize(500).build();

    public DocumentImporter(VectorStore vectorStore, JdbcTemplate jdbcTemplate) {
        this.vectorStore = vectorStore;
        this.jdbcTemplate = jdbcTemplate;
    }

    public void reimport(Path folder) throws IOException {
        log.info("reimporting from [{}]. first deleting everything from vector_store", folder);
        jdbcTemplate.update("DELETE FROM vector_store");   // wipe

        try (Stream<Path> files = Files.list(folder)) {
            files.filter(Files::isRegularFile).forEach(this::importFile);
        }
    }

    private void importFile(Path file) {
        log.info("importing [{}]", file);
        var reader = new TikaDocumentReader(new FileSystemResource(file));

        List<Document> chunks = splitter.apply(reader.get());
        chunks.forEach(d -> d.getMetadata().put("filename", file.getFileName().toString()));
        vectorStore.add(chunks);
        log.info("Imported file successfully.");
    }
}
