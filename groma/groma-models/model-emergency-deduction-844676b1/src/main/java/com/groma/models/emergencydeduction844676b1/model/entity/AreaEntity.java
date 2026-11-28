package com.groma.models.emergencydeduction844676b1.model.entity;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.BaseSimEntity;
import com.groma.base.engine.GromaMemberMeta;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction844676b1.model.entity.api.AreaReadOnly;
import com.groma.models.emergencydeduction844676b1.model.enums.WaterloggingStateEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.List;

/**
 * 区域实体（方圆对象类型原名：区域）
 * <p>
 * 成员写入统一走带世界上下文的 Setter：框架按成员种类发出值变化(v)、状态进入/退出(sa/sx)、引用变化(ra/rv/rx) 事件，
 * 并维护反向引用索引，供类型设计视图里登记的事件接线派发。对象创建期间（首次被引用前）的成员赋值不产生事件。
 * </p>
 * 成员清单：
 * <ul>
 * <li>汇水面积（catchmentArea，BASE，单位 m²）</li>
 * <li>径流系数（runoffCoeff，BASE，单位 0.8）</li>
 * <li>等效蓄水面积（equivalentStorageArea，BASE，单位 m²）</li>
 * <li>地面高程（groundElevation，BASE，单位 m）</li>
 * <li>地表排水能力（surfaceDrainCapacity，BASE，单位 m³/min）</li>
 * <li>积水量（waterVolume，BASE，单位 m³）</li>
 * <li>积水深度（waterDepth，BASE，单位 m）</li>
 * <li>名称（name，BASE）</li>
 * <li>位置（position，REF）</li>
 * <li>积水状态（waterloggingState，ENUM）</li>
 * </ul>
 * 事件接线（类型设计视图）：
 * <ul>
 * <li>当积水状态 进入状态「积水」 发生【状态进入(sa)】 时，启动过程「地表排水」</li>
 * </ul>
 */
@Schema(
        title = "区域",
        description = "方圆对象类型：区域"
)
@GromaMeta(
        id = "eIdrNAXcjZ1OqUVr3LB",
        zhName = "区域",
        kind = "TYPE"
)
public class AreaEntity extends BaseSimEntity implements AreaReadOnly {
    private static final List<GromaMemberMeta> MEMBERS = List.of(new GromaMemberMeta("akg8YG3yPjlwkHg2h7v", "汇水面积", "catchmentArea", "BASE", "double", "m²"), new GromaMemberMeta("0cMt7C0dg5JuQEXCcbw", "径流系数", "runoffCoeff", "BASE", "double", "0.8"), new GromaMemberMeta("98fvn5ZHm7y1PFWygyd", "等效蓄水面积", "equivalentStorageArea", "BASE", "double", "m²"), new GromaMemberMeta("w3KEShYZsq5e07tNgCu", "地面高程", "groundElevation", "BASE", "double", "m"), new GromaMemberMeta("ISBiulbqRYu9DTLTdBX", "地表排水能力", "surfaceDrainCapacity", "BASE", "double", "m³/min"), new GromaMemberMeta("I5aMxoukyy1DY1moXdW", "积水量", "waterVolume", "BASE", "double", "m³"), new GromaMemberMeta("dcrXMnbxACceh8RH8aG", "积水深度", "waterDepth", "BASE", "double", "m"), new GromaMemberMeta("z2JtEUZfeWAxkF9vaof", "名称", "name", "BASE", "java.lang.String", ""), new GromaMemberMeta("nDmWtXMO1R0KFEhdC1B", "位置", "position", "REF", "com.groma.models.emergencydeduction844676b1.model.entity.GeoRegionEntity", ""), new GromaMemberMeta("pdiEfPgAApLsNQ1Cp5j", "积水状态", "waterloggingState", "ENUM", "com.groma.models.emergencydeduction844676b1.model.enums.WaterloggingStateEnum", ""));

    /**
     * 汇水面积（方圆属性成员原名：汇水面积） 单位：m² 默认值：10000
     */
    @Schema(
            title = "汇水面积"
    )
    @GromaMeta(
            id = "akg8YG3yPjlwkHg2h7v",
            zhName = "汇水面积",
            kind = "FIELD",
            unit = "m²"
    )
    private double catchmentArea = 10000.0D;

