package com.groma.models.emergencydeduction844676b1.model.entity;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.BaseSimEntity;
import com.groma.base.engine.GromaMemberMeta;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction844676b1.model.entity.api.LineWithStationReadOnly;
import com.groma.models.emergencydeduction844676b1.model.enums.StationTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.List;

/**
 * 线路与车站实体（方圆对象类型原名：线路与车站）
 * <p>
 * 成员写入统一走带世界上下文的 Setter：框架按成员种类发出值变化(v)、状态进入/退出(sa/sx)、引用变化(ra/rv/rx) 事件，
 * 并维护反向引用索引，供类型设计视图里登记的事件接线派发。对象创建期间（首次被引用前）的成员赋值不产生事件。
 * </p>
 * 成员清单：
 * <ul>
 * <li>地铁线路（metroLine，REF）</li>
 * <li>车站（station，REF）</li>
 * <li>线路里程（lineMileage，BASE，单位 m）</li>
 * <li>站点类型（stationType，ENUM）</li>
 * <li>站号（stationNo，BASE）</li>
 * </ul>
 */
@Schema(
        title = "线路与车站",
        description = "方圆对象类型：线路与车站"
)
@GromaMeta(
        id = "bU09sM7lboHNQ4yfpX8",
        zhName = "线路与车站",
        kind = "TYPE"
)
public class LineWithStationEntity extends BaseSimEntity implements LineWithStationReadOnly {
    private static final List<GromaMemberMeta> MEMBERS = List.of(new GromaMemberMeta("RMXGiHtWwfLlhtvc2qN", "地铁线路", "metroLine", "REF", "com.groma.models.emergencydeduction844676b1.model.entity.MetroLineEntity", ""), new GromaMemberMeta("WaXfPfoMap0FvB1b0Sf", "车站", "station", "REF", "com.groma.models.emergencydeduction844676b1.model.entity.StationEntity", ""), new GromaMemberMeta("qAxmfURJraF6VumzkyH", "线路里程", "lineMileage", "BASE", "double", "m"), new GromaMemberMeta("X6iOVMMRFEgOu7oquve", "站点类型", "stationType", "ENUM", "com.groma.models.emergencydeduction844676b1.model.enums.StationTypeEnum", ""), new GromaMemberMeta("FGwVziXbhicx6BqbRNe", "站号", "stationNo", "BASE", "long", ""));

    /**
     * 地铁线路（方圆对象成员原名：地铁线路）
     */
    @Schema(
            title = "地铁线路"
    )
    @GromaMeta(
            id = "RMXGiHtWwfLlhtvc2qN",
            zhName = "地铁线路",
            kind = "FIELD"
    )
    private MetroLineEntity metroLine = null;

    /**
     * 车站（方圆对象成员原名：车站）
     */
    @Schema(
            title = "车站"
    )
    @GromaMeta(
            id = "WaXfPfoMap0FvB1b0Sf",
            zhName = "车站",
            kind = "FIELD"
    )
    private StationEntity station = null;

    /**
     * 线路里程（方圆属性成员原名：线路里程） 单位：m
     */
    @Schema(
            title = "线路里程"
    )
    @GromaMeta(
            id = "qAxmfURJraF6VumzkyH",
            zhName = "线路里程",
            kind = "FIELD",
            unit = "m"
    )
    private double lineMileage = 0.0D;

    /**
     * 站点类型（方圆状态成员原名：站点类型） 默认值：中间站 初始状态：中间站
     */
    @Schema(
            title = "站点类型"
    )
    @GromaMeta(
            id = "X6iOVMMRFEgOu7oquve",
            zhName = "站点类型",
            kind = "FIELD",
            initState = "中间站"
    )
    private StationTypeEnum stationType = StationTypeEnum.INTERMEDIATE_STATION;

    /**
     * 站号（方圆属性成员原名：站号）
     */
    @Schema(
            title = "站号"
    )
    @GromaMeta(
            id = "FGwVziXbhicx6BqbRNe",
            zhName = "站号",
            kind = "FIELD"
    )
    private long stationNo = 0L;

    /**
     * @param instanceId 实例标识（命名实例取画布对象实体 id，运行期生成的对象由世界上下文分配）
     */
    public LineWithStationEntity(String instanceId) {
        super(instanceId);
    }

    @Override
    public String getTypeCode() {
        return "LineWithStation";
    }

    @Override
    public String getTypeZhName() {
        return "线路与车站";
    }

    @Override
    public List<GromaMemberMeta> memberMetas() {
        return MEMBERS;
    }

    @Override
    public MetroLineEntity getMetroLine() {
        return this.metroLine;
    }

