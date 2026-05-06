/// <reference types="vite/client" />
/// <reference types="unplugin-icons/types/vue" />

export {}

interface ImportMetaEnv {
  /** vite.config 构建时注入，每次构建变化 */
  readonly VITE_UI_BUILD_VERSION: string
}

declare module 'vue' {
  interface GlobalComponents {
    AttachmentSelectorModal: (typeof import('vue'))['DefineComponent']
  }
}
