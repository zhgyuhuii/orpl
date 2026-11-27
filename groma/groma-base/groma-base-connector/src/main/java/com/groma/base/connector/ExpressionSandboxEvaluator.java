package com.groma.base.connector;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * 受控算术/比较/布尔表达式求值沙盒（落实 BE-SEC-002：严格拒绝 Java 互操作指令，空结果安全处理）。
 * <p>
 * 能力（修复原实现仅支持单个二元比较的限制，落实建议 7）：
 * <ul>
 *   <li>逻辑：{@code &&}、{@code ||}、{@code !}（短路求值）与括号优先级；</li>
 *   <li>算术：{@code + - * / %} 与一元负号（double 语义）；</li>
 *   <li>比较：{@code > >= < <= == !=}（数值按 double，{@code ==}/{@code !=} 支持字符串与布尔精确比较）；</li>
 *   <li>字面量：数字、单/双引号字符串、{@code true}/{@code false}；标识符从上下文变量表取值。</li>
 * </ul>
 * 安全边界：无反射/互操作入口；互操作指令正则拦截保留；解析深度上限 32、表达式长度上限 4096；
 * 解析后的 AST 按（管理员白名单/断点条件这类有限集合）缓存在有界表中。
 * 求值任何环节失败一律安全返回 {@code false}，绝不抛出到调用方热路径。
 * </p>
 */
public final class ExpressionSandboxEvaluator {

    private static final Pattern FORBIDDEN_INTEROP = Pattern.compile(
            "\\b(Java\\.type|Packages|importClass|importPackage|load|loadWithNewGlobal|Runtime|ProcessBuilder|System|Class\\.forName)\\b"
    );
    private static final int MAX_EXPR_CHARS = 4096;
    private static final int MAX_PARSE_DEPTH = 32;
    private static final int AST_CACHE_LIMIT = 256;

    private final boolean enabled;
    private final ConcurrentHashMap<String, Node> astCache = new ConcurrentHashMap<>();

