package changcun.desktop_utils.service;

import changcun.desktop_utils.model.NovelBook;
import changcun.desktop_utils.model.NovelChapter;
import changcun.desktop_utils.model.NovelLibrary;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 小说书库的本地持久化与读取。
 * <p>
 * 目录结构（位于用户主目录下）：
 * <pre>
 * ~/.desktop_utils/novels/
 *   ├── library.json          # 书目清单与阅读进度（Gson）
 *   └── books/&lt;id&gt;.txt       # 每本书导入时的原始内容副本
 * </pre>
 * 书籍内容按原样复制保存，读取时做 UTF-8 / GB18030 自适应解码；
 * 由于解码规则确定，记录在 {code progressChar} 中的字符偏移重启后依然有效，
 * 从而支持“下次进入自动跳转到上次进度”。
 */
public class NovelStore {

    private static final String CONFIG_DIR = ".desktop_utils";
    private static final String NOVELS_DIR_NAME = "novels";
    private static final String LIBRARY_FILE = "library.json";
    private static final String BOOKS_DIR_NAME = "books";

    /** 常见章节标题前缀：第X章/卷/节/回、楔子、序章、番外等。 */
    private static final Pattern CHAPTER_PATTERN = Pattern.compile(
            "^(第\\s*[0-9０-９零一二三四五六七八九十百千万两〇]+\\s*[章节回卷部集篇话]"
                    + "|楔子|序章|序言|序|前言|引子|引言|引|尾声|终章|后记|大结局|完结篇|番外)"
                    + "[\\s　:：、.．·—\\-]*");

    private final Path baseDir;
    private final Path libraryFile;
    private final Path booksDir;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public NovelStore() {
        this(Paths.get(System.getProperty("user.home"), CONFIG_DIR, NOVELS_DIR_NAME));
    }

    public NovelStore(Path baseDir) {
        this.baseDir = baseDir;
        this.libraryFile = baseDir.resolve(LIBRARY_FILE);
        this.booksDir = baseDir.resolve(BOOKS_DIR_NAME);
    }

    public Path getBaseDir() {
        return baseDir;
    }

    // ---------------------------------------------------------------------
    // 书库元数据
    // ---------------------------------------------------------------------

    /** 返回全部书目（按加入时间倒序，最新在前）。 */
    public synchronized List<NovelBook> listBooks() {
        List<NovelBook> books = new ArrayList<>(loadLibrary().getBooks());
        books.sort((a, b) -> Long.compare(b.getAddedAt(), a.getAddedAt()));
        return Collections.unmodifiableList(books);
    }

    public synchronized NovelBook findById(String id) {
        return loadLibrary().findById(id);
    }

    private NovelLibrary loadLibrary() {
        NovelLibrary library = new NovelLibrary();
        if (!Files.exists(libraryFile)) {
            return library;
        }
        try (Reader reader = Files.newBufferedReader(libraryFile, StandardCharsets.UTF_8)) {
            NovelLibrary parsed = gson.fromJson(reader, NovelLibrary.class);
            if (parsed != null) {
                if (parsed.getBooks() == null) {
                    parsed.setBooks(new ArrayList<>());
                }
                return parsed;
            }
        } catch (IOException | RuntimeException ignored) {
            // 读取或解析失败时回退为空书库，不影响使用。
        }
        return library;
    }

