package com.groma.models.emergencydeduction.model.entity;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.BaseSimEntity;
import com.groma.base.engine.GromaMemberMeta;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction.model.entity.api.FileInfoReadOnly;
import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.List;

/**
 * 文件信息实体（方圆对象类型原名：文件信息）
 * <p>
 * 成员写入统一走带世界上下文的 Setter：框架按成员种类发出值变化(v)、状态进入/退出(sa/sx)、引用变化(ra/rv/rx) 事件，
 * 并维护反向引用索引，供类型设计视图里登记的事件接线派发。对象创建期间（首次被引用前）的成员赋值不产生事件。
 * </p>
 * 成员清单：
 * <ul>
 * <li>创建时间（createTime，BASE）</li>
 * <li>名称（name，BASE）</li>
 * <li>哈希值（hashValue，BASE）</li>
 * <li>大小（size，BASE）</li>
 * <li>类型（type，BASE）</li>
 * <li>修改时间（modifyTime，BASE）</li>
 * </ul>
 */
@Schema(
        title = "文件信息",
        description = "方圆对象类型：文件信息"
)
@GromaMeta(
        id = "kBqMrvjfh8tkTrIKWEk",
        zhName = "文件信息",
        kind = "TYPE"
)
public class FileInfoEntity extends BaseSimEntity implements FileInfoReadOnly {
    private static final List<GromaMemberMeta> MEMBERS = List.of(new GromaMemberMeta("Qw90Ju01RwLt1yd5L4G", "创建时间", "createTime", "BASE", "long", ""), new GromaMemberMeta("ULUgi4WDxQKuBMJXuen", "名称", "name", "BASE", "java.lang.String", ""), new GromaMemberMeta("KAdhc4M6XrYJ6Qwf5Gr", "哈希值", "hashValue", "BASE", "java.lang.String", ""), new GromaMemberMeta("fW746fHkt66FWhN4YM1", "大小", "size", "BASE", "double", ""), new GromaMemberMeta("JxS0RFjhdtbYQPcgLkA", "类型", "type", "BASE", "java.lang.String", ""), new GromaMemberMeta("qeT9LBbjlW9XrzCmI6S", "修改时间", "modifyTime", "BASE", "long", ""));

    /**
     * 创建时间（方圆属性成员原名：创建时间）
     */
    @Schema(
            title = "创建时间"
    )
    @GromaMeta(
            id = "Qw90Ju01RwLt1yd5L4G",
            zhName = "创建时间",
            kind = "FIELD"
    )
    private long createTime = 0L;

    /**
     * 名称（方圆属性成员原名：名称）
     */
    @Schema(
            title = "名称"
    )
    @GromaMeta(
            id = "ULUgi4WDxQKuBMJXuen",
            zhName = "名称",
            kind = "FIELD"
    )
    private String name = "";

    /**
     * 哈希值（方圆属性成员原名：哈希值）
     */
    @Schema(
            title = "哈希值"
    )
    @GromaMeta(
            id = "KAdhc4M6XrYJ6Qwf5Gr",
            zhName = "哈希值",
            kind = "FIELD"
    )
    private String hashValue = "";

    /**
     * 大小（方圆属性成员原名：大小）
     */
    @Schema(
            title = "大小"
    )
    @GromaMeta(
            id = "fW746fHkt66FWhN4YM1",
            zhName = "大小",
            kind = "FIELD"
    )
    private double size = 0.0D;

    /**
     * 类型（方圆属性成员原名：类型）
     */
    @Schema(
            title = "类型"
    )
    @GromaMeta(
            id = "JxS0RFjhdtbYQPcgLkA",
            zhName = "类型",
            kind = "FIELD"
    )
    private String type = "";

    /**
     * 修改时间（方圆属性成员原名：修改时间）
     */
    @Schema(
            title = "修改时间"
    )
    @GromaMeta(
            id = "qeT9LBbjlW9XrzCmI6S",
            zhName = "修改时间",
            kind = "FIELD"
    )
    private long modifyTime = 0L;

    /**
     * @param instanceId 实例标识（命名实例取画布对象实体 id，运行期生成的对象由世界上下文分配）
     */
    public FileInfoEntity(String instanceId) {
        super(instanceId);
    }

    @Override
    public String getTypeCode() {
        return "FileInfo";
    }

    @Override
    public String getTypeZhName() {
        return "文件信息";
    }

    @Override
    public List<GromaMemberMeta> memberMetas() {
        return MEMBERS;
    }

    @Override
    public long getCreateTime() {
        return this.createTime;
    }

    /**
     * 写入创建时间并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setCreateTime(long value, GromaWorldContext ctx) {
        if (this.createTime == value) {
            return;
        }
        long before = this.createTime;
        this.createTime = value;
        markDirty(0);
        ctx.onMemberChanged(this, "Qw90Ju01RwLt1yd5L4G", before, value);
    }

    /**
     * 直接写入创建时间（不发事件；仅用于初始化与对象创建期）
     */
    public void setCreateTime(long value) {
        this.createTime = value;
    }

    @Override
    public String getName() {
        return this.name;
    }

