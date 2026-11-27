package com.groma.base.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * 单写者推演实例与过程树注册表（落实 BE-INS-001 ~ 003，整棵过程子树完整递归移除）
 */
public final class InstanceRegistry {

    public record ObjectEntry(
            long numericIndex,
            String instanceId,
            String classCode,
            String className,
            Object entityRef,
            boolean rootLevel
    ) {}

    public static final class ProcessNode {
        private final String processId;
        private final ProcessNode parent;
        private final List<ProcessNode> children = new ArrayList<>(4);

        public ProcessNode(String processId, ProcessNode parent) {
            this.processId = processId;
            this.parent = parent;
            if (parent != null) {
                parent.children.add(this);
            }
        }

        public String processId() {
            return processId;
        }

        public ProcessNode parent() {
            return parent;
        }

        public List<ProcessNode> children() {
            return Collections.unmodifiableList(children);
        }

        void detachChild(ProcessNode child) {
            children.remove(child);
        }
    }

    private long nextIndexGenerator = 0L;
    private final Map<Long, ObjectEntry> byNumericIndex = new LinkedHashMap<>();
    private final Map<String, Long> idToNumericIndex = new LinkedHashMap<>();
    private final Map<String, Set<Long>> byClassCode = new LinkedHashMap<>();
    private final Map<String, Set<Long>> byClassName = new LinkedHashMap<>();
    private final Map<Long, Integer> idleStrikeCounter = new LinkedHashMap<>();
    private final Map<String, ProcessNode> allProcesses = new LinkedHashMap<>();

    public long registerObject(String instanceId, String classCode, String className, Object entityRef, boolean rootLevel) {
        long idx = ++nextIndexGenerator;
        ObjectEntry entry = new ObjectEntry(idx, instanceId, classCode, className, entityRef, rootLevel);
        byNumericIndex.put(idx, entry);
        idToNumericIndex.put(instanceId, idx);
        byClassCode.computeIfAbsent(classCode, k -> new LinkedHashSet<>()).add(idx);
        byClassName.computeIfAbsent(className, k -> new LinkedHashSet<>()).add(idx);
        return idx;
    }

    public boolean removeObjectByIndex(long index) {
        ObjectEntry removed = byNumericIndex.remove(index);
        if (removed == null) {
            return false;
        }
        idToNumericIndex.remove(removed.instanceId());
        idleStrikeCounter.remove(index);
        Set<Long> codeSet = byClassCode.get(removed.classCode());
        if (codeSet != null) {
            codeSet.remove(index);
        }
        Set<Long> nameSet = byClassName.get(removed.className());
        if (nameSet != null) {
            nameSet.remove(index);
        }
        return true;
    }

    public ObjectEntry findByIndex(long index) {
        return byNumericIndex.get(index);
    }

    public Set<Long> findIndexesByClassCode(String classCode) {
        Set<Long> s = byClassCode.get(classCode);
        return s == null ? Set.of() : Set.copyOf(s);
    }

    public Set<Long> findIndexesByClassName(String className) {
        Set<Long> s = byClassName.get(className);
        return s == null ? Set.of() : Set.copyOf(s);
    }

    public Set<Long> allObjectIndexes() {
        return Set.copyOf(byNumericIndex.keySet());
    }

    public void registerProcess(ProcessNode node) {
        allProcesses.put(node.processId(), node);
    }

    /**
     * 递归移除目标过程及其下属任意深度的全部后代节点（彻底规避 BE-10 深层孙过程残留）
     */
    public int removeProcessSubtree(String processId) {
        ProcessNode root = allProcesses.get(processId);
        if (root == null) {
            return 0;
        }
        if (root.parent() != null) {
            root.parent().detachChild(root);
        }
        return removeSubtreeRecursive(root);
    }

    private int removeSubtreeRecursive(ProcessNode current) {
        int count = 0;
        List<ProcessNode> snapshotKids = List.copyOf(current.children());
        for (ProcessNode child : snapshotKids) {
            count += removeSubtreeRecursive(child);
        }
        if (allProcesses.remove(current.processId()) != null) {
            count++;
        }
        return count;
    }

    public int activeProcessCount() {
        return allProcesses.size();
    }

    public boolean containsProcess(String processId) {
        return allProcesses.containsKey(processId);
    }

    /**
     * 在单写者推演线程安全点执行空闲实例扫描：连续两次检查均未被引用的非根实例才回收（落实 BE-INS-003）
     */
    public List<Long> sweepIdleAtSafePoint(Predicate<Long> isReferencedPredicate) {
        List<Long> reclaimed = new ArrayList<>();
        for (Long idx : List.copyOf(byNumericIndex.keySet())) {
            ObjectEntry entry = byNumericIndex.get(idx);
            if (entry == null || entry.rootLevel() || isReferencedPredicate.test(idx)) {
                idleStrikeCounter.remove(idx);
                continue;
            }
            int strikes = idleStrikeCounter.getOrDefault(idx, 0) + 1;
            if (strikes >= 2) {
                removeObjectByIndex(idx);
                reclaimed.add(idx);
            } else {
                idleStrikeCounter.put(idx, strikes);
            }
        }
        return reclaimed;
    }
}
