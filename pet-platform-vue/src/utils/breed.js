import { ref } from 'vue'
import { getBreeds } from '../api/pet'

// 品种字典全局缓存：首页筛选、发布页下拉、列表展示共用，只请求一次
const options = ref([])
let pending = null

export function useBreeds() {
  if (!pending) {
    pending = getBreeds()
      .then((list) => {
        options.value = Array.isArray(list) ? list : []
      })
      .catch(() => {
        // 失败后允许下次重新拉取
        pending = null
      })
  }
  return { options, pending }
}

export function breedName(code) {
  const hit = options.value.find((b) => b.code === code)
  return hit ? hit.name : ''
}