    /**
     * 写入名称并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setName(String value, GromaWorldContext ctx) {
        if (java.util.Objects.equals(this.name, value)) {
            return;
        }
        String before = this.name;
        this.name = value;
        markDirty(1);
        ctx.onMemberChanged(this, "ULUgi4WDxQKuBMJXuen", before, value);
    }

    /**
     * 直接写入名称（不发事件；仅用于初始化与对象创建期）
     */
    public void setName(String value) {
        this.name = value;
    }

    @Override
    public String getHashValue() {
        return this.hashValue;
    }

    /**
     * 写入哈希值并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setHashValue(String value, GromaWorldContext ctx) {
        if (java.util.Objects.equals(this.hashValue, value)) {
            return;
        }
        String before = this.hashValue;
        this.hashValue = value;
        markDirty(2);
        ctx.onMemberChanged(this, "KAdhc4M6XrYJ6Qwf5Gr", before, value);
    }

    /**
     * 直接写入哈希值（不发事件；仅用于初始化与对象创建期）
     */
    public void setHashValue(String value) {
        this.hashValue = value;
    }

    @Override
    public double getSize() {
        return this.size;
    }

    /**
     * 写入大小并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setSize(double value, GromaWorldContext ctx) {
        if (this.size == value) {
            return;
        }
        double before = this.size;
        this.size = value;
        markDirty(3);
        ctx.onMemberChanged(this, "fW746fHkt66FWhN4YM1", before, value);
    }

    /**
     * 直接写入大小（不发事件；仅用于初始化与对象创建期）
     */
    public void setSize(double value) {
        this.size = value;
    }

    @Override
    public String getType() {
        return this.type;
    }

    /**
     * 写入类型并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setType(String value, GromaWorldContext ctx) {
        if (java.util.Objects.equals(this.type, value)) {
            return;
        }
        String before = this.type;
        this.type = value;
        markDirty(4);
        ctx.onMemberChanged(this, "JxS0RFjhdtbYQPcgLkA", before, value);
    }

    /**
     * 直接写入类型（不发事件；仅用于初始化与对象创建期）
     */
    public void setType(String value) {
        this.type = value;
    }

    @Override
    public long getModifyTime() {
        return this.modifyTime;
    }

    /**
     * 写入修改时间并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setModifyTime(long value, GromaWorldContext ctx) {
        if (this.modifyTime == value) {
            return;
        }
        long before = this.modifyTime;
        this.modifyTime = value;
        markDirty(5);
        ctx.onMemberChanged(this, "qeT9LBbjlW9XrzCmI6S", before, value);
    }

    /**
     * 直接写入修改时间（不发事件；仅用于初始化与对象创建期）
     */
    public void setModifyTime(long value) {
        this.modifyTime = value;
    }

    @Override
    public Object getMemberValue(String token) {
        switch (token) {
            case "Qw90Ju01RwLt1yd5L4G" -> {
                return this.createTime;
            }
            case "ULUgi4WDxQKuBMJXuen" -> {
                return this.name;
            }
            case "KAdhc4M6XrYJ6Qwf5Gr" -> {
                return this.hashValue;
            }
            case "fW746fHkt66FWhN4YM1" -> {
                return this.size;
            }
            case "JxS0RFjhdtbYQPcgLkA" -> {
                return this.type;
            }
            case "qeT9LBbjlW9XrzCmI6S" -> {
                return this.modifyTime;
            }
            default -> { return BaseSimEntity.MEMBER_NOT_FOUND; }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean setMemberValue(String token, Object value, GromaWorldContext ctx) {
        switch (token) {
            case "Qw90Ju01RwLt1yd5L4G" -> {
                this.setCreateTime(GromaValues.toLong(value), ctx);
            }
            case "ULUgi4WDxQKuBMJXuen" -> {
                this.setName(GromaValues.toText(value), ctx);
            }
            case "KAdhc4M6XrYJ6Qwf5Gr" -> {
                this.setHashValue(GromaValues.toText(value), ctx);
            }
            case "fW746fHkt66FWhN4YM1" -> {
                this.setSize(GromaValues.toDouble(value), ctx);
            }
            case "JxS0RFjhdtbYQPcgLkA" -> {
                this.setType(GromaValues.toText(value), ctx);
            }
            case "qeT9LBbjlW9XrzCmI6S" -> {
                this.setModifyTime(GromaValues.toLong(value), ctx);
            }
            default -> { return false; }
        }
        return true;
    }

    /**
     * 值复制（赋值运算）：基础值与枚举值逐成员复制，对象引用按引用复制；副本以新实例标识登记进世界
     */
    @Override
    public FileInfoEntity copy(GromaWorldContext ctx) {
        FileInfoEntity copy = new FileInfoEntity(ctx.nextInstanceId("FileInfo"));
        copy.openCreation();
        copy.setCreateTime(this.getCreateTime());
        copy.setName(this.getName());
        copy.setHashValue(this.getHashValue());
        copy.setSize(this.getSize());
        copy.setType(this.getType());
        copy.setModifyTime(this.getModifyTime());
        ctx.register(copy);
        return copy;
    }
}
