package com.groma.models.emergencydeduction.model.entity;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.BaseSimEntity;
import com.groma.base.engine.GromaMemberMeta;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction.model.entity.api.MetroLineReadOnly;
import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.List;

/**
 * 地铁线路实体（方圆对象类型原名：地铁线路）
 * <p>
 * 成员写入统一走带世界上下文的 Setter：框架按成员种类发出值变化(v)、状态进入/退出(sa/sx)、引用变化(ra/rv/rx) 事件，
 * 并维护反向引用索引，供类型设计视图里登记的事件接线派发。对象创建期间（首次被引用前）的成员赋值不产生事件。
 * </p>
 * 成员清单：
 * <ul>
 * <li>线路名称（lineName，BASE）</li>
 * <li>起点里程（startMileage，BASE，单位 m）</li>
 * <li>终点里程（endMileage，BASE，单位 m）</li>
 * </ul>
 */
@Schema(
        title = "地铁线路",
        description = "方圆对象类型：地铁线路"
)
@GromaMeta(
        id = "TACREef9cnQRxb0lPDt",
        zhName = "地铁线路",
        kind = "TYPE"
)
public class MetroLineEntity extends BaseSimEntity implements MetroLineReadOnly {
    private static final List<GromaMemberMeta> MEMBERS = List.of(new GromaMemberMeta("Osip4AKZj87pYuthVsO", "线路名称", "lineName", "BASE", "java.lang.String", ""), new GromaMemberMeta("myTbJXCWLUt9n1c6X9A", "起点里程", "startMileage", "BASE", "double", "m"), new GromaMemberMeta("efdxuWnrUmLGrfyi3fs", "终点里程", "endMileage", "BASE", "double", "m"));

    /**
     * 线路名称（方圆属性成员原名：线路名称）
     */
    @Schema(
            title = "线路名称"
    )
    @GromaMeta(
            id = "Osip4AKZj87pYuthVsO",
            zhName = "线路名称",
            kind = "FIELD"
    )
    private String lineName = "";

    /**
     * 起点里程（方圆属性成员原名：起点里程） 单位：m 默认值：0
     */
    @Schema(
            title = "起点里程"
    )
    @GromaMeta(
            id = "myTbJXCWLUt9n1c6X9A",
            zhName = "起点里程",
            kind = "FIELD",
            unit = "m"
    )
    private double startMileage = 0.0D;

    /**
     * 终点里程（方圆属性成员原名：终点里程） 单位：m 默认值：10000
     */
    @Schema(
            title = "终点里程"
    )
    @GromaMeta(
            id = "efdxuWnrUmLGrfyi3fs",
            zhName = "终点里程",
            kind = "FIELD",
            unit = "m"
    )
    private double endMileage = 10000.0D;

    /**
     * @param instanceId 实例标识（命名实例取画布对象实体 id，运行期生成的对象由世界上下文分配）
     */
    public MetroLineEntity(String instanceId) {
        super(instanceId);
    }

    @Override
    public String getTypeCode() {
        return "MetroLine";
    }

    @Override
    public String getTypeZhName() {
        return "地铁线路";
    }

    @Override
    public List<GromaMemberMeta> memberMetas() {
        return MEMBERS;
    }

    @Override
    public String getLineName() {
        return this.lineName;
    }

    /**
     * 写入线路名称并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setLineName(String value, GromaWorldContext ctx) {
        if (java.util.Objects.equals(this.lineName, value)) {
            return;
        }
        String before = this.lineName;
        this.lineName = value;
        markDirty(0);
        ctx.onMemberChanged(this, "Osip4AKZj87pYuthVsO", before, value);
    }

    /**
     * 直接写入线路名称（不发事件；仅用于初始化与对象创建期）
     */
    public void setLineName(String value) {
        this.lineName = value;
    }

    @Override
    public double getStartMileage() {
        return this.startMileage;
    }

    /**
     * 写入起点里程并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setStartMileage(double value, GromaWorldContext ctx) {
        if (this.startMileage == value) {
            return;
        }
        double before = this.startMileage;
        this.startMileage = value;
        markDirty(1);
        ctx.onMemberChanged(this, "myTbJXCWLUt9n1c6X9A", before, value);
    }

    /**
     * 直接写入起点里程（不发事件；仅用于初始化与对象创建期）
     */
    public void setStartMileage(double value) {
        this.startMileage = value;
    }

    @Override
    public double getEndMileage() {
        return this.endMileage;
    }

    /**
     * 写入终点里程并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setEndMileage(double value, GromaWorldContext ctx) {
        if (this.endMileage == value) {
            return;
        }
        double before = this.endMileage;
        this.endMileage = value;
        markDirty(2);
        ctx.onMemberChanged(this, "efdxuWnrUmLGrfyi3fs", before, value);
    }

    /**
     * 直接写入终点里程（不发事件；仅用于初始化与对象创建期）
     */
    public void setEndMileage(double value) {
        this.endMileage = value;
    }

    @Override
    public Object getMemberValue(String token) {
        switch (token) {
            case "Osip4AKZj87pYuthVsO" -> {
                return this.lineName;
            }
            case "myTbJXCWLUt9n1c6X9A" -> {
                return this.startMileage;
            }
            case "efdxuWnrUmLGrfyi3fs" -> {
                return this.endMileage;
            }
            default -> { return BaseSimEntity.MEMBER_NOT_FOUND; }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean setMemberValue(String token, Object value, GromaWorldContext ctx) {
        switch (token) {
            case "Osip4AKZj87pYuthVsO" -> {
                this.setLineName(GromaValues.toText(value), ctx);
            }
            case "myTbJXCWLUt9n1c6X9A" -> {
                this.setStartMileage(GromaValues.toDouble(value), ctx);
            }
            case "efdxuWnrUmLGrfyi3fs" -> {
                this.setEndMileage(GromaValues.toDouble(value), ctx);
            }
            default -> { return false; }
        }
        return true;
    }

    /**
     * 值复制（赋值运算）：基础值与枚举值逐成员复制，对象引用按引用复制；副本以新实例标识登记进世界
     */
    @Override
    public MetroLineEntity copy(GromaWorldContext ctx) {
        MetroLineEntity copy = new MetroLineEntity(ctx.nextInstanceId("MetroLine"));
        copy.openCreation();
        copy.setLineName(this.getLineName());
        copy.setStartMileage(this.getStartMileage());
        copy.setEndMileage(this.getEndMileage());
        ctx.register(copy);
        return copy;
    }
}
