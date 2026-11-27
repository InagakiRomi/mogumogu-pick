import { reactive, toRefs } from 'vue'

export type FeedbackType = 'error' | 'success'

const feedbackState = reactive({
  open: false,
  message: '',
  type: 'error' as FeedbackType,
})

let onCloseCallback: (() => void) | null = null

export function useFeedbackDialog() {
  function showFeedback(message: string, type: FeedbackType = 'error', onClose?: () => void) {
    onCloseCallback = onClose ?? null
    feedbackState.message = message
    feedbackState.type = type
    feedbackState.open = true
  }

  function clearFeedback() {
    onCloseCallback = null
    feedbackState.open = false
    feedbackState.message = ''
  }

  function onOpenChange(open: boolean) {
    feedbackState.open = open
    if (!open) {
      feedbackState.message = ''
      const callback = onCloseCallback
      onCloseCallback = null
      callback?.()
    }
  }

  return {
    ...toRefs(feedbackState),
    showFeedback,
    clearFeedback,
    onOpenChange,
  }
}
