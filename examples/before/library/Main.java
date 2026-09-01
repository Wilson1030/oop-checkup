package library;

import java.util.Scanner;

/** 演示用代码 —— 见 Book.java 说明 */
public class Main {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        LibraryService.addBook("三体", "刘慈欣", "9787536692930", 2008);
        LibraryService.addBook("活着", "余华", "9787506365437", 1993);
        LibraryService.addBook("围城", "钱钟书", "9787020024759", 1947);

        Member m = new Member();
        m.setName("张三");
        m.setCardId("C001");
        m.setBorrowCount(0);
        m.setSuspended(false);
        LibraryService.allMembers.add(m);
        LibraryService.currentUser = "C001";

        boolean running = true;
        while (running) {
            System.out.println("===== 图书管理系统 =====");
            System.out.println("1. 列出全部图书");
            System.out.println("2. 搜索图书");
            System.out.println("3. 借书");
            System.out.println("4. 还书");
            System.out.println("5. 查看统计");
            System.out.println("0. 退出");
            System.out.print("请选择：");
            String choice = sc.next();

            if (choice.equals("1")) {
                for (Book b : LibraryService.allBooks) {
                    System.out.println(LibraryService.describe(b.title, b.author));
                }
            } else if (choice.equals("2")) {
                System.out.print("搜索方式(1书名 2作者 3ISBN)：");
                String modeInput = sc.next();
                SearchMode mode = SearchMode.ByTitle;
                if (modeInput.equals("2")) {
                    mode = SearchMode.ByAuthor;
                } else if (modeInput.equals("3")) {
                    mode = SearchMode.ByIsbn;
                }
                System.out.print("关键词：");
                String kw = sc.next();
                System.out.println("方式：" + SearchService.label(mode));
                for (Book b : SearchService.search(mode, kw)) {
                    System.out.println("  " + b.title + " / " + b.author);
                }
            } else if (choice.equals("3")) {
                System.out.print("书名：");
                String t = sc.next();
                System.out.print("作者：");
                String a = sc.next();
                if (!LibraryService.isAvailable(t, a)) {
                    System.out.println("该书不可借");
                } else if (LibraryService.borrow(LibraryService.currentUser, t, a)) {
                    System.out.println("借阅成功");
                } else {
                    System.out.println("借阅失败");
                }
            } else if (choice.equals("4")) {
                System.out.print("书名：");
                String t = sc.next();
                System.out.print("作者：");
                String a = sc.next();
                if (LibraryService.giveBack(LibraryService.currentUser, t, a)) {
                    System.out.println("归还成功");
                } else {
                    System.out.println("归还失败");
                }
            } else if (choice.equals("5")) {
                System.out.println("藏书总数：" + LibraryService.allBooks.size());
                System.out.println("借出总数：" + LibraryService.totalBorrowed);
                System.out.println("搜索次数：" + SearchService.searchCount);
                int suspended = 0;
                for (Member x : LibraryService.allMembers) {
                    if (x.isSuspended()) {
                        suspended++;
                    }
                }
                System.out.println("停用会员：" + suspended);
            } else if (choice.equals("0")) {
                running = false;
            } else {
                System.out.println("无效选择");
            }
        }
        sc.close();
        System.out.println("再见");
    }
}
