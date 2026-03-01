package io.github.kaiq22unc.localrag.cli;

import picocli.CommandLine;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "query",
        mixinStandardHelpOptions = true,
        description = "Query the localrag index (stub for now)."
)
public class QueryCommand implements Callable<Integer> {

    @CommandLine.Option(
            names = "--index-dir",
            defaultValue = ".localrag/index",
            description = "Directory where the index lives"
    )
    Path indexDir;

    @CommandLine.Option(
            names = "--topk",
            defaultValue = "10",
            description = "Number of results to show"
    )
    int topK;

    @CommandLine.Parameters(
            paramLabel = "QUERY",
            description = "Query text (wrap in quotes)"
    )
    String queryText;

    @Override
    public Integer call() {
        System.out.println("query called (stub)");
        System.out.println("indexDir=" + indexDir);
        System.out.println("topK=" + topK);
        System.out.println("queryText=" + queryText);
        return 0;
    }
}