    /**
     * 径流系数（方圆属性成员原名：径流系数） 单位：0.8
     */
    @Schema(
            title = "径流系数"
    )
    @GromaMeta(
            id = "0cMt7C0dg5JuQEXCcbw",
            zhName = "径流系数",
            kind = "FIELD",
            unit = "0.8"
    )
    private double runoffCoeff = 0.0D;

    /**
     * 等效蓄水面积（方圆属性成员原名：等效蓄水面积） 单位：m² 默认值：1000
     */
    @Schema(
            title = "等效蓄水面积"
    )
    @GromaMeta(
            id = "98fvn5ZHm7y1PFWygyd",
            zhName = "等效蓄水面积",
            kind = "FIELD",
            unit = "m²"
    )
    private double equivalentStorageArea = 1000.0D;

    /**
     * 地面高程（方圆属性成员原名：地面高程） 单位：m 默认值：100
     */
    @Schema(
            title = "地面高程"
    )
    @GromaMeta(
            id = "w3KEShYZsq5e07tNgCu",
            zhName = "地面高程",
            kind = "FIELD",
            unit = "m"
    )
    private double groundElevation = 100.0D;

    /**
     * 地表排水能力（方圆属性成员原名：地表排水能力） 单位：m³/min 默认值：2
     */
    @Schema(
            title = "地表排水能力"
    )
    @GromaMeta(
            id = "ISBiulbqRYu9DTLTdBX",
            zhName = "地表排水能力",
            kind = "FIELD",
            unit = "m³/min"
    )
    private double surfaceDrainCapacity = 2.0D;

    /**
     * 积水量（方圆属性成员原名：积水量） 单位：m³
     */
    @Schema(
            title = "积水量"
    )
    @GromaMeta(
            id = "I5aMxoukyy1DY1moXdW",
            zhName = "积水量",
            kind = "FIELD",
            unit = "m³"
    )
    private double waterVolume = 0.0D;

    /**
     * 积水深度（方圆属性成员原名：积水深度） 单位：m
     */
    @Schema(
            title = "积水深度"
    )
    @GromaMeta(
            id = "dcrXMnbxACceh8RH8aG",
            zhName = "积水深度",
            kind = "FIELD",
            unit = "m"
    )
    private double waterDepth = 0.0D;

    /**
     * 名称（方圆属性成员原名：名称）
     */
    @Schema(
            title = "名称"
    )
    @GromaMeta(
            id = "z2JtEUZfeWAxkF9vaof",
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
            id = "nDmWtXMO1R0KFEhdC1B",
            zhName = "位置",
            kind = "FIELD"
    )
    private GeoRegionEntity position = null;

    /**
     * 积水状态（方圆状态成员原名：积水状态）
     * 事件触发关系：当积水状态 进入状态「积水」 发生【状态进入(sa)】 时，启动过程「地表排水」
     */
    @Schema(
            title = "积水状态"
    )
    @GromaMeta(
            id = "pdiEfPgAApLsNQ1Cp5j",
            zhName = "积水状态",
            kind = "FIELD"
    )
    private WaterloggingStateEnum waterloggingState = WaterloggingStateEnum.NO_WATERLOGGING;

    /**
     * @param instanceId 实例标识（命名实例取画布对象实体 id，运行期生成的对象由世界上下文分配）
     */
    public AreaEntity(String instanceId) {
        super(instanceId);
    }

    @Override
    public String getTypeCode() {
        return "Area";
    }

    @Override
    public String getTypeZhName() {
        return "区域";
    }

    @Override
    public List<GromaMemberMeta> memberMetas() {
        return MEMBERS;
    }

    @Override
    public double getCatchmentArea() {
        return this.catchmentArea;
    }

