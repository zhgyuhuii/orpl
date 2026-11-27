package com.groma.models.emergencydeduction.model.entity;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.BaseSimEntity;
import com.groma.base.engine.GromaMemberMeta;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction.model.entity.api.MetroSectionReadOnly;
import com.groma.models.emergencydeduction.model.enums.WaterloggingStateEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.List;

/**
 * 地铁区间实体（方圆对象类型原名：地铁区间）
 * <p>
 * 成员写入统一走带世界上下文的 Setter：框架按成员种类发出值变化(v)、状态进入/退出(sa/sx)、引用变化(ra/rv/rx) 事件，
 * 并维护反向引用索引，供类型设计视图里登记的事件接线派发。对象创建期间（首次被引用前）的成员赋值不产生事件。
 * </p>
 * 成员清单：
 * <ul>
 * <li>长度（length，BASE，单位 m）</li>
 * <li>等效储水宽度（equivalentStorageWidth，BASE，单位 m）</li>
 * <li>危险水深（dangerDepth，BASE，单位 m）</li>
 * <li>迫停水深（forcedStopDepth，BASE，单位 m）</li>
 * <li>积水量（waterVolume，BASE，单位 m³）</li>
 * <li>代表性水深（representativeDepth，BASE，单位 m）</li>
 * <li>是否放行（releaseAllowed，BASE）</li>
 * <li>名称（name，BASE）</li>
 * <li>位置（position，REF）</li>
 * <li>代表底高程（representativeFloorElevation，BASE，单位 m）</li>
 * <li>积水状态（waterloggingState，ENUM）</li>
 * </ul>
 */
@Schema(
        title = "地铁区间",
        description = "方圆对象类型：地铁区间"
)
@GromaMeta(
        id = "6iap5H5evnoP56yto1c",
        zhName = "地铁区间",
        kind = "TYPE"
)
public class MetroSectionEntity extends BaseSimEntity implements MetroSectionReadOnly {
    private static final List<GromaMemberMeta> MEMBERS = List.of(new GromaMemberMeta("T3HPIYB5pFkj92hQYOb", "长度", "length", "BASE", "double", "m"), new GromaMemberMeta("OEelBaTieUbL1NIgO4p", "等效储水宽度", "equivalentStorageWidth", "BASE", "double", "m"), new GromaMemberMeta("pHyh35RSnAbxKHUj22z", "危险水深", "dangerDepth", "BASE", "double", "m"), new GromaMemberMeta("NzI5uNNldDiLgyxPW0y", "迫停水深", "forcedStopDepth", "BASE", "double", "m"), new GromaMemberMeta("xp71OUmAuU9BDQjVrDQ", "积水量", "waterVolume", "BASE", "double", "m³"), new GromaMemberMeta("SGp0FdRmQRJCz9bv1VR", "代表性水深", "representativeDepth", "BASE", "double", "m"), new GromaMemberMeta("WtxrTa8TynYayEOJ5MS", "是否放行", "releaseAllowed", "BASE", "boolean", ""), new GromaMemberMeta("B3nQ0PWoCetoCSP8gsG", "名称", "name", "BASE", "java.lang.String", ""), new GromaMemberMeta("yVYUyQOGpVapLJTqhyW", "位置", "position", "REF", "com.groma.models.emergencydeduction.model.entity.GeoRouteEntity", ""), new GromaMemberMeta("rI0LtzJ7YZcnDOyE18I", "代表底高程", "representativeFloorElevation", "BASE", "double", "m"), new GromaMemberMeta("Iz8YFJa7WCQJipUfkV4", "积水状态", "waterloggingState", "ENUM", "com.groma.models.emergencydeduction.model.enums.WaterloggingStateEnum", ""));

    /**
     * 长度（方圆属性成员原名：长度） 单位：m
     */
    @Schema(
            title = "长度"
    )
    @GromaMeta(
            id = "T3HPIYB5pFkj92hQYOb",
            zhName = "长度",
            kind = "FIELD",
            unit = "m"
    )
    private double length = 0.0D;

    /**
     * 等效储水宽度（方圆属性成员原名：等效储水宽度） 单位：m 默认值：3
     */
    @Schema(
            title = "等效储水宽度"
    )
    @GromaMeta(
            id = "OEelBaTieUbL1NIgO4p",
            zhName = "等效储水宽度",
            kind = "FIELD",
            unit = "m"
    )
    private double equivalentStorageWidth = 3.0D;

    /**
     * 危险水深（方圆属性成员原名：危险水深） 单位：m 默认值：0.1
     */
    @Schema(
            title = "危险水深"
    )
    @GromaMeta(
            id = "pHyh35RSnAbxKHUj22z",
            zhName = "危险水深",
            kind = "FIELD",
            unit = "m"
    )
    private double dangerDepth = 0.1D;

