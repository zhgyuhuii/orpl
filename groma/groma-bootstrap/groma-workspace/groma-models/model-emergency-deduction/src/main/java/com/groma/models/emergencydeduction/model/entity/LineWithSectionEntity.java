package com.groma.models.emergencydeduction.model.entity;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.BaseSimEntity;
import com.groma.base.engine.GromaMemberMeta;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction.model.entity.api.LineWithSectionReadOnly;
import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.List;

/**
 * 线路与区间实体（方圆对象类型原名：线路与区间）
 * <p>
 * 成员写入统一走带世界上下文的 Setter：框架按成员种类发出值变化(v)、状态进入/退出(sa/sx)、引用变化(ra/rv/rx) 事件，
 * 并维护反向引用索引，供类型设计视图里登记的事件接线派发。对象创建期间（首次被引用前）的成员赋值不产生事件。
 * </p>
 * 成员清单：
 * <ul>
 * <li>地铁线路（metroLine，REF）</li>
 * <li>地铁区间（metroSection，REF）</li>
 * <li>区间起点里程（sectionStartMileage，BASE）</li>
 * <li>区间终点里程（sectionEndMileage，BASE）</li>
 * </ul>
 */
@Schema(
        title = "线路与区间",
        description = "方圆对象类型：线路与区间"
)
@GromaMeta(
        id = "Kj4rOn7Z6eIvs73yjiW",
        zhName = "线路与区间",
        kind = "TYPE"
)
public class LineWithSectionEntity extends BaseSimEntity implements LineWithSectionReadOnly {
    private static final List<GromaMemberMeta> MEMBERS = List.of(new GromaMemberMeta("gvDsKHf6YCqyP9eMVXx", "地铁线路", "metroLine", "REF", "com.groma.models.emergencydeduction.model.entity.MetroLineEntity", ""), new GromaMemberMeta("gUjJuzUZZMDkhW2LEZ5", "地铁区间", "metroSection", "REF", "com.groma.models.emergencydeduction.model.entity.MetroSectionEntity", ""), new GromaMemberMeta("NxqGJkz3z98mPhNtuuM", "区间起点里程", "sectionStartMileage", "BASE", "double", ""), new GromaMemberMeta("2LrLyk9y9KZy4NbmMDp", "区间终点里程", "sectionEndMileage", "BASE", "double", ""));

    /**
     * 地铁线路（方圆对象成员原名：地铁线路）
     */
    @Schema(
            title = "地铁线路"
    )
    @GromaMeta(
            id = "gvDsKHf6YCqyP9eMVXx",
            zhName = "地铁线路",
            kind = "FIELD"
    )
    private MetroLineEntity metroLine = null;

    /**
     * 地铁区间（方圆对象成员原名：地铁区间）
     */
    @Schema(
            title = "地铁区间"
    )
    @GromaMeta(
            id = "gUjJuzUZZMDkhW2LEZ5",
            zhName = "地铁区间",
            kind = "FIELD"
    )
    private MetroSectionEntity metroSection = null;

    /**
     * 区间起点里程（方圆属性成员原名：区间起点里程）
     */
    @Schema(
            title = "区间起点里程"
    )
    @GromaMeta(
            id = "NxqGJkz3z98mPhNtuuM",
            zhName = "区间起点里程",
            kind = "FIELD"
    )
    private double sectionStartMileage = 0.0D;

    /**
     * 区间终点里程（方圆属性成员原名：区间终点里程）
     */
    @Schema(
            title = "区间终点里程"
    )
    @GromaMeta(
            id = "2LrLyk9y9KZy4NbmMDp",
            zhName = "区间终点里程",
            kind = "FIELD"
    )
    private double sectionEndMileage = 0.0D;

    /**
     * @param instanceId 实例标识（命名实例取画布对象实体 id，运行期生成的对象由世界上下文分配）
     */
    public LineWithSectionEntity(String instanceId) {
        super(instanceId);
    }