    /**
     * 写入汇水面积并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setCatchmentArea(double value, GromaWorldContext ctx) {
        if (this.catchmentArea == value) {
            return;
        }
        double before = this.catchmentArea;
        this.catchmentArea = value;
        markDirty(0);
        ctx.onMemberChanged(this, "akg8YG3yPjlwkHg2h7v", before, value);
    }

    /**
     * 直接写入汇水面积（不发事件；仅用于初始化与对象创建期）
     */
    public void setCatchmentArea(double value) {
        this.catchmentArea = value;
    }

    @Override
    public double getRunoffCoeff() {
        return this.runoffCoeff;
    }

    /**
     * 写入径流系数并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setRunoffCoeff(double value, GromaWorldContext ctx) {
        if (this.runoffCoeff == value) {
            return;
        }
        double before = this.runoffCoeff;
        this.runoffCoeff = value;
        markDirty(1);
        ctx.onMemberChanged(this, "0cMt7C0dg5JuQEXCcbw", before, value);
    }

    /**
     * 直接写入径流系数（不发事件；仅用于初始化与对象创建期）
     */
    public void setRunoffCoeff(double value) {
        this.runoffCoeff = value;
    }

    @Override
    public double getEquivalentStorageArea() {
        return this.equivalentStorageArea;
    }

    /**
     * 写入等效蓄水面积并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setEquivalentStorageArea(double value, GromaWorldContext ctx) {
        if (this.equivalentStorageArea == value) {
            return;
        }
        double before = this.equivalentStorageArea;
        this.equivalentStorageArea = value;
        markDirty(2);
        ctx.onMemberChanged(this, "98fvn5ZHm7y1PFWygyd", before, value);
    }

    /**
     * 直接写入等效蓄水面积（不发事件；仅用于初始化与对象创建期）
     */
    public void setEquivalentStorageArea(double value) {
        this.equivalentStorageArea = value;
    }

    @Override
    public double getGroundElevation() {
        return this.groundElevation;
    }

    /**
     * 写入地面高程并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setGroundElevation(double value, GromaWorldContext ctx) {
        if (this.groundElevation == value) {
            return;
        }
        double before = this.groundElevation;
        this.groundElevation = value;
        markDirty(3);
        ctx.onMemberChanged(this, "w3KEShYZsq5e07tNgCu", before, value);
    }

    /**
     * 直接写入地面高程（不发事件；仅用于初始化与对象创建期）
     */
    public void setGroundElevation(double value) {
        this.groundElevation = value;
    }

    @Override
    public double getSurfaceDrainCapacity() {
        return this.surfaceDrainCapacity;
    }

    /**
     * 写入地表排水能力并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setSurfaceDrainCapacity(double value, GromaWorldContext ctx) {
        if (this.surfaceDrainCapacity == value) {
            return;
        }
        double before = this.surfaceDrainCapacity;
        this.surfaceDrainCapacity = value;
        markDirty(4);
        ctx.onMemberChanged(this, "ISBiulbqRYu9DTLTdBX", before, value);
    }

    /**
     * 直接写入地表排水能力（不发事件；仅用于初始化与对象创建期）
     */
    public void setSurfaceDrainCapacity(double value) {
        this.surfaceDrainCapacity = value;
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
        markDirty(5);
        ctx.onMemberChanged(this, "I5aMxoukyy1DY1moXdW", before, value);
    }

    /**
     * 直接写入积水量（不发事件；仅用于初始化与对象创建期）
     */
    public void setWaterVolume(double value) {
        this.waterVolume = value;
    }

    @Override
    public double getWaterDepth() {
        return this.waterDepth;
    }

    /**
     * 写入积水深度并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setWaterDepth(double value, GromaWorldContext ctx) {
        if (this.waterDepth == value) {
            return;
        }
        double before = this.waterDepth;
        this.waterDepth = value;
        markDirty(6);
        ctx.onMemberChanged(this, "dcrXMnbxACceh8RH8aG", before, value);
    }

    /**
     * 直接写入积水深度（不发事件；仅用于初始化与对象创建期）
     */
    public void setWaterDepth(double value) {
        this.waterDepth = value;
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
        ctx.onMemberChanged(this, "z2JtEUZfeWAxkF9vaof", before, value);
    }

    /**
     * 直接写入名称（不发事件；仅用于初始化与对象创建期）
     */
    public void setName(String value) {
        this.name = value;
    }

