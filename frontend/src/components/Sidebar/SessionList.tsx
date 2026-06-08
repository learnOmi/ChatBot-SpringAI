import SessionItem from './SessionItem'
import type { Session } from '../../api/types'

/**
 * 会话列表组件
 *
 * 显示当前用户的所有会话，支持点击选择和删除。
 */
interface SessionListProps {
  sessions: Session[]
  loading: boolean
}

export default function SessionList({ sessions, loading }: SessionListProps) {
  return (
    <div className="session-list-container">
      {loading ? (
        <div style={{ padding: '12px', textAlign: 'center', color: 'rgba(255,255,255,0.4)', fontSize: '13px' }}>
          加载中...
        </div>
      ) : sessions.length === 0 ? (
        <div style={{ padding: '12px', textAlign: 'center', color: 'rgba(255,255,255,0.3)', fontSize: '12px' }}>
          暂无会话
        </div>
      ) : (
        sessions.map((session) => (
          <SessionItem key={session.id} session={session} />
        ))
      )}
    </div>
  )
}
