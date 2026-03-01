package io.github.kaiq22unc.localrag.cli;

import io.github.kaiq22unc.localrag.core.indexer.FileScanner;
import io.github.kaiq22unc.localrag.core.indexer.UTF8TextReader;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "index",
        mixinStandardHelpOptions = true,
        description = "Index one or more paths into the localrag index (stub for now)."
)
public class IndexCommand implements Callable<Integer> {

    @CommandLine.Option(
            names = "--index-dir",
            defaultValue = ".localrag/index",
            description = "Where to store the index"
    )
    Path indexDir;

    @CommandLine.Option(
            names = "--ext",
            defaultValue = "md,txt,java,kt,go,py,c,cpp,h,hpp,xml,yml,yaml,json,sql",
            description = "Comma-separated file extensions to include"
    )
    String extCsv;

    @CommandLine.Option(
            names = "--max-bytes",
            defaultValue = "2000000",
            description = "Skip files larger than this many bytes"
    )
    long maxBytes;

    @CommandLine.Parameters(
            arity = "1..*",
            paramLabel = "PATH",
            description = "Files or directories to index"
    )
    List<Path> inputs;

    @Override
    public Integer call() throws IOException {
        var exts = java.util.Arrays.stream(extCsv.split(","))
                .map(s -> s.trim().toLowerCase(java.util.Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .collect(java.util.stream.Collectors.toSet());

        var scanner = new FileScanner();
        var scan = scanner.scan(inputs, exts, maxBytes);

        var reader = new UTF8TextReader();

        int readOk = 0;
        int readSkipped = 0;

        for (var f : scan.files) {
            if (reader.read(f) != null) readOk++;
            else readSkipped++;
        }

        System.out.println("=== Scan summary ===");
        System.out.println("discovered=" + scan.discovered);
        System.out.println("kept(after filters)=" + scan.files.size());
        System.out.println("skippedByExt=" + scan.skippedByExt);
        System.out.println("skippedTooLarge=" + scan.skippedTooLarge);
        System.out.println("skippedNotRegular=" + scan.skippedNotRegular);

        System.out.println("=== Read summary (UTF-8 only) ===");
        System.out.println("readOk=" + readOk);
        System.out.println("readSkipped=" + readSkipped);
//        System.out.println(reader.read(scan.files.get(0)).contents);

        return 0;
    }
}
