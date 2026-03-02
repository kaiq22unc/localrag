package io.github.kaiq22unc.localrag.core.indexer;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class StreamingChunker {

    @FunctionalInterface
    public interface ChunkConsumer {
        void accept(int chunkId, int startLine, int endLine, String text) throws Exception;
    }

    /**
     * Stream a UTF-8 file line-by-line and emit chunks.
     * Returns number of chunks produced; returns 0 if file isn't valid UTF-8.
     */
    public static int chunkFileByLines(
            Path file,
            int chunkLines,
            int overlapLines,
            ChunkConsumer consumer
    ) throws IOException {

        if (chunkLines <= 0) throw new IllegalArgumentException("chunkLines must be > 0");
        if (overlapLines < 0 || overlapLines >= chunkLines) {
            throw new IllegalArgumentException("overlapLines must be >= 0 and < chunkLines");
        }

        int chunkId = 0;
        int lineNo = 0;              // 1-based line number
        int chunkStartLine = 1;

        List<String> buf = new ArrayList<>(chunkLines);

        try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = br.readLine()) != null) {
                lineNo++;

//                if (buf.isEmpty()) {
//                    chunkStartLine = lineNo; // first line of this chunk window
//                }
                buf.add(line);

                // buffer is full => emit a chunk
                if (buf.size() == chunkLines) {
                    int chunkEndLine = lineNo;
                    String text = String.join("\n", buf);

                    try {
                        consumer.accept(chunkId, chunkStartLine, chunkEndLine, text);
                    } catch (IOException | RuntimeException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }

                    chunkId++;

                    // Slide: keep only overlapLines lines
                    if (overlapLines == 0) {
                        buf.clear();
                    } else {
                        buf = new ArrayList<>(buf.subList(chunkLines - overlapLines, chunkLines));
                    }

                    chunkStartLine = lineNo - overlapLines + 1;
                }
            }

            // Emit trailing partial chunk at EOF
            if (!buf.isEmpty()) {
                int chunkEndLine = lineNo;
                String text = String.join("\n", buf);

                try {
                    consumer.accept(chunkId, chunkStartLine, chunkEndLine, text);
                } catch (IOException | RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                chunkId++;
            }
        } catch (CharacterCodingException e) {
            // not valid UTF-8 (likely binary)
            return 0;
        }

        return chunkId;
    }
}
