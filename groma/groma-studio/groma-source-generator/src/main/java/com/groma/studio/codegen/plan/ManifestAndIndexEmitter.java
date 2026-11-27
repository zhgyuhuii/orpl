package com.groma.studio.codegen.plan;

import com.groma.studio.codegen.plan.PlanModel.MemberInfo;
import com.groma.studio.codegen.plan.PlanModel.ParamSlot;
import com.groma.studio.codegen.plan.PlanModel.ProcInfo;
import com.groma.studio.codegen.plan.PlanModel.SceneInfo;
import com.groma.studio.codegen.plan.PlanModel.TypeInfo;
import com.groma.studio.ir.CompilePlanIR;
import com.groma.studio.naming.DomainGlossaryTokenizer;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 模块级产物生成器：MCP 工具清单（META-INF/groma/mcp/manifest.json）、大模型全局语义导航索引
 * （GROMA_MODEL_INDEX.md，模块根目录与运行期资源各一份）、子模块 pom.xml 与运行时入口的 ServiceLoader 登记。
 */
public final class ManifestAndIndexEmitter {

    /** MCP 工具完整名 = <模型编码>__run_<ref>，上限 64 字符 */
    private static final int MCP_TOOL_NAME_LIMIT = 64;
    private static final ObjectMapper JSON = JsonMapper.builder()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .build();

    private final PlanModel model;

    public ManifestAndIndexEmitter(PlanModel model) {
        this.model = model;
    }

    // ------------------------------------------------------------------ MCP 清单

    public String manifestJson() {
        List<Map<String, Object>> processes = new ArrayList<>();
        Set<String> refs = new LinkedHashSet<>();
        int prefixLength = model.modelCode.replace('-', '_').toLowerCase().length() + "__run_".length();
        for (ProcInfo proc : model.procList) {
            String ref = toolRef(proc, MCP_TOOL_NAME_LIMIT - prefixLength, refs);
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("ref", ref);
            entry.put("entityId", proc.def().entityId());
            entry.put("name", proc.zhName());
            entry.put("description", proc.def().description() == null || proc.def().description().isBlank()
                    ? "执行方圆模型过程「" + proc.zhName() + "」"
                    + (proc.owner() == null ? "（独立过程）" : "（宿主对象：" + proc.owner().zhName() + "）")
                    : proc.def().description());
            entry.put("kind", proc.timeConsuming() ? "process" : "operator");
            entry.put("timeConsuming", proc.timeConsuming());
            entry.put("inputSchema", portsSchema(proc, true));
            entry.put("outputSchema", portsSchema(proc, false));
            entry.put("preconditions", List.of());
            processes.add(entry);
        }
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("manifestFormatVersion", "1.0");
        manifest.put("modelCode", model.modelCode);
        manifest.put("modelVersion", model.modelVersion);
        manifest.put("modelLabel", model.modelLabel);
        manifest.put("sourceDigest", model.ir.sourceDigest());
        manifest.put("scenes", model.scenes.stream().map(SceneInfo::sceneCode).toList());
        manifest.put("processes", processes);
        return JSON.writeValueAsString(manifest);
    }

    /** 过程 → 工具 ref：完整拼音音节下划线连接，超长时截断并追加确定性哈希，保证唯一 */
    private String toolRef(ProcInfo proc, int limit, Set<String> used) {
        String base = String.join("_", DomainGlossaryTokenizer.pinyinSyllables(proc.zhName()));
        if (base.isEmpty()) {
            base = "process";
        }
        String candidate = base.length() > limit ? base.substring(0, Math.max(1, limit - 5)) + "_" + shortHash(base) : base;
        int seq = 2;
        String unique = candidate;
        while (!used.add(unique)) {
            String suffix = "_" + seq++;
            String stem = candidate.length() + suffix.length() > limit
                    ? candidate.substring(0, Math.max(1, limit - suffix.length())) : candidate;
            unique = stem + suffix;
        }
        return unique;
    }

