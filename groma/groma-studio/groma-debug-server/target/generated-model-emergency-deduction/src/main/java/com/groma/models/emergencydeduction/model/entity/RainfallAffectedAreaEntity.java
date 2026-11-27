package com.groma.models.emergencydeduction.model.entity;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.BaseSimEntity;
import com.groma.base.engine.GromaMemberMeta;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction.model.entity.api.RainfallAffectedAreaReadOnly;
import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.Double;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.List;
import java.util.Map;

/**
 * 降雨与受影响区域实体（方圆对象类型原名：降雨与受影响区域）
 * <p>
 * 成员写入统一走带世界上下文的 Setter：框架按成员种类发出值变化(v)、状态进入/退出(sa/sx)、引用变化(ra/rv/rx) 事件，
 * 并维护反向引用索引，供类型设计视图里登记的事件接线派发。对象创建期间（首次被引用前）的成员赋值不产生事件。
 * </p>
 * 成员清单：
 * <ul>
 * <li>降雨（rainfall，REF）</li>
 * <li>区域（area，REF）</li>
 * <li>当前汇水流量（currentCatchmentFlow，BASE，单位 m³/s）</li>
 * <li>本步进汇水量（stepCatchmentVolume，BASE，单位 m³）</li>
 * <li>当前雨强（currentRainIntensity，BASE，单位 mm/min）</li>
 * <li>累计雨量（cumulativeRainfall，BASE，单位 mm）</li>
 * <li>雨量序列（rainfallSeries，MAP）</li>
 * </ul>
 */
@Schema(
        title = "降雨与受影响区域",
        description = "方圆对象类型：降雨与受影响区域"
)
@GromaMeta(
        id = "ZYR2XnvOQL0vp965s7q",
        zhName = "降雨与受影响区域",
        kind = "TYPE"
)
public class RainfallAffectedAreaEntity extends BaseSimEntity implements RainfallAffectedAreaReadOnly {
    private static final List<GromaMemberMeta> MEMBERS = List.of(new GromaMemberMeta("GLWeuTcB89MzugElwt0", "降雨", "rainfall", "REF", "com.groma.models.emergencydeduction.model.entity.RainfallEntity", ""), new GromaMemberMeta("qtEOSTOdGnKsYsTZdtB", "区域", "area", "REF", "com.groma.models.emergencydeduction.model.entity.AreaEntity", ""), new GromaMemberMeta("fgKKe41OQVCpWalhNdk", "当前汇水流量", "currentCatchmentFlow", "BASE", "double", "m³/s"), new GromaMemberMeta("HndwguqaTkiyqJNPDMm", "本步进汇水量", "stepCatchmentVolume", "BASE", "double", "m³"), new GromaMemberMeta("hCyfzjBn9ku9OmkUZGN", "当前雨强", "currentRainIntensity", "BASE", "double", "mm/min"), new GromaMemberMeta("xBY1ctaZuFkqQteAfD7", "累计雨量", "cumulativeRainfall", "BASE", "double", "mm"), new GromaMemberMeta("DN3St5fHDF0AIdYtDNY", "雨量序列", "rainfallSeries", "MAP", "java.util.Map<java.lang.Long, java.lang.Double>", ""));

    /**
     * 降雨（方圆对象成员原名：降雨）
     */
    @Schema(
            title = "降雨"
    )
    @GromaMeta(
            id = "GLWeuTcB89MzugElwt0",
            zhName = "降雨",
            kind = "FIELD"
    )
    private RainfallEntity rainfall = null;

    /**
     * 区域（方圆对象成员原名：区域）
     */
    @Schema(
            title = "区域"
    )
    @GromaMeta(
            id = "qtEOSTOdGnKsYsTZdtB",
            zhName = "区域",
            kind = "FIELD"
    )
    private AreaEntity area = null;

    /**
     * 当前汇水流量（方圆属性成员原名：当前汇水流量） 单位：m³/s
     */
    @Schema(
            title = "当前汇水流量"
    )
    @GromaMeta(
            id = "fgKKe41OQVCpWalhNdk",
            zhName = "当前汇水流量",
            kind = "FIELD",
            unit = "m³/s"
    )
    private double currentCatchmentFlow = 0.0D;

    /**
     * 本步进汇水量（方圆属性成员原名：本步进汇水量） 单位：m³
     */
    @Schema(
            title = "本步进汇水量"
    )
    @GromaMeta(
            id = "HndwguqaTkiyqJNPDMm",
            zhName = "本步进汇水量",
            kind = "FIELD",
            unit = "m³"
    )
    private double stepCatchmentVolume = 0.0D;

