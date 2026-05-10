package org.example;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.WaitUntilState;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class VkSessionClient {
    private static final DateTimeFormatter LOG_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final Path sessionFile;

    public VkSessionClient(Path sessionFile) {
        this.sessionFile = sessionFile;
    }

    public void login() throws IOException {
        Path parent = sessionFile.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (Playwright playwright = Playwright.create()) {
            try (Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(false)
            ); BrowserContext context = browser.newContext()) {
                Page page = getOrCreatePage(context);
                page.navigate("https://vk.com/");
                page.bringToFront();

                System.out.println("A Chrome window is open.");
                System.out.println("Log in to VK in that window, then press Enter here to save the session.");
                System.in.read();

                context.storageState(new BrowserContext.StorageStateOptions().setPath(sessionFile));
            }
        }

        System.out.println("Session saved in: " + sessionFile.toAbsolutePath());
    }

    public List<WallPost> readWall(String owner, String query, int count) throws IOException {
        if (!Files.exists(sessionFile)) {
            throw new IllegalArgumentException("Session file is missing. Run session-login first.");
        }

        try (Playwright playwright = Playwright.create()) {
            try (Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(true)
            ); BrowserContext context = browser.newContext(
                    new Browser.NewContextOptions().setStorageStatePath(sessionFile)
            )) {
                Page page = getOrCreatePage(context);
                page.navigate(buildWallUrl(owner), new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
                page.waitForTimeout(2_000);

                if (looksUnauthenticated(page)) {
                    throw new IllegalArgumentException("VK session is missing or expired. Run session-login again.");
                }

                List<WallPost> posts = collectPosts(page, count);
                if (query == null) {
                    return posts;
                }

                String normalizedQuery = query.toLowerCase();
                return posts.stream()
                        .filter(post -> post.text().toLowerCase().contains(normalizedQuery))
                        .toList();
            }
        }
    }

    public void runOnlineHeartbeat(int intervalSeconds, String target) throws IOException, InterruptedException {
        if (!Files.exists(sessionFile)) {
            throw new IllegalArgumentException("Session file is missing. Run session-login first.");
        }

        String normalizedTarget = target == null ? "feed" : target;
        String targetUrl = buildKeepAliveUrl(normalizedTarget);

        try (Playwright playwright = Playwright.create()) {
            try (Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(true)
            ); BrowserContext context = browser.newContext(
                    new Browser.NewContextOptions().setStorageStatePath(sessionFile)
            )) {
                Page page = getOrCreatePage(context);
                page.navigate(targetUrl, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
                page.waitForTimeout(2_000);

                if (looksUnauthenticated(page)) {
                    throw new IllegalArgumentException("VK session is missing or expired. Run session-login again.");
                }

                System.out.println(logPrefix() + " online-heartbeat started");
                System.out.println(logPrefix() + " target: " + targetUrl);
                System.out.println(logPrefix() + " interval seconds: " + intervalSeconds);
                System.out.println(logPrefix() + " stop with Ctrl+C");

                int cycle = 1;
                while (true) {
                    performHeartbeat(page, targetUrl, cycle);
                    System.out.println(logPrefix() + " heartbeat cycle " + cycle + " completed");
                    cycle++;
                    Thread.sleep(intervalSeconds * 1_000L);
                }
            }
        }
    }

    private List<WallPost> collectPosts(Page page, int count) {
        List<WallPost> posts = new ArrayList<>();

        for (int attempt = 0; attempt < 5 && posts.size() < count; attempt++) {
            posts = extractVisiblePosts(page, count);
            if (posts.size() >= count) {
                break;
            }

            page.evaluate("window.scrollBy(0, document.body.scrollHeight)");
            page.waitForTimeout(1_500);
        }

        if (posts.size() > count) {
            return posts.subList(0, count);
        }
        return posts;
    }

    private void performHeartbeat(Page page, String targetUrl, int cycle) {
        if (cycle % 5 == 1 && !page.url().startsWith(targetUrl)) {
            page.navigate(targetUrl, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
            page.waitForTimeout(1_500);
        } else {
            page.reload(new Page.ReloadOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
            page.waitForTimeout(1_500);
        }

        if (looksUnauthenticated(page)) {
            throw new IllegalArgumentException("VK session expired during online-heartbeat. Run session-login again.");
        }

        page.mouse().move(200, 220);
        page.mouse().wheel(0, 700);
        page.waitForTimeout(1_000);
        page.mouse().move(320, 460);
        page.mouse().wheel(0, -450);
        page.waitForTimeout(1_000);

        page.evaluate(
                """
                () => {
                  window.scrollTo({ top: document.body.scrollHeight, behavior: "instant" });
                  document.dispatchEvent(new MouseEvent("mousemove", { bubbles: true, clientX: 320, clientY: 400 }));
                  document.dispatchEvent(new Event("visibilitychange"));
                  window.scrollTo({ top: 0, behavior: "instant" });
                }
                """
        );
        page.waitForTimeout(1_000);
    }

    @SuppressWarnings("unchecked")
    private List<WallPost> extractVisiblePosts(Page page, int count) {
        Object raw = page.evaluate(
                """
                maxCount => {
                  const results = [];
                  const seen = new Set();
                  const anchors = Array.from(document.querySelectorAll("a[href*='wall']"));

                  for (const anchor of anchors) {
                    const href = anchor.getAttribute("href") || "";
                    const match = href.match(/\\/wall(-?\\d+_\\d+)/);
                    if (!match || seen.has(match[1])) {
                      continue;
                    }

                    let node = anchor;
                    let bestText = "";
                    for (let i = 0; i < 6 && node; i++) {
                      node = node.parentElement;
                      const candidate = (node?.innerText || "").trim();
                      if (candidate.length > bestText.length) {
                        bestText = candidate;
                      }
                    }

                    if (bestText.trim().length > 0) {
                      seen.add(match[1]);
                      results.push({
                        id: match[1],
                        link: new URL(href, location.origin).toString(),
                        text: bestText
                      });
                    }

                    if (results.length >= maxCount) {
                      break;
                    }
                  }

                  return results;
                }
                """,
                count
        );

        List<Map<String, Object>> rows = (List<Map<String, Object>>) raw;
        List<WallPost> posts = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            posts.add(new WallPost(
                    String.valueOf(row.get("id")),
                    String.valueOf(row.get("link")),
                    String.valueOf(row.get("text"))
            ));
        }
        return posts;
    }

    private boolean looksUnauthenticated(Page page) {
        Locator passwordInput = page.locator("input[type='password']");
        Locator loginButton = page.locator("button:has-text('Войти')");
        return passwordInput.count() > 0 || loginButton.count() > 0 || page.url().contains("login");
    }

    private String buildWallUrl(String owner) {
        if (owner.startsWith("http://") || owner.startsWith("https://")) {
            return owner;
        }
        if (owner.startsWith("wall")) {
            return "https://vk.com/" + owner;
        }
        return "https://vk.com/" + owner;
    }

    private String buildKeepAliveUrl(String target) {
        if (target.startsWith("http://") || target.startsWith("https://")) {
            return target;
        }
        if (target.isBlank()) {
            return "https://vk.com/feed";
        }
        return "https://vk.com/" + target;
    }

    private Page getOrCreatePage(BrowserContext context) {
        if (context.pages().isEmpty()) {
            return context.newPage();
        }
        return context.pages().get(0);
    }

    private String logPrefix() {
        return "[" + LOG_TIME_FORMATTER.format(LocalDateTime.now()) + "]";
    }
}
