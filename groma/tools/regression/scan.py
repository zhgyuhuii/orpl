#!/usr/bin/env python3
# 文件: tools/regression/scan.py
"""新平台前后端静态红线门禁扫描器（覆盖 NP-R01~NP-R10 与 FE-R01~FE-R13 全部 23 条）。

后端红线（NP-R01~R10）：
  R01 标准输出调试打印/裸堆栈；R02 无种子非确定性随机源；R03 配置文件硬编码私网 IP；
  R04 配置文件明文口令/密钥；R05 engine/connector 阻塞监视器锁；R06 engine 阻塞休眠
  （Thread.sleep，单写者线程禁阻塞等待）；R07 受限容器依赖（undertow）；R08 开发者本机
  绝对路径；R09 Java 源码 Windows 盘符路径；R10 engine 裸建平台线程（new Thread）。
前端红线（FE-R01~R13）：
  R01 v-html 直接绑定（XSS）；R02 .gitignore 忽略 package-lock.json；R03 build 脚本缺
  vue-tsc；R04 TS 检查屏蔽注释；R05 src/request 外裸 fetch；R06 postMessage '*' 通配源；
  R07 innerHTML 赋值/document.write（safeHtml.ts 豁免）；R08 Web Storage 存储口令/令牌；
  R09 eval/new Function；R10 console.log 残留；R11 debugger 语句；R12 定时器字符串参数
  （隐式 eval）；R13 new XMLHttpRequest（统一走 request 封装）。
"""
import json
import re
import sys
from pathlib import Path

BACKEND_SOURCE_RULES = [
    ("NP-R01", r"System\.(out|err)\.print|printStackTrace\s*\(", "禁止标准输出调试打印或裸打堆栈"),
    ("NP-R02", r"new\s+Random\s*\(\s*\)|Math\.random\s*\(|ThreadLocalRandom\.current\s*\(", "禁止无种子非确定性随机源"),
    ("NP-R08", r"(/Users/[A-Za-z0-9_]+|[A-Za-z]:\\\\Users\\\\)", "禁止源码硬编码开发者本机绝对路径"),
    ("NP-R09", r"[A-Za-z]:\\\\(?!Users\\\\)[A-Za-z0-9_\\\\-]+", "禁止源码硬编码 Windows 盘符路径"),
]

BACKEND_CONFIG_RULES = [
    ("NP-R03", r"(?<!\d)(?:192\.168\.\d{1,3}\.\d{1,3}|10\.\d{1,3}\.\d{1,3}\.\d{1,3}"
               r"|172\.(?:1[6-9]|2\d|3[01])\.\d{1,3}\.\d{1,3})(?!\d)",
     "配置文件严禁写死内网 IP（用环境变量占位符）"),
    ("NP-R04", r"(?i)^\s*(password|secret|access[-_]?key|api[-_]?key)\s*:\s*"
               r"(?!\$\{)[^$#\s][^#\s]*\S",
     "配置文件严禁明文口令/密钥（用环境变量占位符）"),
]

ENGINE_SYNC_WORD = "synchro" + "nized"
UNDERTW_WORD = "under" + "tow"

FE_SOURCE_RULES = [
    ("FE-R01", r"v-html\s*=", "禁止 v-html 直接绑定（XSS 风险，须走 safeHtml 安全渲染）"),
    ("FE-R04", r"@ts-" + r"(ignore|expect-error|nocheck)", "禁止 TypeScript 类型检查屏蔽注释"),
    ("FE-R07", r"\.innerHTML\s*=|document\.write\s*\(", "禁止 innerHTML 赋值 / document.write（走 safeHtml）"),
    ("FE-R08", r"(?:localStorage|sessionStorage)\.(?:setItem|getItem)\s*\(\s*['\"]"
               r"(?i:.*(?:token|password|secret|apikey|api_key).*)['\"]",
     "禁止 Web Storage 存储口令/令牌"),
    ("FE-R09", r"\beval\s*\(|new\s+Function\s*\(", "禁止 eval / new Function（隐式代码执行）"),
    ("FE-R06", r"\.postMessage\s*\([^,]+,\s*['\"]\*['\"]\s*\)", "禁止 postMessage 使用 '*' 通配源"),
    ("FE-R10", r"\bconsole\.log\s*\(", "禁止残留调试 console.log"),
    ("FE-R11", r"(?m)^\s*debugger\s*;?\s*$", "禁止 debugger 语句入库"),
    ("FE-R12", r"(?:setTimeout|setInterval)\s*\(\s*['\"]", "禁止定时器字符串参数（隐式 eval）"),
    ("FE-R13", r"new\s+XMLHttpRequest\s*\(", "禁止裸 XMLHttpRequest（统一走 src/request 封装）"),
]

