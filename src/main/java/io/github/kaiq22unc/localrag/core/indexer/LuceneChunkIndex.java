package io.github.kaiq22unc.localrag.core.indexer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class LuceneChunkIndex implements AutoCloseable {

    private final Directory dir;
    private final Analyzer analyzer;
    private final IndexWriter writer;

    public LuceneChunkIndex(Path indexDir, boolean recreate) throws IOException {
        Files.createDirectories(indexDir);
        this.dir = FSDirectory.open(indexDir);
        this.analyzer = new StandardAnalyzer();

        IndexWriterConfig cfg = new IndexWriterConfig(analyzer);
        cfg.setOpenMode(recreate ? IndexWriterConfig.OpenMode.CREATE : IndexWriterConfig.OpenMode.CREATE_OR_APPEND);

        this.writer = new IndexWriter(dir, cfg);
    }

    /**
     * Upsert chunk doc (safe for re-runs)
     */
    public void upsertChunk(String path, int chunkId, int startLine, int endLine, long modifiedMillis, String text) throws IOException {
        String docId = path + "#" + chunkId;

        Document doc = new Document();
        doc.add(new StringField("docId", docId, Field.Store.NO));          // unique key for update
        doc.add(new StringField("path", path, Field.Store.YES));

        doc.add(new IntPoint("chunkId", chunkId));
        doc.add(new StoredField("chunkId_stored", chunkId));

        doc.add(new IntPoint("startLine", startLine));
        doc.add(new StoredField("startLine_stored", startLine));

        doc.add(new IntPoint("endLine", endLine));
        doc.add(new StoredField("endLine_stored", endLine));

        doc.add(new LongPoint("modified", modifiedMillis));
        doc.add(new StoredField("modified_stored", modifiedMillis));

        // Searchable + stored so we can show snippet without rereading the file
        doc.add(new TextField("contents", text, Field.Store.YES));

        writer.updateDocument(new Term("docId", docId), doc);
    }

    public void commit() throws IOException {
        writer.commit();
    }

    @Override
    public void close() throws IOException {
        try {
            writer.close();
        } finally {
            try {
                analyzer.close();
            } finally {
                dir.close();
            }
        }
    }
}
