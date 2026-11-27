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



def build_rm_c1_semantic_gaps():
    """合成模型 RM-C1：挂账收口（概率组 CDF / psf 失败线 / paa 维持中断 / lastEnd 级联 / R24 真画布自调用）。
    全部过程场景归属（typeEntityId=sc_c1），host=场景根对象。
    叶子工具过程（innerClass/innerType，空设计）不进 processDefs，调用点即工具节点；
    业务过程空设计会崩 orderNodes——一律给至少一个工具节点。"""
    entities = [
        {"id": "enum_md", "type": "ENUM", "label": "收口状态",
         "defineViewId": "defineView_enum_md", "designViewId": "designView_enum_md"},
        {"id": "st_ready", "type": "STATE", "label": "待命"},
        {"id": "st_a", "type": "STATE", "label": "完成"},
        {"id": "st_b", "type": "STATE", "label": "降级"},
        {"id": "st_fault", "type": "STATE", "label": "故障"},
        {"id": "st_cancel", "type": "STATE", "label": "取消"},
        {"id": "sc_c1", "type": "SCENE", "label": "语义收口验证场景",
         "defineViewId": "defineView_sc_c1", "designViewId": "designView_sc_c1"},
        {"id": "attr_mode", "type": "ATTRIBUTE-MEMBER", "label": "收口状态成员", "fieldType": "收口状态",
         "typeEntityId": "sc_c1", "typeRange": "ENUM", "refTypeId": "enum_md",
         "customType": {"id": "enum_md", "label": "收口状态"}, "defaultValue": "待命", "stateInitial": "待命"},
        {"id": "attr_shield", "type": "ATTRIBUTE-MEMBER", "label": "护盾值", "fieldType": "double",
         "typeEntityId": "sc_c1", "typeRange": "BASE", "defaultValue": "100.0"},
        {"id": "attr_times", "type": "ATTRIBUTE-MEMBER", "label": "自调用次数", "fieldType": "long",
         "typeEntityId": "sc_c1", "typeRange": "BASE", "defaultValue": "0"},
        # 共享叶子工具过程：无副作用比较（各业务过程的填充节点）
        {"id": "proc_cmp", "type": "PROCESS", "label": "空比较", "timeRelated": "UNRELATED",
         "innerClass": "compare", "innerType": "eq", "typeEntityId": "sc_c1",
         "defineViewId": "defineView_cmp", "designViewId": "designView_cmp"},
        # 降级赋值：resetToOne 写 1 到绑定的护盾值成员（in-out-influence 边绑定）
        {"id": "proc_flip", "type": "PROCESS", "label": "降级赋值", "timeRelated": "UNRELATED",
         "innerClass": "assignment", "innerType": "resetToOne", "typeEntityId": "sc_c1",
         "defineViewId": "defineView_flip", "designViewId": "designView_flip"},
        # 业务过程（必须至少一个节点）
        {"id": "proc_failchild", "type": "PROCESS", "label": "必败算子", "timeRelated": "UNRELATED",
         "typeEntityId": "sc_c1", "defineViewId": "defineView_fail", "designViewId": "designView_fail"},
        {"id": "proc_slow", "type": "PROCESS", "label": "慢速巡检", "timeRelated": "RELATED",
         "timeUnit": "s", "timeNormal": 6.0, "typeEntityId": "sc_c1",
         "defineViewId": "defineView_slow", "designViewId": "designView_slow"},
        {"id": "proc_guard", "type": "PROCESS", "label": "受护任务", "timeRelated": "RELATED",
         "timeUnit": "s", "timeNormal": 8.0, "typeEntityId": "sc_c1",
         "defineViewId": "defineView_guard", "designViewId": "designView_guard"},
        {"id": "proc_driver_a", "type": "PROCESS", "label": "采样与失败流", "timeRelated": "UNRELATED",
         "typeEntityId": "sc_c1", "defineViewId": "defineView_da", "designViewId": "designView_da"},
        {"id": "proc_driver_b", "type": "PROCESS", "label": "维持中断流", "timeRelated": "RELATED",
         "timeUnit": "s", "timeNormal": 12.0, "typeEntityId": "sc_c1",
         "defineViewId": "defineView_db", "designViewId": "designView_db"},
        {"id": "proc_driver_c", "type": "PROCESS", "label": "结束点级联流", "timeRelated": "RELATED",
         "timeUnit": "s", "timeNormal": 10.0, "typeEntityId": "sc_c1",
         "defineViewId": "defineView_dc", "designViewId": "designView_dc"},
        # 连线实体
        {"id": "lnk_inv_da", "type": "LINK", "linkType": "invocation"},
        {"id": "lnk_inv_db", "type": "LINK", "linkType": "invocation"},
        {"id": "lnk_inv_dc", "type": "LINK", "linkType": "invocation"},
        {"id": "lnk_psf", "type": "LINK", "linkType": "invocation", "event": "psf"},
        {"id": "lnk_out_a", "type": "LINK", "linkType": "out-change", "probability": 0.5, "probabilityGroup": "g_mutex"},
        {"id": "lnk_out_b", "type": "LINK", "linkType": "out-change", "probability": 0.5, "probabilityGroup": "g_mutex"},
        {"id": "lnk_out_fault", "type": "LINK", "linkType": "out-change", "probability": 1.0},
        {"id": "lnk_fixed_shield", "type": "LINK", "linkType": "fixed"},
        {"id": "lnk_cond_fail", "type": "LINK", "linkType": "condition",
         "logic": {"entityId": "", "dataType": "", "expression": "",
                   "conditions": [{"index": 1, "code": ">=", "value": 1}]}},
        {"id": "lnk_agent_guard", "type": "LINK", "linkType": "agent",
         "logic": {"entityId": "", "dataType": "", "expression": "",
                   "conditions": [{"index": 1, "code": "<", "value": 50}]}},
        {"id": "lnk_out_cancel", "type": "LINK", "linkType": "out-change", "probability": 1.0},
    ]

    def node_cell(cid, eid, label, extra=None):
        cell = {"id": cid, "data": eid, "type": "PROCESS", "shape": "node-process", "label": label,
                "x": 0, "y": 0}
        if extra:
            cell.update(extra)
        return cell

    def member_cell(cid, eid, label):
        return {"id": cid, "data": eid, "type": "ATTRIBUTE-MEMBER", "shape": "node-attribute-member",
                "label": label, "x": 0, "y": 0}

    def state_cell(cid, eid, label):
        return {"id": cid, "data": eid, "type": "STATE", "shape": "node-state", "label": label, "x": 0, "y": 0}

    def link_cell(cid, eid, shape, src, dst, event=""):
        cell = {"id": cid, "data": eid, "type": "LINK", "shape": shape,
                "source": {"cell": src, "port": ""}, "target": {"cell": dst, "port": ""},
                "x": 0, "y": 0}
        if event:
            cell["event"] = event
        return cell

    cells_a = {
        "defineView_enum_md": [
            {"id": "cs_ready", "data": "st_ready", "type": "STATE", "shape": "node-state",
             "label": "待命", "stateInitial": "true", "x": 0, "y": 0},
            {"id": "cs_a", "data": "st_a", "type": "STATE", "shape": "node-state", "label": "完成", "x": 0, "y": 20},
            {"id": "cs_b", "data": "st_b", "type": "STATE", "shape": "node-state", "label": "降级", "x": 0, "y": 40},
            {"id": "cs_fault", "data": "st_fault", "type": "STATE", "shape": "node-state", "label": "故障", "x": 0, "y": 60},
            {"id": "cs_cancel", "data": "st_cancel", "type": "STATE", "shape": "node-state", "label": "取消", "x": 0, "y": 80},
        ],
        # 必败算子：定义视图挂启动条件（护盾值 < 50，初始 100 恒假 → canStart 失败 → psf）
        "defineView_fail": [
            member_cell("cf_times", "attr_times", "自调用次数"),
            node_cell("cf_node", "proc_failchild", "必败算子"),
            link_cell("cf_edge", "lnk_cond_fail", "edge-condition", "cf_times", "cf_node"),
        ],
        "designView_fail": [
            node_cell("cf_cmp", "proc_cmp", "空比较"),
        ],
        # 受护任务：定义视图挂维持条件（护盾值 > 50；被翻转后 → paa 中断）
        "defineView_guard": [
            member_cell("cg_shield", "attr_shield", "护盾值"),
            node_cell("cg_node", "proc_guard", "受护任务"),
            link_cell("cg_edge", "lnk_agent_guard", "edge-agent", "cg_shield", "cg_node"),
        ],
        "designView_guard": [
            node_cell("cg_cmp", "proc_cmp", "空比较"),
        ],
        "designView_slow": [
            node_cell("cw_cmp", "proc_cmp", "空比较"),
        ],
        # R24 真画布自环：零时间配额熔断验收
        "designView_self": [
            node_cell("cs_self", "proc_self", "自调用算子"),
            link_cell("cs_loop", "lnk_self_loop", "edge-invocation-self", "cs_self", "cs_self"),
        ],
        # 采样与失败流
        "designView_da": [
            node_cell("ca_mutex", "proc_cmp", "分支采样点", {"firstStart": "true"}),
            node_cell("ca_fail", "proc_failchild", "必败算子"),
            node_cell("ca_handler", "proc_cmp", "失败处理点"),
            member_cell("ca_mode", "attr_mode", "收口状态成员"),
            state_cell("ca_st_a", "st_a", "完成"),
            state_cell("ca_st_b", "st_b", "降级"),
            state_cell("ca_st_fault", "st_fault", "故障"),
            link_cell("ca_e1", "lnk_inv_da", "edge-invocation", "ca_mutex", "ca_fail"),
            link_cell("ca_e2", "lnk_psf", "edge-invocation", "ca_fail", "ca_handler", event="psf"),
            link_cell("ca_e3", "lnk_out_a", "edge-out-change", "ca_mutex", "ca_st_a"),
            link_cell("ca_e4", "lnk_out_b", "edge-out-change", "ca_mutex", "ca_st_b"),
            link_cell("ca_e5", "lnk_out_fault", "edge-out-change", "ca_handler", "ca_st_fault"),
        ],
        # 维持中断流：入口分叉（guard 与 flip 并行）→ flip 立即写 shield=1 → guard 下一拍维持条件失能 → paa
        "designView_db": [
            node_cell("cb_entry", "proc_cmp", "入口", {"firstStart": "true"}),
            node_cell("cb_guard", "proc_guard", "受护任务"),
            node_cell("cb_flip", "proc_flip", "降级赋值"),
            member_cell("cb_shield", "attr_shield", "护盾值"),
            link_cell("cb_e0", "lnk_inv_db", "edge-invocation", "cb_entry", "cb_guard"),
            link_cell("cb_e0b", "lnk_inv_db", "edge-invocation", "cb_entry", "cb_flip"),
            link_cell("cb_e2", "lnk_fixed_shield", "edge-fixed", "cb_shield", "cb_flip"),
        ],
        # 结束点级联流（快速点 lastEnd → 慢速巡检被级联取消）
        "designView_dc": [
            node_cell("cc_quick", "proc_cmp", "快速点", {"firstStart": "true", "lastEnd": "true"}),
            node_cell("cc_slow", "proc_slow", "慢速巡检"),
            member_cell("cc_mode", "attr_mode", "收口状态成员"),
            state_cell("cc_st_cancel", "st_cancel", "取消"),
            link_cell("cc_e1", "lnk_inv_dc", "edge-invocation", "cc_quick", "cc_slow"),
            link_cell("cc_e2", "lnk_out_cancel", "edge-out-change", "cc_slow", "cc_st_cancel"),
        ],
        "designView_sc_c1": [
            node_cell("cd_a", "proc_driver_a", "采样与失败流", {"defaultStart": "true"}),
            node_cell("cd_b", "proc_driver_b", "维持中断流", {"defaultStart": "true"}),
            node_cell("cd_c", "proc_driver_c", "结束点级联流", {"defaultStart": "true"}),
        ],
    }
    for vid in ("defineView_mutex", "designView_mutex", "defineView_handler", "designView_handler",
                "defineView_flip", "designView_flip", "defineView_quick", "designView_quick",
                "defineView_cmp", "designView_cmp", "defineView_slow", "defineView_dc",
                "defineView_db", "defineView_da", "defineView_self", "defineView_sc_c1",
                "designView_enum_md", "defineView_dc"):
        cells_a.setdefault(vid, [])

    part_a = {
        "success": True,
        "obj": {
            "id": "RM_C1_SEMANTIC_GAPS",
            "label": "语义挂账收口基准模型",
            "version": 1,
            "nodes": [{
                "id": "folder_scenes_c1", "type": "SCENES", "label": "运行场景",
                "nodes": [{
                    "id": "node_sc_c1", "entityId": "sc_c1", "type": "SCENE", "label": "语义收口验证场景",
                    "defineViewId": "defineView_sc_c1", "designViewId": "designView_sc_c1",
                    "nodes": [
                        {"id": "n_m1", "entityId": "attr_mode", "type": "ATTRIBUTE-MEMBER", "label": "收口状态成员"},
                        {"id": "n_m2", "entityId": "attr_shield", "type": "ATTRIBUTE-MEMBER", "label": "护盾值"},
                        {"id": "n_m3", "entityId": "attr_times", "type": "ATTRIBUTE-MEMBER", "label": "自调用次数"},
                        {"id": "n_p1", "entityId": "proc_driver_a", "type": "PROCESS", "label": "采样与失败流"},
                        {"id": "n_p2", "entityId": "proc_driver_b", "type": "PROCESS", "label": "维持中断流"},
                        {"id": "n_p3", "entityId": "proc_driver_c", "type": "PROCESS", "label": "结束点级联流"},
                        {"id": "n_p4", "entityId": "proc_failchild", "type": "PROCESS", "label": "必败算子"},
                        {"id": "n_p5", "entityId": "proc_slow", "type": "PROCESS", "label": "慢速巡检"},
                        {"id": "n_p6", "entityId": "proc_guard", "type": "PROCESS", "label": "受护任务"},
                        {"id": "n_p7", "entityId": "proc_cmp", "type": "PROCESS", "label": "空比较"},
                        {"id": "n_p8", "entityId": "proc_flip", "type": "PROCESS", "label": "降级赋值"},
                    ]
                }]
            }],
            "entities": entities,
            "cellMap": dict(cells_a),
        }
    }
    part_b = {"success": True, "obj": {"cellMap": {}}}
    return part_a, part_b



