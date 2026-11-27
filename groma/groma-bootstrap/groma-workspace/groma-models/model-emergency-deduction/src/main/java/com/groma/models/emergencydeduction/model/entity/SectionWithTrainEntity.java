package com.groma.models.emergencydeduction.model.entity;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.BaseSimEntity;
import com.groma.base.engine.GromaMemberMeta;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction.model.entity.api.SectionWithTrainReadOnly;
import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.List;

/**
 * 区间与区间内列车实体（方圆对象类型原名：区间与区间内列车）
 * <p>
 * 成员写入统一走带世界上下文的 Setter：框架按成员种类发出值变化(v)、状态进入/退出(sa/sx)、引用变化(ra/rv/rx) 事件，
 * 并维护反向引用索引，供类型设计视图里登记的事件接线派发。对象创建期间（首次被引用前）的成员赋值不产生事件。
 * </p>
 * 成员清单：
 * <ul>
 * <li>列车（train，REF）</li>
 * <li>区间（section，REF）</li>
 * <li>已行驶距离（travelledDistance，BASE）</li>
 * <li>目标车站（targetStation，REF）</li>
 * <li>出发车站（departureStation，REF）</li>
 * <li>是否到达目标车站（arrivedTargetStation，BASE）</li>
 * </ul>
 */
@Schema(
        title = "区间与区间内列车",
        description = "方圆对象类型：区间与区间内列车"
)
@GromaMeta(
        id = "9ADRltdLnwn0Xr8dmcT",
        zhName = "区间与区间内列车",
        kind = "TYPE"
)
public class SectionWithTrainEntity extends BaseSimEntity implements SectionWithTrainReadOnly {
    private static final List<GromaMemberMeta> MEMBERS = List.of(new GromaMemberMeta("vg1gpRPbsoxrtQkhrDO", "列车", "train", "REF", "com.groma.models.emergencydeduction.model.entity.TrainEntity", ""), new GromaMemberMeta("N6oxK59ZIJFsieHFjtw", "区间", "section", "REF", "com.groma.models.emergencydeduction.model.entity.MetroSectionEntity", ""), new GromaMemberMeta("Z87fcv6mx4wqsz11jWe", "已行驶距离", "travelledDistance", "BASE", "double", ""), new GromaMemberMeta("0KoAaqP8B4qvCCaYplK", "目标车站", "targetStation", "REF", "com.groma.models.emergencydeduction.model.entity.StationEntity", ""), new GromaMemberMeta("PH2cPoljeC0p43Prsld", "出发车站", "departureStation", "REF", "com.groma.models.emergencydeduction.model.entity.StationEntity", ""), new GromaMemberMeta("bWCZ6rcu8LXoqxCT1TG", "是否到达目标车站", "arrivedTargetStation", "BASE", "boolean", ""));

    /**
     * 列车（方圆对象成员原名：列车）
     */
    @Schema(
            title = "列车"
    )
    @GromaMeta(
            id = "vg1gpRPbsoxrtQkhrDO",
            zhName = "列车",
            kind = "FIELD"
    )
    private TrainEntity train = null;

    /**
     * 区间（方圆对象成员原名：区间）
     */
    @Schema(
            title = "区间"
    )
    @GromaMeta(
            id = "N6oxK59ZIJFsieHFjtw",
            zhName = "区间",
            kind = "FIELD"
    )
    private MetroSectionEntity section = null;

    /**
     * 已行驶距离（方圆属性成员原名：已行驶距离） 默认值：0
     */
    @Schema(
            title = "已行驶距离"
    )
    @GromaMeta(
            id = "Z87fcv6mx4wqsz11jWe",
            zhName = "已行驶距离",
            kind = "FIELD"
    )
    private double travelledDistance = 0.0D;

    /**
     * 目标车站（方圆对象成员原名：目标车站）
     */
    @Schema(
            title = "目标车站"
    )
    @GromaMeta(
            id = "0KoAaqP8B4qvCCaYplK",
            zhName = "目标车站",
            kind = "FIELD"
    )
    private StationEntity targetStation = null;

    /**
     * 出发车站（方圆对象成员原名：出发车站）
     */
    @Schema(
            title = "出发车站"
    )
    @GromaMeta(
            id = "PH2cPoljeC0p43Prsld",
            zhName = "出发车站",
            kind = "FIELD"
    )
    private StationEntity departureStation = null;

    /**
     * 是否到达目标车站（方圆属性成员原名：是否到达目标车站） 默认值：false
     */
    @Schema(
            title = "是否到达目标车站"
    )
    @GromaMeta(
            id = "bWCZ6rcu8LXoqxCT1TG",
            zhName = "是否到达目标车站",
            kind = "FIELD"
    )
    private boolean arrivedTargetStation = false;

    /**
     * @param instanceId 实例标识（命名实例取画布对象实体 id，运行期生成的对象由世界上下文分配）
     */
    public SectionWithTrainEntity(String instanceId) {
        super(instanceId);
    }

    @Override
    public String getTypeCode() {
        return "SectionWithTrain";
    }

    @Override
    public String getTypeZhName() {
        return "区间与区间内列车";
    }

    @Override
    public List<GromaMemberMeta> memberMetas() {
        return MEMBERS;
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
        markDirty(0);
        ctx.onMemberChanged(this, "vg1gpRPbsoxrtQkhrDO", before, value);
    }