    public ExpressionSandboxEvaluator(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean evaluateBoolean(String expression, Map<String, Object> contextVars) {
        if (!enabled) {
            throw new SecurityException("[SEC-ERR-002] 表达式执行能力已被管理员配置关闭");
        }
        if (expression == null || expression.isBlank()) {
            return false; // CK-BE-SEC-002-2：空表达式或空结果安全返回 false，不抛空指针
        }
        if (expression.length() > MAX_EXPR_CHARS || FORBIDDEN_INTEROP.matcher(expression).find()) {
            throw new SecurityException("[SEC-ERR-002] 检测到禁止的 Java 互操作调用或超长表达式，拒绝执行表达式");
        }
        Node ast;
        if (astCache.size() < AST_CACHE_LIMIT) {
            ast = astCache.computeIfAbsent(expression.trim(), ExpressionSandboxEvaluator::parseSafely);
        } else {
            ast = parseSafely(expression.trim()); // 缓存已满：直接解析不缓存，防恶意表达式撑表
        }
        if (ast == null) {
            return false;
        }
        try {
            Object result = ast.eval(contextVars == null ? Map.of() : contextVars);
            return result instanceof Boolean b && b;
        } catch (RuntimeException ignored) {
            return false; // 未知变量 / 类型不匹配 / 除零：安全 false，不抛出到热路径
        }
    }

    // ---------------- 解析（递归下降：or → and → equality → comparison → additive → multiplicative → unary → primary） ----------------

    private static Node parseSafely(String expr) {
        try {
            Parser parser = new Parser(expr);
            Node node = parser.parseOr();
            parser.expectEnd();
            return node;
        } catch (RuntimeException ex) {
            return null; // 语法错误：缓存否定结果，安全 false
        }
    }

    private interface Node {
        Object eval(Map<String, Object> vars);
    }

    private record Literal(Object value) implements Node {
        @Override public Object eval(Map<String, Object> vars) {
            return value;
        }
    }

    private record Var(String name) implements Node {
        @Override public Object eval(Map<String, Object> vars) {
            return vars.get(name);
        }
    }

    private record Not(Node operand) implements Node {
        @Override public Object eval(Map<String, Object> vars) {
            return !(boolean) requireBool(operand.eval(vars));
        }
    }

    private record Binary(String op, Node left, Node right) implements Node {
        @Override public Object eval(Map<String, Object> vars) {
            return switch (op) {
                case "&&" -> {
                    boolean l = requireBool(left.eval(vars));
                    yield l && requireBool(right.eval(vars));
                }
                case "||" -> {
                    boolean l = requireBool(left.eval(vars));
                    yield l || requireBool(right.eval(vars));
                }
                case "==", "!=", ">", ">=", "<", "<=" -> compare(op, left.eval(vars), right.eval(vars));
                case "+" -> numeric(left, vars) + numeric(right, vars);
                case "-" -> numeric(left, vars) - numeric(right, vars);
                case "*" -> numeric(left, vars) * numeric(right, vars);
                case "/" -> {
                    double r = numeric(right, vars);
                    yield r == 0.0D ? 0.0D : numeric(left, vars) / r; // 除零安全归零，不抛
                }
                case "%" -> {
                    double r = numeric(right, vars);
                    yield r == 0.0D ? 0.0D : numeric(left, vars) % r;
                }
                default -> false;
            };
        }
    }

    private static boolean compare(String op, Object l, Object r) {
        if ("==".equals(op)) {
            return equalsValue(l, r);
        }
        if ("!=".equals(op)) {
            return !equalsValue(l, r);
        }
        double lv = toNumber(l);
        double rv = toNumber(r);
        return switch (op) {
            case ">" -> lv > rv;
            case ">=" -> lv >= rv;
            case "<" -> lv < rv;
            case "<=" -> lv <= rv;
            default -> false;
        };
    }

    private static boolean equalsValue(Object l, Object r) {
        if (l instanceof Number ln && r instanceof Number rn) {
            return Double.compare(ln.doubleValue(), rn.doubleValue()) == 0;
        }
        return String.valueOf(l).equals(String.valueOf(r));
    }

    private static double numeric(Node node, Map<String, Object> vars) {
        return toNumber(node.eval(vars));
    }

    private static double toNumber(Object value) {
        if (value instanceof Number num) {
            return num.doubleValue();
        }
        if (value instanceof String s && !s.isBlank()) {
            return Double.parseDouble(s.trim());
        }
        if (value instanceof Boolean b) {
            return b ? 1.0D : 0.0D;
        }
        throw new IllegalArgumentException("非数值操作数");
    }

    private static boolean requireBool(Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        throw new IllegalArgumentException("非布尔操作数");
    }

    // ---------------- Parser ----------------

    private static final class Parser {
        private final String src;
        private int pos;
        private int parenDepth;

        Parser(String src) {
            this.src = src;
        }

        Node parseOr() {
            Node left = parseAnd();
            while (match("||")) {
                left = new Binary("||", left, parseAnd());
            }
            return left;
        }

        private Node parseAnd() {
            Node left = parseEquality();
            while (match("&&")) {
                left = new Binary("&&", left, parseEquality());
            }
            return left;
        }

        private Node parseEquality() {
            Node left = parseComparison();
            while (true) {
                skipSpaces();
                if (src.startsWith("==", pos)) {
                    pos += 2;
                    left = new Binary("==", left, parseComparison());
                } else if (src.startsWith("!=", pos)) {
                    pos += 2;
                    left = new Binary("!=", left, parseComparison());
                } else {
                    return left;
                }
            }
        }

        private Node parseComparison() {
            Node left = parseAdditive();
            for (String op : new String[]{">=", "<=", ">", "<"}) {
                if (match(op)) {
                    return new Binary(op, left, parseAdditive());
                }
            }
            return left;
        }

        private Node parseAdditive() {
            Node left = parseMultiplicative();
            while (true) {
                if (matchAt('+')) {
                    left = new Binary("+", left, parseMultiplicative());
                } else if (matchAt('-')) {
                    left = new Binary("-", left, parseMultiplicative());
                } else {
                    return left;
                }
            }
        }

        private Node parseMultiplicative() {
            Node left = parseUnary();
            while (true) {
                if (matchAt('*')) {
                    left = new Binary("*", left, parseUnary());
                } else if (matchAt('/')) {
                    left = new Binary("/", left, parseUnary());
                } else if (matchAt('%')) {
                    left = new Binary("%", left, parseUnary());
                } else {
                    return left;
                }
            }
        }

        private Node parseUnary() {
            skipSpaces();
            if (matchAt('!')) {
                return new Not(parseUnary());
            }
            if (matchAt('-')) {
                return new Binary("-", new Literal(0.0D), parseUnary());
            }
            return parsePrimary();
        }

        private Node parsePrimary() {
            skipSpaces();
            if (pos >= src.length()) {
                throw new IllegalArgumentException("表达式意外结束");
            }
            char c = src.charAt(pos);
            if (c == '(') {
                if (++parenDepth > MAX_PARSE_DEPTH) {
                    throw new IllegalArgumentException("括号嵌套超限");
                }
                pos++;
                Node inner = parseOr();
                skipSpaces();
                if (pos >= src.length() || src.charAt(pos) != ')') {
                    throw new IllegalArgumentException("缺少右括号");
                }
                pos++;
                parenDepth--;
                return inner;
            }
            if (c == '\'' || c == '"') {
                return new Literal(readString(c));
            }
            if (Character.isDigit(c)) {
                return new Literal(readNumber());
            }
            if (Character.isJavaIdentifierStart(c)) {
                String word = readWord();
                return switch (word) {
                    case "true" -> new Literal(Boolean.TRUE);
                    case "false" -> new Literal(Boolean.FALSE);
                    default -> new Var(word);
                };
            }
            throw new IllegalArgumentException("非法字符: " + c);
        }

        private void expectEnd() {
            skipSpaces();
            if (pos < src.length()) {
                throw new IllegalArgumentException("表达式末尾存在未消费内容");
            }
        }

        private String readString(char quote) {
            pos++;
            StringBuilder out = new StringBuilder();
            while (pos < src.length() && src.charAt(pos) != quote) {
                out.append(src.charAt(pos++));
            }
            if (pos >= src.length()) {
                throw new IllegalArgumentException("字符串字面量未闭合");
            }
            pos++;
            return out.toString();
        }

        private double readNumber() {
            int start = pos;
            while (pos < src.length() && (Character.isDigit(src.charAt(pos)) || src.charAt(pos) == '.'
                    || src.charAt(pos) == 'e' || src.charAt(pos) == 'E'
                    || ((src.charAt(pos) == '+' || src.charAt(pos) == '-') && pos > start
                        && (src.charAt(pos - 1) == 'e' || src.charAt(pos - 1) == 'E')))) {
                pos++;
            }
            return Double.parseDouble(src.substring(start, pos));
        }

        private String readWord() {
            int start = pos;
            while (pos < src.length() && Character.isJavaIdentifierPart(src.charAt(pos))) {
                pos++;
            }
            return src.substring(start, pos);
        }

        private boolean match(String token) {
            int save = pos;
            skipSpaces();
            if (src.startsWith(token, pos)) {
                pos += token.length();
                return true;
            }
            pos = save;
            return false;
        }

        private boolean matchAt(char token) {
            skipSpaces();
            if (pos < src.length() && src.charAt(pos) == token) {
                pos++;
                return true;
            }
            return false;
        }

        private void skipSpaces() {
            while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) {
                pos++;
            }
        }
    }
}
