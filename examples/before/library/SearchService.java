package library;

import java.util.ArrayList;
import java.util.List;

/** 演示用代码 —— 见 Book.java 说明 */
public class SearchService {

    static int searchCount = 0;

    public static List<Book> search(SearchMode mode, String keyword) {
        searchCount++;
        List<Book> result = new ArrayList<>();
        for (Book b : LibraryService.allBooks) {
            // 类型分派第 1 处
            switch (mode) {
                case ByTitle:
                    if (b.title.contains(keyword)) result.add(b);
                    break;
                case ByAuthor:
                    if (b.author.contains(keyword)) result.add(b);
                    break;
                case ByIsbn:
                    if (b.getIsbn().equals(keyword)) result.add(b);
                    break;
            }
        }
        return result;
    }

    public static String label(SearchMode mode) {
        // 类型分派第 2 处 —— 新增一种模式要同时改这两处
        switch (mode) {
            case ByTitle:
                return "按书名";
            case ByAuthor:
                return "按作者";
            case ByIsbn:
                return "按 ISBN";
        }
        return "未知";
    }

    public static boolean matches(String title, String author, SearchMode mode, String kw) {
        switch (mode) {
            case ByTitle:
                return title.contains(kw);
            case ByAuthor:
                return author.contains(kw);
            default:
                return false;
        }
    }
}
