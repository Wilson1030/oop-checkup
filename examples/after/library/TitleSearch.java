package library;

/** 按书名搜索 */
public class TitleSearch implements SearchStrategy {

    @Override
    public String label() {
        return "按书名";
    }

    @Override
    public boolean test(Book book, String keyword) {
        return book.titleContains(keyword);
    }
}
