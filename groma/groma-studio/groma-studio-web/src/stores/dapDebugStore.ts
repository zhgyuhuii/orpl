import { defineStore } from 'pinia';
import { ref } from 'vue';

export interface StoppedDebugFrame {
  readonly relativeFile: string;
  readonly line: number;
  readonly simulationTick: number;
  readonly highlightedCanvasElementId: string;
  readonly highlighted3dEntityId: string;
  readonly inlineVariables: Readonly<Record<string, unknown>>;
}

export const useDapDebugStore = defineStore('gromaDapDebug', () => {
  const connected = ref(false);
  const pausedFrame = ref<StoppedDebugFrame | null>(null);
  const breakpointsByFile = ref<Record<string, number[]>>({});
  let ws: WebSocket | null = null;
  let reqSeq = 0;

  function connectDebugChannel(wsBaseUrl: string, sessionId: string): void {
    disconnectDebugChannel();
    const socket = new WebSocket(`${wsBaseUrl}/ws/debug/${encodeURIComponent(sessionId)}`);
    ws = socket;
    socket.onopen = () => {
      connected.value = true;
      sendCommand('initialize', {});
    };
    socket.onmessage = (ev: MessageEvent<string>) => {
      const msg = JSON.parse(ev.data) as Record<string, unknown>;
      if (msg.type === 'event' && msg.event === 'stopped') {
        pausedFrame.value = {
          relativeFile: String(msg.relativeFile ?? ''),
          line: Number(msg.line ?? 1),
          simulationTick: Number(msg.simulationTick ?? 0),
          highlightedCanvasElementId: String(msg.highlightedCanvasElementId ?? ''),
          highlighted3dEntityId: String(msg.highlighted3dEntityId ?? ''),
          inlineVariables: (msg.inlineVariables as Record<string, unknown>) ?? {}
        };
      }
    };
    socket.onclose = () => {
      connected.value = false;
      pausedFrame.value = null;
    };
  }

  function toggleLineBreakpoint(relativeFile: string, line: number): void {
    const existing = breakpointsByFile.value[relativeFile] ?? [];
    const next = existing.includes(line) ? existing.filter((l) => l !== line) : [...existing, line];
    breakpointsByFile.value = { ...breakpointsByFile.value, [relativeFile]: next };
    sendCommand('setBreakpoints', {
      file: relativeFile,
      breakpoints: next.map((ln) => ({ line: ln, condition: '', logMessage: '' }))
    });
  }

  function sendStepCommand(cmd: 'continue' | 'next' | 'stepIn' | 'stepOut' | 'nextSimulationTick'): void {
    if (cmd === 'continue') {
      pausedFrame.value = null;
    }
    sendCommand(cmd, {});
  }

  function sendCommand(command: string, args: Record<string, unknown>): void {
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify({ seq: ++reqSeq, command, arguments: args }));
    }
  }

  function disconnectDebugChannel(): void {
    if (ws) {
      ws.close();
      ws = null;
    }
    connected.value = false;
    pausedFrame.value = null;
  }

  return {
    connected,
    pausedFrame,
    breakpointsByFile,
    connectDebugChannel,
    toggleLineBreakpoint,
    sendStepCommand,
    disconnectDebugChannel
  };
});
