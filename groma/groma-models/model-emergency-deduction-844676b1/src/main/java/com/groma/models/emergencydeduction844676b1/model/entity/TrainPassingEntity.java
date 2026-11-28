package com.groma.models.emergencydeduction844676b1.model.entity;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.common.container.ObservableEntityList;
import com.groma.base.engine.BaseSimEntity;
import com.groma.base.engine.GromaMemberMeta;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction844676b1.model.entity.api.TrainPassingReadOnly;
import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.List;

/**
 * 列车通行实体（方圆对象类型原名：列车通行）
 * <p>
 * 成员写入统一走带世界上下文的 Setter：框架按成员种类发出值变化(v)、状态进入/退出(sa/sx)、引用变化(ra/rv/rx) 事件，
 * 并维护反向引用索引，供类型设计视图里登记的事件接线派发。对象创建期间（首次被引用前）的成员赋值不产生事件。
 * </p>
 * 成员清单：
 * <ul>
 * <li>通行列车（passingTrain，LIST）</li>
 * <li>通行区间（passingSection，REF）</li>
 * </ul>
 */
@Schema(
        title = "列车通行",
        description = "方圆对象类型：列车通行"
)
@GromaMeta(
        id = "8iJr06uEbFRp2d7gT5y",
        zhName = "列车通行",
        kind = "TYPE"
)
public class TrainPassingEntity extends BaseSimEntity implements TrainPassingReadOnly {
    private static final List<GromaMemberMeta> MEMBERS = List.of(new GromaMemberMeta("b1GaRdiCLb2mOttmees", "通行列车", "passingTrain", "LIST", "java.util.List<com.groma.models.emergencydeduction844676b1.model.entity.TrainEntity>", ""), new GromaMemberMeta("Mw6y78qyiaIYPO6RwDY", "通行区间", "passingSection", "REF", "com.groma.models.emergencydeduction844676b1.model.entity.MetroSectionEntity", ""));

    /**
     * 通行列车（方圆属性成员原名：通行列车）
     */
    @Schema(
            title = "通行列车"
    )
    @GromaMeta(
            id = "b1GaRdiCLb2mOttmees",
            zhName = "通行列车",
            kind = "FIELD"
    )
    private List<TrainEntity> passingTrain = new ObservableEntityList<>(new ArrayList<>(16), null);

    /**
     * 通行区间（方圆对象成员原名：通行区间）
     */
    @Schema(
            title = "通行区间"
    )
    @GromaMeta(
            id = "Mw6y78qyiaIYPO6RwDY",
            zhName = "通行区间",
            kind = "FIELD"
    )
    private MetroSectionEntity passingSection = null;

    /**
     * @param instanceId 实例标识（命名实例取画布对象实体 id，运行期生成的对象由世界上下文分配）
     */
    public TrainPassingEntity(String instanceId) {
        super(instanceId);
    }

    @Override
    public String getTypeCode() {
        return "TrainPassing";
    }

    @Override
    public String getTypeZhName() {
        return "列车通行";
    }

    @Override
    public List<GromaMemberMeta> memberMetas() {
        return MEMBERS;
    }

    @Override
    public List<TrainEntity> getPassingTrain() {
        return this.passingTrain;
    }

    /**
     * 写入通行列车并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setPassingTrain(List<TrainEntity> value, GromaWorldContext ctx) {
        if (this.passingTrain == value) {
            return;
        }
        List<TrainEntity> before = this.passingTrain;
        this.passingTrain = ObservableEntityList.observed(value, ctx.collectionSink(this, "b1GaRdiCLb2mOttmees"));
        markDirty(0);
        ctx.onMemberChanged(this, "b1GaRdiCLb2mOttmees", before, value);
    }

    /**
     * 直接写入通行列车（不发事件；仅用于初始化与对象创建期）
     */
    public void setPassingTrain(List<TrainEntity> value) {
        this.passingTrain = value;
    }

    @Override
    public MetroSectionEntity getPassingSection() {
        return this.passingSection;
    }

    /**
     * 写入通行区间并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setPassingSection(MetroSectionEntity value, GromaWorldContext ctx) {
        if (this.passingSection == value) {
            return;
        }
        MetroSectionEntity before = this.passingSection;
        this.passingSection = value;
        markDirty(1);
        ctx.onMemberChanged(this, "Mw6y78qyiaIYPO6RwDY", before, value);
    }

    @Override
    public Object getMemberValue(String token) {
        switch (token) {
            case "b1GaRdiCLb2mOttmees" -> {
                return this.passingTrain;
            }
            case "Mw6y78qyiaIYPO6RwDY" -> {
                return this.passingSection;
            }
            default -> { return BaseSimEntity.MEMBER_NOT_FOUND; }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean setMemberValue(String token, Object value, GromaWorldContext ctx) {
        switch (token) {
            case "b1GaRdiCLb2mOttmees" -> {
                this.setPassingTrain((List<TrainEntity>) GromaValues.decode(value, ctx), ctx);
            }
            case "Mw6y78qyiaIYPO6RwDY" -> {
                this.setPassingSection((MetroSectionEntity) GromaValues.decode(value, ctx), ctx);
            }
            default -> { return false; }
        }
        return true;
    }

    /**
     * 值复制（赋值运算）：基础值与枚举值逐成员复制，对象引用按引用复制；副本以新实例标识登记进世界
     */
    @Override
    public TrainPassingEntity copy(GromaWorldContext ctx) {
        TrainPassingEntity copy = new TrainPassingEntity(ctx.nextInstanceId("TrainPassing"));
        copy.openCreation();
        copy.setPassingTrain(this.getPassingTrain());
        copy.setPassingSection(this.getPassingSection(), ctx);
        ctx.register(copy);
        return copy;
    }
}
