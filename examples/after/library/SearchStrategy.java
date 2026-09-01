package library;

/**
 * 用多态替代 switch —— 新增一种搜索方式只需加一个类，
 * 不必再去修改任何已有的分派点。
 */
public interface SearchStrategy {

    String label();

    boolean test(Book book, String keyword);
}
