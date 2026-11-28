package com.groma.models.emergencydeduction844676b1.model.entity;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.common.container.ObservableEntityList;
import com.groma.base.engine.BaseSimEntity;
import com.groma.base.engine.GromaMemberMeta;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction844676b1.model.entity.api.PlanGeneratorReadOnly;
import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.List;

/**
 * 方案生成器实体（方圆对象类型原名：方案生成器）
 * <p>
 * 成员写入统一走带世界上下文的 Setter：框架按成员种类发出值变化(v)、状态进入/退出(sa/sx)、引用变化(ra/rv/rx) 事件，
 * 并维护反向引用索引，供类型设计视图里登记的事件接线派发。对象创建期间（首次被引用前）的成员赋值不产生事件。
 * </p>
 * 成员清单：
 * <ul>
 * <li>备选方案列表（candidatePlanList，LIST）</li>
 * <li>降雨（rainfall，REF）</li>
 * <li>地面积水区（surfaceWaterloggingArea，REF）</li>
 * <li>挡水墙（floodWall，REF）</li>
 * <li>地铁区间（metroSection，REF）</li>
 * <li>列车（train，REF）</li>
 * </ul>
 */
@Schema(
        title = "方案生成器",
        description = "方圆对象类型：方案生成器"
)
@GromaMeta(
        id = "nd5eUBHKb67qBOkAHs9",
        zhName = "方案生成器",
        kind = "TYPE"
)
public class PlanGeneratorEntity extends BaseSimEntity implements PlanGeneratorReadOnly {
    private static final List<GromaMemberMeta> MEMBERS = List.of(new GromaMemberMeta("lyn4vqIguoVbutUdARW", "备选方案列表", "candidatePlanList", "LIST", "java.util.List<com.groma.models.emergencydeduction844676b1.model.entity.DisposalPlanEntity>", ""), new GromaMemberMeta("7sd1gdSUNacuOMI7apK", "降雨", "rainfall", "REF", "com.groma.models.emergencydeduction844676b1.model.entity.RainfallEntity", ""), new GromaMemberMeta("Y11bqLOOJYHpIDUyHQv", "地面积水区", "surfaceWaterloggingArea", "REF", "com.groma.models.emergencydeduction844676b1.model.entity.AreaEntity", ""), new GromaMemberMeta("qUcAbt0pZHr081flS5W", "挡水墙", "floodWall", "REF", "com.groma.models.emergencydeduction844676b1.model.entity.StationEntity", ""), new GromaMemberMeta("f5AiOO4UlGpWYjtGOOS", "地铁区间", "metroSection", "REF", "com.groma.models.emergencydeduction844676b1.model.entity.MetroSectionEntity", ""), new GromaMemberMeta("LrofSrmMKwJjCcMPQiW", "列车", "train", "REF", "com.groma.models.emergencydeduction844676b1.model.entity.TrainEntity", ""));

    /**
     * 备选方案列表（方圆属性成员原名：备选方案列表）
     */
    @Schema(
            title = "备选方案列表"
    )
    @GromaMeta(
            id = "lyn4vqIguoVbutUdARW",
            zhName = "备选方案列表",
            kind = "FIELD"
    )
    private List<DisposalPlanEntity> candidatePlanList = new ObservableEntityList<>(new ArrayList<>(16), null);

    /**
     * 降雨（方圆对象成员原名：降雨）
     */
    @Schema(
            title = "降雨"
    )
    @GromaMeta(
            id = "7sd1gdSUNacuOMI7apK",
            zhName = "降雨",
            kind = "FIELD"
    )
    private RainfallEntity rainfall = null;

    /**
     * 地面积水区（方圆对象成员原名：地面积水区）
     */
    @Schema(
            title = "地面积水区"
    )
    @GromaMeta(
            id = "Y11bqLOOJYHpIDUyHQv",
            zhName = "地面积水区",
            kind = "FIELD"
    )
    private AreaEntity surfaceWaterloggingArea = null;

    /**
     * 挡水墙（方圆对象成员原名：挡水墙）
     */
    @Schema(
            title = "挡水墙"
    )
    @GromaMeta(
            id = "qUcAbt0pZHr081flS5W",
            zhName = "挡水墙",
            kind = "FIELD"
    )
    private StationEntity floodWall = null;

    /**
     * 地铁区间（方圆对象成员原名：地铁区间）
     */
    @Schema(
            title = "地铁区间"
    )
    @GromaMeta(
            id = "f5AiOO4UlGpWYjtGOOS",
            zhName = "地铁区间",
            kind = "FIELD"
    )
    private MetroSectionEntity metroSection = null;

    /**
     * 列车（方圆对象成员原名：列车）
     */
    @Schema(
            title = "列车"
    )
    @GromaMeta(
            id = "LrofSrmMKwJjCcMPQiW",
            zhName = "列车",
            kind = "FIELD"
    )
    private TrainEntity train = null;

    /**
     * @param instanceId 实例标识（命名实例取画布对象实体 id，运行期生成的对象由世界上下文分配）
     */
    public PlanGeneratorEntity(String instanceId) {
        super(instanceId);
    }

    @Override
    public String getTypeCode() {
        return "PlanGenerator";
    }

    @Override
    public String getTypeZhName() {
        return "方案生成器";
    }

    @Override
    public List<GromaMemberMeta> memberMetas() {
        return MEMBERS;
    }

    @Override
    public List<DisposalPlanEntity> getCandidatePlanList() {
        return this.candidatePlanList;
    }

