package io.github.kaiq22unc.localrag.core.indexer;

import io.github.kaiq22unc.localrag.core.util.ManifestStore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Indexer {

    public static void index(Path indexDir, FileScanner.ScanResult scan, int chunkLines, int overlapLines) throws IOException {
        int totalChunks = 0;
        int readSkipped = 0;
        int readOk = 0;

        Path manifestPath = indexDir.resolve("manifest.tsv"); // .localrag/manifest.tsv if indexDir=.localrag/
        ManifestStore manifestStore = new ManifestStore();
        Map<String, Long> oldManifest = manifestStore.load(manifestPath);
        Map<String, Long> newManifest = new HashMap<>();
        Set<String> seenPaths = new HashSet<>();
        int skippedUnchanged = 0;
        int deletedMissing = 0;

        try (var lucene = new LuceneChunkIndex(indexDir, false)) {
            for (var f: scan.files) {
                String pathStr = f.toAbsolutePath().normalize().toString();
                long modified = Files.getLastModifiedTime(f).toMillis();

                seenPaths.add(pathStr);
                Long prev = oldManifest.get(pathStr);
                if (prev != null && prev == modified) {
                    skippedUnchanged++;
                    newManifest.put(pathStr, modified);
                    continue;
                }

//                var readResult = reader.read(f);
//                if (readResult == null) {
//                    readSkipped++;
//                    continue;
//                }
                readOk++;

                lucene.deleteByPath(pathStr);
//                var chunks = Chunker.chunkByLines(readResult.contents, chunkLines, overlapLines);
//                System.out.printf("%s -> %d chunks%n", pathStr, chunks.size());
//
//                for (var chunk : chunks) {
//                    lucene.upsertChunk(pathStr, chunk.chunkId(), chunk.startLine(), chunk.endLine(), readResult.modifiedMillis, chunk.text());
//                    totalChunks++;
//                }

                int produced = StreamingChunker.chunkFileByLines(f, chunkLines, overlapLines,
                        (chunkId, startLine, endLine, text) -> {
                            lucene.upsertChunk(pathStr, chunkId, startLine, endLine, modified, text);
                        });

                totalChunks += produced;

                newManifest.put(pathStr, modified);
            }

            for (var e : oldManifest.entrySet()) {
                String path = e.getKey();
                if (!seenPaths.contains(path)) {
                    lucene.deleteByPath(path);
                    deletedMissing++;
                }
            }

            lucene.commit();
            System.out.println("skippedUnchanged=" + skippedUnchanged);
            System.out.println("deletedMissing=" + deletedMissing);
            System.out.println("totalChunksIndexed=" + totalChunks);
            System.out.println("=== Read summary (UTF-8 only) ===");
            System.out.println("readOk=" + readOk);
            System.out.println("readSkipped=" + readSkipped);


            // Persist manifest so next run can skip unchanged files
            manifestStore.save(manifestPath, newManifest);
        }
    }
}
