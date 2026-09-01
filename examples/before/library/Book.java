package library;

/**
 * 演示用代码 —— 刻意写成「用 Java 写 C」的形态。
 * 仅用于 README 展示工具输出，不参与任何验证。
 */
public class Book {

    // 直接暴露的可变字段（C 的 struct 习惯）
    public String title;
    public String author;

    private String isbn;
    private int year;
    private boolean borrowed;

    public Book() {
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public boolean isBorrowed() {
        return borrowed;
    }

    public void setBorrowed(boolean borrowed) {
        this.borrowed = borrowed;
    }
}