    /**
     * 写入备选方案列表并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setCandidatePlanList(List<DisposalPlanEntity> value, GromaWorldContext ctx) {
        if (this.candidatePlanList == value) {
            return;
        }
        List<DisposalPlanEntity> before = this.candidatePlanList;
        this.candidatePlanList = ObservableEntityList.observed(value, ctx.collectionSink(this, "lyn4vqIguoVbutUdARW"));
        markDirty(0);
        ctx.onMemberChanged(this, "lyn4vqIguoVbutUdARW", before, value);
    }

    /**
     * 直接写入备选方案列表（不发事件；仅用于初始化与对象创建期）
     */
    public void setCandidatePlanList(List<DisposalPlanEntity> value) {
        this.candidatePlanList = value;
    }

    @Override
    public RainfallEntity getRainfall() {
        return this.rainfall;
    }

    /**
     * 写入降雨并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setRainfall(RainfallEntity value, GromaWorldContext ctx) {
        if (this.rainfall == value) {
            return;
        }
        RainfallEntity before = this.rainfall;
        this.rainfall = value;
        markDirty(1);
        ctx.onMemberChanged(this, "7sd1gdSUNacuOMI7apK", before, value);
    }

    @Override
    public AreaEntity getSurfaceWaterloggingArea() {
        return this.surfaceWaterloggingArea;
    }

    /**
     * 写入地面积水区并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setSurfaceWaterloggingArea(AreaEntity value, GromaWorldContext ctx) {
        if (this.surfaceWaterloggingArea == value) {
            return;
        }
        AreaEntity before = this.surfaceWaterloggingArea;
        this.surfaceWaterloggingArea = value;
        markDirty(2);
        ctx.onMemberChanged(this, "Y11bqLOOJYHpIDUyHQv", before, value);
    }

    @Override
    public StationEntity getFloodWall() {
        return this.floodWall;
    }

    /**
     * 写入挡水墙并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setFloodWall(StationEntity value, GromaWorldContext ctx) {
        if (this.floodWall == value) {
            return;
        }
        StationEntity before = this.floodWall;
        this.floodWall = value;
        markDirty(3);
        ctx.onMemberChanged(this, "qUcAbt0pZHr081flS5W", before, value);
    }

    @Override
    public MetroSectionEntity getMetroSection() {
        return this.metroSection;
    }

    /**
     * 写入地铁区间并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setMetroSection(MetroSectionEntity value, GromaWorldContext ctx) {
        if (this.metroSection == value) {
            return;
        }
        MetroSectionEntity before = this.metroSection;
        this.metroSection = value;
        markDirty(4);
        ctx.onMemberChanged(this, "f5AiOO4UlGpWYjtGOOS", before, value);
    }

    @Override
    public TrainEntity getTrain() {
        return this.train;
    }

    /**
     * 写入列车并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setTrain(TrainEntity value, GromaWorldContext ctx) {
        if (this.train == value) {
            return;
        }
        TrainEntity before = this.train;
        this.train = value;
        markDirty(5);
        ctx.onMemberChanged(this, "LrofSrmMKwJjCcMPQiW", before, value);
    }

    @Override
    public Object getMemberValue(String token) {
        switch (token) {
            case "lyn4vqIguoVbutUdARW" -> {
                return this.candidatePlanList;
            }
            case "7sd1gdSUNacuOMI7apK" -> {
                return this.rainfall;
            }
            case "Y11bqLOOJYHpIDUyHQv" -> {
                return this.surfaceWaterloggingArea;
            }
            case "qUcAbt0pZHr081flS5W" -> {
                return this.floodWall;
            }
            case "f5AiOO4UlGpWYjtGOOS" -> {
                return this.metroSection;
            }
            case "LrofSrmMKwJjCcMPQiW" -> {
                return this.train;
            }
            default -> { return BaseSimEntity.MEMBER_NOT_FOUND; }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean setMemberValue(String token, Object value, GromaWorldContext ctx) {
        switch (token) {
            case "lyn4vqIguoVbutUdARW" -> {
                this.setCandidatePlanList((List<DisposalPlanEntity>) GromaValues.decode(value, ctx), ctx);
            }
            case "7sd1gdSUNacuOMI7apK" -> {
                this.setRainfall((RainfallEntity) GromaValues.decode(value, ctx), ctx);
            }
            case "Y11bqLOOJYHpIDUyHQv" -> {
                this.setSurfaceWaterloggingArea((AreaEntity) GromaValues.decode(value, ctx), ctx);
            }
            case "qUcAbt0pZHr081flS5W" -> {
                this.setFloodWall((StationEntity) GromaValues.decode(value, ctx), ctx);
            }
            case "f5AiOO4UlGpWYjtGOOS" -> {
                this.setMetroSection((MetroSectionEntity) GromaValues.decode(value, ctx), ctx);
            }
            case "LrofSrmMKwJjCcMPQiW" -> {
                this.setTrain((TrainEntity) GromaValues.decode(value, ctx), ctx);
            }
            default -> { return false; }
        }
        return true;
    }

    /**
     * 值复制（赋值运算）：基础值与枚举值逐成员复制，对象引用按引用复制；副本以新实例标识登记进世界
     */
    @Override
    public PlanGeneratorEntity copy(GromaWorldContext ctx) {
        PlanGeneratorEntity copy = new PlanGeneratorEntity(ctx.nextInstanceId("PlanGenerator"));
        copy.openCreation();
        copy.setCandidatePlanList(this.getCandidatePlanList());
        copy.setRainfall(this.getRainfall(), ctx);
        copy.setSurfaceWaterloggingArea(this.getSurfaceWaterloggingArea(), ctx);
        copy.setFloodWall(this.getFloodWall(), ctx);
        copy.setMetroSection(this.getMetroSection(), ctx);
        copy.setTrain(this.getTrain(), ctx);
        ctx.register(copy);
        return copy;
    }
}