    private static String shortHash(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, 4);
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /** 形参 → JSON Schema（入参含宿主实例标识，出参为输出与双向形参） */
    private Map<String, Object> portsSchema(ProcInfo proc, boolean input) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        Map<String, Object> properties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();
        if (input && proc.owner() != null) {
            properties.put("hostInstanceId", paramSchema("宿主对象实例标识（" + proc.owner().zhName() + "）", "string", null));
            required.add("hostInstanceId");
        }
        for (ParamSlot param : proc.params()) {
            boolean included = input ? param.reads() : param.writes();
            if (!included) {
                continue;
            }
            String type;
            List<String> enumLabels = null;
            if (param.type() == com.palantir.javapoet.TypeName.DOUBLE) {
                type = "number";
            } else if (param.type() == com.palantir.javapoet.TypeName.LONG) {
                type = "integer";
            } else if (param.type() == com.palantir.javapoet.TypeName.BOOLEAN) {
                type = "boolean";
            } else if (com.groma.studio.codegen.plan.PlanModel.KIND_ENUM.equals(param.kind())) {
                type = "string";
                enumLabels = new ArrayList<>(model.enumOf(param.type()).constants().keySet());
            } else {
                type = "string";
            }
            String description = param.zhName() + (param.isRef() ? "（对象实例标识）" : "");
            Map<String, Object> prop = paramSchema(description, type, enumLabels);
            properties.put(param.zhName(), prop);
            if (input && param.nonnull()) {
                required.add(param.zhName());
            }
        }
        schema.put("properties", properties);
        schema.put("required", required);
        return schema;
    }

    private static Map<String, Object> paramSchema(String description, String type, List<String> enumLabels) {
        Map<String, Object> prop = new LinkedHashMap<>();
        prop.put("type", type);
        prop.put("description", description);
        if (enumLabels != null) {
            prop.put("enum", enumLabels);
        }
        return prop;
    }

    // ------------------------------------------------------------------ 模块 pom 与 ServiceLoader 登记

    public String modulePom() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                        <groupId>com.groma</groupId>
                        <artifactId>groma-models</artifactId>
                        <version>1.0.0</version>
                    </parent>

                    <artifactId>%s</artifactId>
                    <name>Groma Compiled Model :: %s (v%s)</name>
                    <description>由方圆模型「%s」编译产出的独立五层业务模块（模型 id：%s）。只依赖 groma-base 公共底座，不依赖其它模型子模块。</description>

                    <!-- 严格遵循 NP-GEN-005：模型子模块只依赖 groma-base 公共底座 -->
                    <dependencies>
                        <dependency>
                            <groupId>com.groma</groupId>
                            <artifactId>groma-base-common</artifactId>
                            <version>${project.version}</version>
                        </dependency>
                        <dependency>
                            <groupId>com.groma</groupId>
                            <artifactId>groma-base-engine</artifactId>
                            <version>${project.version}</version>
                        </dependency>
                        <dependency>
                            <groupId>com.groma</groupId>
                            <artifactId>groma-base-websocket</artifactId>
                            <version>${project.version}</version>
                        </dependency>
                        <dependency>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-starter-web</artifactId>
                        </dependency>
                        <dependency>
                            <groupId>io.swagger.core.v3</groupId>
                            <artifactId>swagger-annotations-jakarta</artifactId>
                            <version>2.2.25</version>
                        </dependency>
                    </dependencies>

                    <build>
                        <plugins>
                            <!-- 同步生成 *-sources.jar：大模型与开发者读取源码目录或二进制包时中文业务语义零丢失 -->
                            <plugin>
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-source-plugin</artifactId>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """.formatted(model.moduleDir, escapeXml(model.modelLabel), model.modelVersion,
                escapeXml(model.modelLabel), escapeXml(model.modelId));
    }

    private static String escapeXml(String text) {
        return text == null ? "" : text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    public String serviceRegistration() {
        return model.runtimeCn.canonicalName() + "\n";
    }

    // ------------------------------------------------------------------ 大模型语义索引

    public String indexMarkdown() {
        StringBuilder md = new StringBuilder(4096);
        md.append("# GROMA 模型全局语义导航地图：").append(model.modelLabel).append(" (v").append(model.modelVersion).append(")\n\n");
        md.append("> 本文件由方圆模型编译器自动生成，供大模型与开发者先读这一份建立全局上下文，再按需进入五层源码。\n\n");
        md.append("- **源模型 ID**：`").append(model.modelId).append("` | **模块编码**：`").append(model.modelCode)
          .append("` | **根包空间**：`").append(model.basePkg).append("`\n");
        md.append("- **源文件摘要**：`").append(model.ir.sourceDigest()).append("`\n");
        md.append("- **默认步长**：`").append(model.plan.configuredStepMs()).append(" ms`\n");
        md.append("- **规模**：对象类型 ").append(model.types.size()).append("，枚举 ").append(model.enums.size())
          .append("，过程定义 ").append(model.procList.size()).append("，事件接线 ").append(model.triggers.size())
          .append("，默认启动 ").append(model.defaultStarts.size()).append("，命名实例 ").append(model.instances.size()).append("\n\n");

        md.append("## 1. 权威运行场景清单\n\n");
        md.append("| 场景中文原名 | 场景编码 | 控制器路由 | 沙盒上下文类 |\n|---|---|---|---|\n");
        for (SceneInfo scene : model.scenes) {
            md.append("| ").append(scene.zhName()).append(" | `").append(scene.sceneCode()).append(" | `/api/v1/models/")
              .append(model.modelCode).append("/scenes/").append(scene.sceneCode()).append("` | `")
              .append(scene.contextSimple()).append("` |\n");
        }

        md.append("\n## 2. 领域对象与五层符号对照总表\n\n");
        md.append("| 中文原名 | 类型编码 | Entity 类 | 只读契约 | VO 类 | Service | 成员数 |\n|---|---|---|---|---|---|---|\n");
        for (TypeInfo type : model.types.values()) {
            md.append("| ").append(type.zhName()).append("（").append(type.kind()).append("） | `").append(type.code())
              .append("` | `").append(type.entity().simpleName()).append("` | `").append(type.readOnly().simpleName())
              .append("` | `").append(type.vo().simpleName()).append("` | `").append(type.service().simpleName())
              .append("` | ").append(type.allMembers().size()).append(" |\n");
        }

        md.append("\n### 2.1 成员对照（中文原名 → 英文成员 → 类型 / 单位 / 初始值）\n\n");
        for (TypeInfo type : model.types.values()) {
            if (type.ownMembers().isEmpty()) {
                continue;
            }
            md.append("**").append(type.zhName()).append("**\n\n| 中文原名 | 英文成员 | 访问方法 | 种类 | 单位 | 默认值 |\n|---|---|---|---|---|---|\n");
            for (MemberInfo member : type.ownMembers()) {
                md.append("| ").append(member.zhName()).append(" | `").append(member.field()).append("` | `")
                  .append(member.getter()).append("()` / `").append(member.setterCtx()).append("(v, ctx)` | ")
                  .append(member.kind()).append(" | ").append(nvl(member.unit())).append(" | ")
                  .append(PlanModel.KIND_ENUM.equals(member.kind()) ? nvl(member.initialState()) : nvl(member.defaultValue()))
                  .append(" |\n");
            }
            md.append('\n');
        }

        md.append("## 3. 枚举状态表\n\n| 枚举 | 状态（画布顺序） | 初始状态 |\n|---|---|---|\n");
        for (PlanModel.EnumInfo info : model.enums.values()) {
            md.append("| ").append(info.zhName()).append("（`").append(info.cn().simpleName()).append("`） | ")
              .append(String.join(" / ", info.constants().entrySet().stream()
                      .map(e -> e.getKey() + " `" + e.getValue() + "`").toList()))
              .append(" | ").append(nvl(info.initialLabel())).append(" |\n");
        }

        md.append("\n## 4. 过程契约摘要（入参 / 出参 / 耗时 / 节点数）\n\n");
        md.append("| 宿主 | 过程中文原名 | 状态机类 | 耗时 | 入参 | 出参 | 节点数 |\n|---|---|---|---|---|---|---|\n");
        for (ProcInfo proc : model.procList) {
            md.append("| ").append(proc.owner() == null ? "独立过程" : proc.owner().zhName()).append(" | ")
              .append(proc.zhName()).append(" | `").append(proc.taskSimple()).append("` | ")
              .append(proc.timeConsuming() ? "耗时" : "瞬时").append(" | ")
              .append(joinParams(proc, true)).append(" | ").append(joinParams(proc, false)).append(" | ")
              .append(proc.nodes().size()).append(" |\n");
        }

        md.append("\n### 4.1 过程体节点表（pc → 工具 / 子过程 → 后继）\n\n");
        for (ProcInfo proc : model.procList) {
            if (proc.nodes().isEmpty()) {
                continue;
            }
            md.append("**").append(proc.owner() == null ? "" : proc.owner().zhName() + " · ").append(proc.zhName())
              .append("**\n\n```text\n");
            for (PlanModel.NodeSlot slot : proc.nodes()) {
                md.append(String.format("pc%-3d %s", slot.pc(), slot.node().toolKey().isEmpty()
                        ? "调用 " + slot.node().zhName() + (slot.node().calleeTimeConsuming() ? "（耗时）" : "")
                        : slot.node().zhName() + " [" + slot.node().toolKey() + "]"
                          + (slot.node().slow() ? " [时间有关]" : "")));
                for (CompilePlanIR.ArgBindingIR in : slot.node().inputs()) {
                    md.append("  ").append(in.argName()).append('=').append(in.operand().getClass().getSimpleName());
                }
                md.append("  →");
                for (CompilePlanIR.NextIR next : slot.node().nexts()) {
                    md.append(" pc").append(next.targetPc()).append(next.conditional() ? "?" : "");
                }
                for (CompilePlanIR.PostIR post : slot.node().postActions()) {
                    md.append("  [").append(post.kind()).append(nvl2(" ", post.stateLabel())).append(']');
                }
                md.append('\n');
            }
            md.append("```\n\n");
        }

        md.append("## 5. 事件接线与默认启动（五维关系 → 因果链路）\n\n");
        md.append("| # | 触发条件 | 启动过程 | 成功后置动作 |\n|---|---|---|---|\n");
        int seq = 0;
        for (CompilePlanIR.DefaultStartIR start : model.defaultStarts) {
            seq++;
            TypeInfo owner = model.types.get(start.typeId());
            md.append("| ").append(seq).append(" | ").append(owner == null ? start.typeId() : owner.zhName())
              .append(" 创建（默认启动） | ").append(procZh(start.targetDefKey())).append(" | ")
              .append(describePosts(start.postActions())).append(" |\n");
        }
        for (CompilePlanIR.TriggerIR trigger : model.triggers) {
            seq++;
            TypeInfo source = model.types.get(trigger.sourceTypeId());
            md.append("| ").append(seq).append(" | ").append(source == null ? "?" : source.zhName()).append('.')
              .append(model.memberZhName(trigger.memberToken())).append(" 发生【")
              .append(ProcessEventNames.of(trigger.eventCode())).append("】")
              .append(nvl2(" 状态=", trigger.stateLabel())).append(describeJudge(trigger)).append(" | ")
              .append(procZh(trigger.targetDefKey())).append(" | ").append(describePosts(trigger.postActions())).append(" |\n");
        }

        md.append("\n### 5.1 因果链路拓扑\n\n```mermaid\nflowchart LR\n");
        int edgeSeq = 0;
        for (CompilePlanIR.DefaultStartIR start : model.defaultStarts) {
            TypeInfo owner = model.types.get(start.typeId());
            md.append("  S").append(edgeSeq).append("[\"").append(owner == null ? start.typeId() : owner.zhName())
              .append(" 创建\"] --> P").append(edgeSeq).append("[\"").append(procZh(start.targetDefKey())).append("\"]\n");
            edgeSeq++;
        }
        for (CompilePlanIR.TriggerIR trigger : model.triggers) {
            TypeInfo source = model.types.get(trigger.sourceTypeId());
            md.append("  E").append(edgeSeq).append("[\"").append(source == null ? "?" : source.zhName()).append('.')
              .append(model.memberZhName(trigger.memberToken())).append(' ').append(trigger.eventCode())
              .append(nvl2(" ", trigger.stateLabel())).append("\"] --> P").append(edgeSeq).append("[\"")
              .append(procZh(trigger.targetDefKey())).append("\"]\n");
            edgeSeq++;
        }
        md.append("```\n\n");

        md.append("## 6. 命名实例对象图（对象实体）\n\n| 实例 | 类型 | 构建方法 |\n|---|---|---|\n");
        for (PlanModel.InstanceInfo instance : model.instances) {
            md.append("| ").append(instance.zhName()).append(" | ").append(instance.type().zhName()).append(" | `")
              .append(instance.builderMethod()).append("()` |\n");
        }

        if (!model.plan.associationTypes().isEmpty()) {
            md.append("\n## 7. 关联类型（R07 / R08）\n\n| 关联类型 | 左端 | 右端 | 标签 | 基数 |\n|---|---|---|---|---|\n");
            for (CompilePlanIR.AssocTypeIR assoc : model.plan.associationTypes()) {
                md.append("| ").append(assoc.zhName()).append(" | ").append(assoc.leftZhName()).append(" | ")
                  .append(assoc.rightZhName()).append(" | ").append(nvl(assoc.labelLeftToRight())).append(" / ")
                  .append(nvl(assoc.labelRightToLeft())).append(" | ").append(nvl(assoc.constraintLeft())).append(" : ")
                  .append(nvl(assoc.constraintRight())).append(" |\n");
            }
        }

        if (!model.plan.warnings().isEmpty()) {
            md.append("\n## 8. 解析与编译诊断\n\n");
            for (String warning : model.diagnostics) {
                md.append("- ").append(warning.replace("\n", " ")).append('\n');
            }
        }
        return md.toString();
    }

    private String joinParams(ProcInfo proc, boolean input) {
        List<String> out = new ArrayList<>();
        for (ParamSlot param : proc.params()) {
            if (input ? param.reads() : param.writes()) {
                out.add(param.zhName() + ":" + param.type());
            }
        }
        return out.isEmpty() ? "—" : String.join("，", out);
    }

    private String describeJudge(CompilePlanIR.TriggerIR trigger) {
        if (trigger.judge().isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(" 且");
        for (CompilePlanIR.ConditionIR c : trigger.judge()) {
            sb.append(' ').append(c.op()).append(' ').append(c.literal());
        }
        return sb.toString();
    }

    private String describePosts(List<CompilePlanIR.PostIR> posts) {
        if (posts.isEmpty()) {
            return "—";
        }
        List<String> out = new ArrayList<>();
        for (CompilePlanIR.PostIR post : posts) {
            String target = post.target() instanceof CompilePlanIR.MemberPathIR path
                    ? model.memberZhName(path.memberIds().get(path.memberIds().size() - 1)) : "";
            out.add("OUT_CHANGE".equals(post.kind()) ? target + " → " + post.stateLabel()
                    : "RESULT".equals(post.kind()) ? "生成 " + target : post.kind() + " " + target);
        }
        return String.join("；", out);
    }

    private String procZh(String defKey) {
        ProcInfo proc = model.procs.get(defKey);
        return proc == null ? defKey : proc.zhName();
    }

    private static String nvl(String text) {
        return text == null ? "" : text;
    }

    private static String nvl2(String prefix, String text) {
        return text == null || text.isEmpty() ? "" : prefix + text;
    }
}
