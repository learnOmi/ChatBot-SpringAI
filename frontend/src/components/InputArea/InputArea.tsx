import { useRef, useEffect } from 'react'

/**
 * 输入区域
 *
 * 可自动扩展的 textarea + 发送按钮。
 * Enter 发送，Shift+Enter 换行。
 */
interface InputAreaProps {
  value: string
  onChange: (value: string) => void
  onSubmit: () => void
  onKeyDown?: (e: React.KeyboardEvent<HTMLTextAreaElement>) => void
  disabled?: boolean
  placeholder?: string
}

export default function InputArea({
  value,
  onChange,
  onSubmit,
  onKeyDown,
  disabled = false,
  placeholder = '输入消息...',
}: InputAreaProps) {
  const textareaRef = useRef<HTMLTextAreaElement>(null)

  // 自动调整 textarea 高度
  useEffect(() => {
    const textarea = textareaRef.current
    if (!textarea) return
    textarea.style.height = 'auto'
    textarea.style.height = Math.min(textarea.scrollHeight, 200) + 'px'
  }, [value])

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (onKeyDown) {
      onKeyDown(e)
    }
  }

  return (
    <div className="input-area">
      <div className="input-wrapper">
        <textarea
          ref={textareaRef}
          className="input-textarea"
          placeholder={placeholder}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          onKeyDown={handleKeyDown}
          disabled={disabled}
          rows={1}
        />
        <button
          className="send-btn"
          onClick={onSubmit}
          disabled={disabled || !value.trim()}
          title="发送"
        >
          ➤
        </button>
      </div>
    </div>
  )
}
