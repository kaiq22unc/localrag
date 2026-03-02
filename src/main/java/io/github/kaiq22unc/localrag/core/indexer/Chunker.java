package io.github.kaiq22unc.localrag.core.indexer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Chunker {
    /**
     * @param chunkId   0..n within a file
     * @param startLine 1-based, inclusive
     * @param endLine   1-based, inclusive
     * @param text      chunk text joined with '\n'
     */
    public record Chunk(int chunkId, int startLine, int endLine, String text) {
        @Override
        public int chunkId() {
            return chunkId;
        }

        @Override
        public int startLine() {
            return startLine;
        }

        @Override
        public int endLine() {
            return endLine;
        }

        @Override
        public String text() {
            return text;
        }
    }

    public static List<Chunk> chunkByLines(String contents, int chunkLines, int overlapLines) {
        if (chunkLines <= 0) throw new IllegalArgumentException("chunkLines must be > 0");
        if (overlapLines < 0) throw new IllegalArgumentException("overlapLines must be >= 0");
        if (overlapLines >= chunkLines) throw new IllegalArgumentException("overlapLines must be < chunkLines");

        // Split on any line separator; keep trailing empty lines with -1
        String[] lines = contents.split("\\R", -1);

        int step = Math.max(1, chunkLines - overlapLines);
        List<Chunk> out = new ArrayList<>();

        int chunkId = 0;
        for (int start = 0; start < lines.length; start += step) {
            int endExclusive = Math.min(lines.length, start + chunkLines);

            String text = String.join("\n", Arrays.copyOfRange(lines, start, endExclusive));

            int startLine = start + 1;           // 1-based
            int endLine = endExclusive;          // 1-based inclusive (because endExclusive is count)
            out.add(new Chunk(chunkId++, startLine, endLine, text));

            if (endExclusive == lines.length) break;
        }
        return out;
    }
}