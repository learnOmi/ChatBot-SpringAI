import { create } from 'zustand'

interface UserState {
  userId: string
  setUserId: (id: string) => void
}

export const useUserStore = create<UserState>((set) => ({
  userId: localStorage.getItem('springairobot_userid') || '',
  setUserId: (id: string) => {
    localStorage.setItem('springairobot_userid', id)
    set({ userId: id })
  },
}))
