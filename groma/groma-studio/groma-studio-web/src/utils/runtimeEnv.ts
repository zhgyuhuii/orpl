/**
 * 运行环境推导：REST 统一走同源 /api/v1/studio 前缀（httpClient.ts 已封装），
 * WebSocket 基地址由页面协议推导（http->ws / https->wss）。
 */
export const STUDIO_REST_BASE = '/api/v1/studio';

export function resolveWsBaseUrl(): string {
  const isHttps = window.location.protocol === 'https:';
  const scheme = isHttps ? 'wss:' : 'ws:';
  return `${scheme}//${window.location.host}`;
}
