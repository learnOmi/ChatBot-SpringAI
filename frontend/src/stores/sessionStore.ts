import { create } from 'zustand'
import {
  listUserSessions,
  createSession as apiCreateSession,
  deleteSession as apiDeleteSession,
} from '../api/chatApi'
import type { Session } from '../api/types'

interface SessionState {
  sessions: Session[]
  activeSessionId: string | null
  loading: boolean
  loadSessions: () => Promise<void>
  createNewSession: (title?: string) => Promise<string>
  selectSession: (id: string) => void
  removeSession: (id: string) => Promise<void>
  setActiveSessionId: (id: string | null) => void
}

export const useSessionStore = create<SessionState>((set) => ({
  sessions: [],
  activeSessionId: null,
  loading: false,

  loadSessions: async () => {
    const userId = localStorage.getItem('springairobot_userid')
    if (!userId) {
      set({ sessions: [], loading: false })
      return
    }
    set({ loading: true })
    try {
      const sessions = await listUserSessions(userId)
      set({ sessions, loading: false })
    } catch {
      set({ loading: false })
    }
  },

  createNewSession: async (title?: string) => {
    const userId = localStorage.getItem('springairobot_userid') || undefined
    const id = await apiCreateSession({ userId, title })
    const newSession: Session = {
      id,
      userId: userId || null,
      title: title || '新对话',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      metadata: null,
    }
    set((s) => ({
      sessions: [newSession, ...s.sessions],
      activeSessionId: id,
    }))
    return id
  },

  selectSession: (id: string) => {
    set({ activeSessionId: id })
  },

  removeSession: async (id: string) => {
    await apiDeleteSession(id)
    set((s) => ({
      sessions: s.sessions.filter((session) => session.id !== id),
      activeSessionId: s.activeSessionId === id ? null : s.activeSessionId,
    }))
  },

  setActiveSessionId: (id: string | null) => {
    set({ activeSessionId: id })
  },
}))
