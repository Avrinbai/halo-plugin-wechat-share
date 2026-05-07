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
    /** Halo 控制台全局注册，与 SubmitList 等列表页一致 */
    SearchInput: (typeof import('vue'))['DefineComponent']
    FilterDropdown: (typeof import('vue'))['DefineComponent']
    FilterCleanButton: (typeof import('vue'))['DefineComponent']
  }
}
