package com.groma.models.emergencydeduction844676b1.model.entity;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.BaseSimEntity;
import com.groma.base.engine.GromaMemberMeta;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction844676b1.model.entity.api.Scene2ReadOnly;
import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.List;

/**
 * 场景2实体（方圆对象类型原名：场景2）
 * <p>
 * 成员写入统一走带世界上下文的 Setter：框架按成员种类发出值变化(v)、状态进入/退出(sa/sx)、引用变化(ra/rv/rx) 事件，
 * 并维护反向引用索引，供类型设计视图里登记的事件接线派发。对象创建期间（首次被引用前）的成员赋值不产生事件。
 * </p>
 * 成员清单：
 * <ul>
 * <li>降雨关联区域（rainfallRelatedArea，REF）</li>
 * <li>区域与区域内车站（areaWithStation，REF）</li>
 * <li>区间与端点车站（sectionWithEndStation，REF）</li>
 * <li>线路与列车（lineWithTrain，REF）</li>
 * </ul>
 */
@Schema(
        title = "场景2",
        description = "方圆对象类型：场景2"
)
@GromaMeta(
        id = "fFnAAr3WKsN0Oaghvzj",
        zhName = "场景2",
        kind = "TYPE"
)
public class Scene2Entity extends BaseSimEntity implements Scene2ReadOnly {
    private static final List<GromaMemberMeta> MEMBERS = List.of(new GromaMemberMeta("OlRd4AmwiiBoH9RgrTR", "降雨关联区域", "rainfallRelatedArea", "REF", "com.groma.models.emergencydeduction844676b1.model.entity.RainfallAffectedAreaEntity", ""), new GromaMemberMeta("LBJJ0xxTgEtmyb9GiZu", "区域与区域内车站", "areaWithStation", "REF", "com.groma.models.emergencydeduction844676b1.model.entity.AreaWithStationEntity", ""), new GromaMemberMeta("HgVFnRjnjDVAUqiEA2V", "区间与端点车站", "sectionWithEndStation", "REF", "com.groma.models.emergencydeduction844676b1.model.entity.SectionWithEndStationEntity", ""), new GromaMemberMeta("xTCY2kcgoHtoR9oL0Ka", "线路与列车", "lineWithTrain", "REF", "com.groma.models.emergencydeduction844676b1.model.entity.LineWithTrainEntity", ""));

    /**
     * 降雨关联区域（方圆对象成员原名：降雨关联区域）
     */
    @Schema(
            title = "降雨关联区域"
    )
    @GromaMeta(
            id = "OlRd4AmwiiBoH9RgrTR",
            zhName = "降雨关联区域",
            kind = "FIELD"
    )
    private RainfallAffectedAreaEntity rainfallRelatedArea = null;

    /**
     * 区域与区域内车站（方圆对象成员原名：区域与区域内车站）
     */
    @Schema(
            title = "区域与区域内车站"
    )
    @GromaMeta(
            id = "LBJJ0xxTgEtmyb9GiZu",
            zhName = "区域与区域内车站",
            kind = "FIELD"
    )
    private AreaWithStationEntity areaWithStation = null;

    /**
     * 区间与端点车站（方圆对象成员原名：区间与端点车站）
     */
    @Schema(
            title = "区间与端点车站"
    )
    @GromaMeta(
            id = "HgVFnRjnjDVAUqiEA2V",
            zhName = "区间与端点车站",
            kind = "FIELD"
    )
    private SectionWithEndStationEntity sectionWithEndStation = null;

    /**
     * 线路与列车（方圆对象成员原名：线路与列车）
     */
    @Schema(
            title = "线路与列车"
    )
    @GromaMeta(
            id = "xTCY2kcgoHtoR9oL0Ka",
            zhName = "线路与列车",
            kind = "FIELD"
    )
    private LineWithTrainEntity lineWithTrain = null;

    /**
     * @param instanceId 实例标识（命名实例取画布对象实体 id，运行期生成的对象由世界上下文分配）
     */
    public Scene2Entity(String instanceId) {
        super(instanceId);
    }

    @Override
    public String getTypeCode() {
        return "Scene2";
    }

    @Override
    public String getTypeZhName() {
        return "场景2";
    }

    @Override
    public List<GromaMemberMeta> memberMetas() {
        return MEMBERS;
    }

    @Override
    public RainfallAffectedAreaEntity getRainfallRelatedArea() {
        return this.rainfallRelatedArea;
    }

