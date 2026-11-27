export type MutationType = 'CREATE' | 'UPDATE' | 'REMOVE';

export interface EntityMutation {
  readonly entityId: string;
  readonly type: MutationType;
  readonly seq: number;
}

export interface InFlightSaveBatch {
  readonly watermarkSeq: number;
  readonly items: ReadonlyArray<EntityMutation>;
}

/**
 * 单调递增水位线保存跟踪器（根治存量 P0 缺陷 FE-01：保存请求飞行期间的新编辑绝不被响应回调误删）
 */
export class WatermarkSaveTracker {
  private monotonicSeq = 0;
  private baselineLoaded = false;
  private readonly pendingMap = new Map<string, EntityMutation>();

  public markBaselineLoaded(): void {
    this.baselineLoaded = true;
  }

  public trackMutation(entityId: string, type: MutationType): number {
    if (!this.baselineLoaded) {
      throw new Error('模型基线尚未加载完成，禁止记录增量编辑');
    }
    const seq = ++this.monotonicSeq;
    const existing = this.pendingMap.get(entityId);
    if (existing && existing.type === 'CREATE' && type === 'REMOVE') {
      this.pendingMap.delete(entityId);
      return seq;
    }
    const effectiveType: MutationType =
      existing && existing.type === 'CREATE' && type === 'UPDATE' ? 'CREATE' : type;
    this.pendingMap.set(entityId, { entityId, type: effectiveType, seq });
    return seq;
  }

  public snapshotForSave(): InFlightSaveBatch | null {
    if (this.pendingMap.size === 0) {
      return null;
    }
    return {
      watermarkSeq: this.monotonicSeq,
      items: Array.from(this.pendingMap.values())
    };
  }

  public acknowledgeSaved(watermarkSeq: number): void {
    for (const [id, mut] of this.pendingMap.entries()) {
      if (mut.seq <= watermarkSeq) {
        this.pendingMap.delete(id);
      }
    }
  }

  public pendingCount(): number {
    return this.pendingMap.size;
  }
}
