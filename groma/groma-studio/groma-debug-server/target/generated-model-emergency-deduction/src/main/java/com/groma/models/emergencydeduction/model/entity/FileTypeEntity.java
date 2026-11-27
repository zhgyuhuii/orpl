package com.groma.models.emergencydeduction.model.entity;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.BaseSimEntity;
import com.groma.base.engine.GromaMemberMeta;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction.model.entity.api.FileTypeReadOnly;
import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.List;

/**
 * 文件类型实体（方圆对象类型原名：文件类型）
 * <p>
 * 成员写入统一走带世界上下文的 Setter：框架按成员种类发出值变化(v)、状态进入/退出(sa/sx)、引用变化(ra/rv/rx) 事件，
 * 并维护反向引用索引，供类型设计视图里登记的事件接线派发。对象创建期间（首次被引用前）的成员赋值不产生事件。
 * </p>
 * 成员清单：
 * <ul>
 * <li>路径（path，BASE）</li>
 * <li>名称（name，BASE）</li>
 * <li>类型（type，BASE）</li>
 * <li>大小（size，BASE）</li>
 * <li>创建时间（createTime，BASE）</li>
 * <li>修改时间（modifyTime，BASE）</li>
 * <li>哈希值（hashValue，BASE）</li>
 * </ul>
 * 事件接线（类型设计视图）：
 * <ul>
 * <li>当路径 发生【值变化(v)】 时，启动过程「文件初始化」</li>
 * </ul>
 */
@Schema(
        title = "文件类型",
        description = "方圆对象类型：文件类型"
)
@GromaMeta(
        id = "53fc8e5ecfa54f1ba763324fcc8ff234",
        zhName = "文件类型",
        kind = "TYPE"
)
public class FileTypeEntity extends BaseSimEntity implements FileTypeReadOnly {
    private static final List<GromaMemberMeta> MEMBERS = List.of(new GromaMemberMeta("abc7c313392342d8bb2f1a44ef593248", "路径", "path", "BASE", "java.lang.String", ""), new GromaMemberMeta("f4b4b5db6ed34dab888b16dd765d6e1d", "名称", "name", "BASE", "java.lang.String", ""), new GromaMemberMeta("cac814e0b36746918e3fc1417d61a2b9", "类型", "type", "BASE", "java.lang.String", ""), new GromaMemberMeta("bda2971e273d43348520736daa19043f", "大小", "size", "BASE", "double", ""), new GromaMemberMeta("8bf20fa960b54e8c9fd6447f3b56a3aa", "创建时间", "createTime", "BASE", "long", ""), new GromaMemberMeta("a866e42f8fda44218b66fd816ea74285", "修改时间", "modifyTime", "BASE", "long", ""), new GromaMemberMeta("60d738e97a4e475ca0fac8ded93eb673", "哈希值", "hashValue", "BASE", "java.lang.String", ""));

    /**
     * 路径（方圆属性成员原名：路径）
     * 事件触发关系：当路径 发生【值变化(v)】 时，启动过程「文件初始化」
     */
    @Schema(
            title = "路径"
    )
    @GromaMeta(
            id = "abc7c313392342d8bb2f1a44ef593248",
            zhName = "路径",
            kind = "FIELD"
    )
    private String path = "";

    /**
     * 名称（方圆属性成员原名：名称）
     */
    @Schema(
            title = "名称"
    )
    @GromaMeta(
            id = "f4b4b5db6ed34dab888b16dd765d6e1d",
            zhName = "名称",
            kind = "FIELD"
    )
    private String name = "";

    /**
     * 类型（方圆属性成员原名：类型）
     */
    @Schema(
            title = "类型"
    )
    @GromaMeta(
            id = "cac814e0b36746918e3fc1417d61a2b9",
            zhName = "类型",
            kind = "FIELD"
    )
    private String type = "";

    /**
     * 大小（方圆属性成员原名：大小）
     */
    @Schema(
            title = "大小"
    )
    @GromaMeta(
            id = "bda2971e273d43348520736daa19043f",
            zhName = "大小",
            kind = "FIELD"
    )
    private double size = 0.0D;

    /**
     * 创建时间（方圆属性成员原名：创建时间）
     */
    @Schema(
            title = "创建时间"
    )
    @GromaMeta(
            id = "8bf20fa960b54e8c9fd6447f3b56a3aa",
            zhName = "创建时间",
            kind = "FIELD"
    )
    private long createTime = 0L;

    /**
     * 修改时间（方圆属性成员原名：修改时间）
     */
    @Schema(
            title = "修改时间"
    )
    @GromaMeta(
            id = "a866e42f8fda44218b66fd816ea74285",
            zhName = "修改时间",
            kind = "FIELD"
    )
    private long modifyTime = 0L;

    /**
     * 哈希值（方圆属性成员原名：哈希值）
     */
    @Schema(
            title = "哈希值"
    )
    @GromaMeta(
            id = "60d738e97a4e475ca0fac8ded93eb673",
            zhName = "哈希值",
            kind = "FIELD"
    )
    private String hashValue = "";

    /**
     * @param instanceId 实例标识（命名实例取画布对象实体 id，运行期生成的对象由世界上下文分配）
     */
    public FileTypeEntity(String instanceId) {
        super(instanceId);
    }

