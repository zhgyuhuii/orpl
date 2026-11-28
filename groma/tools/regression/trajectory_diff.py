#!/usr/bin/env python3
"""
文件: tools/regression/trajectory_diff.py
工具编号: CMD-NP-21（挂账 #21 收割，code7/9 轮）：离线事件/快照轨迹逐拍 Diff 对拍器。
脱离 Nacos/Kafka/Redis 直接对拍两份轨迹 JSON——用于 groma 生成服务与存量 ods 运行时的
迁移验收（存量侧导出事件/快照轨迹、生成服务侧同语料同种子导出，本工具逐拍因果比对）。

轨迹 JSON 约定（两侧同构，字段缺失按缺省参与比对）:
{
  "modelCode": "...", "seed": 42, "totalTicks": 300,
  "ticks": [
    {"tick": 1,
     "events": [{"eventCode": "pr", "sourceId": "p1", "memberToken": "", "stateLabel": "", "detail": "..."}],
     "instances": {"inst_1": {"成员名": 数值或字符串, ...}},
     "processes": [{"name": "...", "status": "RUNNING"}]}
  ]
}

比对规则:
- 逐 tick 对齐（按 tick 号，缺失拍记 GAP）；事件按多重集（无序）比对，浮点按 --epsilon 容差；
- 实例成员值浮点容差比对，字符串精确比对；instances 键按中文名归一；
- 输出首个分叉 tick 的因果摘要 + 全程统计，EXIT 0 = 一致 / 1 = 有分叉 / 2 = 用法错误。

用法:
  python3 -I trajectory_diff.py <基准轨迹.json> <被测轨迹.json> [--epsilon 1e-6] [--max-report 5]
"""
import argparse
import json
import math
import sys

EPSILON_DEFAULT = 1e-6


def load(path):
    try:
        with open(path, encoding="utf-8") as handle:
            return json.load(handle)
    except FileNotFoundError:
        print(f"[阻塞] 文件不存在: {path}")
        sys.exit(2)
    except json.JSONDecodeError as ex:
        print(f"[阻塞] JSON 解析失败 {path}: {ex}")
        sys.exit(2)


def tick_index(traj):
    return {t.get("tick"): t for t in traj.get("ticks", [])}


def values_equal(a, b, epsilon):
    if isinstance(a, (int, float)) and isinstance(b, (int, float)) \
            and not isinstance(a, bool) and not isinstance(b, bool):
        return math.isclose(float(a), float(b), rel_tol=0.0, abs_tol=epsilon)
    return a == b


def event_key(ev):
    """事件因果键（无序多重集比对；detail 属自由文本，只参与同键歧义消解）"""
    return (ev.get("eventCode", ""), ev.get("sourceId", ""),
            ev.get("memberToken", ""), ev.get("stateLabel", ""))


def diff_events(base_events, test_events, epsilon):
    """多重集 diff：返回 (缺失于被测, 多出于被测) 的键列表"""
    def multiset(events):
        bag = {}
        for ev in events:
            bag[event_key(ev)] = bag.get(event_key(ev), 0) + 1
        return bag

    base_bag, test_bag = multiset(base_events), multiset(test_events)
    missing = []
    extra = []
    for key, count in base_bag.items():
        delta = count - test_bag.get(key, 0)
        missing.extend([key] * max(0, delta))
    for key, count in test_bag.items():
        delta = count - base_bag.get(key, 0)
        extra.extend([key] * max(0, delta))
    return missing, extra


def diff_instances(base_inst, test_inst, epsilon):
    diffs = []
    for inst_id, members in base_inst.items():
        test_members = test_inst.get(inst_id)
        if test_members is None:
            diffs.append(f"实例缺失: {inst_id}")
            continue
        for member, value in members.items():
            if member not in test_members:
                diffs.append(f"{inst_id}.{member} 被测缺失 (基准={value!r})")
            elif not values_equal(value, test_members[member], epsilon):
                diffs.append(f"{inst_id}.{member} 基准={value!r} 被测={test_members[member]!r}")
    for inst_id in test_inst:
        if inst_id not in base_inst:
            diffs.append(f"多出实例: {inst_id}")
    return diffs


def main():
    parser = argparse.ArgumentParser(description="离线事件/快照轨迹逐拍 Diff 对拍器")
    parser.add_argument("baseline", help="基准轨迹 JSON（如 ods 存量导出）")
    parser.add_argument("candidate", help="被测轨迹 JSON（如 groma 生成服务导出）")
    parser.add_argument("--epsilon", type=float, default=EPSILON_DEFAULT, help="浮点绝对容差（默认 1e-6）")
    parser.add_argument("--max-report", type=int, default=5, help="每拍最多报告的差异条数（默认 5）")
    args = parser.parse_args()

    base, cand = load(args.baseline), load(args.candidate)
    base_ticks, cand_ticks = tick_index(base), tick_index(cand)
    all_ticks = sorted(set(base_ticks) | set(cand_ticks))

    diverged = []
    matched = 0
    for tick in all_ticks:
        bt, ct = base_ticks.get(tick), cand_ticks.get(tick)
        if bt is None:
            diverged.append((tick, [f"基准侧存在、被测侧缺拍 tick={tick}"]))
            continue
        if ct is None:
            diverged.append((tick, [f"被测侧存在、基准侧缺拍 tick={tick}"]))
            continue
        tick_diffs = []
        missing, extra = diff_events(bt.get("events", []), ct.get("events", []), args.epsilon)
        for key in missing[:args.max_report]:
            tick_diffs.append(f"事件缺失于被测: code={key[0]} src={key[1]} member={key[2]} state={key[3]}")
        for key in extra[:args.max_report]:
            tick_diffs.append(f"事件多出于被测: code={key[0]} src={key[1]} member={key[2]} state={key[3]}")
        if missing or extra:
            tick_diffs.append(f"（事件多重集差: 缺 {len(missing)} / 多 {len(extra)}）")
        tick_diffs.extend(diff_instances(bt.get("instances", {}), ct.get("instances", {}), args.epsilon)[:args.max_report])
        if bt.get("processes") is not None and ct.get("processes") is not None:
            base_status = sorted((p.get("name", ""), p.get("status", "")) for p in bt["processes"])
            cand_status = sorted((p.get("name", ""), p.get("status", "")) for p in ct["processes"])
            if base_status != cand_status:
                tick_diffs.append(f"在途过程集合不一致: 基准={base_status} 被测={cand_status}")
        if tick_diffs:
            diverged.append((tick, tick_diffs))
        else:
            matched += 1

    total = len(all_ticks)
    print(f"轨迹对拍: 基准 {args.baseline} vs 被测 {args.candidate}")
    print(f"  总拍 {total} | 一致 {matched} | 分叉 {len(diverged)} | 容差 ε={args.epsilon}")
    if base.get("seed") != cand.get("seed"):
        print(f"  ⚠ 种子不同: 基准 seed={base.get('seed')} 被测 seed={cand.get('seed')}（逐拍可比性存疑）")
    for tick, diffs in diverged[:args.max_report]:
        print(f"  ✗ tick={tick}:")
        for line in diffs:
            print(f"      {line}")
    if diverged:
        print(f"EXIT 1: {len(diverged)} 拍分叉（首分叉 tick={diverged[0][0]}，修复后重跑）")
        sys.exit(1)
    print("EXIT 0: 全程逐拍一致（事件多重集 + 实例成员值 + 在途过程集合）")


if __name__ == "__main__":
    main()
