package com.groma.studio.parser;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 通用有向图环路与最大递归深度检测器（覆盖继承链、成员链、调用链、路径定位链，根治 BL-15 栈溢出）
 */
public final class GraphCycleAndDepthGuard {

    public static final int DEFAULT_MAX_DEPTH = 64;

    private GraphCycleAndDepthGuard() {
    }

    public static Optional<String> detectCycleOrExcessDepth(
            String graphKind,
            Map<String, List<String>> adjacency,
            int maxDepth) {

        Set<String> permanentlyVisited = new HashSet<>();
        for (String startNode : adjacency.keySet()) {
            if (permanentlyVisited.contains(startNode)) {
                continue;
            }
            Set<String> onCurrentPath = new HashSet<>();
            Optional<String> err = dfs(graphKind, startNode, adjacency, onCurrentPath, permanentlyVisited, new ArrayDeque<>(), maxDepth);
            if (err.isPresent()) {
                return err;
            }
        }
        return Optional.empty();
    }

    private static Optional<String> dfs(
            String graphKind,
            String node,
            Map<String, List<String>> adjacency,
            Set<String> onPath,
            Set<String> visited,
            Deque<String> trail,
            int maxDepth) {

        if (trail.size() > maxDepth) {
            return Optional.of("[MDL-DEPTH-ERR] " + graphKind + " 递归深度超过上限 " + maxDepth + "，涉及节点: " + node);
        }
        if (!onPath.add(node)) {
            return Optional.of("[MDL-CYCLE-ERR] " + graphKind + " 检测到闭环依赖: " + String.join(" -> ", trail) + " -> " + node);
        }
        trail.addLast(node);
        for (String next : adjacency.getOrDefault(node, List.of())) {
            if (!visited.contains(next)) {
                Optional<String> hit = dfs(graphKind, next, adjacency, onPath, visited, trail, maxDepth);
                if (hit.isPresent()) {
                    return hit;
                }
            }
        }
        trail.removeLast();
        onPath.remove(node);
        visited.add(node);
        return Optional.empty();
    }
}
