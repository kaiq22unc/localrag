package io.github.kaiq22unc.localrag.core.searcher;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;

public class Searcher {

    public Searcher() {}

    public void search(Path indexDir, String queryText, int topK) throws IOException, ParseException {
        try (Directory dir = FSDirectory.open(indexDir);
             DirectoryReader reader = DirectoryReader.open(dir);
             Analyzer analyzer = new StandardAnalyzer()) {

            IndexSearcher searcher = new IndexSearcher(reader);
            QueryParser parser = new QueryParser("contents", analyzer);

            // Treat user input as plain text (no Lucene query syntax surprises)
            Query q = parser.parse(QueryParser.escape(queryText));

            TopDocs top = searcher.search(q, topK);
            System.out.printf("Hits: %d (showing %d)%n",
                    top.totalHits.value(), Math.min(topK, top.scoreDocs.length));

            for (int i = 0; i < top.scoreDocs.length; i++) {
                ScoreDoc sd = top.scoreDocs[i];
                Document d = searcher.storedFields().document(sd.doc);

                String path = d.get("path");

                int startLine = 0;
                int endLine = 0;
                if (d.getField("startLine_stored") != null) {
                    startLine = d.getField("startLine_stored").numericValue().intValue();
                }
                if (d.getField("endLine_stored") != null) {
                    endLine = d.getField("endLine_stored").numericValue().intValue();
                }

                String contents = d.get("contents"); // only works if you stored it
                String snip = snippet(contents, queryText);

                if (path != null && startLine > 0 && endLine > 0) {
                    System.out.printf("%n[%d] score=%.4f%n%s:%d-%d%n%s%n",
                            i + 1, sd.score, path, startLine, endLine, snip);
                } else {
                    System.out.printf("%n[%d] score=%.4f%n%s%n%s%n",
                            i + 1, sd.score, path, snip);
                }
            }
        }
    }

    private String snippet(String contents, String rawQuery) {
        if (contents == null || contents.isEmpty()) return "";

        String q = rawQuery.toLowerCase(Locale.ROOT).trim();
        String c = contents.toLowerCase(Locale.ROOT);

        String token = firstToken(q);
        int idx = token.isEmpty() ? -1 : c.indexOf(token);
        if (idx < 0) idx = 0;

        int start = Math.max(0, idx - 80);
        int end = Math.min(contents.length(), start + 240);

        String s = contents.substring(start, end).replaceAll("\\s+", " ").trim();
        return end < contents.length() ? s + " …" : s;
    }

    private String firstToken(String q) {
        String[] parts = q.split("\\s+");
        return parts.length == 0 ? q : parts[0];
    }
}