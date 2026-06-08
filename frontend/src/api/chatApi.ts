/**
 * 聊天/Agent/会话 API 调用
 *
 * 覆盖所有后端聊天相关端点：
 * - /api/chat/* (sync, stream, rag, rag/stream, rag/structured, sessions, history)
 * - /api/agent/* (chat, coordinate)
 */

import { request, requestText, buildQuery, API_BASE } from './client'
import type { Session, Message, RagAnswer, ChatParams, AgentChatParams, CreateSessionParams, ChatMessage } from './types'

const CHAT_BASE = '/api/chat'
const AGENT_BASE = '/api/agent'

// ==================== 聊天端点 ====================

/** GET /api/chat/sync?message=&userId=&sessionId= */
export async function sendSyncChat(params: ChatParams): Promise<string> {
  return requestText(buildQuery(`${CHAT_BASE}/sync`, params))
}

/** GET /api/chat/stream?message=&userId=&sessionId= */
export function getStreamChat(params: ChatParams): Promise<ReadableStream<Uint8Array>> {
  const url = `${API_BASE}${buildQuery(CHAT_BASE + '/stream', params)}`
  return fetch(url).then(r => {
    if (!r.ok) throw new Error(`Stream error ${r.status}`)
    return r.body as ReadableStream<Uint8Array>
  })
}

/** GET /api/chat/rag?message=&userId=&sessionId= */
export async function sendRagChat(params: ChatParams): Promise<string> {
  return requestText(buildQuery(`${CHAT_BASE}/rag`, params))
}

/** GET /api/chat/rag/stream?message=&userId=&sessionId= */
export function getRagStreamChat(params: ChatParams): Promise<ReadableStream<Uint8Array>> {
  const url = `${API_BASE}${buildQuery(CHAT_BASE + '/rag/stream', params)}`
  return fetch(url).then(r => {
    if (!r.ok) throw new Error(`Stream error ${r.status}`)
    return r.body as ReadableStream<Uint8Array>
  })
}

/** GET /api/chat/rag/structured?message=&userId=&sessionId= */
export async function sendRagStructuredChat(params: ChatParams): Promise<RagAnswer> {
  return request<RagAnswer>(buildQuery(`${CHAT_BASE}/rag/structured`, params))
}

/** GET /api/chat/rag/extractentities?query=&userId=&sessionId= */
export async function extractEntities(query: string, userId?: string, sessionId?: string): Promise<any[]> {
  return request<any[]>(buildQuery(`${CHAT_BASE}/rag/extractentities`, { query, userId, sessionId }))
}

// ==================== Agent 端点 ====================

/** POST /api/agent/chat?sessionId=&userId=&message= */
export async function sendAgentChat(params: AgentChatParams): Promise<string> {
  return requestText(buildQuery(`${AGENT_BASE}/chat`, params))
}

/** POST /api/agent/coordinate (多模态协调对话) */
export async function sendCoordinatorChat(params: {
  sessionId?: string
  userId?: string
  message: string
  image?: File
}): Promise<string> {
  const formData = new FormData()
  formData.append('message', params.message)
  if (params.sessionId) formData.append('sessionId', params.sessionId)
  if (params.userId) formData.append('userId', params.userId)
  if (params.image) formData.append('image', params.image)

  const url = `${API_BASE}${AGENT_BASE}/coordinate`
  const response = await fetch(url, {
    method: 'POST',
    body: formData,
  })
  if (!response.ok) throw new Error(`Coordinator error ${response.status}`)
  return response.text()
}

// ==================== 会话管理端点 ====================

/** GET /api/chat/user-sessions?userId= */
export async function listUserSessions(userId: string): Promise<Session[]> {
  return request<Session[]>(`${CHAT_BASE}/user-sessions?userId=${encodeURIComponent(userId)}`)
}

/** GET /api/chat/sessions (所有会话) */
export async function listAllSessions(): Promise<Session[]> {
  return request<Session[]>(`${CHAT_BASE}/sessions`)
}

/** POST /api/chat/session?userId=&title= */
export async function createSession(params: CreateSessionParams): Promise<string> {
  return requestText(buildQuery(`${CHAT_BASE}/session`, params), { method: 'POST' })
}

/** DELETE /api/chat/session?sessionId= */
export async function deleteSession(sessionId: string): Promise<void> {
  await fetch(`${API_BASE}${CHAT_BASE}/session?sessionId=${encodeURIComponent(sessionId)}`, {
    method: 'DELETE',
  })
}

/** GET /api/chat/history/{sessionId} */
export async function loadHistory(sessionId: string): Promise<Message[]> {
  return request<Message[]>(`${CHAT_BASE}/history/${sessionId}`)
}

// ==================== 工具函数 ====================

/** 从 API 消息转换为前端 ChatMessage (生成 id 和 createdAt 占位) */
export function apiMessageToChat(apiMsg: Message): ChatMessage {
  return {
    ...apiMsg,
    isStreaming: false,
    ragAnswer: undefined,
  }
}
