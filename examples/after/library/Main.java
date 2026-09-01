package library;

import java.util.Scanner;

/** main 缩成了几行：创建对象、接起来、启动。 */
public class Main {

    public static void main(String[] args) {
        Library library = new Library();
        library.add(new Book("三体", "刘慈欣", "9787536692930", 2008));
        library.add(new Book("活着", "余华", "9787506365437", 1993));
        library.add(new Book("围城", "钱钟书", "9787020024759", 1947));

        Member zhang = new Member("张三", "C001");
        library.register(zhang);

        try (Scanner in = new Scanner(System.in)) {
            new ConsoleUi(library, in, "C001").run();
        }
        System.out.println("再见");
    }
}
