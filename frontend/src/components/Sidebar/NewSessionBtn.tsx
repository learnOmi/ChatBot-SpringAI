import { useSessionStore } from '../../stores/sessionStore'

/**
 * 新建会话按钮
 */
export default function NewSessionBtn() {
  const createNewSession = useSessionStore((s) => s.createNewSession)

  const handleClick = async () => {
    await createNewSession('新对话')
  }

  return (
    <button className="new-chat-btn" onClick={handleClick}>
      + 新对话
    </button>
  )
}
