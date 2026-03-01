package io.github.kaiq22unc.localrag.core.indexer;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class UTF8TextReader {

    public static final class ReadResult {
        public final Path path;
        public final long modifiedMillis;
        public final String contents;

        public ReadResult(Path path, long modifiedMillis, String contents) {
            this.path = path;
            this.modifiedMillis = modifiedMillis;
            this.contents = contents;
        }
    }

    public ReadResult read(Path file) {
        try {
            String text = Files.readString(file, StandardCharsets.UTF_8);
            long modified = Files.getLastModifiedTime(file).toMillis();
            return new ReadResult(file, modified, text);
        } catch (CharacterCodingException e) {
            // Not UTF-8 / likely binary
            return null;
        } catch (Exception e) {
            // unreadable / permission / transient errors
            return null;
        }
    }
}
