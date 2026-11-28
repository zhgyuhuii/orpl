package com.groma.models.emergencydeduction844676b1.model.entity;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.BaseSimEntity;
import com.groma.base.engine.GromaMemberMeta;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction844676b1.model.entity.api.LineWithTrainReadOnly;
import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.List;

/**
 * 线路与列车实体（方圆对象类型原名：线路与列车）
 * <p>
 * 成员写入统一走带世界上下文的 Setter：框架按成员种类发出值变化(v)、状态进入/退出(sa/sx)、引用变化(ra/rv/rx) 事件，
 * 并维护反向引用索引，供类型设计视图里登记的事件接线派发。对象创建期间（首次被引用前）的成员赋值不产生事件。
 * </p>
 * 成员清单：
 * <ul>
 * <li>线路（line，REF）</li>
 * <li>列车（train，REF）</li>
 * <li>列车当前在线路上的里程（trainMileageOnLine，BASE，单位 m）</li>
 * <li>沿里程增大方向（alongIncreasingMileage，BASE）</li>
 * <li>当前停靠关联（currentParkedRelation，REF）</li>
 * <li>当前行驶关联（currentRunningRelation，REF）</li>
 * <li>演示往返（demoRoundTrip，BASE）</li>
 * </ul>
 * 事件接线（类型设计视图）：
 * <ul>
 * <li>当当前停靠关联 发生【获得引用(ra)】 时，启动过程「进站」</li>
 * <li>当当前停靠关联 发生【引用改向(rv)】 时，启动过程「进站」</li>
 * <li>当列车状态 进入状态「停站」 发生【状态进入(sa)】 时，启动过程「停站」</li>
 * <li>当离站是否允许 发生【值变化(v)】 且满足线上判断 = true 时，启动过程「离站」</li>
 * <li>当列车状态 进入状态「行驶」 发生【状态进入(sa)】 时，启动过程「形成区间与列车的关联」</li>
 * <li>当是否到达目标车站 发生【值变化(v)】 且满足线上判断 = true 时，启动过程「形成车站与列车的关联」</li>
 * <li>当当前行驶关联 发生【获得引用(ra)】 时，启动过程「区间行驶」</li>
 * <li>当当前行驶关联 发生【引用改向(rv)】 时，启动过程「区间行驶」</li>
 * </ul>
 */
@Schema(
        title = "线路与列车",
        description = "方圆对象类型：线路与列车"
)
@GromaMeta(
        id = "LM3IeUACJIRWE5PKqrG",
        zhName = "线路与列车",
        kind = "TYPE"
)
public class LineWithTrainEntity extends BaseSimEntity implements LineWithTrainReadOnly {
    private static final List<GromaMemberMeta> MEMBERS = List.of(new GromaMemberMeta("I74UeI1njzX6WjftjtR", "线路", "line", "REF", "com.groma.models.emergencydeduction844676b1.model.entity.MetroLineEntity", ""), new GromaMemberMeta("OxQTIIgXFvoh8WDwjOh", "列车", "train", "REF", "com.groma.models.emergencydeduction844676b1.model.entity.TrainEntity", ""), new GromaMemberMeta("h2d1gowrlEtddEalcvT", "列车当前在线路上的里程", "trainMileageOnLine", "BASE", "double", "m"), new GromaMemberMeta("u79Sh40LbloyGRW4zMk", "沿里程增大方向", "alongIncreasingMileage", "BASE", "boolean", ""), new GromaMemberMeta("0SOv8AQOpa2y8sC7shh", "当前停靠关联", "currentParkedRelation", "REF", "com.groma.models.emergencydeduction844676b1.model.entity.StationWithParkedTrainEntity", ""), new GromaMemberMeta("x3P9T6T0WTa3KXjrMbT", "当前行驶关联", "currentRunningRelation", "REF", "com.groma.models.emergencydeduction844676b1.model.entity.SectionWithTrainEntity", ""), new GromaMemberMeta("VwizqIil29sv75BuLPm", "演示往返", "demoRoundTrip", "BASE", "boolean", ""));

    /**
     * 线路（方圆对象成员原名：线路）
     */
    @Schema(
            title = "线路"
    )
    @GromaMeta(
            id = "I74UeI1njzX6WjftjtR",
            zhName = "线路",
            kind = "FIELD"
    )
    private MetroLineEntity line = null;

    /**
     * 列车（方圆对象成员原名：列车）
     */
    @Schema(
            title = "列车"
    )
    @GromaMeta(
            id = "OxQTIIgXFvoh8WDwjOh",
            zhName = "列车",
            kind = "FIELD"
    )
    private TrainEntity train = null;

