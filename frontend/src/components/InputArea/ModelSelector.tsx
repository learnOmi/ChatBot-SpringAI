/**
 * 模型选择器（Tab 栏）
 *
 * 选择对话模式：同步 / 流式 / RAG / RAG 结构化 / Agent
 */
import type { ChatMode } from '../../api/types'

const MODES: { key: ChatMode; label: string }[] = [
  { key: 'sync', label: '同步' },
  { key: 'stream', label: '流式' },
  { key: 'rag', label: 'RAG' },
  { key: 'ragStructured', label: 'RAG 结构化' },
  { key: 'agent', label: 'Agent' },
]

interface ModelSelectorProps {
  mode: ChatMode
  onChange: (mode: ChatMode) => void
}

export default function ModelSelector({ mode, onChange }: ModelSelectorProps) {
  return (
    <div className="model-selector">
      {MODES.map(({ key, label }) => (
        <button
          key={key}
          className={`model-tab ${mode === key ? 'active' : ''}`}
          onClick={() => onChange(key)}
        >
          {label}
        </button>
      ))}
    </div>
  )
}
