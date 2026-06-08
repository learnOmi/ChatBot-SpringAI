import Sidebar from '../Sidebar/Sidebar'
import ChatArea from '../ChatArea/ChatArea'
import './Layout.css'

/**
 * 应用布局 — 侧边栏 + 主内容区
 */
export default function Layout() {
  return (
    <div className="app-layout">
      <Sidebar />
      <main className="main-area">
        <ChatArea />
      </main>
    </div>
  )
}
