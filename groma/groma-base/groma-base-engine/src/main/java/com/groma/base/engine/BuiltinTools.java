package com.groma.base.engine;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 画布“工具过程”库的运行期语义（算术 / 比较 / 逻辑 / 集合 / 随数），供编译产出代码直接调用
 * <p>
 * 每个方法对应工具库里的一个 innerType（如 arithmetic.add、compare.gt），
 * 生成器按操作数的静态类型选择重载，因此这里没有任何反射或表达式引擎（落实设计书 2.2 节“原生比较、零表达式引擎开销”）。
 * 任何非法运算都以 {@link GromaProcessException} 失败，由过程任务在提交点转为 FAILED 并保留原因。
 * </p>
 */
public final class BuiltinTools {

    private BuiltinTools() {
    }

    // ------------------------------------------------------------------ 算术运算（arithmetic.*）

    public static double add(double a, double b) {
        return a + b;
    }

    public static long add(long a, long b) {
        return a + b;
    }

    public static double minus(double a, double b) {
        return a - b;
    }

    public static long minus(long a, long b) {
        return a - b;
    }

    public static double multiply(double a, double b) {
        return a * b;
    }

    public static long multiply(long a, long b) {
        return a * b;
    }

    public static double divide(double a, double b) {
        if (b == 0.0D) {
            throw new GromaProcessException("算术除零异常: " + a + " / " + b);
        }
        return a / b;
    }

    public static long divide(long a, long b) {
        if (b == 0L) {
            throw new GromaProcessException("算术除零异常: " + a + " / " + b);
        }
        return a / b;
    }

    public static double power(double a, double b) {
        return StrictMath.pow(a, b);
    }

    public static double max(double a, double b) {
        return Math.max(a, b);
    }

    public static long max(long a, long b) {
        return Math.max(a, b);
    }

    public static double min(double a, double b) {
        return Math.min(a, b);
    }

    public static long min(long a, long b) {
        return Math.min(a, b);
    }

    public static double reverse(double a) {
        return -a;
    }

    public static long reverse(long a) {
        return -a;
    }

    public static double absolute(double a) {
        return Math.abs(a);
    }

    public static long absolute(long a) {
        return Math.abs(a);
    }

    public static double sqrt(double a) {
        return StrictMath.sqrt(a);
    }

    public static double cbrt(double a) {
        return StrictMath.cbrt(a);
    }

    public static long mod(long a, long b) {
        if (b == 0L) {
            throw new GromaProcessException("取余除零异常: " + a + " % " + b);
        }
        return a % b;
    }

    /** 文本连接（“加”用于文本时） */
    public static String concat(Object a, Object b) {
        return String.valueOf(a) + String.valueOf(b);
    }

    /** 在 [a, b] 之间取随机数（任务级种子派生随机流，同种子可复现） */
    public static double rand(GromaWorldContext ctx, double a, double b) {
        double r = ctx.randomStream().nextDouble();
        return a + r * (b - a);
    }

    // ------------------------------------------------------------------ 比较运算（compare.*）

    public static boolean gt(double a, double b) {
        return a > b;
    }

    public static boolean ge(double a, double b) {
        return a >= b;
    }

    public static boolean lt(double a, double b) {
        return a < b;
    }

    public static boolean le(double a, double b) {
        return a <= b;
    }

    /** 等于：数值按数值比较，枚举可与状态中文原名比较，其余按对象相等 */
    public static boolean eq(Object a, Object b) {
        if (a instanceof Number na && b instanceof Number nb) {
            return na.doubleValue() == nb.doubleValue();
        }
        if (a instanceof Enum<?> || b instanceof Enum<?>) {
            return Objects.equals(enumLabel(a), enumLabel(b));
        }
        return Objects.equals(a, b);
    }

    public static boolean ne(Object a, Object b) {
        return !eq(a, b);
    }

    private static String enumLabel(Object value) {
        return value instanceof Enum<?> en ? BaseSimEntity.enumZhLabel(en) : String.valueOf(value);
    }

    /** 是同一实例（对象身份比较） */
    public static boolean isSameObject(Object a, Object b) {
        return a == b;
    }

    public static boolean isNotSameObject(Object a, Object b) {
        return a != b;
    }

    // ------------------------------------------------------------------ 逻辑运算（logic.*）

    public static boolean logAnd(boolean a, boolean b) {
        return a && b;
    }

