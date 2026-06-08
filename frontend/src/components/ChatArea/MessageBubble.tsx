import ReactMarkdown from 'react-markdown'
import type { ChatMessage } from '../../api/types'

/**
 * 单条消息气泡
 *
 * 根据角色（user/assistant）显示不同样式。
 * 支持 Markdown 渲染、RAG 结构化来源展示。
 */
interface MessageBubbleProps {
  message: ChatMessage
  isStreaming: boolean
  streamingText?: string
}

export default function MessageBubble({ message, isStreaming, streamingText }: MessageBubbleProps) {
  const { role, content, ragAnswer } = message

  // 流式模式：使用 streamingText 而非 message.content
  const displayContent = isStreaming ? (streamingText || '') : content

  return (
    <div className={`message-bubble ${role}`}>
      <div className="message-avatar">
        {role === 'user' ? 'U' : 'AI'}
      </div>
      <div className="message-content">
        {role === 'assistant' ? (
          <>
            <ReactMarkdown
              components={{
                pre: ({ children }) => <pre>{children}</pre>,
                code: ({ className, children, ...props }) => {
                  const isInline = !className
                  if (isInline) {
                    return <code {...props}>{children}</code>
                  }
                  return (
                    <code className={className} {...props}>
                      {children}
                    </code>
                  )
                },
              }}
            >
              {displayContent}
            </ReactMarkdown>
            {isStreaming && (
              <span className="streaming-cursor" />
            )}
          </>
        ) : (
          <p>{displayContent}</p>
        )}

        {/* RAG 结构化回答：来源和置信度 */}
        {ragAnswer && ragAnswer.sources.length > 0 && (
          <details className="rag-sources">
            <summary>📚 引用来源 ({ragAnswer.sources.length}) · 置信度: {(ragAnswer.confidence * 100).toFixed(0)}%</summary>
            <ul style={{ marginTop: '8px', paddingLeft: '16px' }}>
              {ragAnswer.sources.map((source, i) => (
                <li key={i} style={{ marginBottom: '4px' }}>{source}</li>
              ))}
            </ul>
          </details>
        )}
      </div>
    </div>
  )
}
