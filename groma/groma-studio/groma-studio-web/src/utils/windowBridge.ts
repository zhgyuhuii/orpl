export interface HostFrameEvent {
  readonly type: 'GROMA_SYNC_DIRTY_STATE' | 'GROMA_OPEN_MODEL_TAB';
  readonly modelCode: string;
  readonly dirty: boolean;
}

export function postToTrustedOrigin(
  targetWin: Window | null,
  payload: HostFrameEvent,
  exactOrigin: string
): void {
  if (!targetWin || !exactOrigin || exactOrigin === '*') {
    throw new Error('禁止向未声明或通配源发送跨窗口消息');
  }
  targetWin.postMessage(payload, exactOrigin);
}

export function subscribeTrustedMessages(
  trustedOrigins: ReadonlySet<string>,
  callback: (ev: HostFrameEvent) => void
): () => void {
  const listener = (event: MessageEvent<unknown>): void => {
    if (!trustedOrigins.has(event.origin)) {
      return;
    }
    if (typeof event.data === 'object' && event.data !== null && 'type' in event.data && 'modelCode' in event.data) {
      const msg = event.data as HostFrameEvent;
      if (msg.type === 'GROMA_SYNC_DIRTY_STATE' || msg.type === 'GROMA_OPEN_MODEL_TAB') {
        callback(msg);
      }
    }
  };
  window.addEventListener('message', listener);
  return () => window.removeEventListener('message', listener);
}
