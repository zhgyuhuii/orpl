import { invokeApi } from '../request/httpClient';
import { STUDIO_REST_BASE } from '../utils/runtimeEnv';

/**
 * Studio 后端 REST 门面（唯一 HTTP 出口为 httpClient.invokeApi，前缀 /api/v1/studio）。
 * 端点与 groma-bootstrap 的 SessionRestController / StudioBuildController 对齐：
 * - GET  /api/v1/studio/models                    已挂载模型清单
 * - POST /api/v1/studio/models/build              语料构建并热挂载
 * - GET  /api/v1/studio/sessions                  活跃会话清单
 * - POST /api/v1/studio/sessions                  创建推演会话
 * - GET  /api/v1/studio/sessions/{id}/snapshot    不可变世界快照
 * - POST /api/v1/studio/locks/{modelCode}/acquire|renew  编辑租约锁（editLockManager 使用）
 * 画布图元与五层源码读取端点为 Studio 预留接口（后端未就绪时调用方自行降级）。
 */

export interface SessionSummary {
  readonly sessionId: string;
  readonly modelCode: string;
  readonly modelVersion: string;
  readonly sceneCode: string;
  readonly status: string;
  readonly tickIndex: number;
  readonly worldTimeMs: number;
  readonly speed: number;
  readonly instanceCount: number;
  readonly activeProcessCount: number;
}

export interface MountedModelInfo {
  readonly modelCode: string;
  readonly modelName?: string;
  readonly version?: string;
  readonly mountedAt?: string;
}

export interface StartSessionPayload {
  readonly modelCode: string;
  readonly sceneCode: string;
  readonly initialOverrides?: Readonly<Record<string, unknown>>;
  readonly seed?: number;
  readonly startPaused?: boolean;
}

export interface CanvasCellDTO {
  readonly id: string;
  readonly shape: string;
  readonly label?: string;
  readonly category?: string;
  readonly source?: string;
  readonly target?: string;
}

export interface CanvasPageDTO {
  readonly modelCode: string;
  readonly cells: ReadonlyArray<CanvasCellDTO>;
}

export interface SourceFileDTO {
  readonly relativeFile: string;
  readonly content: string;
}

export async function fetchMountedModels(): Promise<ReadonlyArray<MountedModelInfo>> {
  const payload = await invokeApi<{ models?: ReadonlyArray<MountedModelInfo> }>(
    `${STUDIO_REST_BASE}/models`
  );
  return payload?.models ?? [];
}

export async function fetchModelCanvas(modelCode: string): Promise<CanvasPageDTO> {
  return invokeApi<CanvasPageDTO>(
    `${STUDIO_REST_BASE}/models/${encodeURIComponent(modelCode)}/canvas`
  );
}

export async function fetchModelSource(
  modelCode: string,
  relativeFile: string
): Promise<SourceFileDTO> {
  return invokeApi<SourceFileDTO>(
    `${STUDIO_REST_BASE}/models/${encodeURIComponent(modelCode)}/sources?relativeFile=${encodeURIComponent(relativeFile)}`
  );
}

export async function fetchActiveSessions(): Promise<ReadonlyArray<SessionSummary>> {
  return invokeApi<ReadonlyArray<SessionSummary>>(`${STUDIO_REST_BASE}/sessions`);
}

export async function startSession(payload: StartSessionPayload): Promise<SessionSummary> {
  return invokeApi<SessionSummary>(`${STUDIO_REST_BASE}/sessions`, {
    method: 'POST',
    payload
  });
}

export async function fetchSessionSnapshot(sessionId: string): Promise<unknown> {
  return invokeApi<unknown>(
    `${STUDIO_REST_BASE}/sessions/${encodeURIComponent(sessionId)}/snapshot`
  );
}