    /**
     * 列车当前在线路上的里程（方圆属性成员原名：列车当前在线路上的里程） 单位：m
     */
    @Schema(
            title = "列车当前在线路上的里程"
    )
    @GromaMeta(
            id = "h2d1gowrlEtddEalcvT",
            zhName = "列车当前在线路上的里程",
            kind = "FIELD",
            unit = "m"
    )
    private double trainMileageOnLine = 0.0D;

    /**
     * 沿里程增大方向（方圆属性成员原名：沿里程增大方向） 默认值：true
     */
    @Schema(
            title = "沿里程增大方向"
    )
    @GromaMeta(
            id = "u79Sh40LbloyGRW4zMk",
            zhName = "沿里程增大方向",
            kind = "FIELD"
    )
    private boolean alongIncreasingMileage = true;

    /**
     * 当前停靠关联（方圆对象成员原名：当前停靠关联）
     * 事件触发关系：当当前停靠关联 发生【获得引用(ra)】 时，启动过程「进站」
     * 事件触发关系：当当前停靠关联 发生【引用改向(rv)】 时，启动过程「进站」
     */
    @Schema(
            title = "当前停靠关联"
    )
    @GromaMeta(
            id = "0SOv8AQOpa2y8sC7shh",
            zhName = "当前停靠关联",
            kind = "FIELD"
    )
    private StationWithParkedTrainEntity currentParkedRelation = null;

    /**
     * 当前行驶关联（方圆对象成员原名：当前行驶关联）
     * 事件触发关系：当当前行驶关联 发生【获得引用(ra)】 时，启动过程「区间行驶」
     * 事件触发关系：当当前行驶关联 发生【引用改向(rv)】 时，启动过程「区间行驶」
     */
    @Schema(
            title = "当前行驶关联"
    )
    @GromaMeta(
            id = "x3P9T6T0WTa3KXjrMbT",
            zhName = "当前行驶关联",
            kind = "FIELD"
    )
    private SectionWithTrainEntity currentRunningRelation = null;

    /**
     * 演示往返（方圆属性成员原名：演示往返） 默认值：false
     */
    @Schema(
            title = "演示往返"
    )
    @GromaMeta(
            id = "VwizqIil29sv75BuLPm",
            zhName = "演示往返",
            kind = "FIELD"
    )
    private boolean demoRoundTrip = false;

    /**
     * @param instanceId 实例标识（命名实例取画布对象实体 id，运行期生成的对象由世界上下文分配）
     */
    public LineWithTrainEntity(String instanceId) {
        super(instanceId);
    }

    @Override
    public String getTypeCode() {
        return "LineWithTrain";
    }

    @Override
    public String getTypeZhName() {
        return "线路与列车";
    }

    @Override
    public List<GromaMemberMeta> memberMetas() {
        return MEMBERS;
    }

    @Override
    public MetroLineEntity getLine() {
        return this.line;
    }

    /**
     * 写入线路并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setLine(MetroLineEntity value, GromaWorldContext ctx) {
        if (this.line == value) {
            return;
        }
        MetroLineEntity before = this.line;
        this.line = value;
        markDirty(0);
        ctx.onMemberChanged(this, "I74UeI1njzX6WjftjtR", before, value);
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
        markDirty(1);
        ctx.onMemberChanged(this, "OxQTIIgXFvoh8WDwjOh", before, value);
    }

    @Override
    public double getTrainMileageOnLine() {
        return this.trainMileageOnLine;
    }

    /**
     * 写入列车当前在线路上的里程并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setTrainMileageOnLine(double value, GromaWorldContext ctx) {
        if (this.trainMileageOnLine == value) {
            return;
        }
        double before = this.trainMileageOnLine;
        this.trainMileageOnLine = value;
        markDirty(2);
        ctx.onMemberChanged(this, "h2d1gowrlEtddEalcvT", before, value);
    }

    /**
     * 直接写入列车当前在线路上的里程（不发事件；仅用于初始化与对象创建期）
     */
    public void setTrainMileageOnLine(double value) {
        this.trainMileageOnLine = value;
    }

    @Override
    public boolean isAlongIncreasingMileage() {
        return this.alongIncreasingMileage;
    }

    /**
     * 写入沿里程增大方向并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setAlongIncreasingMileage(boolean value, GromaWorldContext ctx) {
        if (this.alongIncreasingMileage == value) {
            return;
        }
        boolean before = this.alongIncreasingMileage;
        this.alongIncreasingMileage = value;
        markDirty(3);
        ctx.onMemberChanged(this, "u79Sh40LbloyGRW4zMk", before, value);
    }

    /**
     * 直接写入沿里程增大方向（不发事件；仅用于初始化与对象创建期）
     */
    public void setAlongIncreasingMileage(boolean value) {
        this.alongIncreasingMileage = value;
    }

    @Override
    public StationWithParkedTrainEntity getCurrentParkedRelation() {
        return this.currentParkedRelation;
    }