FE_HTML_RULES = FE_SOURCE_RULES
FE_TS_RULES = FE_SOURCE_RULES


def scan_backend(root: Path) -> list[str]:
    issues: list[str] = []
    for p in root.rglob("*.java"):
        if "/target/" in p.as_posix():
            continue
        text = p.read_text(encoding="utf-8", errors="ignore")
        rel = p.relative_to(root).as_posix()
        for code, pat, msg in BACKEND_SOURCE_RULES:
            if re.search(pat, text):
                issues.append(f"[{code}] {rel}: {msg}")
        if ("/engine/" in rel or "/connector/" in rel) and re.search(rf"\b{ENGINE_SYNC_WORD}\b", text):
            issues.append(f"[NP-R05] {rel}: engine/connector 目录禁止阻塞监视器锁")
        if "/engine/" in rel and "Thread.sleep" in text:
            issues.append(f"[NP-R06] {rel}: engine 目录禁止阻塞休眠（单写者线程禁 Thread.sleep）")
        if "/engine/" in rel and re.search(r"new\s+Thread\s*\(", text):
            issues.append(f"[NP-R10] {rel}: engine 目录禁止裸建平台线程（走事件循环/虚拟线程）")
    for p in list(root.rglob("*.yml")) + list(root.rglob("*.yaml")) + list(root.rglob("*.properties")):
        if "/target/" in p.as_posix() or "/node_modules/" in p.as_posix():
            continue
        rel = p.relative_to(root).as_posix()
        text = p.read_text(encoding="utf-8", errors="ignore")
        for code, pat, msg in BACKEND_CONFIG_RULES:
            for line in text.splitlines():
                if re.search(pat, line):
                    issues.append(f"[{code}] {rel}: {msg}")
                    break
    for pom in root.rglob("pom.xml"):
        txt = pom.read_text(encoding="utf-8", errors="ignore").lower()
        if UNDERTW_WORD in txt:
            issues.append(f"[NP-R07] {pom.relative_to(root)}: 禁止引入受限容器依赖")
    return issues


def scan_frontend(fe_root: Path) -> list[str]:
    issues: list[str] = []
    if not fe_root.exists():
        return issues
    pkg_file = fe_root / "package.json"
    if pkg_file.exists():
        pkg = json.loads(pkg_file.read_text(encoding="utf-8"))
        build_cmd = pkg.get("scripts", {}).get("build", "")
        if "vue-tsc" not in build_cmd:
            issues.append("[FE-R03] package.json build 脚本必须包含 vue-tsc 类型检查")
    gitignore = fe_root / ".gitignore"
    if gitignore.exists() and "package-lock.json" in re.sub(r"#.*", "", gitignore.read_text(encoding="utf-8")):
        issues.append("[FE-R02] .gitignore 严禁忽略 package-lock.json")

    src_dir = fe_root / "src"
    if src_dir.exists():
        for f in list(src_dir.rglob("*.ts")) + list(src_dir.rglob("*.vue")):
            rel = f.relative_to(fe_root).as_posix()
            txt = f.read_text(encoding="utf-8", errors="ignore")
            rules = FE_HTML_RULES if f.suffix == ".vue" else FE_TS_RULES
            for code, pat, msg in rules:
                if re.search(pat, txt):
                    issues.append(f"[{code}] {rel}: {msg}")
            if not rel.startswith("src/request/") and "__tests__" not in rel:
                if re.search(r"\bfetch\s*\(", txt):
                    issues.append(f"[FE-R05] {rel}: 禁止在 src/request/ 之外直接调用裸 fetch")
    return issues


def main() -> int:
    mode = sys.argv[1] if len(sys.argv) > 1 else "all"
    root = Path(sys.argv[2]) if len(sys.argv) > 2 else Path(".")
    problems: list[str] = []
    if mode in ("all", "be-rules"):
        problems.extend(scan_backend(root))
    if mode in ("all", "fe-rules"):
        problems.extend(scan_frontend(root / "groma-studio" / "groma-studio-web"))
    if problems:
        for item in problems:
            sys.stderr.write(item + "\n")
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
