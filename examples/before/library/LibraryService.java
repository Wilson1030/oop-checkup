package library;

import java.util.ArrayList;
import java.util.List;

/** 演示用代码 —— 见 Book.java 说明 */
public class LibraryService {

    // 全局可变状态
    public static List<Book> allBooks = new ArrayList<>();
    public static List<Member> allMembers = new ArrayList<>();
    public static int totalBorrowed = 0;
    static String currentUser = "";

    public static void addBook(String title, String author, String isbn, int year) {
        Book b = new Book();
        b.title = title;
        b.author = author;
        b.setIsbn(isbn);
        b.setYear(year);
        b.setBorrowed(false);
        allBooks.add(b);
    }

    public static Book findBook(String title, String author) {
        for (Book b : allBooks) {
            if (b.title.equals(title) && b.author.equals(author)) {
                return b;
            }
        }
        return null;
    }

    public static boolean isAvailable(String title, String author) {
        Book b = findBook(title, author);
        return b != null && !b.isBorrowed();
    }

    public static String describe(String title, String author) {
        Book b = findBook(title, author);
        if (b == null) {
            return "未找到";
        }
        return b.title + " / " + b.author + " / " + b.getYear()
                + (b.isBorrowed() ? " [已借出]" : " [在架]");
    }

    public static boolean borrow(String cardId, String title, String author) {
        Book b = findBook(title, author);
        if (b == null || b.isBorrowed()) {
            return false;
        }
        for (Member m : allMembers) {
            if (m.getCardId().equals(cardId)) {
                if (m.isSuspended() || m.getBorrowCount() >= 5) {
                    return false;
                }
                m.setBorrowCount(m.getBorrowCount() + 1);
                b.setBorrowed(true);
                totalBorrowed++;
                return true;
            }
        }
        return false;
    }

    public static boolean giveBack(String cardId, String title, String author) {
        Book b = findBook(title, author);
        if (b == null || !b.isBorrowed()) {
            return false;
        }
        for (Member m : allMembers) {
            if (m.getCardId().equals(cardId)) {
                m.setBorrowCount(m.getBorrowCount() - 1);
                b.setBorrowed(false);
                totalBorrowed--;
                return true;
            }
        }
        return false;
    }
}
