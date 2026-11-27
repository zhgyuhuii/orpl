export type HttpErrorKind = 'NETWORK' | 'TIMEOUT' | 'HTTP_STATUS' | 'BIZ_REJECTED' | 'NON_JSON_BODY';

export class GromaApiError extends Error {
  public readonly kind: HttpErrorKind;
  public readonly traceId: string;
  public readonly status: number;

  constructor(kind: HttpErrorKind, message: string, traceId = '', status = 0) {
    super(message);
    this.kind = kind;
    this.traceId = traceId;
    this.status = status;
  }
}

export interface ApiRequestConfig {
  readonly method?: 'GET' | 'POST' | 'PUT' | 'DELETE';
  readonly payload?: unknown;
  readonly timeoutMs?: number;
  readonly externalSignal?: AbortSignal;
  readonly headers?: Readonly<Record<string, string>>;
}

export async function invokeApi<T>(endpoint: string, config: ApiRequestConfig = {}): Promise<T> {
  const timeoutMs = config.timeoutMs ?? 30_000;
  const abortCtrl = new AbortController();
  const timerId = globalThis.setTimeout(() => abortCtrl.abort(), timeoutMs);

  if (config.externalSignal) {
    config.externalSignal.addEventListener('abort', () => abortCtrl.abort(), { once: true });
  }

  const reqHeaders: Record<string, string> = {
    Accept: 'application/json',
    ...(config.headers ?? {})
  };
  let bodyText: string | undefined;
  if (config.payload !== undefined) {
    reqHeaders['Content-Type'] = 'application/json; charset=UTF-8';
    bodyText = JSON.stringify(config.payload);
  }

  try {
    const response = await fetch(endpoint, {
      method: config.method ?? 'GET',
      headers: reqHeaders,
      body: bodyText,
      credentials: 'same-origin',
      signal: abortCtrl.signal
    });

    const traceId = response.headers.get('X-Request-Id') ?? '';
    if (!response.ok) {
      const fallbackMsg = response.statusText || `服务端响应异常 (HTTP ${response.status})`;
      throw new GromaApiError('HTTP_STATUS', fallbackMsg, traceId, response.status);
    }

    let envelope: { success?: boolean; message?: string; traceId?: string; data?: T };
    try {
      envelope = (await response.json()) as { success?: boolean; message?: string; traceId?: string; data?: T };
    } catch {
      throw new GromaApiError('NON_JSON_BODY', '网关或服务端返回了非 JSON 格式内容', traceId, response.status);
    }

    const finalTrace = envelope.traceId ?? traceId;
    if (envelope.success === false) {
      throw new GromaApiError('BIZ_REJECTED', envelope.message || '业务校验未通过', finalTrace, response.status);
    }
    return (envelope.data ?? (envelope as unknown as T));
  } catch (err) {
    if (err instanceof GromaApiError) {
      throw err;
    }
    if (err instanceof DOMException && err.name === 'AbortError') {
      throw new GromaApiError('TIMEOUT', `请求已取消或超出 ${timeoutMs}ms 时限`);
    }
    throw new GromaApiError('NETWORK', err instanceof Error ? err.message : '网络连接中断');
  } finally {
    globalThis.clearTimeout(timerId);
  }
}