    /**
     * 当前雨强（方圆属性成员原名：当前雨强） 单位：mm/min
     */
    @Schema(
            title = "当前雨强"
    )
    @GromaMeta(
            id = "hCyfzjBn9ku9OmkUZGN",
            zhName = "当前雨强",
            kind = "FIELD",
            unit = "mm/min"
    )
    private double currentRainIntensity = 0.0D;

    /**
     * 累计雨量（方圆属性成员原名：累计雨量） 单位：mm
     */
    @Schema(
            title = "累计雨量"
    )
    @GromaMeta(
            id = "xBY1ctaZuFkqQteAfD7",
            zhName = "累计雨量",
            kind = "FIELD",
            unit = "mm"
    )
    private double cumulativeRainfall = 0.0D;

    /**
     * 雨量序列（方圆属性成员原名：雨量序列）
     */
    @Schema(
            title = "雨量序列"
    )
    @GromaMeta(
            id = "DN3St5fHDF0AIdYtDNY",
            zhName = "雨量序列",
            kind = "FIELD"
    )
    private Map<Long, Double> rainfallSeries = new java.util.LinkedHashMap<>(16);

    /**
     * @param instanceId 实例标识（命名实例取画布对象实体 id，运行期生成的对象由世界上下文分配）
     */
    public RainfallAffectedAreaEntity(String instanceId) {
        super(instanceId);
    }

    @Override
    public String getTypeCode() {
        return "RainfallAffectedArea";
    }

    @Override
    public String getTypeZhName() {
        return "降雨与受影响区域";
    }

    @Override
    public List<GromaMemberMeta> memberMetas() {
        return MEMBERS;
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
        markDirty(0);
        ctx.onMemberChanged(this, "GLWeuTcB89MzugElwt0", before, value);
    }

    @Override
    public AreaEntity getArea() {
        return this.area;
    }

    /**
     * 写入区域并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setArea(AreaEntity value, GromaWorldContext ctx) {
        if (this.area == value) {
            return;
        }
        AreaEntity before = this.area;
        this.area = value;
        markDirty(1);
        ctx.onMemberChanged(this, "qtEOSTOdGnKsYsTZdtB", before, value);
    }

    @Override
    public double getCurrentCatchmentFlow() {
        return this.currentCatchmentFlow;
    }

    /**
     * 写入当前汇水流量并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setCurrentCatchmentFlow(double value, GromaWorldContext ctx) {
        if (this.currentCatchmentFlow == value) {
            return;
        }
        double before = this.currentCatchmentFlow;
        this.currentCatchmentFlow = value;
        markDirty(2);
        ctx.onMemberChanged(this, "fgKKe41OQVCpWalhNdk", before, value);
    }

    /**
     * 直接写入当前汇水流量（不发事件；仅用于初始化与对象创建期）
     */
    public void setCurrentCatchmentFlow(double value) {
        this.currentCatchmentFlow = value;
    }

    @Override
    public double getStepCatchmentVolume() {
        return this.stepCatchmentVolume;
    }

    /**
     * 写入本步进汇水量并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setStepCatchmentVolume(double value, GromaWorldContext ctx) {
        if (this.stepCatchmentVolume == value) {
            return;
        }
        double before = this.stepCatchmentVolume;
        this.stepCatchmentVolume = value;
        markDirty(3);
        ctx.onMemberChanged(this, "HndwguqaTkiyqJNPDMm", before, value);
    }

    /**
     * 直接写入本步进汇水量（不发事件；仅用于初始化与对象创建期）
     */
    public void setStepCatchmentVolume(double value) {
        this.stepCatchmentVolume = value;
    }

    @Override
    public double getCurrentRainIntensity() {
        return this.currentRainIntensity;
    }

    /**
     * 写入当前雨强并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setCurrentRainIntensity(double value, GromaWorldContext ctx) {
        if (this.currentRainIntensity == value) {
            return;
        }
        double before = this.currentRainIntensity;
        this.currentRainIntensity = value;
        markDirty(4);
        ctx.onMemberChanged(this, "hCyfzjBn9ku9OmkUZGN", before, value);
    }

    /**
     * 直接写入当前雨强（不发事件；仅用于初始化与对象创建期）
     */
    public void setCurrentRainIntensity(double value) {
        this.currentRainIntensity = value;
    }

    @Override
    public double getCumulativeRainfall() {
        return this.cumulativeRainfall;
    }

