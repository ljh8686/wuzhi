package com.wuzhi.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal, dependency-free JSON reader/writer.
 * Values map to: Map<String,Object>, List<Object>, String, Long, Double, Boolean, null.
 */
public final class Json {

    private Json() {}

    // ---------------------------------------------------------------- parse

    public static Object parse(String s) {
        P p = new P(s);
        p.ws();
        Object v = p.value();
        p.ws();
        if (p.i < p.n) throw p.err("trailing content");
        return v;
    }

    private static final class P {
        final String s;
        int i = 0;
        final int n;

        P(String s) { this.s = s; this.n = s.length(); }

        RuntimeException err(String msg) {
            int from = Math.max(0, i - 20);
            int to = Math.min(n, i + 20);
            return new IllegalArgumentException("JSON " + msg + " at " + i + " near: " + s.substring(from, to));
        }

        void ws() {
            while (i < n) {
                char c = s.charAt(i);
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r') i++;
                else break;
            }
        }

        Object value() {
            if (i >= n) throw err("unexpected end");
            char c = s.charAt(i);
            switch (c) {
                case '{': return object();
                case '[': return array();
                case '"': return string();
                case 't': expect("true"); return Boolean.TRUE;
                case 'f': expect("false"); return Boolean.FALSE;
                case 'n': expect("null"); return null;
                default: return number();
            }
        }

        void expect(String lit) {
            if (!s.startsWith(lit, i)) throw err("expected " + lit);
            i += lit.length();
        }

        Map<String, Object> object() {
            Map<String, Object> m = new LinkedHashMap<>();
            i++; // {
            ws();
            if (i < n && s.charAt(i) == '}') { i++; return m; }
            while (true) {
                ws();
                if (i >= n || s.charAt(i) != '"') throw err("expected key");
                String k = string();
                ws();
                if (i >= n || s.charAt(i) != ':') throw err("expected ':'");
                i++;
                ws();
                m.put(k, value());
                ws();
                if (i >= n) throw err("unexpected end in object");
                char c = s.charAt(i);
                if (c == ',') { i++; continue; }
                if (c == '}') { i++; break; }
                throw err("expected ',' or '}'");
            }
            return m;
        }

        List<Object> array() {
            List<Object> l = new ArrayList<>();
            i++; // [
            ws();
            if (i < n && s.charAt(i) == ']') { i++; return l; }
            while (true) {
                ws();
                l.add(value());
                ws();
                if (i >= n) throw err("unexpected end in array");
                char c = s.charAt(i);
                if (c == ',') { i++; continue; }
                if (c == ']') { i++; break; }
                throw err("expected ',' or ']'");
            }
            return l;
        }

        String string() {
            StringBuilder b = new StringBuilder();
            i++; // opening quote
            while (true) {
                if (i >= n) throw err("unterminated string");
                char c = s.charAt(i++);
                if (c == '"') break;
                if (c != '\\') { b.append(c); continue; }
                if (i >= n) throw err("bad escape");
                char e = s.charAt(i++);
                switch (e) {
                    case '"': b.append('"'); break;
                    case '\\': b.append('\\'); break;
                    case '/': b.append('/'); break;
                    case 'b': b.append('\b'); break;
                    case 'f': b.append('\f'); break;
                    case 'n': b.append('\n'); break;
                    case 'r': b.append('\r'); break;
                    case 't': b.append('\t'); break;
                    case 'u':
                        if (i + 4 > n) throw err("bad unicode escape");
                        b.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                        i += 4;
                        break;
                    default: throw err("bad escape \\" + e);
                }
            }
            return b.toString();
        }

        Object number() {
            int st = i;
            if (i < n && (s.charAt(i) == '-' || s.charAt(i) == '+')) i++;
            boolean frac = false, exp = false;
            while (i < n) {
                char c = s.charAt(i);
                if (c >= '0' && c <= '9') { i++; continue; }
                if (c == '.' && !frac && !exp) { frac = true; i++; continue; }
                if ((c == 'e' || c == 'E') && !exp) { exp = true; i++; if (i < n && (s.charAt(i) == '-' || s.charAt(i) == '+')) i++; continue; }
                break;
            }
            if (st == i) throw err("expected value");
            String t = s.substring(st, i);
            try {
                if (!frac && !exp) return Long.parseLong(t);
                return Double.parseDouble(t);
            } catch (NumberFormatException ex) {
                throw err("bad number " + t);
            }
        }
    }

