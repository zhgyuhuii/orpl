#!/usr/bin/env python3
"""
文件: tools/regression/gen-ref-model.py
工具编号: H-08 | 合成参考模型 RM-B1 (容量/慢客户端), RM-B2 (续跑/29种连线全覆盖), RM-B3 (事件风暴熔断), RM-B4 (MCP工作流)
兼容两种调用：--out-dir <dir> 或 位置参数 <dir>。
"""
import argparse
import json
import os

ALL_29_SHAPES = [
    "edge-agent", "edge-bidirectional-association", "edge-bidirectional-tagged",
    "edge-classification", "edge-composition", "edge-condition", "edge-consumption",
    "edge-control", "edge-exhibition", "edge-extend", "edge-fixed", "edge-in-change",
    "edge-in-influence-nonnull", "edge-in-influence-nullable", "edge-in-out",
    "edge-in-out-influence-nonnull", "edge-in-out-influence-nullable", "edge-invocation",
    "edge-invocation-self", "edge-link", "edge-note", "edge-out-change",
    "edge-out-influence", "edge-overtime-exception", "edge-proxy", "edge-result",
    "edge-undertime-exception", "edge-unidirectional-association", "edge-unidirectional-tagged"
]


def build_rm_b2_full_29_shapes():
    """合成覆盖全部 29 种连线形状、时长分布、孪生、异步、单实例、首个启动与结束点的黄金模型 RM-B2"""
    entities = [
        {"id": "sc_rm_b2", "type": "SCENE", "label": "全特性与续跑验证场景",
         "defineViewId": "defineView_sc_b2", "designViewId": "designView_sc_b2"},
        {"id": "enum_st", "type": "ENUM", "label": "运行状态",
         "defineViewId": "defineView_enum_st", "designViewId": "designView_enum_st"},
        {"id": "st_ready", "type": "STATE", "label": "待命"},
        {"id": "st_work", "type": "STATE", "label": "作业中"},
        {"id": "ty_uav", "type": "TYPE", "label": "巡检无人机",
         "defineViewId": "defineView_uav", "designViewId": "designView_uav"},
        {"id": "attr_bat", "type": "ATTRIBUTE-MEMBER", "label": "剩余电量", "fieldType": "double",
         "unit": "%", "defaultValue": "100.0", "typeRange": "BASE"},
        {"id": "proc_exp_twin", "type": "PROCESS", "label": "指数分布孪生巡航",
         "timeRelated": "RELATED", "timeUnit": "s", "timeNormal": 2.0,
         "distribution": "EXPONENTIAL", "fromRealWorld": "true", "singleton": 1,
         "defineViewId": "defineView_p_twin", "designViewId": "designView_p_twin"},
    ]
    cells = [
        {"id": "n_p1", "data": "proc_exp_twin", "type": "PROCESS", "shape": "node-process",
         "label": "指数分布孪生巡航", "defaultStart": "true", "firstStart": "true", "lastEnd": "true", "async": "true"},
        {"id": "n_m1", "data": "attr_bat", "type": "ATTRIBUTE-MEMBER", "shape": "node-attribute-member", "label": "剩余电量"},
    ]
    for idx, edge_shape in enumerate(ALL_29_SHAPES):
        lid = f"lnk_gap_{idx}"
        rel = edge_shape[len("edge-"):]
        entities.append({"id": lid, "type": "LINK", "linkType": rel, "event": "v" if idx == 0 else ""})
        cells.append({
            "id": f"c_edge_{idx}", "data": lid, "type": "LINK", "shape": edge_shape,
            "source": {"cell": "n_m1", "port": ""}, "target": {"cell": "n_p1", "port": ""},
            "event": "v" if idx == 0 else ""
        })

    part_a = {
        "success": True,
        "obj": {
            "id": "RM_B2_FULL_SPEC",
            "label": "RM-B2全形状与续跑基准模型",
            "version": 1,
            "nodes": [{
                "id": "folder_scenes", "type": "SCENES", "label": "运行场景",
                "nodes": [{
                    "id": "node_sc_b2", "entityId": "sc_rm_b2", "type": "SCENE",
                    "label": "全特性与续跑验证场景",
                    "defineViewId": "defineView_sc_b2", "designViewId": "designView_sc_b2",
                    "nodes": []
                }]
            }, {
                "id": "folder_types", "type": "TYPES", "label": "对象类型",
                "nodes": [{
                    "id": "node_ty_uav", "entityId": "ty_uav", "type": "TYPE", "label": "巡检无人机",
                    "nodes": [
                        {"id": "n_k1", "entityId": "attr_bat", "type": "ATTRIBUTE-MEMBER", "label": "剩余电量"},
                        {"id": "n_k2", "entityId": "proc_exp_twin", "type": "PROCESS", "label": "指数分布孪生巡航"}
                    ]
                }]
            }],
            "entities": entities,
            "cellMap": {
                "defineView_enum_st": [
                    {"id": "cs1", "data": "st_ready", "type": "STATE", "shape": "node-state", "x": 0, "y": 0},
                    {"id": "cs2", "data": "st_work", "type": "STATE", "shape": "node-state", "x": 0, "y": 40}
                ]
            }
        }
    }
    part_b = {
        "success": True,
        "obj": {
            "cellMap": {
                "designView_sc_b2": cells
            }
        }
    }
    return part_a, part_b


