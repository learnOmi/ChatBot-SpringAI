import { useCallback, useState } from 'react'
import { useUserStore } from '../stores/userStore'
import { useSessionStore } from '../stores/sessionStore'
import { useMessageStore } from '../stores/messageStore'
import {
  sendSyncChat,
  sendRagChat,
  sendRagStructuredChat,
  sendAgentChat,
  getStreamChat,
} from '../api/chatApi'
import type { ChatMode, ChatMessage } from '../api/types'
import { useStreaming } from './useStreaming'

/**
 * 消息发送 Hook
 *
 * 根据当前模式分发到不同 API，处理同步/流式/结构化 RAG/Agent 对话。
 */

interface UseSendMessageReturn {
  sendMessage: (message: string, mode: ChatMode) => Promise<void>
  isSending: boolean
}

export function useSendMessage(): UseSendMessageReturn {
  const { stream } = useStreaming()
  const [isSending, setIsSending] = useState(false)

  const addMessage = useMessageStore.getState().addMessage
  const updateStreamingText = useMessageStore.getState().updateStreamingText
  const finishStreaming = useMessageStore.getState().finishStreaming

  const sendMessage = useCallback(
    async (message: string, mode: ChatMode) => {
      if (isSending) return
      setIsSending(true)

      const userId = localStorage.getItem('springairobot_userid') || null
      const activeSessionId = useSessionStore.getState().activeSessionId

      // 如果没有活跃会话，创建一个新的
      let sessionId = activeSessionId
      if (!sessionId) {
        const newSession = await useSessionStore.getState().createNewSession()
        sessionId = newSession
        // 更新 userId
        if (userId) {
          useUserStore.getState().setUserId(userId)
        }
      }

      // 乐观添加用户消息
      const userMessage: ChatMessage = {
        id: 0,
        sessionId,
        userId,
        role: 'user',
        content: message,
        tokensUsed: null,
        createdAt: new Date().toISOString(),
        isStreaming: false,
      }
      addMessage(sessionId, userMessage)

      // 清空之前的流式状态
      updateStreamingText(sessionId, '')

      try {
        const chatParams = { message, userId: userId || undefined, sessionId }
        const agentParams = { message, userId: userId || '', sessionId }

        switch (mode) {
          case 'sync': {
            const reply = await sendSyncChat(chatParams)
            addMessage(sessionId, {
              ...userMessage,
              id: 0,
              role: 'assistant',
              content: reply,
              isStreaming: false,
            })
            break
          }

          case 'stream': {
            // 先添加一个占位的流式助手消息
            const streamingMsg: ChatMessage = {
              id: 0,
              sessionId,
              role: 'assistant',
              content: '',
              userId,
              tokensUsed: null,
              createdAt: new Date().toISOString(),
              isStreaming: true,
            }
            addMessage(sessionId, streamingMsg)

            const streamBody = await getStreamChat(chatParams)
            // 需要累积文本
            let accumulated = ''
            await stream(
              streamBody,
              (chunk) => {
                accumulated += chunk
                updateStreamingText(sessionId, accumulated)
              },
              (_fullText) => {
                finishStreaming(sessionId)
              }
            )
            break
          }

          case 'rag': {
            const reply = await sendRagChat(chatParams)
            addMessage(sessionId, {
              ...userMessage,
              id: 0,
              role: 'assistant',
              content: reply,
              isStreaming: false,
            })
            break
          }

          case 'ragStructured': {
            const ragAnswer = await sendRagStructuredChat(chatParams)
            addMessage(sessionId, {
              ...userMessage,
              id: 0,
              role: 'assistant',
              content: ragAnswer.answer,
              isStreaming: false,
              ragAnswer,
            })
            break
          }

          case 'agent': {
            const reply = await sendAgentChat(agentParams)
            addMessage(sessionId, {
              ...userMessage,
              id: 0,
              role: 'assistant',
              content: reply,
              isStreaming: false,
            })
            break
          }
        }

        // 发送完成后刷新会话列表
        useSessionStore.getState().loadSessions()
      } catch (error) {
        console.error('发送消息失败:', error)
        // 添加错误消息
        addMessage(sessionId, {
          ...userMessage,
          id: 0,
          role: 'assistant',
          content: `抱歉，出现错误: ${(error as Error).message}`,
          isStreaming: false,
        })
      } finally {
        setIsSending(false)
      }
    },
    [stream, addMessage, updateStreamingText, finishStreaming, isSending]
  )

  return {
    sendMessage,
    isSending,
  }
}