    /**
     * 迫停水深（方圆属性成员原名：迫停水深） 单位：m 默认值：0.15
     */
    @Schema(
            title = "迫停水深"
    )
    @GromaMeta(
            id = "NzI5uNNldDiLgyxPW0y",
            zhName = "迫停水深",
            kind = "FIELD",
            unit = "m"
    )
    private double forcedStopDepth = 0.15D;

    /**
     * 积水量（方圆属性成员原名：积水量） 单位：m³
     */
    @Schema(
            title = "积水量"
    )
    @GromaMeta(
            id = "xp71OUmAuU9BDQjVrDQ",
            zhName = "积水量",
            kind = "FIELD",
            unit = "m³"
    )
    private double waterVolume = 0.0D;

    /**
     * 代表性水深（方圆属性成员原名：代表性水深） 单位：m
     */
    @Schema(
            title = "代表性水深"
    )
    @GromaMeta(
            id = "SGp0FdRmQRJCz9bv1VR",
            zhName = "代表性水深",
            kind = "FIELD",
            unit = "m"
    )
    private double representativeDepth = 0.0D;

    /**
     * 是否放行（方圆属性成员原名：是否放行） 默认值：true
     */
    @Schema(
            title = "是否放行"
    )
    @GromaMeta(
            id = "WtxrTa8TynYayEOJ5MS",
            zhName = "是否放行",
            kind = "FIELD"
    )
    private boolean releaseAllowed = true;

    /**
     * 名称（方圆属性成员原名：名称）
     */
    @Schema(
            title = "名称"
    )
    @GromaMeta(
            id = "B3nQ0PWoCetoCSP8gsG",
            zhName = "名称",
            kind = "FIELD"
    )
    private String name = "";

    /**
     * 位置（方圆对象成员原名：位置）
     */
    @Schema(
            title = "位置"
    )
    @GromaMeta(
            id = "yVYUyQOGpVapLJTqhyW",
            zhName = "位置",
            kind = "FIELD"
    )
    private GeoRouteEntity position = null;

    /**
     * 代表底高程（方圆属性成员原名：代表底高程） 单位：m
     */
    @Schema(
            title = "代表底高程"
    )
    @GromaMeta(
            id = "rI0LtzJ7YZcnDOyE18I",
            zhName = "代表底高程",
            kind = "FIELD",
            unit = "m"
    )
    private double representativeFloorElevation = 0.0D;

    /**
     * 积水状态（方圆状态成员原名：积水状态）
     */
    @Schema(
            title = "积水状态"
    )
    @GromaMeta(
            id = "Iz8YFJa7WCQJipUfkV4",
            zhName = "积水状态",
            kind = "FIELD"
    )
    private WaterloggingStateEnum waterloggingState = WaterloggingStateEnum.NO_WATERLOGGING;

    /**
     * @param instanceId 实例标识（命名实例取画布对象实体 id，运行期生成的对象由世界上下文分配）
     */
    public MetroSectionEntity(String instanceId) {
        super(instanceId);
    }

    @Override
    public String getTypeCode() {
        return "MetroSection";
    }

    @Override
    public String getTypeZhName() {
        return "地铁区间";
    }

    @Override
    public List<GromaMemberMeta> memberMetas() {
        return MEMBERS;
    }

    @Override
    public double getLength() {
        return this.length;
    }

    /**
     * 写入长度并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setLength(double value, GromaWorldContext ctx) {
        if (this.length == value) {
            return;
        }
        double before = this.length;
        this.length = value;
        markDirty(0);
        ctx.onMemberChanged(this, "T3HPIYB5pFkj92hQYOb", before, value);
    }

    /**
     * 直接写入长度（不发事件；仅用于初始化与对象创建期）
     */
    public void setLength(double value) {
        this.length = value;
    }

    @Override
    public double getEquivalentStorageWidth() {
        return this.equivalentStorageWidth;
    }

    /**
     * 写入等效储水宽度并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setEquivalentStorageWidth(double value, GromaWorldContext ctx) {
        if (this.equivalentStorageWidth == value) {
            return;
        }
        double before = this.equivalentStorageWidth;
        this.equivalentStorageWidth = value;
        markDirty(1);
        ctx.onMemberChanged(this, "OEelBaTieUbL1NIgO4p", before, value);
    }

    /**
     * 直接写入等效储水宽度（不发事件；仅用于初始化与对象创建期）
     */
    public void setEquivalentStorageWidth(double value) {
        this.equivalentStorageWidth = value;
    }

    @Override
    public double getDangerDepth() {
        return this.dangerDepth;
    }