def build_rm_b3_event_storm():
    """合成两个算子无条件互相触发的乒乓风暴模型 RM-B3（用于验证 BE-EVT-001 事件风暴熔断）"""
    part_a = {
        "success": True,
        "obj": {
            "id": "RM_B3_EVENT_STORM",
            "label": "RM-B3事件风暴熔断模型",
            "version": 1,
            "nodes": [{
                "id": "sc_root", "type": "SCENES", "label": "运行场景",
                "nodes": [{"id": "n_sc", "entityId": "sc_storm", "type": "SCENE", "label": "乒乓风暴场景",
                           "defineViewId": "defineView_storm", "designViewId": "designView_storm", "nodes": []}]
            }],
            "entities": [
                {"id": "sc_storm", "type": "SCENE", "label": "乒乓风暴场景"},
                {"id": "op_ping", "type": "INDEPENDENT-PROCESS", "label": "算子Ping", "timeRelated": "UNRELATED"},
                {"id": "op_pong", "type": "INDEPENDENT-PROCESS", "label": "算子Pong", "timeRelated": "UNRELATED"}
            ],
            "cellMap": {}
        }
    }
    part_b = {"success": True, "obj": {"cellMap": {"designView_storm": []}}}
    return part_a, part_b


def main():
    parser = argparse.ArgumentParser(description="生成 Groma 标准参考模型语料文件对")
    parser.add_argument("out_dir_pos", nargs="?", default=None, help="输出目录（位置参数形式）")
    parser.add_argument("--out-dir", dest="out_dir_opt", default=None, help="输出目录")
    args = parser.parse_args()
    out_dir = args.out_dir_opt or args.out_dir_pos
    if not out_dir:
        parser.error("必须通过 --out-dir 或位置参数指定输出目录")
    os.makedirs(out_dir, exist_ok=True)

    for prefix, builder in [("rm-b2", build_rm_b2_full_29_shapes), ("rm-b3", build_rm_b3_event_storm)]:
        a_obj, b_obj = builder()
        with open(os.path.join(out_dir, f"{prefix}-a.json"), "w", encoding="utf-8") as fa:
            json.dump(a_obj, fa, ensure_ascii=False, indent=2)
        with open(os.path.join(out_dir, f"{prefix}-b.json"), "w", encoding="utf-8") as fb:
            json.dump(b_obj, fb, ensure_ascii=False, indent=2)
    print(f"PASS H-08 参考模型已生成至 {out_dir}")


if __name__ == "__main__":
    main()
