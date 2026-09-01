package library;

import java.util.List;
import java.util.Scanner;

/**
 * 界面逻辑从 main 里搬了出来，并且拆成了若干短方法。
 * main 只负责搭好对象、把它们接起来。
 */
public class ConsoleUi {

    private final Library library;
    private final Scanner in;
    private final String currentCard;

    public ConsoleUi(Library library, Scanner in, String currentCard) {
        this.library = library;
        this.in = in;
        this.currentCard = currentCard;
    }

    public void run() {
        boolean running = true;
        while (running) {
            printMenu();
            String choice = in.next();
            if (choice.equals("1")) {
                listBooks();
            } else if (choice.equals("2")) {
                doSearch();
            } else if (choice.equals("3")) {
                doBorrow();
            } else if (choice.equals("4")) {
                doReturn();
            } else if (choice.equals("5")) {
                System.out.println(library.statistics());
            } else if (choice.equals("0")) {
                running = false;
            } else {
                System.out.println("无效选择");
            }
        }
    }

    private void printMenu() {
        System.out.println("===== 图书管理系统 =====");
        System.out.println("1. 列出全部  2. 搜索  3. 借书  4. 还书  5. 统计  0. 退出");
        System.out.print("请选择：");
    }

    private void listBooks() {
        for (Book b : library.allBooks()) {
            System.out.println(b.describe());
        }
    }

    private void doSearch() {
        System.out.print("搜索方式(1书名 2作者 3ISBN)：");
        SearchStrategy strategy = strategyOf(in.next());
        System.out.print("关键词：");
        String keyword = in.next();
        System.out.println("方式：" + strategy.label());
        List<Book> hits = library.search(strategy, keyword);
        for (Book b : hits) {
            System.out.println("  " + b.shortLabel());
        }
    }

    /** 唯一一处把输入映射成对象；新增搜索方式只需在这里加一行 */
    private SearchStrategy strategyOf(String input) {
        if (input.equals("2")) {
            return new AuthorSearch();
        }
        if (input.equals("3")) {
            return new IsbnSearch();
        }
        return new TitleSearch();
    }

    private void doBorrow() {
        boolean ok = library.borrow(currentCard, askBook());
        System.out.println(ok ? "借阅成功" : "借阅失败");
    }

    private void doReturn() {
        boolean ok = library.giveBack(currentCard, askBook());
        System.out.println(ok ? "归还成功" : "归还失败");
    }

    private BookRef askBook() {
        System.out.print("书名：");
        String title = in.next();
        System.out.print("作者：");
        String author = in.next();
        return new BookRef(title, author);
    }
}
