/// <reference types="vite/client" />
/// <reference types="unplugin-icons/types/vue" />

export {}

declare module 'vue' {
  interface GlobalComponents {
    AttachmentSelectorModal: (typeof import('vue'))['DefineComponent']
  }
}
