package library;

/**
 * 「书名 + 作者」总是一起出现 —— 说明它们本来就是一个概念。
 * 把散装参数收进一个类之后，7 个方法的参数列表都缩成了 1 个。
 */
public class BookRef {

    private final String title;
    private final String author;

    public BookRef(String title, String author) {
        this.title = title;
        this.author = author;
    }

    public boolean matches(String t, String a) {
        return title.equals(t) && author.equals(a);
    }

    public String label() {
        return title + " / " + author;
    }
}
