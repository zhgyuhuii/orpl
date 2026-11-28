package com.groma.models.emergencydeduction844676b1.model.entity;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.BaseSimEntity;
import com.groma.base.engine.GromaMemberMeta;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction844676b1.model.entity.api.GeoPointReadOnly;
import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.List;

/**
 * 地理点位实体（方圆对象类型原名：地理点位）
 * <p>
 * 成员写入统一走带世界上下文的 Setter：框架按成员种类发出值变化(v)、状态进入/退出(sa/sx)、引用变化(ra/rv/rx) 事件，
 * 并维护反向引用索引，供类型设计视图里登记的事件接线派发。对象创建期间（首次被引用前）的成员赋值不产生事件。
 * </p>
 * 成员清单：
 * <ul>
 * <li>经度（longitude，BASE）</li>
 * <li>纬度（latitude，BASE）</li>
 * <li>高度（height，BASE）</li>
 * </ul>
 */
@Schema(
        title = "地理点位",
        description = "方圆对象类型：地理点位"
)
@GromaMeta(
        id = "c4998ee1638a4a9ca0029f21d57850d4",
        zhName = "地理点位",
        kind = "TYPE"
)
public class GeoPointEntity extends BaseSimEntity implements GeoPointReadOnly {
    private static final List<GromaMemberMeta> MEMBERS = List.of(new GromaMemberMeta("c66c8d5f512d410c8667aea645329f8a", "经度", "longitude", "BASE", "double", ""), new GromaMemberMeta("c345363c20924664b8e94fe5188df806", "纬度", "latitude", "BASE", "double", ""), new GromaMemberMeta("138cb6dd6b224361b9fe2a08f0beca74", "高度", "height", "BASE", "double", ""));

    /**
     * 经度（方圆属性成员原名：经度）
     */
    @Schema(
            title = "经度"
    )
    @GromaMeta(
            id = "c66c8d5f512d410c8667aea645329f8a",
            zhName = "经度",
            kind = "FIELD"
    )
    private double longitude = 0.0D;

    /**
     * 纬度（方圆属性成员原名：纬度）
     */
    @Schema(
            title = "纬度"
    )
    @GromaMeta(
            id = "c345363c20924664b8e94fe5188df806",
            zhName = "纬度",
            kind = "FIELD"
    )
    private double latitude = 0.0D;

    /**
     * 高度（方圆属性成员原名：高度）
     */
    @Schema(
            title = "高度"
    )
    @GromaMeta(
            id = "138cb6dd6b224361b9fe2a08f0beca74",
            zhName = "高度",
            kind = "FIELD"
    )
    private double height = 0.0D;

    /**
     * @param instanceId 实例标识（命名实例取画布对象实体 id，运行期生成的对象由世界上下文分配）
     */
    public GeoPointEntity(String instanceId) {
        super(instanceId);
    }

    @Override
    public String getTypeCode() {
        return "GeoPoint";
    }

    @Override
    public String getTypeZhName() {
        return "地理点位";
    }

    @Override
    public List<GromaMemberMeta> memberMetas() {
        return MEMBERS;
    }

    @Override
    public double getLongitude() {
        return this.longitude;
    }

    /**
     * 写入经度并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setLongitude(double value, GromaWorldContext ctx) {
        if (this.longitude == value) {
            return;
        }
        double before = this.longitude;
        this.longitude = value;
        markDirty(0);
        ctx.onMemberChanged(this, "c66c8d5f512d410c8667aea645329f8a", before, value);
    }

    /**
     * 直接写入经度（不发事件；仅用于初始化与对象创建期）
     */
    public void setLongitude(double value) {
        this.longitude = value;
    }

    @Override
    public double getLatitude() {
        return this.latitude;
    }

    /**
     * 写入纬度并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setLatitude(double value, GromaWorldContext ctx) {
        if (this.latitude == value) {
            return;
        }
        double before = this.latitude;
        this.latitude = value;
        markDirty(1);
        ctx.onMemberChanged(this, "c345363c20924664b8e94fe5188df806", before, value);
    }

    /**
     * 直接写入纬度（不发事件；仅用于初始化与对象创建期）
     */
    public void setLatitude(double value) {
        this.latitude = value;
    }

    @Override
    public double getHeight() {
        return this.height;
    }

    /**
     * 写入高度并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setHeight(double value, GromaWorldContext ctx) {
        if (this.height == value) {
            return;
        }
        double before = this.height;
        this.height = value;
        markDirty(2);
        ctx.onMemberChanged(this, "138cb6dd6b224361b9fe2a08f0beca74", before, value);
    }

    /**
     * 直接写入高度（不发事件；仅用于初始化与对象创建期）
     */
    public void setHeight(double value) {
        this.height = value;
    }

    @Override
    public Object getMemberValue(String token) {
        switch (token) {
            case "c66c8d5f512d410c8667aea645329f8a" -> {
                return this.longitude;
            }
            case "c345363c20924664b8e94fe5188df806" -> {
                return this.latitude;
            }
            case "138cb6dd6b224361b9fe2a08f0beca74" -> {
                return this.height;
            }
            default -> { return BaseSimEntity.MEMBER_NOT_FOUND; }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean setMemberValue(String token, Object value, GromaWorldContext ctx) {
        switch (token) {
            case "c66c8d5f512d410c8667aea645329f8a" -> {
                this.setLongitude(GromaValues.toDouble(value), ctx);
            }
            case "c345363c20924664b8e94fe5188df806" -> {
                this.setLatitude(GromaValues.toDouble(value), ctx);
            }
            case "138cb6dd6b224361b9fe2a08f0beca74" -> {
                this.setHeight(GromaValues.toDouble(value), ctx);
            }
            default -> { return false; }
        }
        return true;
    }

    /**
     * 值复制（赋值运算）：基础值与枚举值逐成员复制，对象引用按引用复制；副本以新实例标识登记进世界
     */
    @Override
    public GeoPointEntity copy(GromaWorldContext ctx) {
        GeoPointEntity copy = new GeoPointEntity(ctx.nextInstanceId("GeoPoint"));
        copy.openCreation();
        copy.setLongitude(this.getLongitude());
        copy.setLatitude(this.getLatitude());
        copy.setHeight(this.getHeight());
        ctx.register(copy);
        return copy;
    }
}
