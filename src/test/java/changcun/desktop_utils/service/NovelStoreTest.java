package changcun.desktop_utils.service;

import changcun.desktop_utils.model.NovelBook;
import changcun.desktop_utils.model.NovelChapter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NovelStoreTest {

    @TempDir
    Path tempDir;

    private NovelStore newStore() {
        return new NovelStore(tempDir.resolve("novels"));
    }

    @Test
    void listEmptyWhenNothingImported() {
        assertTrue(newStore().listBooks().isEmpty());
    }

    @Test
    void importUtf8AndReadBack() throws Exception {
        String text = "第一章 开端\n这是正文内容。\n第二行内容……\n";
        Path src = tempDir.resolve("第一本.txt");
        Files.writeString(src, text, StandardCharsets.UTF_8);

        NovelStore store = newStore();
        NovelBook book = store.importBook("我的书", src);

        assertEquals("我的书", book.getTitle());
        assertEquals(text.length(), book.getCharCount());
        assertEquals(0, book.getProgressChar());
        assertEquals(text, store.readContent(book));
        assertEquals(1, store.listBooks().size());
    }

    @Test
    void importDefaultsTitleToFileNameWithoutExtension() throws Exception {
        Path src = tempDir.resolve("凡人修仙传.txt");
        Files.writeString(src, "正文", StandardCharsets.UTF_8);
        NovelBook book = newStore().importBook(null, src);
        assertEquals("凡人修仙传", book.getTitle());
        assertEquals("凡人修仙传.txt", book.getFileName());
    }

    @Test
    void importGb18030ContentIsDecoded() throws Exception {
        String text = "第一章 测试\n我是来自 GBK 编码的中文小说。\n";
        Path src = tempDir.resolve("gbk.txt");
        Files.write(src, text.getBytes(Charset.forName("GBK")));

        NovelStore store = newStore();
        NovelBook book = store.importBook("GBK 书", src);
        assertEquals(text, store.readContent(book));
    }

    @Test
    void importRejectsBlankContent() throws Exception {
        Path src = tempDir.resolve("blank.txt");
        Files.writeString(src, "  \r\n\t ", StandardCharsets.UTF_8);
        org.junit.jupiter.api.Assertions.assertThrows(java.io.IOException.class,
                () -> newStore().importBook(null, src));
    }

    @Test
    void saveProgressAndReloadPersists() throws Exception {
        String text = "第一章\n".repeat(50);
        Path src = tempDir.resolve("book.txt");
        Files.writeString(src, text, StandardCharsets.UTF_8);

        Path base = tempDir.resolve("novels");
        NovelStore store = new NovelStore(base);
        NovelBook book = store.importBook("进度书", src);

        store.saveProgress(book.getId(), 123);
        NovelStore store2 = new NovelStore(base);
        NovelBook reloaded = store2.findById(book.getId());
        assertEquals(123, reloaded.getProgressChar());
    }

    @Test
    void saveProgressClampsToCharCount() throws Exception {
        Path src = tempDir.resolve("book.txt");
        Files.writeString(src, "一二三四五", StandardCharsets.UTF_8);
        NovelStore store = newStore();
        NovelBook book = store.importBook(null, src);

        store.saveProgress(book.getId(), 999999);
        assertEquals(5, store.findById(book.getId()).getProgressChar());
    }

    @Test
    void renameBookUpdatesTitle() throws Exception {
        Path src = tempDir.resolve("old.txt");
        Files.writeString(src, "正文", StandardCharsets.UTF_8);
        NovelStore store = newStore();
        NovelBook book = store.importBook("旧名", src);

        store.renameBook(book.getId(), "新名");
        assertEquals("新名", store.findById(book.getId()).getTitle());
    }

    @Test
    void deleteRemovesBookAndContentFile() throws Exception {
        Path src = tempDir.resolve("gone.txt");
        Files.writeString(src, "正文", StandardCharsets.UTF_8);
        NovelStore store = newStore();
        NovelBook book = store.importBook("待删", src);

        Path contentFile = tempDir.resolve("novels").resolve("books")
                .resolve(book.getId() + ".txt");
        assertTrue(Files.exists(contentFile));

        store.deleteBook(book.getId());
        assertTrue(store.listBooks().isEmpty());
        assertFalse(Files.exists(contentFile));
    }

    @Test
    void decodeContentStripsUtf8Bom() {
        byte[] withBom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF, 'a', 'b', 'c'};
        assertEquals("abc", NovelStore.decodeContent(withBom));
    }

    @Test
    void decodeContentFallsBackForGb18030Bytes() {
        String text = "中文测试";
        byte[] gbkBytes = text.getBytes(Charset.forName("GBK"));
        assertEquals(text, NovelStore.decodeContent(gbkBytes));
    }

    @Test
    void detectChaptersFindsCommonHeadings() {
        String content = "第一章 风起\n正文正文\n"
                + "第2章 雨落\n正文正文\n"
                + "楔子\n开始的话\n"
                + "普通短句不算章节\n"
                + "后记\n感谢阅读\n";
        List<NovelChapter> chapters = NovelStore.detectChapters(content);
        assertEquals(4, chapters.size());
        assertEquals("第一章 风起", chapters.get(0).getTitle());
        assertEquals("第2章 雨落", chapters.get(1).getTitle());
        assertEquals("楔子", chapters.get(2).getTitle());
        assertEquals("后记", chapters.get(3).getTitle());
    }

    @Test
    void detectChaptersSkipsLongDialogueLines() {
        String content = "第一章 开端\n这是一句很长的话，绝对不是章节标题，"
                + "它应该超过六十个字符的长度限制因此不会被误判为目录条目。\n";
        List<NovelChapter> chapters = NovelStore.detectChapters(content);
        assertEquals(1, chapters.size());
        assertEquals(0, chapters.get(0).getStartOffset());
    }
}
