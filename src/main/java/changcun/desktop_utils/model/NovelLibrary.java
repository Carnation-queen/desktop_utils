package changcun.desktop_utils.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 本地小说书库（书目清单）。
 */
public class NovelLibrary {

    private List<NovelBook> books = new ArrayList<>();

    public List<NovelBook> getBooks() {
        return books;
    }

    public void setBooks(List<NovelBook> books) {
        this.books = books != null ? books : new ArrayList<>();
    }

    public NovelBook findById(String id) {
        for (NovelBook book : books) {
            if (book.getId().equals(id)) {
                return book;
            }
        }
        return null;
    }

    public boolean removeById(String id) {
        return books.removeIf(book -> book.getId().equals(id));
    }

    public int size() {
        return books.size();
    }
}
