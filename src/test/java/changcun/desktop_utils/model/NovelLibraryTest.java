package changcun.desktop_utils.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NovelLibraryTest {

    @Test
    void addFindRemove() {
        NovelLibrary library = new NovelLibrary();
        assertEquals(0, library.size());

        NovelBook a = new NovelBook();
        a.setId("a");
        a.setTitle("书 A");
        NovelBook b = new NovelBook();
        b.setId("b");
        b.setTitle("书 B");
        library.getBooks().add(a);
        library.getBooks().add(b);

        assertEquals(2, library.size());
        assertEquals(a, library.findById("a"));
        assertNull(library.findById("missing"));
        assertTrue(library.removeById("a"));
        assertFalse(library.removeById("a"));
        assertEquals(1, library.size());
    }
}
