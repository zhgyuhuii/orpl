package com.groma.models.smartplanning.model.entity;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.models.smartplanning.model.vo.RoutePlanItemVO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 智能筹划态势地图（对应真实模型 TYPE: eid=901fc6ae6e2d4316bef91d3b9a72e364）
 */
@GromaMeta(id = "901fc6ae6e2d4316bef91d3b9a72e364", zhName = "智能筹划态势地图", kind = "TYPE")
public class SituationMapEntity {

    private String activePlanId = "";
    private final List<RoutePlanItemVO> plottedRoutes = new ArrayList<>(16);

    public void renderPlanRoutes(String planId, List<RoutePlanItemVO> routes) {
        this.activePlanId = planId;
        this.plottedRoutes.clear();
        this.plottedRoutes.addAll(routes);
    }

    public String getActivePlanId() { return activePlanId; }
    public List<RoutePlanItemVO> getPlottedRoutes() { return Collections.unmodifiableList(plottedRoutes); }
}
