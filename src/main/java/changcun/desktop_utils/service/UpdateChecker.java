package changcun.desktop_utils.service;

import changcun.desktop_utils.model.AppSettings;
import changcun.desktop_utils.model.UpdateCheckResult;
import changcun.desktop_utils.model.UpdateInfo;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.DoubleConsumer;

/**
 * 更新检查与下载：读取 GitHub Releases 最新版本接口，
 * 与当前版本比较，并支持下载更新资产。
 */
public class UpdateChecker {

    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 10_000;
    private static final int DOWNLOAD_READ_TIMEOUT_MS = 60_000;

    private final String updateUrl;

    public UpdateChecker(String updateUrl) {
        this.updateUrl = updateUrl == null || updateUrl.isBlank()
                ? AppSettings.DEFAULT_UPDATE_URL
                : updateUrl.trim();
    }

    public String getUpdateUrl() {
        return updateUrl;
    }

    /** 同步执行一次更新检查（需在后台线程调用，避免阻塞界面）。 */
    public UpdateCheckResult checkForUpdate() {
        try {
            String json = fetchJson();
            UpdateInfo latest = parseRelease(json);
            if (latest == null) {
                return UpdateCheckResult.error("无法解析更新接口返回的内容");
            }
            String current = AppVersion.current();
            if (AppVersion.compare(latest.getVersion(), current) > 0) {
                return UpdateCheckResult.updateAvailable(current, latest);
            }
            return UpdateCheckResult.upToDate(current, latest);
        } catch (IOException e) {
            String message = e.getMessage();
            return UpdateCheckResult.error(message == null || message.isBlank()
                    ? "网络连接失败，请检查网络或更新源地址" : message);
        } catch (RuntimeException e) {
            return UpdateCheckResult.error("检查更新失败：" + e.getMessage());
        }
    }

    /**
     * 下载更新文件到目标目录，返回下载后的文件路径。
     *
     * @param info     最新版本信息
     * @param targetDir 保存目录
     * @param progress 下载进度回调（0~100），可为 null
     */
    public Path download(UpdateInfo info, Path targetDir, DoubleConsumer progress) throws IOException {
        String url = info.getDownloadUrl();
        if (url == null || url.isBlank()) {
            throw new IOException("该版本没有提供可直接下载的文件");
        }

        Files.createDirectories(targetDir);
        Path target = targetDir.resolve(fileNameFromUrl(url, info.getVersion()));

        HttpURLConnection conn = (HttpURLConnection) toUrl(url).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(DOWNLOAD_READ_TIMEOUT_MS);
        conn.setRequestProperty("User-Agent", "desktop-utils");
        conn.setInstanceFollowRedirects(true);

        int code = conn.getResponseCode();
        if (code < 200 || code >= 300) {
            conn.disconnect();
            throw new IOException("下载失败：HTTP " + code);
        }

        long total = conn.getContentLengthLong();
        long downloaded = 0;
        try (InputStream in = conn.getInputStream();
             OutputStream out = Files.newOutputStream(target)) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
                downloaded += n;
                if (progress != null && total > 0) {
                    progress.accept(Math.min(100.0, downloaded * 100.0 / total));
                }
            }
        } finally {
            conn.disconnect();
        }
        return target;
    }

    private String fetchJson() throws IOException {
        HttpURLConnection conn = (HttpURLConnection) toUrl(updateUrl).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        conn.setRequestProperty("Accept", "application/vnd.github+json");
        conn.setRequestProperty("User-Agent", "desktop-utils");
        conn.setInstanceFollowRedirects(true);

        int code = conn.getResponseCode();
        try {
            if (code == HttpURLConnection.HTTP_NOT_FOUND) {
                throw new IOException("尚未发布任何版本（HTTP 404）");
            }
            InputStream in = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
            if (in == null) {
                throw new IOException("服务器返回 HTTP " + code);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } finally {
            conn.disconnect();
        }
    }

    private UpdateInfo parseRelease(String json) {
        JsonObject root;
        try {
            root = JsonParser.parseString(json).getAsJsonObject();
        } catch (RuntimeException e) {
            return null;
        }

        String tag = text(root, "tag_name");
        if (tag == null || tag.isBlank()) {
            return null;
        }
        String version = tag.trim().replaceFirst("^[vV]", "");
        if (version.isBlank()) {
            return null;
        }

        UpdateInfo info = new UpdateInfo();
        info.setVersion(version);
        info.setPageUrl(text(root, "html_url"));
        info.setNotes(text(root, "body"));

        JsonElement assetsEl = root.get("assets");
        if (assetsEl != null && assetsEl.isJsonArray()) {
            JsonArray assets = assetsEl.getAsJsonArray();
            for (JsonElement el : assets) {
                if (!el.isJsonObject()) {
                    continue;
                }
                JsonObject asset = el.getAsJsonObject();
                String url = text(asset, "browser_download_url");
                if (url != null && !url.isBlank()) {
                    info.setDownloadUrl(url);
                    break;
                }
            }
        }
        return info;
    }

    private static String text(JsonObject obj, String key) {
        if (obj == null) {
            return null;
        }
        JsonElement el = obj.get(key);
        if (el == null || el.isJsonNull() || !el.isJsonPrimitive()) {
            return null;
        }
        return el.getAsString();
    }

    private static String fileNameFromUrl(String url, String version) {
        String name = url.substring(url.lastIndexOf('/') + 1);
        int query = name.indexOf('?');
        if (query >= 0) {
            name = name.substring(0, query);
        }
        name = URLDecoder.decode(name, StandardCharsets.UTF_8).trim();
        if (name.isEmpty() || !name.contains(".")) {
            name = "desktop_utils-" + version + ".exe";
        }
        return name;
    }

    private static URL toUrl(String url) throws IOException {
        try {
            return URI.create(url).toURL();
        } catch (IllegalArgumentException e) {
            throw new IOException("更新源地址无效：" + url);
        }
    }
}
