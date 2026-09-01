package library;

/** 重构后 —— 借阅规则由 Member 自己判断，而不是散落在 Service 里 */
public class Member {

    private final String name;
    private final String cardId;
    private int borrowCount;
    private boolean suspended;

    private static final int MAX_BORROW = 5;

    public Member(String name, String cardId) {
        this.name = name;
        this.cardId = cardId;
        this.borrowCount = 0;
        this.suspended = false;
    }

    public boolean hasCard(String id) {
        return cardId.equals(id);
    }

    /** 规则收敛在这里：外部不需要知道 borrowCount 和 suspended 的存在 */
    public boolean canBorrow() {
        return !suspended && borrowCount < MAX_BORROW;
    }

    public void recordBorrow() {
        if (!canBorrow()) {
            throw new IllegalStateException(name + " 当前不可借阅");
        }
        borrowCount++;
    }

    public void recordReturn() {
        if (borrowCount > 0) {
            borrowCount--;
        }
    }

    public boolean isSuspended() {
        return suspended;
    }

    public void suspend() {
        suspended = true;
    }

    public String describe() {
        return name + "(" + cardId + ") 已借 " + borrowCount + " 本";
    }
}
