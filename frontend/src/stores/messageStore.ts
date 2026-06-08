import { create } from 'zustand'
import type { Message as ApiMessage, ChatMessage } from '../api/types'

interface MessageState {
  // sessionId -> 消息列表
  messages: Record<string, ChatMessage[]>
  // sessionId -> 是否正在流式输出
  streaming: Record<string, boolean>
  // sessionId -> 当前流式输出的累积文本（用于显示中回复）
  streamingText: Record<string, string>

  addMessage: (sessionId: string, message: ChatMessage) => void
  addApiMessage: (sessionId: string, apiMsg: ApiMessage) => void
  updateStreamingText: (sessionId: string, text: string) => void
  finishStreaming: (sessionId: string) => void
  setStreaming: (sessionId: string, streaming: boolean) => void
  clearMessages: (sessionId: string) => void
  replaceHistory: (sessionId: string, messages: ApiMessage[]) => void
}

export const useMessageStore = create<MessageState>((set) => ({
  messages: {},
  streaming: {},
  streamingText: {},

  addMessage: (sessionId, message) =>
    set((s) => ({
      messages: {
        ...s.messages,
        [sessionId]: [...(s.messages[sessionId] || []), message],
      },
    })),

  addApiMessage: (sessionId, apiMsg) => {
    const chatMsg: ChatMessage = {
      ...apiMsg,
      isStreaming: false,
    }
    set((s) => ({
      messages: {
        ...s.messages,
        [sessionId]: [...(s.messages[sessionId] || []), chatMsg],
      },
    }))
  },

  updateStreamingText: (sessionId, text) =>
    set((s) => ({
      streamingText: { ...s.streamingText, [sessionId]: text },
    })),

  finishStreaming: (sessionId) =>
    set((s) => {
      const streamingMsgs = s.messages[sessionId] || []
      const lastMsg = streamingMsgs[streamingMsgs.length - 1]

      if (lastMsg && lastMsg.role === 'assistant' && lastMsg.isStreaming) {
        // 用流式文本替换最后一条流式消息
        const updatedMessages = [...streamingMsgs]
        updatedMessages[updatedMessages.length - 1] = {
          ...lastMsg,
          content: s.streamingText[sessionId] || lastMsg.content,
          isStreaming: false,
        }
        return {
          messages: { ...s.messages, [sessionId]: updatedMessages },
          streaming: { ...s.streaming, [sessionId]: false },
          streamingText: { ...s.streamingText, [sessionId]: '' },
        }
      }

      // 如果没有流式消息，创建新的
      return {
        messages: {
          ...s.messages,
          [sessionId]: [
            ...(s.messages[sessionId] || []),
            {
              id: 0,
              sessionId,
              role: 'assistant',
              content: s.streamingText[sessionId] || '',
              userId: null,
              tokensUsed: null,
              createdAt: new Date().toISOString(),
              isStreaming: false,
            },
          ],
        },
        streaming: { ...s.streaming, [sessionId]: false },
        streamingText: { ...s.streamingText, [sessionId]: '' },
      }
    }),

  setStreaming: (sessionId, streaming) =>
    set((s) => ({ streaming: { ...s.streaming, [sessionId]: streaming } })),

  clearMessages: (sessionId) =>
    set((s) => {
      const copy = { ...s.messages }
      delete copy[sessionId]
      return { messages: copy }
    }),

  replaceHistory: (sessionId, messages) =>
    set((s) => {
      const chatMessages: ChatMessage[] = messages.map((msg) => ({
        ...msg,
        isStreaming: false,
      }))
      return {
        messages: {
          ...s.messages,
          [sessionId]: chatMessages,
        },
      }
    }),
}))
