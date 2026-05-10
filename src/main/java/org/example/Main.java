package org.example;

import java.nio.file.Path;
import java.util.List;

public class Main {
    private static final Path DEFAULT_SESSION_FILE = Path.of("vk-session.json");

    public static void main(String[] args) throws Exception {
        try {
            AppCommand command = AppCommand.parse(args);
            VkSessionClient sessionClient = new VkSessionClient(resolveSessionFile());

            switch (command.mode()) {
                case SESSION_LOGIN -> sessionClient.login();
                case SESSION_CHECK -> {
                    List<WallPost> posts = sessionClient.readWall(command.owner(), command.query(), command.count());
                    ConsolePrinter.printPosts(posts, command.query());
                }
            }
        } catch (IllegalArgumentException e) {
            printUsage(e.getMessage());
        }
    }

    private static void printUsage(String errorMessage) {
        System.err.println(errorMessage);
        System.err.println();
        System.err.println("Usage:");
        System.err.println("  ./gradlew run --args=\"session-login\"");
        System.err.println("  ./gradlew run --args=\"session-check <domain-or-wall-path> [query] [count]\"");
        System.err.println();
        System.err.println("Examples:");
        System.err.println("  ./gradlew run --args=\"session-login\"");
        System.err.println("  ./gradlew run --args=\"session-check durov java 10\"");
        System.err.println("  ./gradlew run --args=\"session-check wall-1 news 20\"");
        System.err.println();
        System.err.println("Notes:");
        System.err.println("  - session-login opens Chromium and saves the VK session in vk-session.json.");
        System.err.println("  - session-check reuses the saved VK session file without VK token.");
        System.err.println("  - Optional env: VK_SESSION_FILE=/path/to/vk-session.json");
        System.err.println("  - If Playwright browsers are missing, run the install command from the final instructions.");
    }

    private static Path resolveSessionFile() {
        String configuredPath = System.getenv("VK_SESSION_FILE");
        if (configuredPath == null || configuredPath.isBlank()) {
            return DEFAULT_SESSION_FILE;
        }
        return Path.of(configuredPath);
    }
}
