import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getUnreadCount } from '../api/notification'
import { useUserStore } from './user'

// 未读通知数（顶部菜单红点），路由切换与通知页操作后调用 refresh 同步
export const useNotificationStore = defineStore('notification', () => {
  const unread = ref(0)

  async function refresh() {
    const userStore = useUserStore()
    if (!userStore.token) {
      unread.value = 0
      return
    }
    try {
      unread.value = (await getUnreadCount()) || 0
    } catch (e) {
      // 红点获取失败不打断主流程
    }
  }

  function reset() {
    unread.value = 0
  }

  return { unread, refresh, reset }
})
