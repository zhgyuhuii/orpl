package com.groma.models.emergencydeduction.model.entity;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.BaseSimEntity;
import com.groma.base.engine.GromaMemberMeta;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction.model.entity.api.RainfallCatchmentReadOnly;
import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.List;

/**
 * 降雨汇水实体（方圆对象类型原名：降雨汇水）
 * <p>
 * 成员写入统一走带世界上下文的 Setter：框架按成员种类发出值变化(v)、状态进入/退出(sa/sx)、引用变化(ra/rv/rx) 事件，
 * 并维护反向引用索引，供类型设计视图里登记的事件接线派发。对象创建期间（首次被引用前）的成员赋值不产生事件。
 * </p>
 * 成员清单：
 * <ul>
 * <li>降雨来源（rainfallSource，REF）</li>
 * <li>汇水区域（catchmentArea，LIST）</li>
 * </ul>
 */
@Schema(
        title = "降雨汇水",
        description = "方圆对象类型：降雨汇水"
)
@GromaMeta(
        id = "E32hWqA7cw2F3sNSc8K",
        zhName = "降雨汇水",
        kind = "TYPE"
)
public class RainfallCatchmentEntity extends BaseSimEntity implements RainfallCatchmentReadOnly {
    private static final List<GromaMemberMeta> MEMBERS = List.of(new GromaMemberMeta("ba15DPUPM07QgiJWWfb", "降雨来源", "rainfallSource", "REF", "com.groma.models.emergencydeduction.model.entity.RainfallEntity", ""), new GromaMemberMeta("Shu45x1YIGfSadlCre9", "汇水区域", "catchmentArea", "LIST", "java.util.List<com.groma.models.emergencydeduction.model.entity.AreaEntity>", ""));

    /**
     * 降雨来源（方圆对象成员原名：降雨来源）
     */
    @Schema(
            title = "降雨来源"
    )
    @GromaMeta(
            id = "ba15DPUPM07QgiJWWfb",
            zhName = "降雨来源",
            kind = "FIELD"
    )
    private RainfallEntity rainfallSource = null;

    /**
     * 汇水区域（方圆属性成员原名：汇水区域）
     */
    @Schema(
            title = "汇水区域"
    )
    @GromaMeta(
            id = "Shu45x1YIGfSadlCre9",
            zhName = "汇水区域",
            kind = "FIELD"
    )
    private List<AreaEntity> catchmentArea = new java.util.ArrayList<>(16);

    /**
     * @param instanceId 实例标识（命名实例取画布对象实体 id，运行期生成的对象由世界上下文分配）
     */
    public RainfallCatchmentEntity(String instanceId) {
        super(instanceId);
    }

    @Override
    public String getTypeCode() {
        return "RainfallCatchment";
    }

    @Override
    public String getTypeZhName() {
        return "降雨汇水";
    }

    @Override
    public List<GromaMemberMeta> memberMetas() {
        return MEMBERS;
    }

    @Override
    public RainfallEntity getRainfallSource() {
        return this.rainfallSource;
    }

    /**
     * 写入降雨来源并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setRainfallSource(RainfallEntity value, GromaWorldContext ctx) {
        if (this.rainfallSource == value) {
            return;
        }
        RainfallEntity before = this.rainfallSource;
        this.rainfallSource = value;
        markDirty(0);
        ctx.onMemberChanged(this, "ba15DPUPM07QgiJWWfb", before, value);
    }

    @Override
    public List<AreaEntity> getCatchmentArea() {
        return this.catchmentArea;
    }

    /**
     * 写入汇水区域并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setCatchmentArea(List<AreaEntity> value, GromaWorldContext ctx) {
        if (this.catchmentArea == value) {
            return;
        }
        List<AreaEntity> before = this.catchmentArea;
        this.catchmentArea = value;
        markDirty(1);
        ctx.onMemberChanged(this, "Shu45x1YIGfSadlCre9", before, value);
    }

    /**
     * 直接写入汇水区域（不发事件；仅用于初始化与对象创建期）
     */
    public void setCatchmentArea(List<AreaEntity> value) {
        this.catchmentArea = value;
    }

    @Override
    public Object getMemberValue(String token) {
        switch (token) {
            case "ba15DPUPM07QgiJWWfb" -> {
                return this.rainfallSource;
            }
            case "Shu45x1YIGfSadlCre9" -> {
                return this.catchmentArea;
            }
            default -> { return BaseSimEntity.MEMBER_NOT_FOUND; }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean setMemberValue(String token, Object value, GromaWorldContext ctx) {
        switch (token) {
            case "ba15DPUPM07QgiJWWfb" -> {
                this.setRainfallSource((RainfallEntity) GromaValues.decode(value, ctx), ctx);
            }
            case "Shu45x1YIGfSadlCre9" -> {
                this.setCatchmentArea((List<AreaEntity>) GromaValues.decode(value, ctx), ctx);
            }
            default -> { return false; }
        }
        return true;
    }

    /**
     * 值复制（赋值运算）：基础值与枚举值逐成员复制，对象引用按引用复制；副本以新实例标识登记进世界
     */
    @Override
    public RainfallCatchmentEntity copy(GromaWorldContext ctx) {
        RainfallCatchmentEntity copy = new RainfallCatchmentEntity(ctx.nextInstanceId("RainfallCatchment"));
        copy.openCreation();
        copy.setRainfallSource(this.getRainfallSource(), ctx);
        copy.setCatchmentArea(this.getCatchmentArea());
        ctx.register(copy);
        return copy;
    }
}
