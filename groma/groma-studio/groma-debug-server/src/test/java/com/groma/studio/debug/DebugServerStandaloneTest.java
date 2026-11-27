package com.groma.studio.debug;

import com.groma.studio.codegen.AstSourceMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 覆盖条件断点、日志断点、在线改值、三向高亮联动与时统冻结零追帧漂移
 */
class DebugServerStandaloneTest {

    @Test
    void clockFreezeShiftsDeadlineByExactPauseDuration() {
        long t0 = 1_000_000_000L;
        ClockFreezeCompensator comp = new ClockFreezeCompensator(t0 + 100_000_000L);
        comp.enterDebugFreeze(t0 + 20_000_000L);
        // 模拟用户在断点上停留查看变量 20 秒 (20_000_000_000 ns)
        long shifted = comp.exitDebugFreezeAndShiftDeadline(t0 + 20_020_000_000L);
        Assertions.assertEquals(t0 + 20_100_000_000L, shifted, "唤醒截止时刻必须精确顺延断点暂停的 20 秒，消除恢复追帧");
    }

    @Test
    void conditionalBreakpointMutateValueAndThreeWayHighlight() throws Exception {
        AstSourceMap sourceMap = new AstSourceMap();
        String javaFile = "src/main/java/com/groma/models/uav/service/impl/UavServiceImpl.java";
        sourceMap.recordMapping("node_cruise_op_01", "扣除能耗算子", javaFile, 40, 48);

        VirtualThreadDebugSession debugSession = new VirtualThreadDebugSession("dbg_01", sourceMap, System.nanoTime());
        DapWebSocketRouter router = new DapWebSocketRouter();
        router.registerDebugSession("dbg_01", debugSession);

        // 1. 设置一条条件断点 (remainingBattery < 25.0) 和一条日志断点
        router.handleDapRequest("dbg_01", new DapProtocolModels.DapRequest(1, "setBreakpoints", Map.of(
                "file", javaFile,
                "breakpoints", List.of(
                        Map.of("line", 42, "condition", "remainingBattery < 25.0", "logMessage", ""),
                        Map.of("line", 41, "condition", "", "logMessage", "当前电量={remainingBattery}")
                )
        )));

        // 电量 80.0 时：41 行打印 Logpoint 不挂起，42 行条件不满足直接放行
        debugSession.onLineProbe(javaFile, 41, "deductStepEnergy", 5L, "uav_01", Map.of("remainingBattery", 80.0D), e -> {});
        Assertions.assertEquals(List.of("[Tick=5] 当前电量=80.0"), debugSession.drainLogpointOutputs());

        Map<String, Object> passThrough = debugSession.onLineProbe(
                javaFile, 42, "deductStepEnergy", 5L, "uav_01", Map.of("remainingBattery", 80.0D), e -> {});
        Assertions.assertEquals(80.0D, passThrough.get("remainingBattery"));

        // 电量降至 22.0 时：42 行在虚拟线程内精准挂起，并携带 2D 画布图元 ID 与 3D 实体 ID
        AtomicReference<DapProtocolModels.DapStoppedEvent> stoppedRef = new AtomicReference<>();
        CompletableFuture<Map<String, Object>> vtFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return debugSession.onLineProbe(
                        javaFile, 42, "deductStepEnergy", 6L, "uav_01", Map.of("remainingBattery", 22.0D), stoppedRef::set);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }, Thread.ofVirtual()::start);

        while (stoppedRef.get() == null) {
            Thread.sleep(5L);
        }
        Assertions.assertEquals("node_cruise_op_01", stoppedRef.get().highlightedCanvasElementId());
        Assertions.assertEquals("uav_01", stoppedRef.get().highlighted3dEntityId());

        // 在线热修改变量 remainingBattery = 15.0 后点击 Continue 恢复运行
        router.handleDapRequest("dbg_01", new DapProtocolModels.DapRequest(
                2, "setVariable", Map.of("name", "remainingBattery", "value", 15.0D)));
        router.handleDapRequest("dbg_01", new DapProtocolModels.DapRequest(3, "continue", Map.of()));

        Map<String, Object> resumedVars = vtFuture.get(3, TimeUnit.SECONDS);
        Assertions.assertEquals(15.0D, resumedVars.get("remainingBattery"));
    }
}
