package library;

/** 按 ISBN 搜索 */
public class IsbnSearch implements SearchStrategy {

    @Override
    public String label() {
        return "按 ISBN";
    }

    @Override
    public boolean test(Book book, String keyword) {
        return book.hasIsbn(keyword);
    }
}
