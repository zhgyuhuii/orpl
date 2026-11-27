import { invokeApi } from '../request/httpClient';

export interface LeaseState {
  readonly modelCode: string;
  readonly leaseToken: string;
  readonly holderName: string;
  readonly readOnlyMode: boolean;
}

/**
 * 模型协同编辑租约续期管理器（落实 FE-SAV-003：锁丢失自动切换只读保护并释放后台续期句柄）
 */
export class ModelEditLockManager {
  private renewTimerId: ReturnType<typeof globalThis.setInterval> | null = null;
  private currentLease: LeaseState | null = null;

  public async acquireLease(modelCode: string, onLostLease: () => void): Promise<LeaseState> {
    this.stopRenewal();
    const lease = await invokeApi<LeaseState>(`/api/v1/studio/locks/${encodeURIComponent(modelCode)}/acquire`, {
      method: 'POST'
    });
    this.currentLease = lease;
    if (!lease.readOnlyMode) {
      this.renewTimerId = globalThis.setInterval(async () => {
        try {
          await invokeApi<void>(`/api/v1/studio/locks/${encodeURIComponent(modelCode)}/renew`, {
            method: 'POST',
            payload: { leaseToken: lease.leaseToken }
          });
        } catch {
          this.stopRenewal();
          onLostLease();
        }
      }, 20_000);
    }
    return lease;
  }

  public stopRenewal(): void {
    if (this.renewTimerId !== null) {
      globalThis.clearInterval(this.renewTimerId);
      this.renewTimerId = null;
    }
    this.currentLease = null;
  }

  public getLease(): LeaseState | null {
    return this.currentLease;
  }
}
