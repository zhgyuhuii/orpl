package com.groma.studio.ir;

import java.util.List;
import java.util.Locale;

/**
 * 编译计划里的 Java 类型占位编解码
 * <p>
 * 操作数的 javaType 用前缀字符串表示尚未命名解析的类型：
 * 基础类型直接写 double / long / boolean / String / Object；
 * 领域对象写 "E:类型id"，枚举写 "EN:枚举id"，集合写 "LIST:元素类型" 与 "MAP:键类型:值类型"。
 * 生成器在命名引擎解析出类名后再替换成真实 Java 类型名。
 * </p>
 */
public final class GromaTypes {

    public static final String DOUBLE = "double";
    public static final String LONG = "long";
    public static final String BOOLEAN = "boolean";
    public static final String STRING = "String";
    public static final String OBJECT = "Object";

    public static final String PREFIX_ENTITY = "E:";
    public static final String PREFIX_ENUM = "EN:";
    public static final String PREFIX_LIST = "LIST:";
    public static final String PREFIX_MAP = "MAP:";

    private GromaTypes() {
    }

    public static String entity(String typeId) {
        return PREFIX_ENTITY + typeId;
    }

    public static String enumType(String enumId) {
        return PREFIX_ENUM + enumId;
    }

    public static String list(String elementType) {
        return PREFIX_LIST + elementType;
    }

    public static String map(String keyType, String valueType) {
        return PREFIX_MAP + keyType + ":" + valueType;
    }

    public static boolean isEntity(String t) {
        return t != null && t.startsWith(PREFIX_ENTITY);
    }

    public static boolean isEnum(String t) {
        return t != null && t.startsWith(PREFIX_ENUM);
    }

    public static boolean isList(String t) {
        return t != null && t.startsWith(PREFIX_LIST);
    }

    public static boolean isMap(String t) {
        return t != null && t.startsWith(PREFIX_MAP);
    }

    public static boolean isCollection(String t) {
        return isList(t) || isMap(t);
    }

    public static String entityIdOf(String t) {
        return isEntity(t) ? t.substring(PREFIX_ENTITY.length()) : null;
    }

    public static String enumIdOf(String t) {
        return isEnum(t) ? t.substring(PREFIX_ENUM.length()) : null;
    }

    public static String elementTypeOf(String t) {
        if (isList(t)) {
            return t.substring(PREFIX_LIST.length());
        }
        if (isMap(t)) {
            String body = t.substring(PREFIX_MAP.length());
            int sep = body.indexOf(':');
            return sep < 0 ? OBJECT : body.substring(sep + 1);
        }
        return OBJECT;
    }

    /**
     * 基础类型字段在 Java 中的类型。
     * datetime / date / time 统一用 long 表示（公元纪元毫秒，东八区），这样时间差、按秒加减等运算都是整数运算，
     * 映射集的时间键也能与“仿真时间.时间日期”直接比较。
     */
    public static String baseJavaType(String fieldType) {
        if (fieldType == null) {
            return OBJECT;
        }
        return switch (fieldType.trim().toLowerCase(Locale.ROOT)) {
            case "double", "float", "decimal", "number" -> DOUBLE;
            case "long", "int", "integer" -> LONG;
            case "boolean", "bool" -> BOOLEAN;
            case "datetime", "date", "time" -> LONG;
            case "string", "text" -> STRING;
            default -> OBJECT;
        };
    }

    public static final String DATE_TIME_TYPES = "datetime|date|time";

    public static boolean isDateTime(String fieldType) {
        return fieldType != null && DATE_TIME_TYPES.contains(fieldType.trim().toLowerCase(Locale.ROOT));
    }

    /** 集合/对象类型在 Java 中的装箱形态（用于 List / Map 的元素类型） */
    public static String boxed(String t) {
        if (DOUBLE.equals(t)) {
            return "Double";
        }
        if (LONG.equals(t)) {
            return "Long";
        }
        if (BOOLEAN.equals(t)) {
            return "Boolean";
        }
        return t;
    }

    public static boolean isNumeric(String t) {
        return DOUBLE.equals(t) || LONG.equals(t);
    }

    public static boolean isPrimitive(String t) {
        return DOUBLE.equals(t) || LONG.equals(t) || BOOLEAN.equals(t);
    }

    /** 数值类型的默认零值字面量 */
    public static String zeroLiteral(String t) {
        if (LONG.equals(t)) {
            return "0L";
        }
        if (BOOLEAN.equals(t)) {
            return "false";
        }
        if (STRING.equals(t)) {
            return "\"\"";
        }
        return "0.0D";
    }

    /** 两个数值操作数运算后的类型：任一为 double 得 double，否则 long */
    public static String numericResult(String a, String b) {
        if (DOUBLE.equals(a) || DOUBLE.equals(b)) {
            return DOUBLE;
        }
        if (LONG.equals(a) && LONG.equals(b)) {
            return LONG;
        }
        return OBJECT;
    }

    public static String describe(CompilePlanIR.OperandIR operand) {
        if (operand instanceof CompilePlanIR.MemberPathIR m) {
            return describe(m.root()) + "." + String.join(".", m.memberIds());
        }
        if (operand instanceof CompilePlanIR.SelfIR) {
            return "self";
        }
        if (operand instanceof CompilePlanIR.ParamIR p) {
            return "param(" + p.zhName() + ")";
        }
        if (operand instanceof CompilePlanIR.LocalIR l) {
            return "local(" + l.zhName() + ")";
        }
        if (operand instanceof CompilePlanIR.ConstIR c) {
            return "const(" + c.zhName() + ")";
        }
        if (operand instanceof CompilePlanIR.NodeOutputIR n) {
            return "out(" + n.cellId() + "." + n.argName() + ")";
        }
        if (operand instanceof CompilePlanIR.NamedInstanceIR n) {
            return "instance(" + n.instanceId() + ")";
        }
        if (operand instanceof CompilePlanIR.BuiltinIR b) {
            return "builtin(" + b.key() + (b.base() == null ? "" : " of " + describe(b.base())) + ")";
        }
        return "?";
    }

    /** 嵌套列表不可变性保护（避免 List.of 不接受 null 元素） */
    public static <T> List<T> safe(List<T> list) {
        return list == null ? List.of() : list;
    }
}
