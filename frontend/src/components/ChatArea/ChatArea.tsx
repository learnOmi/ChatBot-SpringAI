import MessageBubble from './MessageBubble'
import { useMessageStore, useSessionStore } from '../../stores'
import { useSendMessage } from '../../hooks/useSendMessage'
import { useAutoScroll } from '../../hooks/useAutoScroll'
import ModelSelector from '../InputArea/ModelSelector'
import InputArea from '../InputArea/InputArea'
import type { ChatMode } from '../../api/types'
import { useEffect, useState } from 'react'

/**
 * 聊天主区域
 *
 * 包含消息列表、模型选择器、输入区域。
 */

export default function ChatArea() {
  const { messages, streamingText } = useMessageStore()
  const activeSessionId = useSessionStore((s) => s.activeSessionId)
  const { sendMessage, isSending } = useSendMessage()
  const [mode, setMode] = useState<ChatMode>('sync')
  const [inputValue, setInputValue] = useState('')

  const scrollRef = useAutoScroll(activeSessionId ? messages[activeSessionId || ''] : [])

  const chatMessages = activeSessionId ? (messages[activeSessionId] || []) : []
  const streamingMsg = activeSessionId ? (streamingText[activeSessionId] || '') : ''

  // 加载历史消息
  useEffect(() => {
    if (!activeSessionId) return
    const existing = messages[activeSessionId]
    if (existing && existing.length > 0) return

    import('../../api/chatApi').then(({ loadHistory }) => {
      loadHistory(activeSessionId).then((apiMessages) => {
        useMessageStore.getState().replaceHistory(activeSessionId, apiMessages)
      }).catch(() => {})
    })
  }, [activeSessionId, messages])

  const handleSubmit = () => {
    if (!inputValue.trim() || isSending) return
    sendMessage(inputValue.trim(), mode)
    setInputValue('')
  }

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSubmit()
    }
  }

  // 检测流式消息位置
  const streamingIndex = chatMessages.length > 0
    ? [...chatMessages].reverse().findIndex((m) => m.role === 'assistant' && m.isStreaming)
    : -1
  const hasStreamingMsg = streamingIndex >= 0
  const effectiveStreamingIndex = chatMessages.length - 1 - streamingIndex

  // 空状态
  if (!activeSessionId || chatMessages.length === 0) {
    return (
      <div className="chat-area">
        <div className="messages-container" ref={scrollRef}>
          <div className="empty-state">
            <div className="empty-state-icon">💬</div>
            <div className="empty-state-text">开始一段对话</div>
            <div className="empty-state-sub">选择对话模式，发送消息开始</div>
          </div>
        </div>
        <ModelSelector mode={mode} onChange={setMode} />
        <InputArea
          value={inputValue}
          onChange={setInputValue}
          onSubmit={handleSubmit}
          onKeyDown={handleKeyDown}
          disabled={isSending}
          placeholder={getPlaceholder(mode)}
        />
      </div>
    )
  }

  return (
    <div className="chat-area">
      <div className="messages-container" ref={scrollRef}>
        <div className="messages-list">
          {chatMessages.map((msg, i) => (
            <MessageBubble
              key={`${msg.id || i}-${msg.createdAt}`}
              message={msg}
              isStreaming={hasStreamingMsg && i === effectiveStreamingIndex}
              streamingText={i === effectiveStreamingIndex ? streamingMsg : undefined}
            />
          ))}
        </div>
      </div>
      <ModelSelector mode={mode} onChange={setMode} />
      <InputArea
        value={inputValue}
        onChange={setInputValue}
        onSubmit={handleSubmit}
        onKeyDown={handleKeyDown}
        disabled={isSending}
        placeholder={getPlaceholder(mode)}
      />
    </div>
  )
}

function getPlaceholder(mode: ChatMode): string {
  const placeholders: Record<ChatMode, string> = {
    sync: '输入消息...',
    stream: '输入消息，体验流式输出...',
    rag: '基于知识库提问...',
    ragStructured: '提问以获取带引用来源的回答...',
    agent: '输入问题，智能体将自动调用工具...',
  }
  return placeholders[mode]
}