    // ---------------------------------------------------------------- write

    public static String write(Object v) {
        StringBuilder b = new StringBuilder(512);
        writeValue(b, v, -1, 0);
        return b.toString();
    }

    public static String writePretty(Object v) {
        StringBuilder b = new StringBuilder(2048);
        writeValue(b, v, 2, 0);
        return b.toString();
    }

    private static void writeValue(StringBuilder b, Object v, int indent, int depth) {
        if (v == null) { b.append("null"); return; }
        if (v instanceof String) { writeString(b, (String) v); return; }
        if (v instanceof Boolean) { b.append(v.toString()); return; }
        if (v instanceof Number) {
            if (v instanceof Double || v instanceof Float) {
                double d = ((Number) v).doubleValue();
                if (d == Math.rint(d) && !Double.isInfinite(d) && Math.abs(d) < 1e15) {
                    b.append((long) d);
                } else {
                    b.append(d);
                }
            } else {
                b.append(((Number) v).longValue());
            }
            return;
        }
        if (v instanceof Map) {
            Map<?, ?> m = (Map<?, ?>) v;
            if (m.isEmpty()) { b.append("{}"); return; }
            b.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> e : m.entrySet()) {
                if (!first) b.append(',');
                first = false;
                newline(b, indent, depth + 1);
                writeString(b, String.valueOf(e.getKey()));
                b.append(':');
                if (indent > 0) b.append(' ');
                writeValue(b, e.getValue(), indent, depth + 1);
            }
            newline(b, indent, depth);
            b.append('}');
            return;
        }
        if (v instanceof List) {
            List<?> l = (List<?>) v;
            if (l.isEmpty()) { b.append("[]"); return; }
            b.append('[');
            boolean first = true;
            for (Object o : l) {
                if (!first) b.append(',');
                first = false;
                newline(b, indent, depth + 1);
                writeValue(b, o, indent, depth + 1);
            }
            newline(b, indent, depth);
            b.append(']');
            return;
        }
        // fallback: toString as string
        writeString(b, String.valueOf(v));
    }

    private static void newline(StringBuilder b, int indent, int depth) {
        if (indent < 0) return;
        b.append('\n');
        for (int i = 0; i < indent * depth; i++) b.append(' ');
    }

    private static void writeString(StringBuilder b, String s) {
        b.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': b.append("\\\""); break;
                case '\\': b.append("\\\\"); break;
                case '\n': b.append("\\n"); break;
                case '\r': b.append("\\r"); break;
                case '\t': b.append("\\t"); break;
                case '\b': b.append("\\b"); break;
                case '\f': b.append("\\f"); break;
                default:
                    if (c < 0x20 || c == 0x7f) {
                        b.append(String.format("\\u%04x", (int) c));
                    } else {
                        b.append(c);
                    }
            }
        }
        b.append('"');
    }

    // ---------------------------------------------------------------- access

    @SuppressWarnings("unchecked")
    public static Map<String, Object> obj(Object o) {
        return o instanceof Map ? (Map<String, Object>) o : new LinkedHashMap<>();
    }

    @SuppressWarnings("unchecked")
    public static List<Object> arr(Object o) {
        return o instanceof List ? (List<Object>) o : new ArrayList<>();
    }

    public static String str(Map<String, Object> m, String k, String def) {
        Object v = m.get(k);
        return v instanceof String ? (String) v : def;
    }

    public static long lng(Map<String, Object> m, String k, long def) {
        Object v = m.get(k);
        if (v instanceof Number) return ((Number) v).longValue();
        if (v instanceof String) {
            try { return Long.parseLong((String) v); } catch (Exception ignored) { }
        }
        return def;
    }

    public static int integer(Map<String, Object> m, String k, int def) {
        return (int) lng(m, k, def);
    }

    public static double dbl(Map<String, Object> m, String k, double def) {
        Object v = m.get(k);
        return v instanceof Number ? ((Number) v).doubleValue() : def;
    }

    public static boolean bool(Map<String, Object> m, String k, boolean def) {
        Object v = m.get(k);
        return v instanceof Boolean ? (Boolean) v : def;
    }

    public static List<String> strList(Map<String, Object> m, String k) {
        List<String> out = new ArrayList<>();
        for (Object o : arr(m.get(k))) if (o instanceof String) out.add((String) o);
        return out;
    }
}