    /**
     * 写入当前停靠关联并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setCurrentParkedRelation(StationWithParkedTrainEntity value,
            GromaWorldContext ctx) {
        if (this.currentParkedRelation == value) {
            return;
        }
        StationWithParkedTrainEntity before = this.currentParkedRelation;
        this.currentParkedRelation = value;
        markDirty(4);
        ctx.onMemberChanged(this, "0SOv8AQOpa2y8sC7shh", before, value);
    }

    @Override
    public SectionWithTrainEntity getCurrentRunningRelation() {
        return this.currentRunningRelation;
    }

    /**
     * 写入当前行驶关联并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setCurrentRunningRelation(SectionWithTrainEntity value, GromaWorldContext ctx) {
        if (this.currentRunningRelation == value) {
            return;
        }
        SectionWithTrainEntity before = this.currentRunningRelation;
        this.currentRunningRelation = value;
        markDirty(5);
        ctx.onMemberChanged(this, "x3P9T6T0WTa3KXjrMbT", before, value);
    }

    @Override
    public boolean isDemoRoundTrip() {
        return this.demoRoundTrip;
    }

    /**
     * 写入演示往返并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setDemoRoundTrip(boolean value, GromaWorldContext ctx) {
        if (this.demoRoundTrip == value) {
            return;
        }
        boolean before = this.demoRoundTrip;
        this.demoRoundTrip = value;
        markDirty(6);
        ctx.onMemberChanged(this, "VwizqIil29sv75BuLPm", before, value);
    }

    /**
     * 直接写入演示往返（不发事件；仅用于初始化与对象创建期）
     */
    public void setDemoRoundTrip(boolean value) {
        this.demoRoundTrip = value;
    }

    @Override
    public Object getMemberValue(String token) {
        switch (token) {
            case "I74UeI1njzX6WjftjtR" -> {
                return this.line;
            }
            case "OxQTIIgXFvoh8WDwjOh" -> {
                return this.train;
            }
            case "h2d1gowrlEtddEalcvT" -> {
                return this.trainMileageOnLine;
            }
            case "u79Sh40LbloyGRW4zMk" -> {
                return this.alongIncreasingMileage;
            }
            case "0SOv8AQOpa2y8sC7shh" -> {
                return this.currentParkedRelation;
            }
            case "x3P9T6T0WTa3KXjrMbT" -> {
                return this.currentRunningRelation;
            }
            case "VwizqIil29sv75BuLPm" -> {
                return this.demoRoundTrip;
            }
            default -> { return BaseSimEntity.MEMBER_NOT_FOUND; }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean setMemberValue(String token, Object value, GromaWorldContext ctx) {
        switch (token) {
            case "I74UeI1njzX6WjftjtR" -> {
                this.setLine((MetroLineEntity) GromaValues.decode(value, ctx), ctx);
            }
            case "OxQTIIgXFvoh8WDwjOh" -> {
                this.setTrain((TrainEntity) GromaValues.decode(value, ctx), ctx);
            }
            case "h2d1gowrlEtddEalcvT" -> {
                this.setTrainMileageOnLine(GromaValues.toDouble(value), ctx);
            }
            case "u79Sh40LbloyGRW4zMk" -> {
                this.setAlongIncreasingMileage(GromaValues.toBoolean(value), ctx);
            }
            case "0SOv8AQOpa2y8sC7shh" -> {
                this.setCurrentParkedRelation((StationWithParkedTrainEntity) GromaValues.decode(value, ctx), ctx);
            }
            case "x3P9T6T0WTa3KXjrMbT" -> {
                this.setCurrentRunningRelation((SectionWithTrainEntity) GromaValues.decode(value, ctx), ctx);
            }
            case "VwizqIil29sv75BuLPm" -> {
                this.setDemoRoundTrip(GromaValues.toBoolean(value), ctx);
            }
            default -> { return false; }
        }
        return true;
    }

    /**
     * 值复制（赋值运算）：基础值与枚举值逐成员复制，对象引用按引用复制；副本以新实例标识登记进世界
     */
    @Override
    public LineWithTrainEntity copy(GromaWorldContext ctx) {
        LineWithTrainEntity copy = new LineWithTrainEntity(ctx.nextInstanceId("LineWithTrain"));
        copy.openCreation();
        copy.setLine(this.getLine(), ctx);
        copy.setTrain(this.getTrain(), ctx);
        copy.setTrainMileageOnLine(this.getTrainMileageOnLine());
        copy.setAlongIncreasingMileage(this.isAlongIncreasingMileage());
        copy.setCurrentParkedRelation(this.getCurrentParkedRelation(), ctx);
        copy.setCurrentRunningRelation(this.getCurrentRunningRelation(), ctx);
        copy.setDemoRoundTrip(this.isDemoRoundTrip());
        ctx.register(copy);
        return copy;
    }
}
