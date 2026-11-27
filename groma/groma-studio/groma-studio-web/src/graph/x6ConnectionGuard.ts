export type GromaNodeCategory = 'OBJECT_TYPE' | 'STATE_ITEM' | 'OPERATOR' | 'PROCESS' | 'NOTE';

export interface CanvasNodeMeta {
  readonly cellId: string;
  readonly category: GromaNodeCategory;
}

export interface ExistingEdgeMeta {
  readonly edgeId: string;
  readonly shape: string;
  readonly sourceId: string;
  readonly targetId: string;
}

export const ALL_29_EDGE_SHAPES: ReadonlySet<string> = new Set([
  'edge-extend',
  'edge-composition',
  'edge-exhibition',
  'edge-classification',
  'edge-proxy',
  'edge-control',
  'edge-unidirectional-association',
  'edge-bidirectional-association',
  'edge-unidirectional-tagged',
  'edge-bidirectional-tagged',
  'edge-consumption',
  'edge-result',
  'edge-in-change',
  'edge-out-change',
  'edge-in-out',
  'edge-in-influence-nullable',
  'edge-in-influence-nonnull',
  'edge-out-influence',
  'edge-in-out-influence-nullable',
  'edge-in-out-influence-nonnull',
  'edge-agent',
  'edge-condition',
  'edge-invocation',
  'edge-invocation-self',
  'edge-overtime-exception',
  'edge-undertime-exception',
  'edge-note',
  'edge-fixed',
  'edge-link'
]);

export interface ConnectionCheckVerdict {
  readonly allowed: boolean;
  readonly reason: string;
}

/**
 * 画布连线前置守卫（拦截未知形状、非自调用自环以及 VAL-009 重复同向关系，根治存量 FE-15）
 */
export function validateCanvasConnection(
  edgeShape: string,
  source: CanvasNodeMeta,
  target: CanvasNodeMeta,
  existingEdges: ReadonlyArray<ExistingEdgeMeta>
): ConnectionCheckVerdict {
  if (!ALL_29_EDGE_SHAPES.has(edgeShape)) {
    return { allowed: false, reason: `未注册的连线形状: ${edgeShape}` };
  }

  if (source.cellId === target.cellId && edgeShape !== 'edge-invocation-self' && edgeShape !== 'edge-composition') {
    return { allowed: false, reason: '仅自调用关系（invocation-self）或递归聚合允许连接自身节点' };
  }

  if (edgeShape === 'edge-overtime-exception' || edgeShape === 'edge-undertime-exception') {
    if (source.category !== 'PROCESS') {
      return { allowed: false, reason: '超时/欠时异常连线只能从耗时过程节点引出' };
    }
  }

  if (edgeShape !== 'edge-fixed') {
    const duplicate = existingEdges.some(
      (e) => e.sourceId === source.cellId && e.targetId === target.cellId && e.shape === edgeShape
    );
    if (duplicate) {
      return { allowed: false, reason: '违反标准 10.1.3（VAL-009）：同一对节点间已存在相同的同向关系连线' };
    }
  }

  return { allowed: true, reason: 'OK' };
}
