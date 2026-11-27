package com.groma.base.connector;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

/**
 * 表达式沙盒升级用例：逻辑与或/括号优先级/算术/字符串相等（对照建议 7 的能力清单）
 */
class ExpressionSandboxEvaluatorUpgradeTest {

    private final ExpressionSandboxEvaluator sandbox = new ExpressionSandboxEvaluator(true);

    @Test
    void legacySingleComparisonStillSupported() {
        Assertions.assertTrue(sandbox.evaluateBoolean("waterDepth >= 0.5", Map.of("waterDepth", 0.8D)));
        Assertions.assertFalse(sandbox.evaluateBoolean("waterDepth >= 0.5", Map.of("waterDepth", 0.2D)));
        Assertions.assertFalse(sandbox.evaluateBoolean(null, Map.of()));
        Assertions.assertFalse(sandbox.evaluateBoolean("", Map.of()));
    }

    @Test
    void logicalAndOrNotWithParentheses() {
        Map<String, Object> vars = Map.of("a", 10D, "b", 3D, "flag", true);
        Assertions.assertTrue(sandbox.evaluateBoolean("a > 5 && b < 5", vars));
        Assertions.assertTrue(sandbox.evaluateBoolean("a < 5 || b < 5", vars));
        Assertions.assertFalse(sandbox.evaluateBoolean("a < 5 || b > 5", vars));
        Assertions.assertTrue(sandbox.evaluateBoolean("!(a < 5)", vars));
        Assertions.assertTrue(sandbox.evaluateBoolean("(a > 5 && b > 1) || !flag", vars));
        Assertions.assertFalse(sandbox.evaluateBoolean("(a > 5 && b > 5) || !flag", vars));
    }

    @Test
    void arithmeticPrecedenceAndUnaryMinus() {
        Assertions.assertTrue(sandbox.evaluateBoolean("a + b * 2 > 15", Map.of("a", 10D, "b", 3D)));
        Assertions.assertFalse(sandbox.evaluateBoolean("(a + b) * 2 > 27", Map.of("a", 10D, "b", 3D)));
        Assertions.assertTrue(sandbox.evaluateBoolean("(a + b) * 2 > 25", Map.of("a", 10D, "b", 3D)));
        Assertions.assertTrue(sandbox.evaluateBoolean("-a + 15 == 5", Map.of("a", 10D)));
        Assertions.assertTrue(sandbox.evaluateBoolean("a % 3 == 1", Map.of("a", 10D)));
    }

    @Test
    void stringEqualityAndBooleanLiterals() {
        Assertions.assertTrue(sandbox.evaluateBoolean("state == 'RUNNING'", Map.of("state", "RUNNING")));
        Assertions.assertTrue(sandbox.evaluateBoolean("state != \"IDLE\"", Map.of("state", "RUNNING")));
        Assertions.assertTrue(sandbox.evaluateBoolean("flag == true", Map.of("flag", true)));
        Assertions.assertTrue(sandbox.evaluateBoolean("true", Map.of()));
        Assertions.assertFalse(sandbox.evaluateBoolean("false", Map.of()));
    }

    @Test
    void unknownVariableAndMalformedSafelyFalse() {
        Assertions.assertFalse(sandbox.evaluateBoolean("unknownVar > 1", Map.of()));
        Assertions.assertFalse(sandbox.evaluateBoolean("a > ", Map.of("a", 1D)));
        Assertions.assertFalse(sandbox.evaluateBoolean("(a > 1", Map.of("a", 1D)));
        Assertions.assertFalse(sandbox.evaluateBoolean("a >< 1", Map.of("a", 1D)));
    }

    @Test
    void interopAndOversizeRejected() {
        Assertions.assertThrows(SecurityException.class,
                () -> sandbox.evaluateBoolean("Runtime.getRuntime()", Map.of()));
        Assertions.assertThrows(SecurityException.class,
                () -> sandbox.evaluateBoolean("x".repeat(5000) + " > 1", Map.of()));
    }

    @Test
    void disabledSandboxThrows() {
        ExpressionSandboxEvaluator off = new ExpressionSandboxEvaluator(false);
        Assertions.assertThrows(SecurityException.class, () -> off.evaluateBoolean("true", Map.of()));
    }
}
