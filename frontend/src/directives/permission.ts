import type { Directive } from 'vue'
import { useAuthStore } from '../stores/auth'

const apply = (el: HTMLElement, required: string | string[]) => {
  const authStore = useAuthStore()
  const ok = authStore.hasPermission(required)
  el.style.display = ok ? '' : 'none'
}

export const permissionDirective: Directive<HTMLElement, string | string[]> = {
  mounted(el, binding) {
    apply(el, binding.value)
  },
  updated(el, binding) {
    apply(el, binding.value)
  },
}

