package com.oopc.explain;

/**
 * Minimal JSON helpers — deliberately not a full parser.
 *
 * We only need two things: escape a string when building a request, and pull a
 * single string field out of a response. Pulling in Jackson (~2 MB) for that
 * would be poor value, and this tool's selling point is that it has one
 * dependency and no network requirement.
 *
 * Anything this cannot handle results in null, which makes the caller fall back
 * to the template explainer. Failing to a working default is always acceptable
 * here; producing wrong text is not.
 */
final class Json {

    private Json() {}

    /** Escape a string for embedding in a JSON document. */
    static String escape(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                case '\b': sb.append("\\b");  break;
                case '\f': sb.append("\\f");  break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    /**
     * Extract the string value of the first occurrence of "key": "...".
     * Returns null when the key is absent or the value is not a string.
     */
    static String stringField(String json, String key) {
        int at = indexOfKey(json, key);
        if (at < 0) return null;
        int i = skipToValue(json, at);
        if (i < 0 || i >= json.length() || json.charAt(i) != '"') return null;
        return readString(json, i);
    }

    static boolean boolField(String json, String key, boolean def) {
        int at = indexOfKey(json, key);
        if (at < 0) return def;
        int i = skipToValue(json, at);
        if (i < 0) return def;
        if (json.startsWith("true", i)) return true;
        if (json.startsWith("false", i)) return false;
        return def;
    }

    static int intField(String json, String key, int def) {
        int at = indexOfKey(json, key);
        if (at < 0) return def;
        int i = skipToValue(json, at);
        if (i < 0) return def;
        int j = i;
        while (j < json.length() && (Character.isDigit(json.charAt(j)) || json.charAt(j) == '-')) j++;
        try {
            return Integer.parseInt(json.substring(i, j));
        } catch (Exception e) {
            return def;
        }
    }

    private static int indexOfKey(String json, String key) {
        String needle = "\"" + key + "\"";
        return json.indexOf(needle);
    }

    private static int skipToValue(String json, int keyStart) {
        int i = json.indexOf(':', keyStart);
        if (i < 0) return -1;
        i++;
        while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
        return i;
    }

    /** Read a JSON string literal starting at the opening quote; handles escapes. */
    private static String readString(String json, int openQuote) {
        StringBuilder sb = new StringBuilder();
        for (int i = openQuote + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '"') return sb.toString();
            if (c != '\\') {
                sb.append(c);
                continue;
            }
            if (++i >= json.length()) return null;
            char e = json.charAt(i);
            switch (e) {
                case 'n':  sb.append('\n'); break;
                case 'r':  sb.append('\r'); break;
                case 't':  sb.append('\t'); break;
                case 'b':  sb.append('\b'); break;
                case 'f':  sb.append('\f'); break;
                case '"':  sb.append('"');  break;
                case '\\': sb.append('\\'); break;
                case '/':  sb.append('/');  break;
                case 'u':
                    if (i + 4 >= json.length()) return null;
                    try {
                        sb.append((char) Integer.parseInt(json.substring(i + 1, i + 5), 16));
                    } catch (Exception ex) {
                        return null;
                    }
                    i += 4;
                    break;
                default:
                    return null;
            }
        }
        return null;
    }
}
