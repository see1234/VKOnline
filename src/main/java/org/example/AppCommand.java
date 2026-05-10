package org.example;

public record AppCommand(Mode mode, String owner, String query, int count) {
    private static final int DEFAULT_COUNT = 10;
    private static final int MAX_COUNT = 50;

    public static AppCommand parse(String[] args) {
        if (args.length == 0) {
            throw new IllegalArgumentException("Command is required.");
        }

        String command = args[0];
        return switch (command) {
            case "session-login" -> new AppCommand(Mode.SESSION_LOGIN, null, null, 0);
            case "session-check" -> parseSessionCheck(args);
            default -> throw new IllegalArgumentException("Unknown command: " + command);
        };
    }

    private static AppCommand parseSessionCheck(String[] args) {
        if (args.length < 2) {
            throw new IllegalArgumentException("session-check requires a VK domain or wall path.");
        }

        String owner = args[1];
        String query = args.length > 2 ? blankToNull(args[2]) : null;
        int count = args.length > 3 ? parseCount(args[3]) : DEFAULT_COUNT;

        return new AppCommand(Mode.SESSION_CHECK, owner, query, count);
    }

    private static int parseCount(String rawCount) {
        try {
            int parsed = Integer.parseInt(rawCount);
            if (parsed < 1 || parsed > MAX_COUNT) {
                throw new IllegalArgumentException("Count must be between 1 and " + MAX_COUNT + ".");
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Count must be a number.", e);
        }
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
