package testdata;

import java.util.Scanner;

/**
 * 检查项 6 的最小测试用例 —— 单元测试，不是验证样本。
 *
 * 用途：回答「规则实现写对了吗」，不回答「真实世界里存在吗」。
 * 后者由 B2 的盲测新样本回答。
 *
 * 本文件刻意写成「所有逻辑塞在 main 里」的形态，
 * main 方法体约 70 行，预期触发检查项 6 的「中等」档（50–100 行）。
 */
public class MainBloatCase {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String[] names = new String[100];
        int[] chinese = new int[100];
        int[] math = new int[100];
        int[] english = new int[100];
        int count = 0;
        boolean running = true;

        while (running) {
            System.out.println("=== 学生成绩管理系统 ===");
            System.out.println("1. 添加学生");
            System.out.println("2. 查询学生");
            System.out.println("3. 统计平均分");
            System.out.println("4. 排序输出");
            System.out.println("0. 退出");
            System.out.print("请选择：");
            int choice = sc.nextInt();

            if (choice == 1) {
                System.out.print("姓名：");
                names[count] = sc.next();
                System.out.print("语文：");
                chinese[count] = sc.nextInt();
                System.out.print("数学：");
                math[count] = sc.nextInt();
                System.out.print("英语：");
                english[count] = sc.nextInt();
                count++;
                System.out.println("添加成功，当前共 " + count + " 人");
            } else if (choice == 2) {
                System.out.print("请输入姓名：");
                String target = sc.next();
                boolean found = false;
                for (int i = 0; i < count; i++) {
                    if (names[i].equals(target)) {
                        int total = chinese[i] + math[i] + english[i];
                        System.out.println("语文 " + chinese[i]
                                + " 数学 " + math[i]
                                + " 英语 " + english[i]
                                + " 总分 " + total);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    System.out.println("查无此人");
                }
            } else if (choice == 3) {
                int sumC = 0, sumM = 0, sumE = 0;
                for (int i = 0; i < count; i++) {
                    sumC += chinese[i];
                    sumM += math[i];
                    sumE += english[i];
                }
                if (count == 0) {
                    System.out.println("暂无数据");
                } else {
                    System.out.println("语文平均 " + (sumC / count));
                    System.out.println("数学平均 " + (sumM / count));
                    System.out.println("英语平均 " + (sumE / count));
                }
            } else if (choice == 4) {
                for (int i = 0; i < count - 1; i++) {
                    for (int j = 0; j < count - 1 - i; j++) {
                        int t1 = chinese[j] + math[j] + english[j];
                        int t2 = chinese[j + 1] + math[j + 1] + english[j + 1];
                        if (t1 < t2) {
                            String tn = names[j];
                            names[j] = names[j + 1];
                            names[j + 1] = tn;
                            int tc = chinese[j];
                            chinese[j] = chinese[j + 1];
                            chinese[j + 1] = tc;
                            int tm = math[j];
                            math[j] = math[j + 1];
                            math[j + 1] = tm;
                            int te = english[j];
                            english[j] = english[j + 1];
                            english[j + 1] = te;
                        }
                    }
                }
                for (int i = 0; i < count; i++) {
                    System.out.println((i + 1) + ". " + names[i] + " "
                            + (chinese[i] + math[i] + english[i]));
                }
            } else if (choice == 0) {
                running = false;
            } else {
                System.out.println("无效选择");
            }
        }
        sc.close();
        System.out.println("再见");
    }
}
