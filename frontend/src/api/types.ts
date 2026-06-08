// ==================== 后端 DTO 镜像 ====================

/** 会话实体 (mirrors ConversationSession.java) */
export interface Session {
  id: string
  userId: string | null
  createdAt: string        // ISO datetime
  updatedAt: string
  title: string | null
  metadata: string | null  // JSONB string
}

/** 消息实体 (mirrors ConversationMessage.java) */
export interface Message {
  id: number
  sessionId: string
  userId: string | null
  role: 'user' | 'assistant'
  content: string
  tokensUsed: number | null
  createdAt: string        // ISO datetime
}

/** RAG 结构化回答 (mirrors RagAnswer.java) */
export interface RagAnswer {
  answer: string
  sources: string[]
  confidence: number
}

/** 实体抽取 (mirrors EntityExtraction.java) */
export interface EntityExtraction {
  name: string
  type: string
  description: string
}

/** 批量处理响应 */
export interface BatchProcessResponse {
  totalFiles: number
  successCount: number
  failureCount: number
  totalProcessingTimeMs: number
  results: BatchProcessResult[]
}

export interface BatchProcessResult {
  fileName: string
  success: boolean
  content: string
  error: string | null
  fileType: string
  processingTimeMs: number
}

// ==================== 前端业务类型 ====================

/** 对话模式 */
export type ChatMode = 'sync' | 'stream' | 'rag' | 'ragStructured' | 'agent'

/** 前端 UI 消息 (扩展 API 消息) */
export interface ChatMessage extends Message {
  isStreaming?: boolean
  ragAnswer?: RagAnswer  // RAG structured 模式下附加的结构化数据
}

/** 聊天参数 */
export interface ChatParams {
  message: string
  userId?: string
  sessionId?: string
}

// Make these interfaces compatible with Record<string, string | undefined>
// by adding an explicit index signature cast in the API layer

/** Agent 聊天参数 */
export interface AgentChatParams {
  sessionId?: string
  userId: string
  message: string
}

/** 创建会话参数 */
export interface CreateSessionParams {
  userId?: string
  title?: string
}