    public static boolean logOr(boolean a, boolean b) {
        return a || b;
    }

    public static boolean logXor(boolean a, boolean b) {
        return a ^ b;
    }

    public static boolean logNot(boolean a) {
        return !a;
    }

    public static boolean isNull(Object a) {
        return a == null;
    }

    /** 包含元素（对象组 / 对象集） */
    public static boolean contains(Iterable<?> items, Object element) {
        if (items == null) {
            return false;
        }
        for (Object item : items) {
            if (Objects.equals(item, element)) {
                return true;
            }
        }
        return false;
    }

    /** 获取键对应的值（映射集） */
    public static Object getByKey(Map<?, ?> map, Object key) {
        if (map == null) {
            throw new GromaProcessException("获取键对应的值失败: 映射为空");
        }
        return map.get(key);
    }

    /** 包含键（映射集） */
    public static boolean containsKey(Map<?, ?> map, Object key) {
        return map != null && map.containsKey(key);
    }

    /** 对象组长度（集合内置成员“长度”） */
    public static long length(Object collection) {
        if (collection instanceof List<?> list) {
            return list.size();
        }
        if (collection instanceof Map<?, ?> map) {
            return map.size();
        }
        return 0L;
    }

    // ------------------------------------------------------------------ 线上判断（画布连线 logic.conditions）

    /**
     * 画布线上判断：以文本形式给出比较符与字面量，按值的实际类型解释比较
     * （布尔按 true / false，枚举按状态中文原名或常量名，数值按数值比较，其余按文本比较）
     *
     * @param op      比较符：= != > >= < <=
     * @param literal 字面量文本；空串表示“值为真”
     */
    public static boolean judge(Object value, String op, String literal) {
        String operator = op == null || op.isBlank() ? "=" : op.trim();
        if (literal == null || literal.isEmpty() || "null".equals(literal)) {
            boolean truthy = isTrue(value);
            return "!=".equals(operator) ? !truthy : truthy;
        }
        if (value == null) {
            return "!=".equals(operator);
        }
        if ("=".equals(operator) || "!=".equals(operator)) {
            boolean same;
            if (value instanceof Enum<?> en) {
                same = en.name().equals(literal) || BaseSimEntity.enumZhLabel(en).equals(literal);
            } else if (value instanceof Boolean b) {
                same = b == isTrue(literal);
            } else if (value instanceof Number n && isNumeric(literal)) {
                same = n.doubleValue() == Double.parseDouble(literal);
            } else {
                same = String.valueOf(value).equals(literal);
            }
            return "=".equals(operator) ? same : !same;
        }
        if (value instanceof Number n && isNumeric(literal)) {
            double left = n.doubleValue();
            double right = Double.parseDouble(literal);
            return switch (operator) {
                case ">" -> left > right;
                case ">=" -> left >= right;
                case "<" -> left < right;
                case "<=" -> left <= right;
                default -> false;
            };
        }
        int cmp = String.valueOf(value).compareTo(literal);
        return switch (operator) {
            case ">" -> cmp > 0;
            case ">=" -> cmp >= 0;
            case "<" -> cmp < 0;
            case "<=" -> cmp <= 0;
            default -> false;
        };
    }

