import { definePlugin } from '@halo-dev/ui-shared'
import { markRaw } from 'vue'
import RiShareForwardLine from '~icons/ri/share-forward-line'

declare global {
  interface Window {
    __WECHAT_SHARE_UI_BUILD_VERSION__?: string
  }
}

if (typeof window !== 'undefined') {
  window.__WECHAT_SHARE_UI_BUILD_VERSION__ = import.meta.env.VITE_UI_BUILD_VERSION
}

export default definePlugin({
  components: {},
  routes: [
    {
      parentName: 'ToolsRoot',
      route: {
        path: '/wechat-share',
        name: 'WechatSharePluginRoot',
        component: () =>
          import(/* webpackChunkName: "WechatSharePluginOutlet" */ './views/WechatSharePluginOutlet.vue'),
        redirect: { name: 'WechatShareCards' },
        meta: {
          title: '自定义微信分享卡片',
          menu: {
            name: '自定义微信分享卡片',
            icon: markRaw(RiShareForwardLine),
            priority: 35,
          },
        },
        children: [
          {
            path: 'cards',
            name: 'WechatShareCards',
            component: () =>
              import(/* webpackChunkName: "WechatShareConsoleView" */ './views/WechatShareConsoleView.vue'),
            meta: {
              title: '自定义微信分享卡片',
              searchable: true,
            },
          },
        ],
      },
    },
  ],
  extensionPoints: {},
})
