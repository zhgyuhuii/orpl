package com.groma.base.engine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 外部按名写入成员值时的类型转换（MCP 干预、WebSocket 指令、REST 请求体、存档回填都走这里）
 */
public final class GromaValues {

    private GromaValues() {
    }

    public static double toDouble(Object raw) {
        if (raw instanceof Number n) {
            return n.doubleValue();
        }
        if (raw instanceof Boolean b) {
            return b ? 1.0D : 0.0D;
        }
        if (raw instanceof String s && !s.isBlank()) {
            try {
                return Double.parseDouble(s.trim());
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("无法转换为数值: " + s);
            }
        }
        throw new IllegalArgumentException("无法转换为数值: " + raw);
    }

    public static long toLong(Object raw) {
        if (raw instanceof Number n) {
            return n.longValue();
        }
        if (raw instanceof Boolean b) {
            return b ? 1L : 0L;
        }
        if (raw instanceof String s && !s.isBlank()) {
            try {
                return Long.parseLong(s.trim());
            } catch (NumberFormatException ex) {
                return (long) toDouble(raw);
            }
        }
        throw new IllegalArgumentException("无法转换为整数: " + raw);
    }

    public static boolean toBoolean(Object raw) {
        if (raw instanceof Boolean b) {
            return b;
        }
        if (raw instanceof Number n) {
            return n.doubleValue() != 0.0D;
        }
        if (raw instanceof String s) {
            String t = s.trim().toLowerCase(Locale.ROOT);
            return "true".equals(t) || "是".equals(s.trim()) || "1".equals(t);
        }
        throw new IllegalArgumentException("无法转换为判断值: " + raw);
    }

    public static String toText(Object raw) {
        return raw == null ? null : String.valueOf(raw);
    }

    /**
     * 枚举转换：支持 "@enum:常量名"、"常量名"、"状态中文原名" 三种写法
     */
    public static <E extends Enum<E>> E toEnum(Class<E> enumType, Object raw) {
        if (raw == null) {
            return null;
        }
        if (enumType.isInstance(raw)) {
            return enumType.cast(raw);
        }
        String text = String.valueOf(raw).trim();
        if (text.startsWith(BaseSimEntity.ENUM_PREFIX)) {
            text = text.substring(BaseSimEntity.ENUM_PREFIX.length());
        }
        for (E constant : enumType.getEnumConstants()) {
            if (constant.name().equals(text)) {
                return constant;
            }
        }
        for (E constant : enumType.getEnumConstants()) {
            if (BaseSimEntity.enumZhLabel(constant).equals(String.valueOf(raw).trim())) {
                return constant;
            }
        }
        throw new IllegalArgumentException("无法转换为枚举 " + enumType.getSimpleName() + ": " + raw);
    }

    /**
     * 存档编码：对象引用 → "@ref:实例标识"，枚举 → "@enum:常量名"，集合逐元素编码，其余原样
     */
    @SuppressWarnings("unchecked")
    public static Object encode(Object raw) {
        if (raw instanceof BaseSimEntity ref) {
            return BaseSimEntity.REF_PREFIX + ref.getInstanceId();
        }
        if (raw instanceof Enum<?> en) {
            return BaseSimEntity.ENUM_PREFIX + en.name();
        }
        if (raw instanceof Map<?, ?> map) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                out.put(String.valueOf(encode(e.getKey())), encode(e.getValue()));
            }
            return out;
        }
        if (raw instanceof List<?> list) {
            List<Object> out = new ArrayList<>(list.size());
            for (Object item : list) {
                out.add(encode(item));
            }
            return out;
        }
        return raw;
    }

    /**
     * 存档解码：把 "@ref:" 引用解析回世界里的实例，集合逐元素解码
     */
    @SuppressWarnings("unchecked")
    public static Object decode(Object raw, GromaWorldContext ctx) {
        if (raw instanceof String s && s.startsWith(BaseSimEntity.REF_PREFIX)) {
            BaseSimEntity target = ctx == null ? null : ctx.findInstance(s.substring(BaseSimEntity.REF_PREFIX.length()));
            if (target == null) {
                throw new IllegalStateException("存档引用的对象实例不存在: " + s);
            }
            return target;
        }
        if (raw instanceof Map<?, ?> map) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                out.put(String.valueOf(e.getKey()), decode(e.getValue(), ctx));
            }
            return out;
        }
        if (raw instanceof List<?> list) {
            List<Object> out = new ArrayList<>(list.size());
            for (Object item : list) {
                out.add(decode(item, ctx));
            }
            return out;
        }
        return raw;
    }

    /** 判断一个原始值是否是存档里的对象引用编码 */
    public static boolean isRef(Object raw) {
        return raw instanceof String s && s.startsWith(BaseSimEntity.REF_PREFIX);
    }

    /**
     * 把集合元素里的 "@ref:实例标识" 解析成实体，其余元素原样返回
     */
    public static List<Object> decodeList(List<?> raw, GromaWorldContext ctx) {
        List<Object> out = new ArrayList<>(raw == null ? 0 : raw.size());
        if (raw == null) {
            return out;
        }
        for (Object item : raw) {
            out.add(decodeItem(item, ctx));
        }
        return out;
    }

    public static Map<String, Object> decodeMap(Map<?, ?> raw, GromaWorldContext ctx) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (raw == null) {
            return out;
        }
        for (Map.Entry<?, ?> e : raw.entrySet()) {
            out.put(String.valueOf(e.getKey()), decodeItem(e.getValue(), ctx));
        }
        return out;
    }

    private static Object decodeItem(Object item, GromaWorldContext ctx) {
        if (isRef(item)) {
            BaseSimEntity target = ctx.findInstance(((String) item).substring(BaseSimEntity.REF_PREFIX.length()));
            if (target == null) {
                throw new IllegalArgumentException("引用的对象实例不存在: " + item);
            }
            return target;
        }
        return item;
    }
}
