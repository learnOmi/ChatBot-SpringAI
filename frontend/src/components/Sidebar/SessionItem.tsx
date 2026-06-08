import { useSessionStore } from '../../stores/sessionStore'
import type { Session } from '../../api/types'

/**
 * 单个会话项
 *
 * 显示会话标题和时间，支持点击选中、hover 显示删除按钮。
 */
interface SessionItemProps {
  session: Session
}

function formatTime(dateStr: string): string {
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)

  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes} 分钟前`
  if (hours < 24) return `${hours} 小时前`
  return date.toLocaleDateString()
}

export default function SessionItem({ session }: SessionItemProps) {
  const { activeSessionId, selectSession, removeSession } = useSessionStore()
  const isActive = session.id === activeSessionId

  const handleDelete = async (e: React.MouseEvent) => {
    e.stopPropagation()
    if (confirm(`确定删除「${session.title}」吗？`)) {
      await removeSession(session.id)
    }
  }

  return (
    <div
      className={`session-item ${isActive ? 'active' : ''}`}
      onClick={() => selectSession(session.id)}
    >
      <span className="session-item-title">{session.title}</span>
      <span className="session-item-time">{formatTime(session.createdAt)}</span>
      {!isActive && (
        <button className="delete-btn" onClick={handleDelete} title="删除">
          ×
        </button>
      )}
    </div>
  )
}