    /**
     * 写入降雨关联区域并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setRainfallRelatedArea(RainfallAffectedAreaEntity value, GromaWorldContext ctx) {
        if (this.rainfallRelatedArea == value) {
            return;
        }
        RainfallAffectedAreaEntity before = this.rainfallRelatedArea;
        this.rainfallRelatedArea = value;
        markDirty(0);
        ctx.onMemberChanged(this, "OlRd4AmwiiBoH9RgrTR", before, value);
    }

    @Override
    public AreaWithStationEntity getAreaWithStation() {
        return this.areaWithStation;
    }

    /**
     * 写入区域与区域内车站并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setAreaWithStation(AreaWithStationEntity value, GromaWorldContext ctx) {
        if (this.areaWithStation == value) {
            return;
        }
        AreaWithStationEntity before = this.areaWithStation;
        this.areaWithStation = value;
        markDirty(1);
        ctx.onMemberChanged(this, "LBJJ0xxTgEtmyb9GiZu", before, value);
    }

    @Override
    public SectionWithEndStationEntity getSectionWithEndStation() {
        return this.sectionWithEndStation;
    }

    /**
     * 写入区间与端点车站并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setSectionWithEndStation(SectionWithEndStationEntity value, GromaWorldContext ctx) {
        if (this.sectionWithEndStation == value) {
            return;
        }
        SectionWithEndStationEntity before = this.sectionWithEndStation;
        this.sectionWithEndStation = value;
        markDirty(2);
        ctx.onMemberChanged(this, "HgVFnRjnjDVAUqiEA2V", before, value);
    }

    @Override
    public LineWithTrainEntity getLineWithTrain() {
        return this.lineWithTrain;
    }

    /**
     * 写入线路与列车并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setLineWithTrain(LineWithTrainEntity value, GromaWorldContext ctx) {
        if (this.lineWithTrain == value) {
            return;
        }
        LineWithTrainEntity before = this.lineWithTrain;
        this.lineWithTrain = value;
        markDirty(3);
        ctx.onMemberChanged(this, "xTCY2kcgoHtoR9oL0Ka", before, value);
    }

    @Override
    public Object getMemberValue(String token) {
        switch (token) {
            case "OlRd4AmwiiBoH9RgrTR" -> {
                return this.rainfallRelatedArea;
            }
            case "LBJJ0xxTgEtmyb9GiZu" -> {
                return this.areaWithStation;
            }
            case "HgVFnRjnjDVAUqiEA2V" -> {
                return this.sectionWithEndStation;
            }
            case "xTCY2kcgoHtoR9oL0Ka" -> {
                return this.lineWithTrain;
            }
            default -> { return BaseSimEntity.MEMBER_NOT_FOUND; }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean setMemberValue(String token, Object value, GromaWorldContext ctx) {
        switch (token) {
            case "OlRd4AmwiiBoH9RgrTR" -> {
                this.setRainfallRelatedArea((RainfallAffectedAreaEntity) GromaValues.decode(value, ctx), ctx);
            }
            case "LBJJ0xxTgEtmyb9GiZu" -> {
                this.setAreaWithStation((AreaWithStationEntity) GromaValues.decode(value, ctx), ctx);
            }
            case "HgVFnRjnjDVAUqiEA2V" -> {
                this.setSectionWithEndStation((SectionWithEndStationEntity) GromaValues.decode(value, ctx), ctx);
            }
            case "xTCY2kcgoHtoR9oL0Ka" -> {
                this.setLineWithTrain((LineWithTrainEntity) GromaValues.decode(value, ctx), ctx);
            }
            default -> { return false; }
        }
        return true;
    }

    /**
     * 值复制（赋值运算）：基础值与枚举值逐成员复制，对象引用按引用复制；副本以新实例标识登记进世界
     */
    @Override
    public Scene2Entity copy(GromaWorldContext ctx) {
        Scene2Entity copy = new Scene2Entity(ctx.nextInstanceId("Scene2"));
        copy.openCreation();
        copy.setRainfallRelatedArea(this.getRainfallRelatedArea(), ctx);
        copy.setAreaWithStation(this.getAreaWithStation(), ctx);
        copy.setSectionWithEndStation(this.getSectionWithEndStation(), ctx);
        copy.setLineWithTrain(this.getLineWithTrain(), ctx);
        ctx.register(copy);
        return copy;
    }
}
