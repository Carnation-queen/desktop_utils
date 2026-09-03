package changcun.desktop_utils.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NovelBookTest {

    @Test
    void defaultsAreEmpty() {
        NovelBook book = new NovelBook();
        assertEquals("", book.getId());
        assertEquals("", book.getTitle());
        assertEquals(0, book.getCharCount());
        assertEquals(0, book.getProgressChar());
    }

    @Test
    void progressPercentComputesRatio() {
        NovelBook book = new NovelBook();
        book.setCharCount(1000);
        book.setProgressChar(250);
        assertEquals(25.0, book.progressPercent(), 0.0001);
    }

    @Test
    void progressNeverNegative() {
        NovelBook book = new NovelBook();
        book.setCharCount(100);
        book.setProgressChar(-5);
        assertEquals(0, book.getProgressChar());
    }
}