    @Override
    public String getTypeCode() {
        return "FileType";
    }

    @Override
    public String getTypeZhName() {
        return "文件类型";
    }

    @Override
    public List<GromaMemberMeta> memberMetas() {
        return MEMBERS;
    }

    @Override
    public String getPath() {
        return this.path;
    }

    /**
     * 写入路径并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setPath(String value, GromaWorldContext ctx) {
        if (java.util.Objects.equals(this.path, value)) {
            return;
        }
        String before = this.path;
        this.path = value;
        markDirty(0);
        ctx.onMemberChanged(this, "abc7c313392342d8bb2f1a44ef593248", before, value);
    }

    /**
     * 直接写入路径（不发事件；仅用于初始化与对象创建期）
     */
    public void setPath(String value) {
        this.path = value;
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
        ctx.onMemberChanged(this, "f4b4b5db6ed34dab888b16dd765d6e1d", before, value);
    }

    /**
     * 直接写入名称（不发事件；仅用于初始化与对象创建期）
     */
    public void setName(String value) {
        this.name = value;
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
        markDirty(2);
        ctx.onMemberChanged(this, "cac814e0b36746918e3fc1417d61a2b9", before, value);
    }

    /**
     * 直接写入类型（不发事件；仅用于初始化与对象创建期）
     */
    public void setType(String value) {
        this.type = value;
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
        ctx.onMemberChanged(this, "bda2971e273d43348520736daa19043f", before, value);
    }

    /**
     * 直接写入大小（不发事件；仅用于初始化与对象创建期）
     */
    public void setSize(double value) {
        this.size = value;
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
        markDirty(4);
        ctx.onMemberChanged(this, "8bf20fa960b54e8c9fd6447f3b56a3aa", before, value);
    }

    /**
     * 直接写入创建时间（不发事件；仅用于初始化与对象创建期）
     */
    public void setCreateTime(long value) {
        this.createTime = value;
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
        ctx.onMemberChanged(this, "a866e42f8fda44218b66fd816ea74285", before, value);
    }

    /**
     * 直接写入修改时间（不发事件；仅用于初始化与对象创建期）
     */
    public void setModifyTime(long value) {
        this.modifyTime = value;
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
        markDirty(6);
        ctx.onMemberChanged(this, "60d738e97a4e475ca0fac8ded93eb673", before, value);
    }

    /**
     * 直接写入哈希值（不发事件；仅用于初始化与对象创建期）
     */
    public void setHashValue(String value) {
        this.hashValue = value;
    }

    @Override
    public Object getMemberValue(String token) {
        switch (token) {
            case "abc7c313392342d8bb2f1a44ef593248" -> {
                return this.path;
            }
            case "f4b4b5db6ed34dab888b16dd765d6e1d" -> {
                return this.name;
            }
            case "cac814e0b36746918e3fc1417d61a2b9" -> {
                return this.type;
            }
            case "bda2971e273d43348520736daa19043f" -> {
                return this.size;
            }
            case "8bf20fa960b54e8c9fd6447f3b56a3aa" -> {
                return this.createTime;
            }
            case "a866e42f8fda44218b66fd816ea74285" -> {
                return this.modifyTime;
            }
            case "60d738e97a4e475ca0fac8ded93eb673" -> {
                return this.hashValue;
            }
            default -> { return BaseSimEntity.MEMBER_NOT_FOUND; }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean setMemberValue(String token, Object value, GromaWorldContext ctx) {
        switch (token) {
            case "abc7c313392342d8bb2f1a44ef593248" -> {
                this.setPath(GromaValues.toText(value), ctx);
            }
            case "f4b4b5db6ed34dab888b16dd765d6e1d" -> {
                this.setName(GromaValues.toText(value), ctx);
            }
            case "cac814e0b36746918e3fc1417d61a2b9" -> {
                this.setType(GromaValues.toText(value), ctx);
            }
            case "bda2971e273d43348520736daa19043f" -> {
                this.setSize(GromaValues.toDouble(value), ctx);
            }
            case "8bf20fa960b54e8c9fd6447f3b56a3aa" -> {
                this.setCreateTime(GromaValues.toLong(value), ctx);
            }
            case "a866e42f8fda44218b66fd816ea74285" -> {
                this.setModifyTime(GromaValues.toLong(value), ctx);
            }
            case "60d738e97a4e475ca0fac8ded93eb673" -> {
                this.setHashValue(GromaValues.toText(value), ctx);
            }
            default -> { return false; }
        }
        return true;
    }

    /**
     * 值复制（赋值运算）：基础值与枚举值逐成员复制，对象引用按引用复制；副本以新实例标识登记进世界
     */
    @Override
    public FileTypeEntity copy(GromaWorldContext ctx) {
        FileTypeEntity copy = new FileTypeEntity(ctx.nextInstanceId("FileType"));
        copy.openCreation();
        copy.setPath(this.getPath());
        copy.setName(this.getName());
        copy.setType(this.getType());
        copy.setSize(this.getSize());
        copy.setCreateTime(this.getCreateTime());
        copy.setModifyTime(this.getModifyTime());
        copy.setHashValue(this.getHashValue());
        ctx.register(copy);
        return copy;
    }
}
