package com.groma.models.emergencydeduction.model.entity;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.BaseSimEntity;
import com.groma.base.engine.GromaMemberMeta;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction.model.entity.api.FloodProtectionReadOnly;
import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.List;

/**
 * 挡水防护实体（方圆对象类型原名：挡水防护）
 * <p>
 * 成员写入统一走带世界上下文的 Setter：框架按成员种类发出值变化(v)、状态进入/退出(sa/sx)、引用变化(ra/rv/rx) 事件，
 * 并维护反向引用索引，供类型设计视图里登记的事件接线派发。对象创建期间（首次被引用前）的成员赋值不产生事件。
 * </p>
 * 成员清单：
 * <ul>
 * <li>防护挡水墙（protectiveFloodWall，LIST）</li>
 * <li>防护区间（protectedSection，REF）</li>
 * </ul>
 */
@Schema(
        title = "挡水防护",
        description = "方圆对象类型：挡水防护"
)
@GromaMeta(
        id = "a0FNVLNrTFTjGHd2bYh",
        zhName = "挡水防护",
        kind = "TYPE"
)
public class FloodProtectionEntity extends BaseSimEntity implements FloodProtectionReadOnly {
    private static final List<GromaMemberMeta> MEMBERS = List.of(new GromaMemberMeta("m0CoVE4j8kj040bE79P", "防护挡水墙", "protectiveFloodWall", "LIST", "java.util.List<com.groma.models.emergencydeduction.model.entity.StationEntity>", ""), new GromaMemberMeta("bwGKCA8ZWLc3ok0uzQz", "防护区间", "protectedSection", "REF", "com.groma.models.emergencydeduction.model.entity.MetroSectionEntity", ""));

    /**
     * 防护挡水墙（方圆属性成员原名：防护挡水墙）
     */
    @Schema(
            title = "防护挡水墙"
    )
    @GromaMeta(
            id = "m0CoVE4j8kj040bE79P",
            zhName = "防护挡水墙",
            kind = "FIELD"
    )
    private List<StationEntity> protectiveFloodWall = new java.util.ArrayList<>(16);

    /**
     * 防护区间（方圆对象成员原名：防护区间）
     */
    @Schema(
            title = "防护区间"
    )
    @GromaMeta(
            id = "bwGKCA8ZWLc3ok0uzQz",
            zhName = "防护区间",
            kind = "FIELD"
    )
    private MetroSectionEntity protectedSection = null;

    /**
     * @param instanceId 实例标识（命名实例取画布对象实体 id，运行期生成的对象由世界上下文分配）
     */
    public FloodProtectionEntity(String instanceId) {
        super(instanceId);
    }

    @Override
    public String getTypeCode() {
        return "FloodProtection";
    }

    @Override
    public String getTypeZhName() {
        return "挡水防护";
    }

    @Override
    public List<GromaMemberMeta> memberMetas() {
        return MEMBERS;
    }

    @Override
    public List<StationEntity> getProtectiveFloodWall() {
        return this.protectiveFloodWall;
    }

    /**
     * 写入防护挡水墙并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setProtectiveFloodWall(List<StationEntity> value, GromaWorldContext ctx) {
        if (this.protectiveFloodWall == value) {
            return;
        }
        List<StationEntity> before = this.protectiveFloodWall;
        this.protectiveFloodWall = value;
        markDirty(0);
        ctx.onMemberChanged(this, "m0CoVE4j8kj040bE79P", before, value);
    }

    /**
     * 直接写入防护挡水墙（不发事件；仅用于初始化与对象创建期）
     */
    public void setProtectiveFloodWall(List<StationEntity> value) {
        this.protectiveFloodWall = value;
    }

    @Override
    public MetroSectionEntity getProtectedSection() {
        return this.protectedSection;
    }

    /**
     * 写入防护区间并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setProtectedSection(MetroSectionEntity value, GromaWorldContext ctx) {
        if (this.protectedSection == value) {
            return;
        }
        MetroSectionEntity before = this.protectedSection;
        this.protectedSection = value;
        markDirty(1);
        ctx.onMemberChanged(this, "bwGKCA8ZWLc3ok0uzQz", before, value);
    }

    @Override
    public Object getMemberValue(String token) {
        switch (token) {
            case "m0CoVE4j8kj040bE79P" -> {
                return this.protectiveFloodWall;
            }
            case "bwGKCA8ZWLc3ok0uzQz" -> {
                return this.protectedSection;
            }
            default -> { return BaseSimEntity.MEMBER_NOT_FOUND; }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean setMemberValue(String token, Object value, GromaWorldContext ctx) {
        switch (token) {
            case "m0CoVE4j8kj040bE79P" -> {
                this.setProtectiveFloodWall((List<StationEntity>) GromaValues.decode(value, ctx), ctx);
            }
            case "bwGKCA8ZWLc3ok0uzQz" -> {
                this.setProtectedSection((MetroSectionEntity) GromaValues.decode(value, ctx), ctx);
            }
            default -> { return false; }
        }
        return true;
    }

    /**
     * 值复制（赋值运算）：基础值与枚举值逐成员复制，对象引用按引用复制；副本以新实例标识登记进世界
     */
    @Override
    public FloodProtectionEntity copy(GromaWorldContext ctx) {
        FloodProtectionEntity copy = new FloodProtectionEntity(ctx.nextInstanceId("FloodProtection"));
        copy.openCreation();
        copy.setProtectiveFloodWall(this.getProtectiveFloodWall());
        copy.setProtectedSection(this.getProtectedSection(), ctx);
        ctx.register(copy);
        return copy;
    }
}