    /**
     * 写入地铁线路并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setMetroLine(MetroLineEntity value, GromaWorldContext ctx) {
        if (this.metroLine == value) {
            return;
        }
        MetroLineEntity before = this.metroLine;
        this.metroLine = value;
        markDirty(0);
        ctx.onMemberChanged(this, "RMXGiHtWwfLlhtvc2qN", before, value);
    }

    @Override
    public StationEntity getStation() {
        return this.station;
    }

    /**
     * 写入车站并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setStation(StationEntity value, GromaWorldContext ctx) {
        if (this.station == value) {
            return;
        }
        StationEntity before = this.station;
        this.station = value;
        markDirty(1);
        ctx.onMemberChanged(this, "WaXfPfoMap0FvB1b0Sf", before, value);
    }

    @Override
    public double getLineMileage() {
        return this.lineMileage;
    }

    /**
     * 写入线路里程并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setLineMileage(double value, GromaWorldContext ctx) {
        if (this.lineMileage == value) {
            return;
        }
        double before = this.lineMileage;
        this.lineMileage = value;
        markDirty(2);
        ctx.onMemberChanged(this, "qAxmfURJraF6VumzkyH", before, value);
    }

    /**
     * 直接写入线路里程（不发事件；仅用于初始化与对象创建期）
     */
    public void setLineMileage(double value) {
        this.lineMileage = value;
    }

    @Override
    public StationTypeEnum getStationType() {
        return this.stationType;
    }

    /**
     * 写入站点类型并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setStationType(StationTypeEnum value, GromaWorldContext ctx) {
        if (this.stationType == value) {
            return;
        }
        StationTypeEnum before = this.stationType;
        this.stationType = value;
        markDirty(3);
        ctx.onMemberChanged(this, "X6iOVMMRFEgOu7oquve", before, value);
    }

    /**
     * 直接写入站点类型（不发事件；仅用于初始化与对象创建期）
     */
    public void setStationType(StationTypeEnum value) {
        this.stationType = value;
    }

    @Override
    public long getStationNo() {
        return this.stationNo;
    }

    /**
     * 写入站号并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setStationNo(long value, GromaWorldContext ctx) {
        if (this.stationNo == value) {
            return;
        }
        long before = this.stationNo;
        this.stationNo = value;
        markDirty(4);
        ctx.onMemberChanged(this, "FGwVziXbhicx6BqbRNe", before, value);
    }

    /**
     * 直接写入站号（不发事件；仅用于初始化与对象创建期）
     */
    public void setStationNo(long value) {
        this.stationNo = value;
    }

    @Override
    public Object getMemberValue(String token) {
        switch (token) {
            case "RMXGiHtWwfLlhtvc2qN" -> {
                return this.metroLine;
            }
            case "WaXfPfoMap0FvB1b0Sf" -> {
                return this.station;
            }
            case "qAxmfURJraF6VumzkyH" -> {
                return this.lineMileage;
            }
            case "X6iOVMMRFEgOu7oquve" -> {
                return this.stationType;
            }
            case "FGwVziXbhicx6BqbRNe" -> {
                return this.stationNo;
            }
            default -> { return BaseSimEntity.MEMBER_NOT_FOUND; }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean setMemberValue(String token, Object value, GromaWorldContext ctx) {
        switch (token) {
            case "RMXGiHtWwfLlhtvc2qN" -> {
                this.setMetroLine((MetroLineEntity) GromaValues.decode(value, ctx), ctx);
            }
            case "WaXfPfoMap0FvB1b0Sf" -> {
                this.setStation((StationEntity) GromaValues.decode(value, ctx), ctx);
            }
            case "qAxmfURJraF6VumzkyH" -> {
                this.setLineMileage(GromaValues.toDouble(value), ctx);
            }
            case "X6iOVMMRFEgOu7oquve" -> {
                this.setStationType(value == null ? null : GromaValues.toEnum(StationTypeEnum.class, value), ctx);
            }
            case "FGwVziXbhicx6BqbRNe" -> {
                this.setStationNo(GromaValues.toLong(value), ctx);
            }
            default -> { return false; }
        }
        return true;
    }

    /**
     * 值复制（赋值运算）：基础值与枚举值逐成员复制，对象引用按引用复制；副本以新实例标识登记进世界
     */
    @Override
    public LineWithStationEntity copy(GromaWorldContext ctx) {
        LineWithStationEntity copy = new LineWithStationEntity(ctx.nextInstanceId("LineWithStation"));
        copy.openCreation();
        copy.setMetroLine(this.getMetroLine(), ctx);
        copy.setStation(this.getStation(), ctx);
        copy.setLineMileage(this.getLineMileage());
        copy.setStationType(this.getStationType());
        copy.setStationNo(this.getStationNo());
        ctx.register(copy);
        return copy;
    }
}
