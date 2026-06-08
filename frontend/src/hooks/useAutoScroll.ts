import { useEffect, useRef } from 'react'

/**
 * 自动滚动 Hook
 *
 * 将 ref 引用的元素滚动到底部，当有新消息时触发。
 */

export function useAutoScroll(dependency: unknown) {
  const containerRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const container = containerRef.current
    if (!container) return
    container.scrollTop = container.scrollHeight
  }, [dependency])

  return containerRef
}
