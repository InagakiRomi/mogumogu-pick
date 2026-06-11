import { reactive, toRefs } from 'vue'

export type FeedbackType = 'error' | 'success'

const feedbackState = reactive({
  open: false,
  message: '',
  type: 'error' as FeedbackType,
})

export function useFeedbackDialog() {
  function showFeedback(message: string, type: FeedbackType = 'error') {
    feedbackState.message = message
    feedbackState.type = type
    feedbackState.open = true
  }

  function clearFeedback() {
    feedbackState.open = false
    feedbackState.message = ''
  }

  function onOpenChange(open: boolean) {
    feedbackState.open = open
    if (!open) {
      feedbackState.message = ''
    }
  }

  return {
    ...toRefs(feedbackState),
    showFeedback,
    clearFeedback,
    onOpenChange,
  }
}
