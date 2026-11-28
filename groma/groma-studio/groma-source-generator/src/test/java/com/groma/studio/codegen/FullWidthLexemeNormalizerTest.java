package com.groma.studio.codegen;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 全角词法归一化回归（code5.html 3.5 收割，闭环 12.4 E6 挂账）：
 * 代码区全角符号归一为 ASCII，字符串字面量与注释内容原样保留（防语义漂移）。
 */
class FullWidthLexemeNormalizerTest {

    @Test
    void codeAreaFullWidthSymbolsAreNormalized() {
        String raw = "if（a＜b）｛result＝true；｝";
        String out = ProcessCodeAstTranslator.normalizeFullWidthLexeme(raw);
        Assertions.assertEquals("if(a<b){result=true;}", out);
    }

    @Test
    void stringLiteralsKeepFullWidthContent() {
        String raw = "String s = \"状态：（进行中）\"；int x＝1；";
        String out = ProcessCodeAstTranslator.normalizeFullWidthLexeme(raw);
        Assertions.assertEquals("String s = \"状态：（进行中）\";int x=1;", out,
                "字面量内的全角必须原样保留，代码区的全角归一");
    }

    @Test
    void commentsKeepFullWidthContent() {
        String raw = "// 全角注释：（保留）\nint a＝1；/* 块注释：｛原样｝ */ int b＝2；";
        String out = ProcessCodeAstTranslator.normalizeFullWidthLexeme(raw);
        Assertions.assertTrue(out.contains("// 全角注释：（保留）"), "行注释原样");
        Assertions.assertTrue(out.contains("/* 块注释：｛原样｝ */"), "块注释原样");
        Assertions.assertTrue(out.contains("int a=1;") && out.contains("int b=2;"), "代码区归一");
    }

    @Test
    void escapedQuotesInsideLiteralDoNotBreakScanner() {
        String raw = "String s = \\\\\"引号\\\"（内）\\\\\"；x＝1；";
        String out = ProcessCodeAstTranslator.normalizeFullWidthLexeme(raw);
        Assertions.assertTrue(out.endsWith("x=1;"), "转义引号不得提前终止字面量扫描: " + out);
    }

    @Test
    void asciiOnlyInputIsFastPathUntouched() {
        String raw = "int a = 1;";
        Assertions.assertSame(raw, ProcessCodeAstTranslator.normalizeFullWidthLexeme(raw),
                "纯 ASCII 输入走零拷贝快路径");
    }
}
