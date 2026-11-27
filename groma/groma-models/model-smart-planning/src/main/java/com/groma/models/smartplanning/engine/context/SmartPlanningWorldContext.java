package com.groma.models.smartplanning.engine.context;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.AbstractWorldSession;
import com.groma.base.engine.WorldSnapshot;
import com.groma.models.smartplanning.model.dto.SmartPlanningRequestDTO;
import com.groma.models.smartplanning.model.dto.SyncStrikeForceInputDTO;
import com.groma.models.smartplanning.model.entity.CombatIntentAgentEntity;
import com.groma.models.smartplanning.model.entity.CombatPlanAgentEntity;
import com.groma.models.smartplanning.model.entity.ScenarioConfigAgentEntity;
import com.groma.models.smartplanning.model.entity.SessionRecordManagerEntity;
import com.groma.models.smartplanning.model.entity.SituationMapEntity;
import com.groma.models.smartplanning.model.vo.RoutePlanItemVO;
import com.groma.models.smartplanning.model.vo.SmartPlanningSnapshotVO;
import com.groma.models.smartplanning.service.IRoutePlanningSyncArrivalService;

import java.util.List;
import java.util.Map;

/**
 * 权威场景单写者沙盒：《智能筹划应用场景》（eid=hC1vBfHTMO5wGNwivnD）
 */
@GromaMeta(id = "hC1vBfHTMO5wGNwivnD", zhName = "智能筹划应用场景", kind = "SCENE_CONTEXT")
public final class SmartPlanningWorldContext extends AbstractWorldSession {

    private final SmartPlanningRequestDTO request;
    private final IRoutePlanningSyncArrivalService routeService;

    private SessionRecordManagerEntity sessionManager;
    private CombatIntentAgentEntity intentAgent;
    private CombatPlanAgentEntity planAgent;
    private ScenarioConfigAgentEntity configAgent;
    private SituationMapEntity situationMap;

    public SmartPlanningWorldContext(
            String sessionId,
            long startEpochMs,
            long seed,
            SmartPlanningRequestDTO request,
            IRoutePlanningSyncArrivalService routeService) {
        super(sessionId, "smart-planning", "844", "smart-planning-scene", startEpochMs, 1_000L, seed);
        this.request = request;
        this.routeService = routeService;
    }

    @Override
    protected void initEntitiesAndDefaultStarts() {
        this.sessionManager = new SessionRecordManagerEntity(this.sessionId);
        this.intentAgent = new CombatIntentAgentEntity();
        this.planAgent = new CombatPlanAgentEntity();
        this.configAgent = new ScenarioConfigAgentEntity();
        this.situationMap = new SituationMapEntity();
        sessionManager.appendRecord("发起智能筹划会话", "init", "会话创建完成");
    }

    @Override
    protected void loadArchiveState(WorldSnapshot archiveSnap) {
        initEntitiesAndDefaultStarts();
    }

    @Override
    protected void dispatchSingleEvent(WorldSnapshot.SimEventRecord event) {
    }

    @Override
    protected void executeActiveProcessesAtTick(long tick, long worldTimeMs) {
        // 按 process-tree.json 的 6 步一级编排序逐帧推进筹划流水线
        if (tick == 0L) {
            String intent = nonBlankOr(request.ourIntent(), "对关键海空节点实施 H 时多路同步协同行动");
            String cons = nonBlankOr(request.constraints(), "无线电静默，误差不超过 ±5 秒");
            // 对应 TEMPLATE-PROCESS《非空判断》
            String analyzed = "[意图结构化] 核心目标：" + intent + " | 刚性约束：" + cons;
            intentAgent.updateAnalysis(intent, cons, analyzed);
            sessionManager.appendRecord("分析作战意图", "process", analyzed);
        } else if (tick == 1L) {
            long hHour = request.syncHHourSeconds() != null ? request.syncHHourSeconds() : 7_200L;
            List<SyncStrikeForceInputDTO> forces = (request.availableForces() == null || request.availableForces().isEmpty())
                    ? defaultSampleForces()
                    : request.availableForces();
            List<RoutePlanItemVO> routes = routeService.computeSynchronizedRoutes(hHour, forces);
            String planId = "PLAN_" + this.sessionId.toUpperCase();
            String summary = "已生成 " + routes.size() + " 路兵力 H=" + hHour + "s 同步到达协同计划";
            planAgent.recordGeneratedPlan(planId, summary);
            situationMap.renderPlanRoutes(planId, routes);
            sessionManager.appendRecord("生成作战计划", "result", summary);
        } else if (tick == 2L) {
            String cfgJson = "{\"planId\":\"" + planAgent.getPlanId()
                    + "\",\"routeCount\":" + situationMap.getPlottedRoutes().size() + ",\"readyForDeduction\":true}";
            configAgent.setScenarioConfigJson(cfgJson);
            sessionManager.markCompleted(cfgJson);
            sessionManager.appendRecord("智能生成想定配置", "done", cfgJson);
        }
    }

    @Override
    protected void commitCompletedProcesses() {
    }

    @Override
    protected void sweepIdleInstancesAtSafePoint(long tick) {
    }

    @Override
    protected boolean evaluatePersistentStopCondition() {
        return sessionManager.isCompleted() && this.tickIndex >= 3L;
    }

    @Override
    protected boolean evaluateOneShotPauseCondition() {
        return false;
    }

    @Override
    protected WorldSnapshot buildImmutableSnapshot() {
        Map<String, Map<String, Object>> inst = Map.of(
                "作战意图分析智能体", Map.of("analyzedIntent", intentAgent.getAnalyzedIntentResult()),
                "作战计划生成智能体", Map.of("planId", planAgent.getPlanId(), "persisted", planAgent.isPersisted()),
                "智能筹划态势地图", Map.of("routeCount", situationMap.getPlottedRoutes().size())
        );
        return new WorldSnapshot(
                this.sessionId, this.modelCode, this.modelVersion, this.sceneCode, 0L,
                this.tickIndex, currentWorldTimeMs(), effectiveSpeed(),
                sessionManager.isCompleted() ? "COMPLETED" : "RUNNING",
                inst, List.of(), List.of(), copyProblems()
        );
    }

    public SmartPlanningSnapshotVO toVO() {
        return new SmartPlanningSnapshotVO(
                this.sessionId,
                this.tickIndex,
                intentAgent.getAnalyzedIntentResult(),
                planAgent.getPlanId(),
                planAgent.getGeneratedPlanSummary(),
                configAgent.getScenarioConfigJson(),
                situationMap.getPlottedRoutes(),
                sessionManager.getRecords()
        );
    }

    private static List<SyncStrikeForceInputDTO> defaultSampleForces() {
        return List.of(
                new SyncStrikeForceInputDTO("编队A-远程巡航组", "基地1", 26.05D, 119.30D, "目标点Alpha", 24.50D, 121.00D, 780.0D),
                new SyncStrikeForceInputDTO("编队B-高速突击组", "基地2", 24.90D, 118.58D, "目标点Alpha", 24.50D, 121.00D, 920.0D)
        );
    }

    private static String nonBlankOr(String v, String def) {
        return (v == null || v.isBlank()) ? def : v.trim();
    }
}