    private static boolean isNumeric(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        try {
            Double.parseDouble(text);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    /** 四舍六入五成双取整（StrictMath.round） */
    public static long roundToLong(double value) {
        return StrictMath.round(value);
    }

    /** 任意值转数值（非数值按“空为 0、非空为 1”） */
    public static double toDoubleValue(Object value) {
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        return value == null ? 0.0D : 1.0D;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> asList(Object collection, String what) {
        if (!(collection instanceof List<?>)) {
            throw new GromaProcessException(what + "失败: 对象组" + (collection == null ? "为空" : "类型不匹配"));
        }
        return (List<Object>) collection;
    }

    // ------------------------------------------------------------------ 集合内置过程（修改类，入参为集合对象）

    @SuppressWarnings("unchecked")
    private static Set<Object> asSet(Object collection, String what) {
        if (!(collection instanceof java.util.Set<?>)) {
            throw new GromaProcessException(what + "失败: 对象集" + (collection == null ? "为空" : "类型不匹配"));
        }
        return (Set<Object>) collection;
    }

    @SuppressWarnings("unchecked")
    private static Map<Object, Object> asMap(Object collection, String what) {
        if (!(collection instanceof Map<?, ?>)) {
            throw new GromaProcessException(what + "失败: 映射集" + (collection == null ? "为空" : "类型不匹配"));
        }
        return (Map<Object, Object>) collection;
    }

    public static void addTail(Object items, Object element) {
        asList(items, "尾部增加元素").add(element);
    }

    public static void addHead(Object items, Object element) {
        asList(items, "头部增加元素").add(0, element);
    }

    public static Object removeHead(Object items) {
        List<Object> list = asList(items, "头部移除元素");
        if (list.isEmpty()) {
            throw new GromaProcessException("头部移除元素失败: 对象组为空");
        }
        return list.remove(0);
    }

    public static Object removeTail(Object items) {
        List<Object> list = asList(items, "尾部移除元素");
        if (list.isEmpty()) {
            throw new GromaProcessException("尾部移除元素失败: 对象组为空");
        }
        return list.remove(list.size() - 1);
    }

    // ------------------------------------------------------------------ 集合内置过程（innerCollectionArray / innerCollectionMap）

    /**
     * 按标识取元素（对象组）。标识是方圆语言的一基序号：范围 [1, 长度]（存量引擎 ArrayClazz.getAt 同此），
     * 因此画布上“长度 >= 标识 → 取元素 → 标识递增”的循环从 1 走到长度。
     */
    public static Object getByIndex(List<?> items, long index) {
        if (items == null) {
            throw new GromaProcessException("获取数组元素失败，数组为空");
        }
        return items.get(zeroBased(items, index, "按标识取元素"));
    }

    /** 一基序号越界检查：返回零基下标 */
    private static int zeroBased(List<?> list, long ordinal, String what) {
        if (ordinal < 1L || ordinal > list.size()) {
            throw new GromaProcessException(what + "失败: 所给标识(" + ordinal + ")越界，范围应为：[1, " + list.size() + "]");
        }
        return (int) (ordinal - 1L);
    }

    // 对象组的“标识”是一基序号（范围 [1, 长度]），与存量引擎 ArrayClazz 的 getAt / setAt / removeAt / addAt 一致

    public static Object getByIndex(Object items, long index) {
        List<Object> list = asList(items, "按标识取元素");
        return list.get(zeroBased(list, index, "按标识取元素"));
    }

    public static Object removeAt(Object items, long index) {
        List<Object> list = asList(items, "指定标识移除");
        return list.remove(zeroBased(list, index, "指定标识移除"));
    }

    public static void setByIndex(Object items, long index, Object element) {
        List<Object> list = asList(items, "按标识存元素");
        list.set(zeroBased(list, index, "按标识存元素"), element);
    }

    public static void addAt(Object items, long index, Object element) {
        List<Object> list = asList(items, "指定标识增加");
        if (index < 1L || index > list.size() + 1L) {
            throw new GromaProcessException("指定标识增加失败: 所给标识(" + index + ")越界，范围应为：[1, " + (list.size() + 1) + "]");
        }
        list.add((int) (index - 1L), element);
    }

    public static void clearCollection(Object collection) {
        if (collection instanceof List<?> list) {
            list.clear();
        } else if (collection instanceof java.util.Set<?> set) {
            set.clear();
        } else if (collection instanceof Map<?, ?> map) {
            map.clear();
        } else if (collection == null) {
            throw new GromaProcessException("清空失败: 集合为空");
        }
    }

    /** 集合 JSON 文本序列化的共享映射器（tools.jackson 3 非受检异常语义） */
    private static final tools.jackson.databind.ObjectMapper ITEMS_JSON_MAPPER =
            new tools.jackson.databind.ObjectMapper();

    /**
     * 存量方圆手写代码集合序列化习惯调用 {@code 列表.itemsToNJson()} 的桥（收割自 code4.html E6）：
     * 集合元素为实体时逐个转存档状态快照（成员中文标识为键），其余元素按 JSON 原生类型直出；
     * 单实体/Map/标量输入包装为单元素数组，null 输入返回 {@code "[]"}。
     * 输出标准 JSON 数组文本，供写入文本型成员或跨过程传递。
     */
    public static String itemsToNJson(Object items) {
        Object payload;
        if (items == null) {
            payload = List.of();
        } else if (items instanceof java.util.Collection<?> coll) {
            List<Object> normalized = new java.util.ArrayList<>(coll.size());
            for (Object elem : coll) {
                normalized.add(elem instanceof BaseSimEntity entity ? entity.exportState() : elem);
            }
            payload = normalized;
        } else if (items instanceof BaseSimEntity entity) {
            payload = List.of(entity.exportState());
        } else {
            payload = List.of(items);
        }
        return ITEMS_JSON_MAPPER.writeValueAsString(payload);
    }

    public static void copyElements(Object target, Object source) {
        if (target instanceof List<?> && source instanceof java.util.Collection<?> src) {
            asList(target, "复制元素").addAll(src);
        } else if (target instanceof java.util.Set<?> && source instanceof java.util.Collection<?> src) {
            asSet(target, "复制元素").addAll(src);
        } else if (target instanceof Map<?, ?> && source instanceof Map<?, ?> src) {
            asMap(target, "复制键值对").putAll(src);
        } else {
            throw new GromaProcessException("复制元素失败: 来源集与目标集类型不匹配");
        }
    }

    public static void addToSet(Object set, Object element) {
        asSet(set, "增加元素").add(element);
    }

    public static void removeFromSet(Object set, Object element) {
        asSet(set, "移除元素").remove(element);
    }

    public static void putValue(Object map, Object key, Object value) {
        asMap(map, "增加键值对").put(key, value);
    }

    public static void removeKey(Object map, Object key) {
        asMap(map, "移除键").remove(key);
    }

    public static Object getByKey(Object map, Object key) {
        return asMap(map, "获取键对应的值").get(key);
    }

    public static boolean containsKey(Object map, Object key) {
        return asMap(map, "包含键").containsKey(key);
    }

    public static boolean containsElement(Object collection, Object element) {
        if (collection instanceof java.util.Collection<?> col) {
            return col.contains(element);
        }
        if (collection instanceof Map<?, ?> map) {
            return map.containsKey(element);
        }
        return false;
    }

    public static Object keyArrayOf(Object map) {
        return new java.util.ArrayList<>(asMap(map, "获取键对象组").keySet());
    }

    /**
     * 文本转键值对（键值对转文本的逆操作）：解析 toJsonText 产出的 [{"key":k,"value":v},...] 格式文本。
     * 键按整数、值按双精度解析，无法解析的项跳过；空文本返回空映射
     */
    public static java.util.LinkedHashMap<Long, Double> textToKeyValue(Object raw) {
        java.util.LinkedHashMap<Long, Double> out = new java.util.LinkedHashMap<>();
        if (raw == null) {
            return out;
        }
        String body = raw.toString().trim();
        if (body.startsWith("[") && body.endsWith("]")) {
            body = body.substring(1, body.length() - 1);
        }
        if (body.isEmpty()) {
            return out;
        }
        for (String item : body.split("\\},\\s*\\{")) {
            String entry = item.replace('{', ' ').replace('}', ' ').trim();
            int valueMark = entry.indexOf("\"value\"");
            if (!entry.startsWith("\"key\"") || valueMark < 0) {
                continue;
            }
            Long key = parseLongText(entry.substring("\"key\":".length(), entry.indexOf(",\"value\"")));
            Double value = parseDoubleText(entry.substring(entry.indexOf(':', valueMark) + 1));
            if (key != null && value != null) {
                out.put(key, value);
            }
        }
        return out;
    }

    /** 文本转键值对的无值形态：把解析结果并入目标映射（画布上以组成线绑定 host 集合） */
    public static void textToKeyValueInto(Object map, Object raw) {
        java.util.Map<?, ?> target = asMap(map, "文本转键值对");
        for (java.util.Map.Entry<Long, Double> entry : textToKeyValue(raw).entrySet()) {
            ((java.util.Map<Object, Object>) target).put(entry.getKey(), entry.getValue());
        }
    }

    private static Long parseLongText(String text) {
        try {
            return Long.parseLong(text.trim().replace("\"", ""));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static Double parseDoubleText(String text) {
        try {
            return Double.parseDouble(text.trim().replace("\"", ""));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /** 集合转 JSONArray 风格文本（对象输出其实例标识与快照值） */
    public static String toJsonText(Object value) {
        if (value instanceof Map<?, ?> map) {
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Map.Entry<?, ?> e : map.entrySet()) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                sb.append("{\"key\":").append(jsonScalar(e.getKey()))
                        .append(",\"value\":").append(jsonScalar(e.getValue())).append('}');
            }
            return sb.append(']').toString();
        }
        if (value instanceof Iterable<?> items) {
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object item : items) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                sb.append(jsonScalar(item));
            }
            return sb.append(']').toString();
        }
        return jsonScalar(value);
    }

    private static String jsonScalar(Object value) {
        if (value instanceof BaseSimEntity entity) {
            return "\"" + entity.getInstanceId() + "\"";
        }
        if (value instanceof Enum<?> en) {
            return "\"" + BaseSimEntity.enumZhLabel(en) + "\"";
        }
        if (value instanceof String || value instanceof Character) {
            return "\"" + String.valueOf(value).replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        }
        return String.valueOf(value);
    }

    /** 截取文本：从起始位置开始截取指定长度 */
    public static String extractText(String source, long start, long length) {
        if (source == null) {
            throw new GromaProcessException("截取文本失败: 源文本为空");
        }
        if (start < 0L || start > source.length()) {
            throw new GromaProcessException("截取文本失败: 起始位置 " + start + " 超出源文本范围");
        }
        int end = (int) Math.min(source.length(), start + Math.max(0L, length));
        return source.substring((int) start, end);
    }

    public static String joinText(Object textGroup, String separator) {
        StringBuilder sb = new StringBuilder();
        if (textGroup instanceof Iterable<?> items) {
            boolean first = true;
            for (Object item : items) {
                if (!first) {
                    sb.append(separator);
                }
                first = false;
                sb.append(item);
            }
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ 精度运算与时间处理

    /** 按舍入模式保留 digits 位小数 */
    public static double round(double value, long digits, java.math.RoundingMode mode) {
        if (digits < 0L || digits > 12L) {
            throw new GromaProcessException("保留小数位数必须在 0 ~ 12 之间，实际 " + digits);
        }
        return java.math.BigDecimal.valueOf(value).setScale((int) digits, mode).doubleValue();
    }

    /** 画布时间的解析时区：东八区 */
    public static final java.time.ZoneOffset CANVAS_ZONE = java.time.ZoneOffset.ofHours(8);

    /** 按格式把文本解析为公元纪元毫秒（画布日期 / 时间 / 日期时间统一用 long 表示） */
    public static long parseTime(String text, String pattern) {
        try {
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern(pattern);
            if (pattern.contains("H")) {
                return java.time.LocalDateTime.parse(text, formatter).toInstant(CANVAS_ZONE).toEpochMilli();
            }
            return java.time.LocalDate.parse(text, formatter).atStartOfDay(CANVAS_ZONE).toInstant().toEpochMilli();
        } catch (RuntimeException ex) {
            throw new GromaProcessException("时间文本 " + text + " 不符合格式 " + pattern);
        }
    }

    /** 公元纪元毫秒按格式输出文本 */
    public static String formatTime(long epochMillis, String pattern) {
        return java.time.format.DateTimeFormatter.ofPattern(pattern).withZone(CANVAS_ZONE)
                .format(java.time.Instant.ofEpochMilli(epochMillis));
    }

    /** 通用未支持的内置工具：过程以明确原因失败，而不是静默忽略 */
    public static GromaProcessException unsupported(String toolKey) {
        return new GromaProcessException("暂不支持的内置工具: " + toolKey + "（该工具依赖外部本体数据或尚未纳入编译语义表）");
    }

    // ------------------------------------------------------------------ 条件判断辅助

    /** 条件值转布尔：Boolean 直接返回；数值非零为真；文本等于“是/true/1”为真 */
    public static boolean isTrue(Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof Number n) {
            return n.doubleValue() != 0.0D;
        }
        if (value instanceof String s) {
            String t = s.trim();
            return "true".equalsIgnoreCase(t) || "是".equals(t) || "1".equals(t);
        }
        return value != null;
    }

    /** 非空守卫：为空时以“非空输入参数未满足”失败 */
    public static <T> T requireNonNull(T value, String what) {
        if (value == null) {
            throw new GromaProcessException("非空输入参数未满足: " + what);
        }
        return value;
    }

    /** 成员路径中间节点为空时的失败信息（与存量“参数实例未初始化，过程失败”一致） */
    public static GromaProcessException pathMissing(String path) {
        return new GromaProcessException("成员路径缺失，无法继续执行: " + path);
    }
}
