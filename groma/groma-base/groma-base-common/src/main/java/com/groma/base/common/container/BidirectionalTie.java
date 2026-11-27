package com.groma.base.common.container;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 双向关联正反向同步索引管理器基类（R08 bidirectional-association）
 */
public class BidirectionalTie<A, B> {

    private final Map<A, LinkedHashSet<B>> forwardMap = new LinkedHashMap<>();
    private final Map<B, LinkedHashSet<A>> reverseMap = new LinkedHashMap<>();

    public void bind(A left, B right) {
        if (left == null || right == null) {
            return;
        }
        forwardMap.computeIfAbsent(left, k -> new LinkedHashSet<>()).add(right);
        reverseMap.computeIfAbsent(right, k -> new LinkedHashSet<>()).add(left);
    }

    public void unbind(A left, B right) {
        Set<B> rights = forwardMap.get(left);
        if (rights != null) {
            rights.remove(right);
        }
        Set<A> lefts = reverseMap.get(right);
        if (lefts != null) {
            lefts.remove(left);
        }
    }

    public Set<B> getRights(A left) {
        Set<B> set = forwardMap.get(left);
        return set == null ? Set.of() : Collections.unmodifiableSet(set);
    }

    public Set<A> getLefts(B right) {
        Set<A> set = reverseMap.get(right);
        return set == null ? Set.of() : Collections.unmodifiableSet(set);
    }
}
