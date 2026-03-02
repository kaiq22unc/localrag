package io.github.kaiq22unc.localrag.core.indexer;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public final class FileScanner {

    public static final class ScanResult {
        public final List<Path> files;   // passed filters
        public final int discovered;     // total files seen
        public final int skippedByExt;
        public final int skippedTooLarge;
        public final int skippedNotRegular;

        private ScanResult(List<Path> files, int discovered, int skippedByExt, int skippedTooLarge, int skippedNotRegular) {
            this.files = files;
            this.discovered = discovered;
            this.skippedByExt = skippedByExt;
            this.skippedTooLarge = skippedTooLarge;
            this.skippedNotRegular = skippedNotRegular;
        }
    }

    public ScanResult scan(List<Path> inputs, Set<String> exts, long maxBytes) throws IOException {
        Objects.requireNonNull(inputs);
        Objects.requireNonNull(exts);

        List<Path> kept = new ArrayList<>();
        Counter c = new Counter();

        for (Path input : inputs) {
            if (!Files.exists(input)) continue;

            if (Files.isDirectory(input)) {
                Files.walkFileTree(input, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        handleFile(file, exts, maxBytes, kept, c);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } else {
                handleFile(input, exts, maxBytes, kept, c);
            }
        }

        return new ScanResult(kept, c.discovered, c.skippedByExt, c.skippedTooLarge, c.skippedNotRegular);
    }

    private static void handleFile(Path file, Set<String> exts, long maxBytes, List<Path> kept, Counter c) throws IOException {
        c.discovered++;

        if (!Files.isRegularFile(file)) {
            c.skippedNotRegular++;
            return;
        }

        String ext = getExt(file.getFileName().toString());
        if (!exts.contains(ext)) {
            c.skippedByExt++;
            return;
        }

        long size = Files.size(file);
        if (size > maxBytes) {
            c.skippedTooLarge++;
            return;
        }

        kept.add(file);
    }

    private static String getExt(String name) {
        int idx = name.lastIndexOf('.');
        if (idx < 0 || idx == name.length() - 1) return "";
        return name.substring(idx + 1).toLowerCase(Locale.ROOT);
    }

    private static final class Counter {
        int discovered = 0;
        int skippedByExt = 0;
        int skippedTooLarge = 0;
        int skippedNotRegular = 0;
    }
}