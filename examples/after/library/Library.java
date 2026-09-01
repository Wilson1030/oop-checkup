package library;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 重构后 —— 不再是一堆 static 方法 + 全局变量，
 * 而是一个有身份、有状态、能被创建多个的正常对象。
 */
public class Library {

    private final List<Book> books = new ArrayList<>();
    private final List<Member> members = new ArrayList<>();
    private int borrowedCount;

    public void add(Book book) {
        books.add(book);
    }

    public void register(Member member) {
        members.add(member);
    }

    public List<Book> allBooks() {
        return new ArrayList<>(books);
    }

    public Optional<Book> find(BookRef ref) {
        return books.stream().filter(b -> b.is(ref)).findFirst();
    }

    public Optional<Member> memberOf(String cardId) {
        return members.stream().filter(m -> m.hasCard(cardId)).findFirst();
    }

    public List<Book> search(SearchStrategy strategy, String keyword) {
        List<Book> hits = new ArrayList<>();
        for (Book b : books) {
            if (strategy.test(b, keyword)) {
                hits.add(b);
            }
        }
        return hits;
    }

    public boolean borrow(String cardId, BookRef ref) {
        Optional<Book> book = find(ref);
        Optional<Member> member = memberOf(cardId);
        if (book.isEmpty() || member.isEmpty()) {
            return false;
        }
        if (!book.get().isAvailable() || !member.get().canBorrow()) {
            return false;
        }
        book.get().lend();
        member.get().recordBorrow();
        borrowedCount++;
        return true;
    }

    public boolean giveBack(String cardId, BookRef ref) {
        Optional<Book> book = find(ref);
        Optional<Member> member = memberOf(cardId);
        if (book.isEmpty() || member.isEmpty() || book.get().isAvailable()) {
            return false;
        }
        book.get().giveBack();
        member.get().recordReturn();
        borrowedCount--;
        return true;
    }

    public String statistics() {
        long suspended = members.stream().filter(Member::isSuspended).count();
        return "藏书 " + books.size()
                + " · 借出 " + borrowedCount
                + " · 会员 " + members.size()
                + " · 停用 " + suspended;
    }
}
