import { useCallback, useRef } from 'react'

/**
 * 流式读取 Hook
 *
 * 用于读取后端 Flux<String> 产生的 ReadableStream。
 * 后端直接以 chunked transfer encoding 发送原始文本（非 SSE 格式）。
 */

interface UseStreamingReturn {
  stream: (
    stream: ReadableStream<Uint8Array>,
    onChunk: (text: string) => void,
    onComplete: (fullText: string) => void
  ) => Promise<void>
  stop: () => void
}

export function useStreaming(): UseStreamingReturn {
  const abortRef = useRef<AbortController | null>(null)

  const stream = useCallback(
    async (
      stream: ReadableStream<Uint8Array>,
      onChunk: (text: string) => void,
      onComplete: (fullText: string) => void
    ) => {
      // 取消之前的流
      abortRef.current?.abort()
      abortRef.current = new AbortController()
      const signal = abortRef.current.signal

      const reader = stream.getReader()
      if (!reader) return

      const decoder = new TextDecoder('utf-8')
      let fullText = ''

      try {
        while (true) {
          const { done, value } = await reader.read()
          if (done || signal.aborted) break
          const text = decoder.decode(value, { stream: true })
          fullText += text
          onChunk(text)
        }
      } finally {
        reader.releaseLock()
      }

      if (!signal.aborted) {
        onComplete(fullText)
      }
    },
    []
  )

  const stop = useCallback(() => {
    abortRef.current?.abort()
  }, [])

  return { stream, stop }
}
