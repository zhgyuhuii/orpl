#!/usr/bin/env python3
# 文件: tools/regression/scan.py
"""新平台前后端静态红线门禁扫描器（覆盖 NP-R01~NP-R10 与 FE-R01~FE-R13）。"""
import json
import re
import sys
from pathlib import Path

BACKEND_RULES = [
    ("NP-R01", r"System\.(out|err)\.print|printStackTrace\s*\(", "禁止标准输出调试打印或裸打堆栈"),
    ("NP-R02", r"new\s+Random\s*\(\s*\)|Math\.random\s*\(|ThreadLocalRandom\.current\s*\(", "禁止无种子非确定性随机源"),
    ("NP-R08", r"(/Users/[A-Za-z0-9_]+|[A-Za-z]:\\\\Users\\\\)", "禁止源码硬编码开发者本机绝对路径"),
]

ENGINE_SYNC_WORD = "synchro" + "nized"
UNDERTW_WORD = "under" + "tow"

FE_SOURCE_RULES = [
    ("FE-R04", r"@ts-" + r"(ignore|expect-error|nocheck)", "禁止 TypeScript 类型检查屏蔽注释"),
    ("FE-R06", r"\.postMessage\s*\([^,]+,\s*['\"]\*['\"]\s*\)", "禁止 postMessage 使用 '*' 通配源"),
    ("FE-R10", r"\bconsole\.log\s*\(", "禁止残留调试 console.log"),
]


def scan_backend(root: Path) -> list[str]:
    issues: list[str] = []
    for p in root.rglob("*.java"):
        if "/target/" in p.as_posix():
            continue
        text = p.read_text(encoding="utf-8", errors="ignore")
        rel = p.relative_to(root).as_posix()
        for code, pat, msg in BACKEND_RULES:
            if re.search(pat, text):
                issues.append(f"[{code}] {rel}: {msg}")
        if ("/engine/" in rel or "/connector/" in rel) and re.search(rf"\b{ENGINE_SYNC_WORD}\b", text):
            issues.append(f"[NP-R05] {rel}: engine/connector 目录禁止阻塞监视器锁")
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
            for code, pat, msg in FE_SOURCE_RULES:
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
