package io.github.kaiq22unc.localrag.core.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

public final class ManifestStore {

    public Map<String, Long> load(Path manifestPath) throws IOException {
        Map<String, Long> map = new HashMap<>();
        if (!Files.exists(manifestPath)) return map;

        try (BufferedReader br = Files.newBufferedReader(manifestPath, StandardCharsets.UTF_8)) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] parts = line.split("\t", 2);
                if (parts.length != 2) continue;
                String path = parts[0];
                long modified;
                try { modified = Long.parseLong(parts[1]); }
                catch (NumberFormatException e) { continue; }
                map.put(path, modified);
            }
        }
        return map;
    }

    public void save(Path manifestPath, Map<String, Long> map) throws IOException {
        Files.createDirectories(manifestPath.getParent());
        try (BufferedWriter bw = Files.newBufferedWriter(
                manifestPath, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

            for (var e : map.entrySet()) {
                bw.write(e.getKey());
                bw.write('\t');
                bw.write(Long.toString(e.getValue()));
                bw.newLine();
            }
        }
    }
}