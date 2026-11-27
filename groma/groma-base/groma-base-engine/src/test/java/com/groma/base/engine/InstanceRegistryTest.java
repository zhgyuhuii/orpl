package com.groma.base.engine;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;

/**
 * 严格对应 CK-BE-INS-001-1 ~ 4 与 CK-BE-INS-003-2
 */
class InstanceRegistryTest {

    @Test
    void removeProcessRemovesWholeSubtree() {
        // CK-BE-INS-001-1：构造四层深过程树，移除根节点后孙节点与曾孙节点全部清除，计数归零（规避 BE-10）
        InstanceRegistry reg = new InstanceRegistry();
        InstanceRegistry.ProcessNode l1 = new InstanceRegistry.ProcessNode("p_root", null);
        InstanceRegistry.ProcessNode l2 = new InstanceRegistry.ProcessNode("p_child", l1);
        InstanceRegistry.ProcessNode l3 = new InstanceRegistry.ProcessNode("p_grandchild", l2);
        InstanceRegistry.ProcessNode l4 = new InstanceRegistry.ProcessNode("p_great_grandchild", l3);

        reg.registerProcess(l1);
        reg.registerProcess(l2);
        reg.registerProcess(l3);
        reg.registerProcess(l4);
        Assertions.assertEquals(4, reg.activeProcessCount());

        int removed = reg.removeProcessSubtree("p_root");
        Assertions.assertEquals(4, removed);
        Assertions.assertEquals(0, reg.activeProcessCount());
        Assertions.assertFalse(reg.containsProcess("p_great_grandchild"));
    }

    @Test
    void removeInstanceUpdatesAllIndexes() {
        // CK-BE-INS-001-2：移除后按索引、按类编码、按类名均查不到该实例
        InstanceRegistry reg = new InstanceRegistry();
        long idx = reg.registerObject("uav_01", "c_uav", "UavEntity", new Object(), false);
        Assertions.assertTrue(reg.removeObjectByIndex(idx));
        Assertions.assertNull(reg.findByIndex(idx));
        Assertions.assertEquals(Set.of(), reg.findIndexesByClassCode("c_uav"));
        Assertions.assertEquals(Set.of(), reg.findIndexesByClassName("UavEntity"));
    }

    @Test
    void referencedInstanceIsNeverReclaimed() {
        // CK-BE-INS-003-2：两次检查之间重新被引用的实例绝不会被误删（规避 BE-07）
        InstanceRegistry reg = new InstanceRegistry();
        long idx = reg.registerObject("tmp_01", "c_tmp", "TmpEntity", new Object(), false);

        // 第 1 次检查闲置（记 1 次 strike，不删）
        Assertions.assertTrue(reg.sweepIdleAtSafePoint(id -> false).isEmpty());
        // 第 2 次检查时恢复引用（strike 清零）
        Assertions.assertTrue(reg.sweepIdleAtSafePoint(id -> id == idx).isEmpty());
        // 第 3 次再次闲置（重新记第 1 次 strike，仍保留）
        Assertions.assertTrue(reg.sweepIdleAtSafePoint(id -> false).isEmpty());
        Assertions.assertNotNull(reg.findByIndex(idx));
        // 第 4 次连续第二次闲置 -> 安全回收
        Assertions.assertEquals(1, reg.sweepIdleAtSafePoint(id -> false).size());
        Assertions.assertNull(reg.findByIndex(idx));
    }
}
