package library;

/** 按作者搜索 */
public class AuthorSearch implements SearchStrategy {

    @Override
    public String label() {
        return "按作者";
    }

    @Override
    public boolean test(Book book, String keyword) {
        return book.authorContains(keyword);
    }
}