    /**
     * 写入危险水深并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setDangerDepth(double value, GromaWorldContext ctx) {
        if (this.dangerDepth == value) {
            return;
        }
        double before = this.dangerDepth;
        this.dangerDepth = value;
        markDirty(2);
        ctx.onMemberChanged(this, "pHyh35RSnAbxKHUj22z", before, value);
    }

    /**
     * 直接写入危险水深（不发事件；仅用于初始化与对象创建期）
     */
    public void setDangerDepth(double value) {
        this.dangerDepth = value;
    }

    @Override
    public double getForcedStopDepth() {
        return this.forcedStopDepth;
    }

    /**
     * 写入迫停水深并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setForcedStopDepth(double value, GromaWorldContext ctx) {
        if (this.forcedStopDepth == value) {
            return;
        }
        double before = this.forcedStopDepth;
        this.forcedStopDepth = value;
        markDirty(3);
        ctx.onMemberChanged(this, "NzI5uNNldDiLgyxPW0y", before, value);
    }

    /**
     * 直接写入迫停水深（不发事件；仅用于初始化与对象创建期）
     */
    public void setForcedStopDepth(double value) {
        this.forcedStopDepth = value;
    }

    @Override
    public double getWaterVolume() {
        return this.waterVolume;
    }

    /**
     * 写入积水量并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setWaterVolume(double value, GromaWorldContext ctx) {
        if (this.waterVolume == value) {
            return;
        }
        double before = this.waterVolume;
        this.waterVolume = value;
        markDirty(4);
        ctx.onMemberChanged(this, "xp71OUmAuU9BDQjVrDQ", before, value);
    }

    /**
     * 直接写入积水量（不发事件；仅用于初始化与对象创建期）
     */
    public void setWaterVolume(double value) {
        this.waterVolume = value;
    }

    @Override
    public double getRepresentativeDepth() {
        return this.representativeDepth;
    }

    /**
     * 写入代表性水深并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setRepresentativeDepth(double value, GromaWorldContext ctx) {
        if (this.representativeDepth == value) {
            return;
        }
        double before = this.representativeDepth;
        this.representativeDepth = value;
        markDirty(5);
        ctx.onMemberChanged(this, "SGp0FdRmQRJCz9bv1VR", before, value);
    }

    /**
     * 直接写入代表性水深（不发事件；仅用于初始化与对象创建期）
     */
    public void setRepresentativeDepth(double value) {
        this.representativeDepth = value;
    }

    @Override
    public boolean isReleaseAllowed() {
        return this.releaseAllowed;
    }

    /**
     * 写入是否放行并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setReleaseAllowed(boolean value, GromaWorldContext ctx) {
        if (this.releaseAllowed == value) {
            return;
        }
        boolean before = this.releaseAllowed;
        this.releaseAllowed = value;
        markDirty(6);
        ctx.onMemberChanged(this, "WtxrTa8TynYayEOJ5MS", before, value);
    }

    /**
     * 直接写入是否放行（不发事件；仅用于初始化与对象创建期）
     */
    public void setReleaseAllowed(boolean value) {
        this.releaseAllowed = value;
    }

    @Override
    public String getName() {
        return this.name;
    }

    /**
     * 写入名称并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setName(String value, GromaWorldContext ctx) {
        if (java.util.Objects.equals(this.name, value)) {
            return;
        }
        String before = this.name;
        this.name = value;
        markDirty(7);
        ctx.onMemberChanged(this, "B3nQ0PWoCetoCSP8gsG", before, value);
    }

    /**
     * 直接写入名称（不发事件；仅用于初始化与对象创建期）
     */
    public void setName(String value) {
        this.name = value;
    }

    @Override
    public GeoRouteEntity getPosition() {
        return this.position;
    }

    /**
     * 写入位置并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setPosition(GeoRouteEntity value, GromaWorldContext ctx) {
        if (this.position == value) {
            return;
        }
        GeoRouteEntity before = this.position;
        this.position = value;
        markDirty(8);
        ctx.onMemberChanged(this, "yVYUyQOGpVapLJTqhyW", before, value);
    }

    @Override
    public double getRepresentativeFloorElevation() {
        return this.representativeFloorElevation;
    }

    /**
     * 写入代表底高程并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setRepresentativeFloorElevation(double value, GromaWorldContext ctx) {
        if (this.representativeFloorElevation == value) {
            return;
        }
        double before = this.representativeFloorElevation;
        this.representativeFloorElevation = value;
        markDirty(9);
        ctx.onMemberChanged(this, "rI0LtzJ7YZcnDOyE18I", before, value);
    }

    /**
     * 直接写入代表底高程（不发事件；仅用于初始化与对象创建期）
     */
    public void setRepresentativeFloorElevation(double value) {
        this.representativeFloorElevation = value;
    }

    @Override
    public WaterloggingStateEnum getWaterloggingState() {
        return this.waterloggingState;
    }

