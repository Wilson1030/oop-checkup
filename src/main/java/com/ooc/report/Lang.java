package com.ooc.report;

/** Output language. */
public enum Lang {
    ZH, EN;

    public static Lang parse(String s) {
        if (s == null) return ZH;
        String v = s.trim().toLowerCase();
        if (v.startsWith("en")) return EN;
        return ZH;
    }

    public boolean isEn() {
        return this == EN;
    }

    /** Pick one of two values by language. */
    public String pick(String zh, String en) {
        return this == EN ? en : zh;
    }
}