    /**
     * 写入累计雨量并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setCumulativeRainfall(double value, GromaWorldContext ctx) {
        if (this.cumulativeRainfall == value) {
            return;
        }
        double before = this.cumulativeRainfall;
        this.cumulativeRainfall = value;
        markDirty(5);
        ctx.onMemberChanged(this, "xBY1ctaZuFkqQteAfD7", before, value);
    }

    /**
     * 直接写入累计雨量（不发事件；仅用于初始化与对象创建期）
     */
    public void setCumulativeRainfall(double value) {
        this.cumulativeRainfall = value;
    }

    @Override
    public Map<Long, Double> getRainfallSeries() {
        return this.rainfallSeries;
    }

    /**
     * 写入雨量序列并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setRainfallSeries(Map<Long, Double> value, GromaWorldContext ctx) {
        if (this.rainfallSeries == value) {
            return;
        }
        Map<Long, Double> before = this.rainfallSeries;
        this.rainfallSeries = value;
        markDirty(6);
        ctx.onMemberChanged(this, "DN3St5fHDF0AIdYtDNY", before, value);
    }

    /**
     * 直接写入雨量序列（不发事件；仅用于初始化与对象创建期）
     */
    public void setRainfallSeries(Map<Long, Double> value) {
        this.rainfallSeries = value;
    }

    @Override
    public Object getMemberValue(String token) {
        switch (token) {
            case "GLWeuTcB89MzugElwt0" -> {
                return this.rainfall;
            }
            case "qtEOSTOdGnKsYsTZdtB" -> {
                return this.area;
            }
            case "fgKKe41OQVCpWalhNdk" -> {
                return this.currentCatchmentFlow;
            }
            case "HndwguqaTkiyqJNPDMm" -> {
                return this.stepCatchmentVolume;
            }
            case "hCyfzjBn9ku9OmkUZGN" -> {
                return this.currentRainIntensity;
            }
            case "xBY1ctaZuFkqQteAfD7" -> {
                return this.cumulativeRainfall;
            }
            case "DN3St5fHDF0AIdYtDNY" -> {
                return this.rainfallSeries;
            }
            default -> { return BaseSimEntity.MEMBER_NOT_FOUND; }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean setMemberValue(String token, Object value, GromaWorldContext ctx) {
        switch (token) {
            case "GLWeuTcB89MzugElwt0" -> {
                this.setRainfall((RainfallEntity) GromaValues.decode(value, ctx), ctx);
            }
            case "qtEOSTOdGnKsYsTZdtB" -> {
                this.setArea((AreaEntity) GromaValues.decode(value, ctx), ctx);
            }
            case "fgKKe41OQVCpWalhNdk" -> {
                this.setCurrentCatchmentFlow(GromaValues.toDouble(value), ctx);
            }
            case "HndwguqaTkiyqJNPDMm" -> {
                this.setStepCatchmentVolume(GromaValues.toDouble(value), ctx);
            }
            case "hCyfzjBn9ku9OmkUZGN" -> {
                this.setCurrentRainIntensity(GromaValues.toDouble(value), ctx);
            }
            case "xBY1ctaZuFkqQteAfD7" -> {
                this.setCumulativeRainfall(GromaValues.toDouble(value), ctx);
            }
            case "DN3St5fHDF0AIdYtDNY" -> {
                this.setRainfallSeries((Map<Long, Double>) GromaValues.decode(value, ctx), ctx);
            }
            default -> { return false; }
        }
        return true;
    }

    /**
     * 值复制（赋值运算）：基础值与枚举值逐成员复制，对象引用按引用复制；副本以新实例标识登记进世界
     */
    @Override
    public RainfallAffectedAreaEntity copy(GromaWorldContext ctx) {
        RainfallAffectedAreaEntity copy = new RainfallAffectedAreaEntity(ctx.nextInstanceId("RainfallAffectedArea"));
        copy.openCreation();
        copy.setRainfall(this.getRainfall(), ctx);
        copy.setArea(this.getArea(), ctx);
        copy.setCurrentCatchmentFlow(this.getCurrentCatchmentFlow());
        copy.setStepCatchmentVolume(this.getStepCatchmentVolume());
        copy.setCurrentRainIntensity(this.getCurrentRainIntensity());
        copy.setCumulativeRainfall(this.getCumulativeRainfall());
        copy.setRainfallSeries(this.getRainfallSeries());
        ctx.register(copy);
        return copy;
    }
}
