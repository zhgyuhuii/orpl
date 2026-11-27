package com.groma.models.emergencydeduction.model.entity;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.BaseSimEntity;
import com.groma.base.engine.GromaMemberMeta;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction.model.entity.api.RainfallReadOnly;
import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.List;

/**
 * 降雨实体（方圆对象类型原名：降雨）
 * <p>
 * 成员写入统一走带世界上下文的 Setter：框架按成员种类发出值变化(v)、状态进入/退出(sa/sx)、引用变化(ra/rv/rx) 事件，
 * 并维护反向引用索引，供类型设计视图里登记的事件接线派发。对象创建期间（首次被引用前）的成员赋值不产生事件。
 * </p>
 * 成员清单：
 * <ul>
 * </ul>
 */
@Schema(
        title = "降雨",
        description = "方圆对象类型：降雨"
)
@GromaMeta(
        id = "KVueSWbpHnSmDysqWWj",
        zhName = "降雨",
        kind = "TYPE"
)
public class RainfallEntity extends BaseSimEntity implements RainfallReadOnly {
    private static final List<GromaMemberMeta> MEMBERS = List.of();

    /**
     * @param instanceId 实例标识（命名实例取画布对象实体 id，运行期生成的对象由世界上下文分配）
     */
    public RainfallEntity(String instanceId) {
        super(instanceId);
    }

    @Override
    public String getTypeCode() {
        return "Rainfall";
    }

    @Override
    public String getTypeZhName() {
        return "降雨";
    }

    @Override
    public List<GromaMemberMeta> memberMetas() {
        return MEMBERS;
    }

    @Override
    public Object getMemberValue(String token) {
        switch (token) {
            default -> { return BaseSimEntity.MEMBER_NOT_FOUND; }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean setMemberValue(String token, Object value, GromaWorldContext ctx) {
        switch (token) {
            default -> { return false; }
        }
    }

    /**
     * 值复制（赋值运算）：基础值与枚举值逐成员复制，对象引用按引用复制；副本以新实例标识登记进世界
     */
    @Override
    public RainfallEntity copy(GromaWorldContext ctx) {
        RainfallEntity copy = new RainfallEntity(ctx.nextInstanceId("Rainfall"));
        copy.openCreation();
        ctx.register(copy);
        return copy;
    }
}
