import Layout from './components/Layout/Layout'

/**
 * 根组件
 *
 * 组合 Layout 和 ChatArea。
 * 会话创建/选择由 ChatArea 内部通过 store 管理。
 */
export default function App() {
  return (
    <Layout />
  )
}
