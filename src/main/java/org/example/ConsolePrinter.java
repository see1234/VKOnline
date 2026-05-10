package org.example;

import java.util.List;

public class ConsolePrinter {
    public static void printPosts(List<WallPost> posts, String query) {
        if (posts.isEmpty()) {
            if (query == null) {
                System.out.println("No posts found.");
            } else {
                System.out.println("No posts matched query: " + query);
            }
            return;
        }

        System.out.println("Found posts: " + posts.size());
        for (WallPost post : posts) {
            System.out.println();
            System.out.println("Post: " + post.id());
            System.out.println("Link: " + post.link());
            System.out.println("Text: " + shorten(post.text()));
        }
    }

    private static String shorten(String text) {
        String sanitized = text == null ? "" : text.replaceAll("\\s+", " ").trim();
        if (sanitized.length() <= 220) {
            return sanitized;
        }
        return sanitized.substring(0, 217) + "...";
    }
}