    @Override
    public String getTypeCode() {
        return "LineWithSection";
    }

    @Override
    public String getTypeZhName() {
        return "线路与区间";
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
        ctx.onMemberChanged(this, "gvDsKHf6YCqyP9eMVXx", before, value);
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
        markDirty(1);
        ctx.onMemberChanged(this, "gUjJuzUZZMDkhW2LEZ5", before, value);
    }

    @Override
    public double getSectionStartMileage() {
        return this.sectionStartMileage;
    }

    /**
     * 写入区间起点里程并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setSectionStartMileage(double value, GromaWorldContext ctx) {
        if (this.sectionStartMileage == value) {
            return;
        }
        double before = this.sectionStartMileage;
        this.sectionStartMileage = value;
        markDirty(2);
        ctx.onMemberChanged(this, "NxqGJkz3z98mPhNtuuM", before, value);
    }

    /**
     * 直接写入区间起点里程（不发事件；仅用于初始化与对象创建期）
     */
    public void setSectionStartMileage(double value) {
        this.sectionStartMileage = value;
    }

    @Override
    public double getSectionEndMileage() {
        return this.sectionEndMileage;
    }

    /**
     * 写入区间终点里程并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setSectionEndMileage(double value, GromaWorldContext ctx) {
        if (this.sectionEndMileage == value) {
            return;
        }
        double before = this.sectionEndMileage;
        this.sectionEndMileage = value;
        markDirty(3);
        ctx.onMemberChanged(this, "2LrLyk9y9KZy4NbmMDp", before, value);
    }

    /**
     * 直接写入区间终点里程（不发事件；仅用于初始化与对象创建期）
     */
    public void setSectionEndMileage(double value) {
        this.sectionEndMileage = value;
    }

    @Override
    public Object getMemberValue(String token) {
        switch (token) {
            case "gvDsKHf6YCqyP9eMVXx" -> {
                return this.metroLine;
            }
            case "gUjJuzUZZMDkhW2LEZ5" -> {
                return this.metroSection;
            }
            case "NxqGJkz3z98mPhNtuuM" -> {
                return this.sectionStartMileage;
            }
            case "2LrLyk9y9KZy4NbmMDp" -> {
                return this.sectionEndMileage;
            }
            default -> { return BaseSimEntity.MEMBER_NOT_FOUND; }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean setMemberValue(String token, Object value, GromaWorldContext ctx) {
        switch (token) {
            case "gvDsKHf6YCqyP9eMVXx" -> {
                this.setMetroLine((MetroLineEntity) GromaValues.decode(value, ctx), ctx);
            }
            case "gUjJuzUZZMDkhW2LEZ5" -> {
                this.setMetroSection((MetroSectionEntity) GromaValues.decode(value, ctx), ctx);
            }
            case "NxqGJkz3z98mPhNtuuM" -> {
                this.setSectionStartMileage(GromaValues.toDouble(value), ctx);
            }
            case "2LrLyk9y9KZy4NbmMDp" -> {
                this.setSectionEndMileage(GromaValues.toDouble(value), ctx);
            }
            default -> { return false; }
        }
        return true;
    }

    /**
     * 值复制（赋值运算）：基础值与枚举值逐成员复制，对象引用按引用复制；副本以新实例标识登记进世界
     */
    @Override
    public LineWithSectionEntity copy(GromaWorldContext ctx) {
        LineWithSectionEntity copy = new LineWithSectionEntity(ctx.nextInstanceId("LineWithSection"));
        copy.openCreation();
        copy.setMetroLine(this.getMetroLine(), ctx);
        copy.setMetroSection(this.getMetroSection(), ctx);
        copy.setSectionStartMileage(this.getSectionStartMileage());
        copy.setSectionEndMileage(this.getSectionEndMileage());
        ctx.register(copy);
        return copy;
    }
}
