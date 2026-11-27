package com.groma.studio.naming;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 * 正向最大匹配（FMM）工业 / 应急 / 筹划仿真词库分词器 + 离线完整拼音兜底（严禁拼音首字母缩写）
 * <p>
 * 四级确定性转译管线里的 Tier 2（分词 + 领域词典）与 Tier 4（完整音节全拼兜底）在这里实现：
 * </p>
 * <ol>
 *   <li>内置领域词典：代码内的小型核心词 + 资源文件 {@code groma-glossary.properties}（业务词根与整词）；</li>
 *   <li>企业自定义术语表：{@link #registerCustomTerm(String, String)}，优先级最高；</li>
 *   <li>未命中词库的单字：完整拼音音节（多音字按建模语境固定取音），无法转拼音的字符用 u + 4 位十六进制码点。</li>
 * </ol>
 */
public final class DomainGlossaryTokenizer {

    /** 词典词条最大长度（汉字数） */
    public static final int MAX_TERM_LENGTH = 16;

    private static final Map<String, String> BUILTIN_DICT;
    private static final Map<Character, String> COMMON_CHAR_SYLLABLES;
    private static final Map<Character, String> POLYPHONIC_SYLLABLES;
    private static final HanyuPinyinOutputFormat PINYIN_FORMAT;

    static {
        Map<String, String> d = new LinkedHashMap<>();
        d.put("应急决策推演", "emergency_deduction");
        d.put("智能筹划", "smart_planning");
        d.put("应用场景", "app_scene");
        d.put("三站两区间", "three_station_two_section");
        d.put("地铁区间", "metro_section");
        d.put("地铁线路", "metro_line");
        d.put("挡水墙", "flood_wall");
        d.put("乘客群", "passenger_group");
        d.put("处置方案", "disposal_plan");
        d.put("方案生成器", "plan_generator");
        d.put("方案执行器", "plan_executor");
        d.put("作战意图", "combat_intent");
        d.put("作战计划", "combat_plan");
        d.put("想定配置", "scenario_config");
        d.put("态势地图", "situation_map");
        d.put("智能体", "agent");
        d.put("会话记录", "session_record");
        d.put("航路规划", "route_planning");
        d.put("同步到达", "sync_arrival");
        d.put("无人机", "uav");
        d.put("海岛", "island");
        d.put("巡检", "patrol");
        d.put("巡航", "cruise");
        d.put("扫描", "scan");
        d.put("剩余电量", "remaining_battery");
        d.put("积水量", "water_volume");
        d.put("积水深度", "water_depth");
        d.put("积水状态", "waterlogging_state");
        d.put("代表性水深", "representative_depth");
        d.put("迫停水深", "forced_stop_depth");
        d.put("危险水深", "danger_depth");
        d.put("底高程", "floor_elevation");
        d.put("顶高程", "top_elevation");
        d.put("水面高程", "water_surface_elevation");
        d.put("汇水面积", "catchment_area");
        d.put("径流系数", "runoff_coeff");
        d.put("排水", "drain");
        d.put("降雨", "rainfall");
        d.put("水流交换", "water_exchange");
        d.put("车站", "station");
        d.put("列车", "train");
        d.put("区域", "area");
        d.put("行驶", "run");
        d.put("速度", "speed");
        d.put("位置", "position");
        d.put("方向", "direction");
        d.put("长度", "length");
        d.put("宽度", "width");
        d.put("状态", "state");
        d.put("故障", "faulted");
        d.put("在线", "online");
        d.put("放行", "release_allowed");
        d.put("有效", "valid");
        d.put("计算", "compute");
        d.put("生成", "generate");
        d.put("分析", "analyze");
        d.put("获取", "fetch");
        d.put("更新", "update");
        d.put("保存", "save");
        d.put("算法", "algorithm");
        d.put("管理", "manager");
        // 领域词典外置为模块化分片（基础词库 + 领域词库；按领域分片扩充无需改代码，落实建议 8）。
        // 加载顺序即优先级：基础 groma-glossary.properties 优先，领域分片补充（同名词条先加载者胜出）。
        for (String shard : List.of("groma-glossary.properties",
                "groma-glossary-rail-transit.properties",
                "groma-glossary-emergency.properties",
                "groma-glossary-unmanned-system.properties")) {
            try (InputStream in = DomainGlossaryTokenizer.class.getResourceAsStream(
                    "/com/groma/studio/naming/" + shard)) {
                if (in == null) {
                    continue; // 领域分片可选，缺失不报错
                }
                Properties glossary = new Properties();
                glossary.load(new InputStreamReader(in, StandardCharsets.UTF_8));
                for (String zhTerm : glossary.stringPropertyNames()) {
                    String english = glossary.getProperty(zhTerm) == null ? "" : glossary.getProperty(zhTerm).trim();
                    if (!zhTerm.isBlank() && !english.isBlank()) {
                        d.putIfAbsent(zhTerm.trim(), english);
                    }
                }
            } catch (IOException ex) {
                throw new ExceptionInInitializerError("加载领域词典分片 " + shard + " 失败: " + ex.getMessage());
            }
        }
        BUILTIN_DICT = Collections.unmodifiableMap(d);

        Map<Character, String> py = new LinkedHashMap<>();
        py.put('场', "chang"); py.put('景', "jing"); py.put('过', "guo"); py.put('程', "cheng");
        py.put('对', "dui"); py.put('象', "xiang"); py.put('属', "shu"); py.put('性', "xing");
        py.put('水', "shui"); py.put('深', "shen"); py.put('站', "zhan"); py.put('车', "che");
        py.put('线', "xian"); py.put('路', "lu"); py.put('停', "ting"); py.put('进', "jin");
        py.put('离', "li"); py.put('折', "zhe"); py.put('返', "fan"); py.put('定', "ding");
        py.put('值', "zhi"); py.put('参', "can"); py.put('数', "shu"); py.put('默', "mo");
        py.put('认', "ren"); py.put('首', "shou"); py.put('末', "mo"); py.put('上', "shang");
        py.put('下', "xia"); py.put('左', "zuo"); py.put('右', "you"); py.put('前', "qian");
        py.put('后', "hou"); py.put('高', "gao"); py.put('低', "di"); py.put('大', "da");
        py.put('小', "xiao"); py.put('号', "hao");
        COMMON_CHAR_SYLLABLES = Collections.unmodifiableMap(py);

        // 多音字按建模语境固定取音（pinyin4j 的首个读音在这些字上不稳定，显式指定保证确定性）
        Map<Character, String> fixes = new LinkedHashMap<>();
        fixes.put('行', "xing"); fixes.put('长', "chang"); fixes.put('重', "zhong"); fixes.put('量', "liang");
        fixes.put('为', "wei"); fixes.put('与', "yu"); fixes.put('中', "zhong");
        fixes.put('种', "zhong"); fixes.put('转', "zhuan"); fixes.put('处', "chu"); fixes.put('传', "chuan");
        fixes.put('单', "dan"); fixes.put('当', "dang"); fixes.put('倒', "dao");
        fixes.put('调', "diao"); fixes.put('度', "du"); fixes.put('分', "fen"); fixes.put('差', "cha");
        fixes.put('称', "cheng"); fixes.put('乘', "cheng"); fixes.put('更', "geng"); fixes.put('供', "gong");
        fixes.put('冠', "guan"); fixes.put('和', "he"); fixes.put('横', "heng"); fixes.put('划', "hua");
        fixes.put('还', "hai"); fixes.put('系', "xi"); fixes.put('假', "jia"); fixes.put('将', "jiang");
        fixes.put('强', "qiang"); fixes.put('教', "jiao"); fixes.put('角', "jiao"); fixes.put('校', "xiao");
        fixes.put('尽', "jin"); fixes.put('看', "kan"); fixes.put('空', "kong"); fixes.put('露', "lu");
        fixes.put('率', "lv"); fixes.put('落', "luo"); fixes.put('埋', "mai"); fixes.put('模', "mo");
        fixes.put('难', "nan"); fixes.put('宁', "ning"); fixes.put('片', "pian"); fixes.put('铺', "pu");
        fixes.put('曲', "qu"); fixes.put('圈', "quan"); fixes.put('散', "san"); fixes.put('色', "se");
        fixes.put('扇', "shan"); fixes.put('少', "shao"); fixes.put('省', "sheng"); fixes.put('石', "shi");
        fixes.put('熟', "shu"); fixes.put('宿', "su"); fixes.put('提', "ti"); fixes.put('挑', "tiao");
        fixes.put('吐', "tu"); fixes.put('瓦', "wa"); fixes.put('鲜', "xian"); fixes.put('相', "xiang");
        fixes.put('削', "xiao"); fixes.put('血', "xue"); fixes.put('兴', "xing"); fixes.put('应', "ying");
        fixes.put('着', "zhe"); fixes.put('钻', "zuan"); fixes.put('作', "zuo"); fixes.put('都', "dou");
        fixes.put('发', "fa"); fixes.put('干', "gan"); fixes.put('好', "hao"); fixes.put('会', "hui");
        fixes.put('降', "jiang"); fixes.put('觉', "jue"); fixes.put('乐', "le"); fixes.put('没', "mei");
        fixes.put('区', "qu"); fixes.put('要', "yao"); fixes.put('得', "de"); fixes.put('地', "di");
        POLYPHONIC_SYLLABLES = Collections.unmodifiableMap(fixes);

        PINYIN_FORMAT = new HanyuPinyinOutputFormat();
        PINYIN_FORMAT.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        PINYIN_FORMAT.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        PINYIN_FORMAT.setVCharType(HanyuPinyinVCharType.WITH_V);
    }

    private final Map<String, String> customGlossary = new LinkedHashMap<>();

    public void registerCustomTerm(String zhTerm, String englishSnakeWords) {
        if (zhTerm != null && !zhTerm.isBlank() && englishSnakeWords != null && !englishSnakeWords.isBlank()) {
            customGlossary.put(zhTerm.trim(), englishSnakeWords.trim().toLowerCase(Locale.ROOT));
        }
    }

    public List<String> tokenize(String cleanedText) {
        List<String> tokens = new ArrayList<>();
        if (cleanedText == null) {
            return List.of("element");
        }
        for (String segment : cleanedText.split("\\s+")) {
            if (segment.isEmpty()) {
                continue;
            }
            if (segment.matches("^[A-Za-z0-9_]+$")) {
                splitCamelOrSnake(segment, tokens);
            } else {
                forwardMaxMatchChinese(segment, tokens);
            }
        }
        return tokens.isEmpty() ? List.of("element") : tokens;
    }

    private void forwardMaxMatchChinese(String text, List<String> out) {
        int len = text.length();
        int i = 0;
        while (i < len) {
            char ch = text.charAt(i);
            if (ch < 128 && Character.isLetterOrDigit(ch)) {
                int j = i + 1;
                while (j < len && text.charAt(j) < 128 && Character.isLetterOrDigit(text.charAt(j))) {
                    j++;
                }
                splitCamelOrSnake(text.substring(i, j), out);
                i = j;
                continue;
            }
            String matched = null;
            // 最长 16 字：领域里的整词（如“列车当前在线路上的里程”）必须整体命中，避免被切碎成词根 + 拼音
            int maxSpan = Math.min(MAX_TERM_LENGTH, len - i);
            for (int span = maxSpan; span >= 2; span--) {
                String sub = text.substring(i, i + span);
                if (customGlossary.containsKey(sub)) {
                    matched = customGlossary.get(sub);
                    i += span;
                    break;
                }
                if (BUILTIN_DICT.containsKey(sub)) {
                    matched = BUILTIN_DICT.get(sub);
                    i += span;
                    break;
                }
            }
            if (matched != null) {
                Collections.addAll(out, matched.split("_"));
            } else {
                String single = String.valueOf(ch);
                if (BUILTIN_DICT.containsKey(single)) {
                    Collections.addAll(out, BUILTIN_DICT.get(single).split("_"));
                } else if (COMMON_CHAR_SYLLABLES.containsKey(ch)) {
                    out.add(COMMON_CHAR_SYLLABLES.get(ch));
                } else {
                    // Tier 4 兜底：完整拼音音节（严禁首字母缩写）；无法转拼音的字符用 u + 4 位十六进制码点
                    String syllable = syllableOf(ch);
                    out.add(syllable == null || syllable.isBlank() ? "u" + HexUtil.toHex4(ch) : syllable);
                }
                i++;
            }
        }
    }

    /**
     * Tier 4 兜底：完整拼音音节。多音字按建模语境固定取音
     */
    private static String syllableOf(char ch) {
        String fixed = POLYPHONIC_SYLLABLES.get(ch);
        if (fixed != null) {
            return fixed;
        }
        try {
            String[] syllables = PinyinHelper.toHanyuPinyinStringArray(ch, PINYIN_FORMAT);
            if (syllables != null && syllables.length > 0) {
                return syllables[0];
            }
        } catch (BadHanyuPinyinOutputFormatCombination ex) {
            throw new IllegalStateException("拼音输出格式配置非法", ex);
        }
        return null;
    }

    /**
     * 完整拼音音节序列（不查词典、不做首字母缩写）：供 MCP 工具名等必须与业务词典无关、纯 ASCII 的场合使用。
     * 汉字逐字取完整音节，ASCII 字母数字原样小写，其余字符作为分隔。
     */
    public static List<String> pinyinSyllables(String text) {
        List<String> out = new ArrayList<>();
        if (text == null) {
            return out;
        }
        StringBuilder ascii = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch < 128 && Character.isLetterOrDigit(ch)) {
                ascii.append(Character.toLowerCase(ch));
                continue;
            }
            if (ascii.length() > 0) {
                out.add(ascii.toString());
                ascii.setLength(0);
            }
            if (Character.isIdeographic(ch)) {
                String syllable = syllableOf(ch);
                if (syllable != null && !syllable.isBlank()) {
                    out.add(syllable);
                }
            }
        }
        if (ascii.length() > 0) {
            out.add(ascii.toString());
        }
        return out;
    }

    private static void splitCamelOrSnake(String ascii, List<String> out) {
        for (String part : ascii.split("_")) {
            if (!part.isEmpty()) {
                String splitCamel = part.replaceAll("([a-z0-9])([A-Z])", "$1_$2");
                for (String w : splitCamel.split("_")) {
                    if (!w.isEmpty()) {
                        out.add(w.toLowerCase(Locale.ROOT));
                    }
                }
            }
        }
    }

    private static final class HexUtil {
        static String toHex4(char c) {
            return String.format("%04x", (int) c);
        }
    }
}
