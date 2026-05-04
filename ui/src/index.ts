import { definePlugin } from '@halo-dev/ui-shared'
import { markRaw } from 'vue'
import RiShareForwardLine from '~icons/ri/share-forward-line'

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
            component: () => import(/* webpackChunkName: "WechatShareCardsView" */ './views/WechatShareCardsView.vue'),
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
