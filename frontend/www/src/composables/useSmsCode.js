import { ref, onBeforeUnmount } from 'vue'
import { api } from '../api/http.js'

/** 发送短信验证码 + 60 秒倒计时。开发环境（模拟短信）会返回验证码，用于演示提示。 */
export function useSmsCode(scene) {
  const countdown = ref(0)
  const sending = ref(false)
  const mockCode = ref('')
  let timer = null

  async function send(phone) {
    if (countdown.value > 0 || sending.value) return
    sending.value = true
    try {
      const r = await api.sendSms(phone, scene)
      mockCode.value = r.mockCode || ''
      countdown.value = r.interval || 60
      timer = setInterval(() => {
        countdown.value -= 1
        if (countdown.value <= 0) clearInterval(timer)
      }, 1000)
    } finally {
      sending.value = false
    }
  }

  onBeforeUnmount(() => clearInterval(timer))
  return { countdown, sending, mockCode, send }
}
