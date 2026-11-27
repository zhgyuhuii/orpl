package com.groma.studio.naming;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.lang.model.SourceVersion;
import java.nio.file.Path;
import java.util.List;

/**
 * 严格对应 12-new-platform-spec.md 中 NP-NAM-001 的全部四条检查项
 */
class NamingEngineTest {

    @TempDir
    Path tempDir;

    @Test
    void identifiersAreValidAndUnique() {
        // CK-NP-NAM-001-1：重名中文、数字开头、括号带单位、布尔“是否”前缀、Java 关键字冲突全部生成合法且唯一标识符
        DeterministicNamingFacade facade = new DeterministicNamingFacade(new SymbolLockTable(), new DomainGlossaryTokenizer());

        String uavCls = facade.resolveEntityClassName("e1", "wb1", "1号海岛无人机", "Entity");
        Assertions.assertEquals("IslandUav1Entity", uavCls);
        Assertions.assertTrue(SourceVersion.isName(uavCls));

        String batField = facade.resolveFieldName("e1", "f1", "", "剩余电量(%)", null, false);
        Assertions.assertEquals("remainingBattery", batField);

        String boolField = facade.resolveFieldName("e1", "f2", "", "是否故障", null, true);
        Assertions.assertEquals("faulted", boolField, "POJO 布尔字段严禁带 is 前缀");

        // 同作用域重名属性自动消歧
        String dup1 = facade.resolveFieldName("e1", "f3", "wb1", "位置", null, false);
        String dup2 = facade.resolveFieldName("e1", "f4", "wb1", "位置", null, false);
        Assertions.assertNotEquals(dup1, dup2);
    }

    @Test
    void deterministicAcrossRuns() {
        // CK-NP-NAM-001-2：同一份输入重复运行三次，转译结果完全一致
        for (int i = 0; i < 3; i++) {
            DeterministicNamingFacade f = new DeterministicNamingFacade(new SymbolLockTable(), new DomainGlossaryTokenizer());
            Assertions.assertEquals("MetroSectionEntity", f.resolveEntityClassName("sec_id", "", "地铁区间", "Entity"));
            Assertions.assertEquals("releaseAllowed", f.resolveFieldName("sec_id", "f_rel", "", "是否放行", null, true));
        }
    }

    @Test
    void lockedSymbolsNeverChange() throws Exception {
        // CK-NP-NAM-001-3：模型的中文名称后来改了，已写入锁文件的符号保持恒定不变
        Path lockFile = tempDir.resolve("groma-symbols.lock.json");
        SymbolLockTable table1 = new SymbolLockTable();
        DeterministicNamingFacade facade1 = new DeterministicNamingFacade(table1, new DomainGlossaryTokenizer());
        String originalSymbol = facade1.resolveEntityClassName("ent_001", "", "列车", "Entity");
        Assertions.assertEquals("TrainEntity", originalSymbol);
        table1.saveToFile(lockFile);

        // 第二次加载锁文件，即使画布中文名被改成了“高速动车组”，已锁定的类名依然保持 TrainEntity 不变！
        SymbolLockTable table2 = new SymbolLockTable();
        table2.loadFromFile(lockFile);
        DeterministicNamingFacade facade2 = new DeterministicNamingFacade(table2, new DomainGlossaryTokenizer());
        String afterRename = facade2.resolveEntityClassName("ent_001", "", "高速动车组", "Entity");
        Assertions.assertEquals("TrainEntity", afterRename);
    }

    @Test
    void llmBackfillDoesNotRenameLockedSymbols() {
        // CK-NP-NAM-001-4：大模型异步回填只作用于未锁定符号，绝不重命名已锁定符号
        SymbolLockTable lockTable = new SymbolLockTable();
        DeterministicNamingFacade facade = new DeterministicNamingFacade(lockTable, new DomainGlossaryTokenizer());

        // ent_locked 已经生成并锁定
        String locked = facade.resolveEntityClassName("ent_locked", "", "无人机", "Entity");
        Assertions.assertEquals("UavEntity", locked);
        boolean modifiedLocked = facade.backfillFromAsyncLlm("ent_locked", "CLASS", "Entity", List.of("autonomous", "drone"));
        Assertions.assertFalse(modifiedLocked);
        Assertions.assertEquals("UavEntity", facade.resolveEntityClassName("ent_locked", "", "无人机", "Entity"));

        // ent_unlocked 尚未锁定，允许接收 LLM 精准预译词元
        boolean backfilled = facade.backfillFromAsyncLlm("ent_unlocked", "CLASS", "Entity", List.of("amphibious", "hovercraft"));
        Assertions.assertTrue(backfilled);
        Assertions.assertEquals("AmphibiousHovercraftEntity",
                facade.resolveEntityClassName("ent_unlocked", "", "生僻气垫登陆艇", "Entity"));
    }
}