    /**
     * 写入积水状态并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setWaterloggingState(WaterloggingStateEnum value, GromaWorldContext ctx) {
        if (this.waterloggingState == value) {
            return;
        }
        WaterloggingStateEnum before = this.waterloggingState;
        this.waterloggingState = value;
        markDirty(10);
        ctx.onMemberChanged(this, "Iz8YFJa7WCQJipUfkV4", before, value);
    }

    /**
     * 直接写入积水状态（不发事件；仅用于初始化与对象创建期）
     */
    public void setWaterloggingState(WaterloggingStateEnum value) {
        this.waterloggingState = value;
    }

    @Override
    public Object getMemberValue(String token) {
        switch (token) {
            case "T3HPIYB5pFkj92hQYOb" -> {
                return this.length;
            }
            case "OEelBaTieUbL1NIgO4p" -> {
                return this.equivalentStorageWidth;
            }
            case "pHyh35RSnAbxKHUj22z" -> {
                return this.dangerDepth;
            }
            case "NzI5uNNldDiLgyxPW0y" -> {
                return this.forcedStopDepth;
            }
            case "xp71OUmAuU9BDQjVrDQ" -> {
                return this.waterVolume;
            }
            case "SGp0FdRmQRJCz9bv1VR" -> {
                return this.representativeDepth;
            }
            case "WtxrTa8TynYayEOJ5MS" -> {
                return this.releaseAllowed;
            }
            case "B3nQ0PWoCetoCSP8gsG" -> {
                return this.name;
            }
            case "yVYUyQOGpVapLJTqhyW" -> {
                return this.position;
            }
            case "rI0LtzJ7YZcnDOyE18I" -> {
                return this.representativeFloorElevation;
            }
            case "Iz8YFJa7WCQJipUfkV4" -> {
                return this.waterloggingState;
            }
            default -> { return BaseSimEntity.MEMBER_NOT_FOUND; }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean setMemberValue(String token, Object value, GromaWorldContext ctx) {
        switch (token) {
            case "T3HPIYB5pFkj92hQYOb" -> {
                this.setLength(GromaValues.toDouble(value), ctx);
            }
            case "OEelBaTieUbL1NIgO4p" -> {
                this.setEquivalentStorageWidth(GromaValues.toDouble(value), ctx);
            }
            case "pHyh35RSnAbxKHUj22z" -> {
                this.setDangerDepth(GromaValues.toDouble(value), ctx);
            }
            case "NzI5uNNldDiLgyxPW0y" -> {
                this.setForcedStopDepth(GromaValues.toDouble(value), ctx);
            }
            case "xp71OUmAuU9BDQjVrDQ" -> {
                this.setWaterVolume(GromaValues.toDouble(value), ctx);
            }
            case "SGp0FdRmQRJCz9bv1VR" -> {
                this.setRepresentativeDepth(GromaValues.toDouble(value), ctx);
            }
            case "WtxrTa8TynYayEOJ5MS" -> {
                this.setReleaseAllowed(GromaValues.toBoolean(value), ctx);
            }
            case "B3nQ0PWoCetoCSP8gsG" -> {
                this.setName(GromaValues.toText(value), ctx);
            }
            case "yVYUyQOGpVapLJTqhyW" -> {
                this.setPosition((GeoRouteEntity) GromaValues.decode(value, ctx), ctx);
            }
            case "rI0LtzJ7YZcnDOyE18I" -> {
                this.setRepresentativeFloorElevation(GromaValues.toDouble(value), ctx);
            }
            case "Iz8YFJa7WCQJipUfkV4" -> {
                this.setWaterloggingState(value == null ? null : GromaValues.toEnum(WaterloggingStateEnum.class, value), ctx);
            }
            default -> { return false; }
        }
        return true;
    }

    /**
     * 值复制（赋值运算）：基础值与枚举值逐成员复制，对象引用按引用复制；副本以新实例标识登记进世界
     */
    @Override
    public MetroSectionEntity copy(GromaWorldContext ctx) {
        MetroSectionEntity copy = new MetroSectionEntity(ctx.nextInstanceId("MetroSection"));
        copy.openCreation();
        copy.setLength(this.getLength());
        copy.setEquivalentStorageWidth(this.getEquivalentStorageWidth());
        copy.setDangerDepth(this.getDangerDepth());
        copy.setForcedStopDepth(this.getForcedStopDepth());
        copy.setWaterVolume(this.getWaterVolume());
        copy.setRepresentativeDepth(this.getRepresentativeDepth());
        copy.setReleaseAllowed(this.isReleaseAllowed());
        copy.setName(this.getName());
        copy.setPosition(this.getPosition(), ctx);
        copy.setRepresentativeFloorElevation(this.getRepresentativeFloorElevation());
        copy.setWaterloggingState(this.getWaterloggingState());
        ctx.register(copy);
        return copy;
    }
}
