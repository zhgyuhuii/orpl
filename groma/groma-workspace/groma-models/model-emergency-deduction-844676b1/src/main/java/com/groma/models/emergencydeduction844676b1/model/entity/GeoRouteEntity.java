package com.groma.models.emergencydeduction844676b1.model.entity;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.common.container.ObservableEntityList;
import com.groma.base.engine.BaseSimEntity;
import com.groma.base.engine.GromaMemberMeta;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction844676b1.model.entity.api.GeoRouteReadOnly;
import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.List;

/**
 * 地理路线实体（方圆对象类型原名：地理路线）
 * <p>
 * 成员写入统一走带世界上下文的 Setter：框架按成员种类发出值变化(v)、状态进入/退出(sa/sx)、引用变化(ra/rv/rx) 事件，
 * 并维护反向引用索引，供类型设计视图里登记的事件接线派发。对象创建期间（首次被引用前）的成员赋值不产生事件。
 * </p>
 * 成员清单：
 * <ul>
 * <li>坐标组（coordinateGroup，LIST）</li>
 * </ul>
 */
@Schema(
        title = "地理路线",
        description = "方圆对象类型：地理路线"
)
@GromaMeta(
        id = "3e8a3fd5eea84ed5a3a97e97cfbf8b21",
        zhName = "地理路线",
        kind = "TYPE"
)
public class GeoRouteEntity extends BaseSimEntity implements GeoRouteReadOnly {
    private static final List<GromaMemberMeta> MEMBERS = List.of(new GromaMemberMeta("d8c2a35176844ee2b88e141d1630fdc2", "坐标组", "coordinateGroup", "LIST", "java.util.List<com.groma.models.emergencydeduction844676b1.model.entity.GeoPointEntity>", ""));

    /**
     * 坐标组（方圆属性成员原名：坐标组）
     */
    @Schema(
            title = "坐标组"
    )
    @GromaMeta(
            id = "d8c2a35176844ee2b88e141d1630fdc2",
            zhName = "坐标组",
            kind = "FIELD"
    )
    private List<GeoPointEntity> coordinateGroup = new ObservableEntityList<>(new ArrayList<>(16), null);

    /**
     * @param instanceId 实例标识（命名实例取画布对象实体 id，运行期生成的对象由世界上下文分配）
     */
    public GeoRouteEntity(String instanceId) {
        super(instanceId);
    }

    @Override
    public String getTypeCode() {
        return "GeoRoute";
    }

    @Override
    public String getTypeZhName() {
        return "地理路线";
    }

    @Override
    public List<GromaMemberMeta> memberMetas() {
        return MEMBERS;
    }

    @Override
    public List<GeoPointEntity> getCoordinateGroup() {
        return this.coordinateGroup;
    }

    /**
     * 写入坐标组并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setCoordinateGroup(List<GeoPointEntity> value, GromaWorldContext ctx) {
        if (this.coordinateGroup == value) {
            return;
        }
        List<GeoPointEntity> before = this.coordinateGroup;
        this.coordinateGroup = ObservableEntityList.observed(value, ctx.collectionSink(this, "d8c2a35176844ee2b88e141d1630fdc2"));
        markDirty(0);
        ctx.onMemberChanged(this, "d8c2a35176844ee2b88e141d1630fdc2", before, value);
    }

    /**
     * 直接写入坐标组（不发事件；仅用于初始化与对象创建期）
     */
    public void setCoordinateGroup(List<GeoPointEntity> value) {
        this.coordinateGroup = value;
    }

    @Override
    public Object getMemberValue(String token) {
        switch (token) {
            case "d8c2a35176844ee2b88e141d1630fdc2" -> {
                return this.coordinateGroup;
            }
            default -> { return BaseSimEntity.MEMBER_NOT_FOUND; }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean setMemberValue(String token, Object value, GromaWorldContext ctx) {
        switch (token) {
            case "d8c2a35176844ee2b88e141d1630fdc2" -> {
                this.setCoordinateGroup((List<GeoPointEntity>) GromaValues.decode(value, ctx), ctx);
            }
            default -> { return false; }
        }
        return true;
    }

    /**
     * 值复制（赋值运算）：基础值与枚举值逐成员复制，对象引用按引用复制；副本以新实例标识登记进世界
     */
    @Override
    public GeoRouteEntity copy(GromaWorldContext ctx) {
        GeoRouteEntity copy = new GeoRouteEntity(ctx.nextInstanceId("GeoRoute"));
        copy.openCreation();
        copy.setCoordinateGroup(this.getCoordinateGroup());
        ctx.register(copy);
        return copy;
    }
}
