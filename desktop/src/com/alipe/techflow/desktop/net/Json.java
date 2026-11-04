package com.alipe.techflow.desktop.net;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Leitor e escritor de JSON mínimo, sem dependências. */
public final class Json {
    private final String s;
    private int i;

    private Json(String s) { this.s = s; }

    public static Object parse(String text) {
        Json j = new Json(text);
        j.skip();
        Object v = j.value();
        j.skip();
        if (j.i != j.s.length()) throw j.error("conteúdo extra após o JSON");
        return v;
    }

    public static String stringify(Object value) {
        StringBuilder sb = new StringBuilder();
        write(sb, value, -1, 0);
        return sb.toString();
    }

    public static String pretty(Object value) {
        StringBuilder sb = new StringBuilder();
        write(sb, value, 2, 0);
        return sb.toString();
    }

    // ---------- leitura ----------

    private Object value() {
        if (i >= s.length()) throw error("fim inesperado");
        char c = s.charAt(i);
        return switch (c) {
            case '{' -> object();
            case '[' -> array();
            case '"' -> string();
            case 't' -> literal("true", Boolean.TRUE);
            case 'f' -> literal("false", Boolean.FALSE);
            case 'n' -> literal("null", null);
            default -> number();
        };
    }

    private Map<String, Object> object() {
        Map<String, Object> map = new LinkedHashMap<>();
        i++;
        skip();
        if (peek() == '}') { i++; return map; }
        while (true) {
            skip();
            String key = string();
            skip();
            expect(':');
            skip();
            map.put(key, value());
            skip();
            if (peek() == ',') { i++; continue; }
            expect('}');
            return map;
        }
    }

    private List<Object> array() {
        List<Object> list = new ArrayList<>();
        i++;
        skip();
        if (peek() == ']') { i++; return list; }
        while (true) {
            skip();
            list.add(value());
            skip();
            if (peek() == ',') { i++; continue; }
            expect(']');
            return list;
        }
    }

    private String string() {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (i < s.length()) {
            char c = s.charAt(i++);
            if (c == '"') return sb.toString();
            if (c == '\\') {
                char e = s.charAt(i++);
                switch (e) {
                    case 'n' -> sb.append('\n');
                    case 't' -> sb.append('\t');
                    case 'r' -> sb.append('\r');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'u' -> { sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16)); i += 4; }
                    default -> sb.append(e);
                }
            } else {
                sb.append(c);
            }
        }
        throw error("texto sem aspas de fechamento");
    }

    private Object number() {
        int start = i;
        while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
        String t = s.substring(start, i);
        if (t.isEmpty()) throw error("valor inesperado");
        try {
            if (t.contains(".") || t.contains("e") || t.contains("E")) return Double.parseDouble(t);
            return Long.parseLong(t);
        } catch (NumberFormatException e) {
            throw error("número inválido");
        }
    }

    private Object literal(String word, Object result) {
        if (!s.startsWith(word, i)) throw error("valor inesperado");
        i += word.length();
        return result;
    }

    private void skip() {
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
    }

    private char peek() { return i < s.length() ? s.charAt(i) : '\0'; }

    private void expect(char c) {
        if (peek() != c) throw error("esperado '" + c + "'");
        i++;
    }

    private IllegalArgumentException error(String msg) {
        return new IllegalArgumentException("JSON inválido na posição " + i + ": " + msg);
    }

    // ---------- escrita ----------

    private static void write(StringBuilder sb, Object v, int indent, int level) {
        if (v == null) { sb.append("null"); return; }
        if (v instanceof String str) { quote(sb, str); return; }
        if (v instanceof Number || v instanceof Boolean) { sb.append(v); return; }
        if (v instanceof Map<?, ?> map) {
            if (map.isEmpty()) { sb.append("{}"); return; }
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> e : map.entrySet()) {
                if (!first) sb.append(',');
                first = false;
                newline(sb, indent, level + 1);
                quote(sb, String.valueOf(e.getKey()));
                sb.append(indent >= 0 ? ": " : ":");
                write(sb, e.getValue(), indent, level + 1);
            }
            newline(sb, indent, level);
            sb.append('}');
            return;
        }
        if (v instanceof List<?> list) {
            if (list.isEmpty()) { sb.append("[]"); return; }
            sb.append('[');
            boolean first = true;
            for (Object o : list) {
                if (!first) sb.append(',');
                first = false;
                newline(sb, indent, level + 1);
                write(sb, o, indent, level + 1);
            }
            newline(sb, indent, level);
            sb.append(']');
            return;
        }
        quote(sb, v.toString());
    }

    private static void newline(StringBuilder sb, int indent, int level) {
        if (indent < 0) return;
        sb.append('\n').append(" ".repeat(indent * level));
    }

    private static void quote(StringBuilder sb, String t) {
        sb.append('"');
        for (int k = 0; k < t.length(); k++) {
            char c = t.charAt(k);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        sb.append('"');
    }
}