    private void saveLibrary(NovelLibrary library) {
        try {
            Files.createDirectories(baseDir);
            Files.writeString(libraryFile, gson.toJson(library), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ---------------------------------------------------------------------
    // 导入 / 改名 / 删除 / 进度
    // ---------------------------------------------------------------------

    /** 导入一个 TXT 小说文件到书库，返回新书目；标题为空时取文件名（去扩展名）。 */
    public synchronized NovelBook importBook(String title, Path sourceFile) throws IOException {
        if (sourceFile == null || !Files.isRegularFile(sourceFile)) {
            throw new IOException("导入失败：文件不存在。");
        }
        byte[] data = Files.readAllBytes(sourceFile);
        String content = decodeContent(data);
        if (content.isBlank()) {
            throw new IOException("导入失败：文件内容为空。");
        }

        NovelBook book = new NovelBook();
        book.setId(UUID.randomUUID().toString().replace("-", ""));
        book.setTitle(title == null || title.isBlank() ? defaultTitle(sourceFile) : title.trim());
        book.setFileName(sourceFile.getFileName().toString());
        book.setCharCount(content.length());
        book.setProgressChar(0);
        long now = System.currentTimeMillis();
        book.setAddedAt(now);
        book.setUpdatedAt(now);

        Files.createDirectories(booksDir);
        Files.write(contentFile(book), data);

        NovelLibrary library = loadLibrary();
        library.getBooks().add(book);
        saveLibrary(library);
        return book;
    }

    /** 重命名书籍标题。 */
    public synchronized void renameBook(String id, String newTitle) {
        NovelLibrary library = loadLibrary();
        NovelBook book = library.findById(id);
        if (book != null && newTitle != null && !newTitle.isBlank()) {
            book.setTitle(newTitle.trim());
            saveLibrary(library);
        }
    }

    /** 从书库删除书籍并移除其内容文件。 */
    public synchronized void deleteBook(String id) {
        NovelLibrary library = loadLibrary();
        if (library.removeById(id)) {
            saveLibrary(library);
            try {
                Files.deleteIfExists(booksDir.resolve(id + ".txt"));
            } catch (IOException ignored) {
                // 内容文件删除失败不阻塞书库更新。
            }
        }
    }

    /** 保存某本书的阅读进度（字符偏移）。 */
    public synchronized void saveProgress(String id, long charOffset) {
        NovelLibrary library = loadLibrary();
        NovelBook book = library.findById(id);
        if (book != null) {
            book.setProgressChar(Math.max(0, Math.min(charOffset, book.getCharCount())));
            book.setUpdatedAt(System.currentTimeMillis());
            saveLibrary(library);
        }
    }

    /** 读取某本书解码后的全文（供阅读器使用）。 */
    public String readContent(NovelBook book) throws IOException {
        Path file = contentFile(book);
        if (!Files.exists(file)) {
            throw new IOException("内容文件缺失：" + book.getTitle());
        }
        return decodeContent(Files.readAllBytes(file));
    }

    private Path contentFile(NovelBook book) {
        return booksDir.resolve(book.getId() + ".txt");
    }

    private static String defaultTitle(Path sourceFile) {
        String name = sourceFile.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    // ---------------------------------------------------------------------
    // 文本解码与章节检测
    // ---------------------------------------------------------------------

    /**
     * 将字节解码为字符串：优先 UTF-8（含 BOM），失败回退 GB18030（兼容 GBK/GB2312），
     * 兼容 UTF-16 BOM。返回结果永不抛异常。
     */
    public static String decodeContent(byte[] data) {
        if (data == null) {
            return "";
        }
        // BOM 判定
        if (data.length >= 3 && (data[0] & 0xFF) == 0xEF && (data[1] & 0xFF) == 0xBB && (data[2] & 0xFF) == 0xBF) {
            return new String(data, 3, data.length - 3, StandardCharsets.UTF_8);
        }
        if (data.length >= 2 && (data[0] & 0xFF) == 0xFE && (data[1] & 0xFF) == 0xFF) {
            return new String(data, 2, data.length - 2, StandardCharsets.UTF_16BE);
        }
        if (data.length >= 2 && (data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xFE) {
            return new String(data, 2, data.length - 2, StandardCharsets.UTF_16LE);
        }
        // 严格 UTF-8 尝试
        try {
            CharsetDecoder utf8 = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT);
            return utf8.decode(ByteBuffer.wrap(data)).toString();
        } catch (CharacterCodingException ignored) {
            // 非 UTF-8，按 GB18030 解码（GBK/GB2312 的超集）
        }
        try {
            return new String(data, Charset.forName("GB18030"));
        } catch (RuntimeException ignored) {
            return new String(data, StandardCharsets.UTF_8);
        }
    }

    /** 从解码后的全文扫描章节标题，返回目录条目（按出现顺序）。 */
    public static List<NovelChapter> detectChapters(String content) {
        if (content == null || content.isEmpty()) {
            return Collections.emptyList();
        }
        List<NovelChapter> chapters = new ArrayList<>();
        int lineStart = 0;
        int i = 0;
        int n = content.length();
        while (i < n) {
            int lineEnd = content.indexOf('\n', i);
            if (lineEnd < 0) {
                lineEnd = n;
            }
            String line = content.substring(lineStart, lineEnd);
            String trimmed = line.trim();
            if (isChapterHeading(trimmed)) {
                chapters.add(new NovelChapter(trimmed, lineStart));
            }
            if (lineEnd >= n) {
                break;
            }
            i = lineEnd + 1;
            lineStart = i;
        }
        return chapters;
    }

    private static boolean isChapterHeading(String line) {
        if (line.isEmpty() || line.length() > 60) {
            return false;
        }
        return CHAPTER_PATTERN.matcher(line).find();
    }
}
