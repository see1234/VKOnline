package org.example;

public record AppCommand(Mode mode, String owner, String query, int count, int intervalSeconds) {
    private static final int DEFAULT_COUNT = 10;
    private static final int MAX_COUNT = 50;
    private static final int DEFAULT_INTERVAL_SECONDS = 240;
    private static final int MIN_INTERVAL_SECONDS = 30;

    public static AppCommand parse(String[] args) {
        if (args.length == 0) {
            throw new IllegalArgumentException("Command is required.");
        }

        String command = args[0];
        return switch (command) {
            case "session-login" -> new AppCommand(Mode.SESSION_LOGIN, null, null, 0, 0);
            case "session-check" -> parseSessionCheck(args);
            case "online-heartbeat" -> parseOnlineHeartbeat(args);
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

        return new AppCommand(Mode.SESSION_CHECK, owner, query, count, 0);
    }

    private static AppCommand parseOnlineHeartbeat(String[] args) {
        int intervalSeconds = args.length > 1 ? parseIntervalSeconds(args[1]) : DEFAULT_INTERVAL_SECONDS;
        String owner = args.length > 2 ? blankToNull(args[2]) : "feed";
        return new AppCommand(Mode.ONLINE_HEARTBEAT, owner, null, 0, intervalSeconds);
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

    private static int parseIntervalSeconds(String rawValue) {
        try {
            int parsed = Integer.parseInt(rawValue);
            if (parsed < MIN_INTERVAL_SECONDS) {
                throw new IllegalArgumentException("Heartbeat interval must be at least " + MIN_INTERVAL_SECONDS + " seconds.");
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Heartbeat interval must be a number.", e);
        }
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
