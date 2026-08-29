package org.learnings.ai.shoppingassistant.rag.loaddocs;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Profile;
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
@Profile("ingest")
public class DocumentImporter {

    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;
    private final TokenTextSplitter splitter = TokenTextSplitter.builder().withChunkSize(500).build();

    public DocumentImporter(VectorStore vectorStore, JdbcTemplate jdbcTemplate) {
        this.vectorStore = vectorStore;
        this.jdbcTemplate = jdbcTemplate;
    }

    public void reimport(Path folder) throws IOException {
        List<Path> files = validateFolderAndGetFilePaths(folder);

        log.info("reimporting from [{}]. first deleting everything from vector_store", folder);
        jdbcTemplate.update("DELETE FROM vector_store");   // wipe
        files.forEach(this::importFile);
    }

    private void importFile(Path file) {
        log.info("importing [{}]", file);
        var reader = new TikaDocumentReader(new FileSystemResource(file));

        List<Document> chunks = splitter.apply(reader.get());
        vectorStore.add(chunks);
        log.info("Imported file successfully.");
    }

    private static @NonNull List<Path> validateFolderAndGetFilePaths(Path folder) throws IOException {
        if (!Files.isDirectory(folder)) {
            throw new IllegalArgumentException("Not a directory: " + folder.toAbsolutePath());
        }
        List<Path> files;
        try (Stream<Path> stream = Files.list(folder)) {
            files = stream.filter(Files::isRegularFile).toList();
        }
        if (files.isEmpty()) {
            throw new IllegalArgumentException("No files found in " + folder.toAbsolutePath());
        }
        return files;
    }
}
