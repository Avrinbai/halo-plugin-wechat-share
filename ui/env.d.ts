/// <reference types="vite/client" />
/// <reference types="unplugin-icons/types/vue" />

export {}

interface ImportMetaEnv {
  readonly VITE_UI_BUILD_VERSION: string
}

declare module 'vue' {
  interface GlobalComponents {
    AttachmentSelectorModal: (typeof import('vue'))['DefineComponent']
    SearchInput: (typeof import('vue'))['DefineComponent']
    FilterDropdown: (typeof import('vue'))['DefineComponent']
    FilterCleanButton: (typeof import('vue'))['DefineComponent']
  }
}
