package com.replacer;

import java.util.ArrayList;
import java.util.List;

public class JsonUtil {

    public static class TypeData {
        public final String type;
        public final String match;
        public final String replace;

        public TypeData(String type, String match, String replace) {
            this.type = type;
            this.match = match;
            this.replace = replace;
        }
    }

    public static class RuleData {
        public final String name;
        public final List<TypeData> typeRows;

        public RuleData(String name, List<TypeData> typeRows) {
            this.name = name;
            this.typeRows = typeRows;
        }
    }

    public static String exportRules(List<ReplacerTab.RulePanel> rulePanels) {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (int i = 0; i < rulePanels.size(); i++) {
            ReplacerTab.RulePanel rule = rulePanels.get(i);
            sb.append("  {\n");
            sb.append("    \"name\": ").append(escapeJson(rule.getName())).append(",\n");
            sb.append("    \"typeRows\": [\n");
            List<ReplacerTab.TypeRow> rows = rule.getTypeRows();
            for (int j = 0; j < rows.size(); j++) {
                ReplacerTab.TypeRow row = rows.get(j);
                sb.append("      {\n");
                sb.append("        \"type\": ").append(escapeJson(row.getType())).append(",\n");
                sb.append("        \"match\": ").append(escapeJson(row.getMatch())).append(",\n");
                sb.append("        \"replace\": ").append(escapeJson(row.getReplace())).append("\n");
                sb.append("      }");
                if (j < rows.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("    ]\n");
            sb.append("  }");
            if (i < rulePanels.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("]");
        return sb.toString();
    }

    public static List<RuleData> parseRules(String json) {
        List<RuleData> rules = new ArrayList<>();
        Parser parser = new Parser(json.trim());
        parser.skipWhitespace();
        parser.expect('[');
        parser.skipWhitespace();

        while (parser.peek() != ']') {
            rules.add(parseRule(parser));
            parser.skipWhitespace();
            if (parser.peek() == ',') parser.advance();
            parser.skipWhitespace();
        }
        parser.expect(']');
        return rules;
    }

    private static RuleData parseRule(Parser p) {
        p.skipWhitespace();
        p.expect('{');
        String name = null;
        List<TypeData> typeRows = null;

        while (p.peek() != '}') {
            p.skipWhitespace();
            String key = p.readString();
            p.skipWhitespace();
            p.expect(':');
            p.skipWhitespace();

            switch (key) {
                case "name":
                    name = p.readString();
                    break;
                case "typeRows":
                    typeRows = parseTypeRows(p);
                    break;
                default:
                    p.skipValue();
                    break;
            }
            p.skipWhitespace();
            if (p.peek() == ',') p.advance();
        }
        p.expect('}');
        if (name == null) name = "";
        if (typeRows == null) typeRows = new ArrayList<>();
        return new RuleData(name, typeRows);
    }

    private static List<TypeData> parseTypeRows(Parser p) {
        List<TypeData> rows = new ArrayList<>();
        p.expect('[');
        p.skipWhitespace();
        while (p.peek() != ']') {
            rows.add(parseTypeRow(p));
            p.skipWhitespace();
            if (p.peek() == ',') p.advance();
            p.skipWhitespace();
        }
        p.expect(']');
        return rows;
    }

    private static TypeData parseTypeRow(Parser p) {
        p.skipWhitespace();
        p.expect('{');
        String type = null, match = null, replace = null;

        while (p.peek() != '}') {
            p.skipWhitespace();
            String key = p.readString();
            p.skipWhitespace();
            p.expect(':');
            p.skipWhitespace();

            switch (key) {
                case "type":
                    type = p.readString();
                    break;
                case "match":
                    match = p.readString();
                    break;
                case "replace":
                    replace = p.readString();
                    break;
                default:
                    p.skipValue();
                    break;
            }
            p.skipWhitespace();
            if (p.peek() == ',') p.advance();
        }
        p.expect('}');
        return new TypeData(
                type != null ? type : "",
                match != null ? match : "",
                replace != null ? replace : ""
        );
    }

    private static String escapeJson(String value) {
        if (value == null) return "\"\"";
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:   sb.append(c);
            }
        }
        sb.append("\"");
        return sb.toString();
    }

    private static class Parser {
        private final String input;
        private int pos;

        Parser(String input) {
            this.input = input;
            this.pos = 0;
        }

        char peek() {
            skipWhitespace();
            if (pos >= input.length()) throw new RuntimeException("Unexpected end of JSON");
            return input.charAt(pos);
        }

        void advance() {
            pos++;
        }

        void expect(char c) {
            skipWhitespace();
            if (pos >= input.length() || input.charAt(pos) != c) {
                throw new RuntimeException("Expected '" + c + "' at position " + pos);
            }
            pos++;
        }

        void skipWhitespace() {
            while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) {
                pos++;
            }
        }

        String readString() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (pos < input.length()) {
                char c = input.charAt(pos);
                if (c == '"') {
                    pos++;
                    return sb.toString();
                }
                if (c == '\\') {
                    pos++;
                    if (pos >= input.length()) break;
                    char escaped = input.charAt(pos);
                    switch (escaped) {
                        case '"':  sb.append('"'); break;
                        case '\\': sb.append('\\'); break;
                        case 'n':  sb.append('\n'); break;
                        case 'r':  sb.append('\r'); break;
                        case 't':  sb.append('\t'); break;
                        case '/':  sb.append('/'); break;
                        default:   sb.append(escaped);
                    }
                } else {
                    sb.append(c);
                }
                pos++;
            }
            throw new RuntimeException("Unterminated string");
        }

        void skipValue() {
            skipWhitespace();
            char c = peek();
            if (c == '"') {
                readString();
            } else if (c == '{') {
                skipObject();
            } else if (c == '[') {
                skipArray();
            } else {
                // number, boolean, null
                while (pos < input.length() && ",]}".indexOf(input.charAt(pos)) < 0
                        && !Character.isWhitespace(input.charAt(pos))) {
                    pos++;
                }
            }
        }

        void skipObject() {
            expect('{');
            while (peek() != '}') {
                readString(); // key
                expect(':');
                skipValue();
                skipWhitespace();
                if (peek() == ',') advance();
            }
            expect('}');
        }

        void skipArray() {
            expect('[');
            while (peek() != ']') {
                skipValue();
                skipWhitespace();
                if (peek() == ',') advance();
            }
            expect(']');
        }
    }
}
