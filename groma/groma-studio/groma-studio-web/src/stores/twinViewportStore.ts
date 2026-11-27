import { defineStore } from 'pinia';
import { ref } from 'vue';

export interface TwinSnapshotState {
  readonly sessionId: string;
  readonly tick: number;
  readonly worldTimeMs: number;
  readonly speed: number;
  readonly status: string;
  readonly instances: Readonly<Record<string, unknown>>;
}

export const useTwinViewportStore = defineStore('gromaTwinViewport', () => {
  const latestSnapshot = ref<TwinSnapshotState | null>(null);
  const focused3dEntityId = ref<string>('');
  let simSocket: WebSocket | null = null;
  let heartbeatTimer: ReturnType<typeof globalThis.setInterval> | null = null;

  function connectSituationRoom(wsBaseUrl: string, modelCode: string, sceneCode: string, sessionId: string): void {
    disconnectSituationRoom();
    const url = `${wsBaseUrl}/ws/groma/${encodeURIComponent(modelCode)}/${encodeURIComponent(sceneCode)}/${encodeURIComponent(sessionId)}`;
    const socket = new WebSocket(url);
    simSocket = socket;

    socket.onopen = () => {
      heartbeatTimer = globalThis.setInterval(() => {
        if (simSocket && simSocket.readyState === WebSocket.OPEN) {
          simSocket.send(`HRT:${Date.now()}`);
        }
      }, 15_000);
    };

    socket.onmessage = (ev: MessageEvent<string>) => {
      if (typeof ev.data === 'string' && ev.data.startsWith('TP:')) {
        const parsed = JSON.parse(ev.data.slice(3)) as TwinSnapshotState;
        latestSnapshot.value = parsed;
      }
    };

    socket.onclose = () => {
      clearHeartbeat();
    };
  }

  function sendControlFrame(rawPrefixCmd: string): void {
    if (simSocket && simSocket.readyState === WebSocket.OPEN) {
      simSocket.send(rawPrefixCmd);
    }
  }

  function clearHeartbeat(): void {
    if (heartbeatTimer !== null) {
      globalThis.clearInterval(heartbeatTimer);
      heartbeatTimer = null;
    }
  }

  function disconnectSituationRoom(): void {
    clearHeartbeat();
    if (simSocket) {
      simSocket.close();
      simSocket = null;
    }
  }

  return {
    latestSnapshot,
    focused3dEntityId,
    connectSituationRoom,
    sendControlFrame,
    disconnectSituationRoom
  };
});
