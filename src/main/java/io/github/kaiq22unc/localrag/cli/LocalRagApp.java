package io.github.kaiq22unc.localrag.cli;

import picocli.CommandLine;

@CommandLine.Command(
        name = "localrag",
        mixinStandardHelpOptions = true,
        description = "Local file indexing + search (RAG later). Run with --help for options.",
         subcommands = { IndexCommand.class, QueryCommand.class }
)
public class LocalRagApp implements Runnable {
    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new LocalRagApp()).execute(args);
        System.exit(exitCode);
    }
}
