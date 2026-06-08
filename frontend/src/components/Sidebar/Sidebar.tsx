import { useState, useEffect } from 'react'
import { useUserStore } from '../../stores/userStore'
import { useSessionStore } from '../../stores/sessionStore'
import SessionList from './SessionList'
import NewSessionBtn from './NewSessionBtn'

/**
 * 侧边栏
 *
 * 包含用户 ID 输入、新建会话按钮和会话列表。
 */
export default function Sidebar() {
  const { userId, setUserId } = useUserStore()
  const { sessions, loading, loadSessions } = useSessionStore()
  const [inputValue, setInputValue] = useState(userId)

  // 同步 store 中的 userId
  useEffect(() => {
    setInputValue(userId)
  }, [userId])

  // 用户 ID 变化时触发加载会话
  useEffect(() => {
    if (userId) {
      loadSessions()
    }
  }, [userId, loadSessions])

  const handleSaveUserId = () => {
    setUserId(inputValue.trim())
  }

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      handleSaveUserId()
    }
  }

  return (
    <aside className="sidebar">
      <div className="sidebar-header">
        <h1>SpringAIRobot</h1>
        <input
          type="text"
          className="user-id-input"
          placeholder="输入用户 ID..."
          value={inputValue}
          onChange={(e) => setInputValue(e.target.value)}
          onKeyDown={handleKeyDown}
          onBlur={handleSaveUserId}
        />
        <NewSessionBtn />
      </div>

      <SessionList
        sessions={sessions}
        loading={loading}
      />
    </aside>
  )
}
