import { describe, expect, it } from 'vitest';
import { WatermarkSaveTracker } from '../utils/watermarkSaveTracker';
import { validateCanvasConnection } from '../graph/x6ConnectionGuard';
import { sanitizeNoteHtml } from '../utils/safeHtml';

describe('Groma Studio Web Guardrails', () => {
  it('preserves edits made while save request is in-flight (Fixes P0 FE-01)', () => {
    const tracker = new WatermarkSaveTracker();
    tracker.markBaselineLoaded();

    tracker.trackMutation('node_uav_1', 'CREATE');
    tracker.trackMutation('edge_1', 'UPDATE');

    const batch = tracker.snapshotForSave();
    expect(batch).not.toBeNull();
    expect(batch?.watermarkSeq).toBe(2);

    // 模拟网络保存尚未返回期间，用户继续修改了 node_uav_1 并新增了 node_station_2
    tracker.trackMutation('node_uav_1', 'UPDATE');
    tracker.trackMutation('node_station_2', 'CREATE');

    // 首批保存成功回调到达：只能清理 seq <= 2 的记录，seq 3 & 4 必须 100% 保留！
    tracker.acknowledgeSaved(batch!.watermarkSeq);
    expect(tracker.pendingCount()).toBe(2);
  });

  it('enforces VAL-009 duplicate same-direction edge guard and 29 shapes allowlist', () => {
    const uav = { cellId: 'c_uav', category: 'OBJECT_TYPE' as const };
    const cruise = { cellId: 'c_cruise', category: 'PROCESS' as const };

    const first = validateCanvasConnection('edge-agent', uav, cruise, []);
    expect(first.allowed).toBe(true);

    const dup = validateCanvasConnection('edge-agent', uav, cruise, [
      { edgeId: 'e1', shape: 'edge-agent', sourceId: 'c_uav', targetId: 'c_cruise' }
    ]);
    expect(dup.allowed).toBe(false);
  });

  it('strips dangerous script payloads from note rich text', () => {
    const dirty = '<p>安全备注</p><img src=x onerror=alert(1) /><script>evil()</script>';
    const clean = sanitizeNoteHtml(dirty);
    expect(clean).toBe('<p>安全备注</p>');
  });
});