    @Override
    public GeoRegionEntity getPosition() {
        return this.position;
    }

    /**
     * 写入位置并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setPosition(GeoRegionEntity value, GromaWorldContext ctx) {
        if (this.position == value) {
            return;
        }
        GeoRegionEntity before = this.position;
        this.position = value;
        markDirty(8);
        ctx.onMemberChanged(this, "nDmWtXMO1R0KFEhdC1B", before, value);
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
        markDirty(9);
        ctx.onMemberChanged(this, "pdiEfPgAApLsNQ1Cp5j", before, value);
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
            case "akg8YG3yPjlwkHg2h7v" -> {
                return this.catchmentArea;
            }
            case "0cMt7C0dg5JuQEXCcbw" -> {
                return this.runoffCoeff;
            }
            case "98fvn5ZHm7y1PFWygyd" -> {
                return this.equivalentStorageArea;
            }
            case "w3KEShYZsq5e07tNgCu" -> {
                return this.groundElevation;
            }
            case "ISBiulbqRYu9DTLTdBX" -> {
                return this.surfaceDrainCapacity;
            }
            case "I5aMxoukyy1DY1moXdW" -> {
                return this.waterVolume;
            }
            case "dcrXMnbxACceh8RH8aG" -> {
                return this.waterDepth;
            }
            case "z2JtEUZfeWAxkF9vaof" -> {
                return this.name;
            }
            case "nDmWtXMO1R0KFEhdC1B" -> {
                return this.position;
            }
            case "pdiEfPgAApLsNQ1Cp5j" -> {
                return this.waterloggingState;
            }
            default -> { return BaseSimEntity.MEMBER_NOT_FOUND; }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean setMemberValue(String token, Object value, GromaWorldContext ctx) {
        switch (token) {
            case "akg8YG3yPjlwkHg2h7v" -> {
                this.setCatchmentArea(GromaValues.toDouble(value), ctx);
            }
            case "0cMt7C0dg5JuQEXCcbw" -> {
                this.setRunoffCoeff(GromaValues.toDouble(value), ctx);
            }
            case "98fvn5ZHm7y1PFWygyd" -> {
                this.setEquivalentStorageArea(GromaValues.toDouble(value), ctx);
            }
            case "w3KEShYZsq5e07tNgCu" -> {
                this.setGroundElevation(GromaValues.toDouble(value), ctx);
            }
            case "ISBiulbqRYu9DTLTdBX" -> {
                this.setSurfaceDrainCapacity(GromaValues.toDouble(value), ctx);
            }
            case "I5aMxoukyy1DY1moXdW" -> {
                this.setWaterVolume(GromaValues.toDouble(value), ctx);
            }
            case "dcrXMnbxACceh8RH8aG" -> {
                this.setWaterDepth(GromaValues.toDouble(value), ctx);
            }
            case "z2JtEUZfeWAxkF9vaof" -> {
                this.setName(GromaValues.toText(value), ctx);
            }
            case "nDmWtXMO1R0KFEhdC1B" -> {
                this.setPosition((GeoRegionEntity) GromaValues.decode(value, ctx), ctx);
            }
            case "pdiEfPgAApLsNQ1Cp5j" -> {
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
    public AreaEntity copy(GromaWorldContext ctx) {
        AreaEntity copy = new AreaEntity(ctx.nextInstanceId("Area"));
        copy.openCreation();
        copy.setCatchmentArea(this.getCatchmentArea());
        copy.setRunoffCoeff(this.getRunoffCoeff());
        copy.setEquivalentStorageArea(this.getEquivalentStorageArea());
        copy.setGroundElevation(this.getGroundElevation());
        copy.setSurfaceDrainCapacity(this.getSurfaceDrainCapacity());
        copy.setWaterVolume(this.getWaterVolume());
        copy.setWaterDepth(this.getWaterDepth());
        copy.setName(this.getName());
        copy.setPosition(this.getPosition(), ctx);
        copy.setWaterloggingState(this.getWaterloggingState());
        ctx.register(copy);
        return copy;
    }
}