def build_rm_c2_self_recursion():
    """合成模型 RM-C2：R24 真画布自调用验收——默认启动即自环，零时间配额熔断 + SOE 会话级兜底（独立会话隔离）。"""
    entities = [
        {"id": "sc_c2", "type": "SCENE", "label": "自调用验收场景",
         "defineViewId": "defineView_sc_c2", "designViewId": "designView_sc_c2"},
        {"id": "proc_recur", "type": "PROCESS", "label": "自递归算子", "timeRelated": "UNRELATED",
         "typeEntityId": "sc_c2", "defineViewId": "defineView_recur", "designViewId": "designView_recur"},
        {"id": "proc_cmp2", "type": "PROCESS", "label": "空比较", "timeRelated": "UNRELATED",
         "innerClass": "compare", "innerType": "eq", "typeEntityId": "sc_c2",
         "defineViewId": "defineView_cmp2", "designViewId": "designView_cmp2"},
        {"id": "lnk_self_loop2", "type": "LINK", "linkType": "invocation-self"},
        {"id": "lnk_inv2", "type": "LINK", "linkType": "invocation"},
    ]
    cells_a = {
        "designView_recur": [
            {"id": "cr_self", "data": "proc_recur", "type": "PROCESS", "shape": "node-process",
             "label": "自递归算子", "x": 0, "y": 0},
            {"id": "cr_loop", "data": "lnk_self_loop2", "type": "LINK", "shape": "edge-invocation-self",
             "source": {"cell": "cr_self", "port": ""}, "target": {"cell": "cr_self", "port": ""}, "x": 0, "y": 0},
        ],
        "designView_sc_c2": [
            {"id": "cd_recur", "data": "proc_recur", "type": "PROCESS", "shape": "node-process",
             "label": "自递归算子", "defaultStart": "true", "x": 0, "y": 0},
        ],
    }
    for vid in ("defineView_sc_c2", "defineView_recur", "defineView_cmp2", "designView_cmp2"):
        cells_a.setdefault(vid, [])
    part_a = {
        "success": True,
        "obj": {
            "id": "RM_C2_SELF_RECURSION",
            "label": "自调用验收基准模型",
            "version": 1,
            "nodes": [{
                "id": "folder_scenes_c2", "type": "SCENES", "label": "运行场景",
                "nodes": [{
                    "id": "node_sc_c2", "entityId": "sc_c2", "type": "SCENE", "label": "自调用验收场景",
                    "defineViewId": "defineView_sc_c2", "designViewId": "designView_sc_c2",
                    "nodes": [
                        {"id": "n_q1", "entityId": "proc_recur", "type": "PROCESS", "label": "自递归算子"},
                        {"id": "n_q2", "entityId": "proc_cmp2", "type": "PROCESS", "label": "空比较"},
                    ]
                }]
            }],
            "entities": entities,
            "cellMap": dict(cells_a),
        }
    }
    part_b = {"success": True, "obj": {"cellMap": {}}}
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

    for prefix, builder in [("rm-b2", build_rm_b2_full_29_shapes), ("rm-b3", build_rm_b3_event_storm),
                            ("rm-c1", build_rm_c1_semantic_gaps), ("rm-c2", build_rm_c2_self_recursion)]:
        a_obj, b_obj = builder()
        with open(os.path.join(out_dir, f"{prefix}-a.json"), "w", encoding="utf-8") as fa:
            json.dump(a_obj, fa, ensure_ascii=False, indent=2)
        with open(os.path.join(out_dir, f"{prefix}-b.json"), "w", encoding="utf-8") as fb:
            json.dump(b_obj, fb, ensure_ascii=False, indent=2)
    print(f"PASS H-08 参考模型已生成至 {out_dir}")


if __name__ == "__main__":
    main()