    @Override
    public MetroSectionEntity getSection() {
        return this.section;
    }

    /**
     * 写入区间并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setSection(MetroSectionEntity value, GromaWorldContext ctx) {
        if (this.section == value) {
            return;
        }
        MetroSectionEntity before = this.section;
        this.section = value;
        markDirty(1);
        ctx.onMemberChanged(this, "N6oxK59ZIJFsieHFjtw", before, value);
    }

    @Override
    public double getTravelledDistance() {
        return this.travelledDistance;
    }

    /**
     * 写入已行驶距离并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setTravelledDistance(double value, GromaWorldContext ctx) {
        if (this.travelledDistance == value) {
            return;
        }
        double before = this.travelledDistance;
        this.travelledDistance = value;
        markDirty(2);
        ctx.onMemberChanged(this, "Z87fcv6mx4wqsz11jWe", before, value);
    }

    /**
     * 直接写入已行驶距离（不发事件；仅用于初始化与对象创建期）
     */
    public void setTravelledDistance(double value) {
        this.travelledDistance = value;
    }

    @Override
    public StationEntity getTargetStation() {
        return this.targetStation;
    }

    /**
     * 写入目标车站并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setTargetStation(StationEntity value, GromaWorldContext ctx) {
        if (this.targetStation == value) {
            return;
        }
        StationEntity before = this.targetStation;
        this.targetStation = value;
        markDirty(3);
        ctx.onMemberChanged(this, "0KoAaqP8B4qvCCaYplK", before, value);
    }

    @Override
    public StationEntity getDepartureStation() {
        return this.departureStation;
    }

    /**
     * 写入出发车站并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setDepartureStation(StationEntity value, GromaWorldContext ctx) {
        if (this.departureStation == value) {
            return;
        }
        StationEntity before = this.departureStation;
        this.departureStation = value;
        markDirty(4);
        ctx.onMemberChanged(this, "PH2cPoljeC0p43Prsld", before, value);
    }

    @Override
    public boolean isArrivedTargetStation() {
        return this.arrivedTargetStation;
    }

    /**
     * 写入是否到达目标车站并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setArrivedTargetStation(boolean value, GromaWorldContext ctx) {
        if (this.arrivedTargetStation == value) {
            return;
        }
        boolean before = this.arrivedTargetStation;
        this.arrivedTargetStation = value;
        markDirty(5);
        ctx.onMemberChanged(this, "bWCZ6rcu8LXoqxCT1TG", before, value);
    }

    /**
     * 直接写入是否到达目标车站（不发事件；仅用于初始化与对象创建期）
     */
    public void setArrivedTargetStation(boolean value) {
        this.arrivedTargetStation = value;
    }

    @Override
    public Object getMemberValue(String token) {
        switch (token) {
            case "vg1gpRPbsoxrtQkhrDO" -> {
                return this.train;
            }
            case "N6oxK59ZIJFsieHFjtw" -> {
                return this.section;
            }
            case "Z87fcv6mx4wqsz11jWe" -> {
                return this.travelledDistance;
            }
            case "0KoAaqP8B4qvCCaYplK" -> {
                return this.targetStation;
            }
            case "PH2cPoljeC0p43Prsld" -> {
                return this.departureStation;
            }
            case "bWCZ6rcu8LXoqxCT1TG" -> {
                return this.arrivedTargetStation;
            }
            default -> { return BaseSimEntity.MEMBER_NOT_FOUND; }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean setMemberValue(String token, Object value, GromaWorldContext ctx) {
        switch (token) {
            case "vg1gpRPbsoxrtQkhrDO" -> {
                this.setTrain((TrainEntity) GromaValues.decode(value, ctx), ctx);
            }
            case "N6oxK59ZIJFsieHFjtw" -> {
                this.setSection((MetroSectionEntity) GromaValues.decode(value, ctx), ctx);
            }
            case "Z87fcv6mx4wqsz11jWe" -> {
                this.setTravelledDistance(GromaValues.toDouble(value), ctx);
            }
            case "0KoAaqP8B4qvCCaYplK" -> {
                this.setTargetStation((StationEntity) GromaValues.decode(value, ctx), ctx);
            }
            case "PH2cPoljeC0p43Prsld" -> {
                this.setDepartureStation((StationEntity) GromaValues.decode(value, ctx), ctx);
            }
            case "bWCZ6rcu8LXoqxCT1TG" -> {
                this.setArrivedTargetStation(GromaValues.toBoolean(value), ctx);
            }
            default -> { return false; }
        }
        return true;
    }

    /**
     * 值复制（赋值运算）：基础值与枚举值逐成员复制，对象引用按引用复制；副本以新实例标识登记进世界
     */
    @Override
    public SectionWithTrainEntity copy(GromaWorldContext ctx) {
        SectionWithTrainEntity copy = new SectionWithTrainEntity(ctx.nextInstanceId("SectionWithTrain"));
        copy.openCreation();
        copy.setTrain(this.getTrain(), ctx);
        copy.setSection(this.getSection(), ctx);
        copy.setTravelledDistance(this.getTravelledDistance());
        copy.setTargetStation(this.getTargetStation(), ctx);
        copy.setDepartureStation(this.getDepartureStation(), ctx);
        copy.setArrivedTargetStation(this.isArrivedTargetStation());
        ctx.register(copy);
        return copy;
    }
}
