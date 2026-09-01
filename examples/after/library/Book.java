package library;

/**
 * 重构后 —— 数据与操作数据的行为在同一个地方。
 * 字段私有，通过方法表达意图，而不是暴露 setter 让外部随意改。
 */
public class Book {

    private final String title;
    private final String author;
    private final String isbn;
    private final int year;
    private boolean borrowed;

    public Book(String title, String author, String isbn, int year) {
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.year = year;
        this.borrowed = false;
    }

    /** 行为长在数据旁边：不再需要把字段传出去 */
    public boolean isAvailable() {
        return !borrowed;
    }

    public void lend() {
        if (borrowed) {
            throw new IllegalStateException("该书已借出");
        }
        borrowed = true;
    }

    public void giveBack() {
        if (!borrowed) {
            throw new IllegalStateException("该书未借出");
        }
        borrowed = false;
    }

    public boolean is(BookRef ref) {
        return ref.matches(title, author);
    }

    public boolean titleContains(String kw) {
        return title.contains(kw);
    }

    public boolean authorContains(String kw) {
        return author.contains(kw);
    }

    public boolean hasIsbn(String value) {
        return isbn.equals(value);
    }

    public String describe() {
        return title + " / " + author + " / " + year
                + (borrowed ? " [已借出]" : " [在架]");
    }

    public String shortLabel() {
        return title + " / " + author;
    }
}
