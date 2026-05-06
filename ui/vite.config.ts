import fs from 'node:fs'
import path from 'node:path'
import { execSync } from 'node:child_process'
import { fileURLToPath, URL } from 'node:url'

import { viteConfig } from '@halo-dev/ui-plugin-bundler-kit'
import Icons from 'unplugin-icons/vite'
import type { Plugin } from 'vite'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const pluginRoot = path.resolve(__dirname, '..')

function readGradleVersion(root: string): string {
  try {
    const text = fs.readFileSync(path.join(root, 'gradle.properties'), 'utf8')
    const m = text.match(/^\s*version\s*=\s*([^\s#]+)/m)
    return (m?.[1] || '0.0.0').trim()
  } catch {
    return '0.0.0'
  }
}

function gitShort(): string {
  try {
    return execSync('git rev-parse --short HEAD', { cwd: pluginRoot, encoding: 'utf8' }).trim()
  } catch {
    return 'nogit'
  }
}

const UI_BUILD_VERSION = `${readGradleVersion(pluginRoot)}-${gitShort()}-${Date.now()}`

function uiBuildStampPlugin(version: string): Plugin {
  const banner = `/*! wechat-share-ui ${version} */`
  return {
    name: 'wechat-share-ui-build-stamp',
    /** lib + IIFE 会在 Rollup 输出后再包一层，故在落盘后写入版本注释，确保文件内容与 ETag 随构建变化 */
    writeBundle(options) {
      const dir = options.dir
      if (!dir) return
      try {
        const mainPath = path.join(dir, 'main.js')
        if (fs.existsSync(mainPath)) {
          const code = fs.readFileSync(mainPath, 'utf8')
          if (!code.startsWith('/*! wechat-share-ui')) {
            fs.writeFileSync(mainPath, `${banner}\n${code}`, 'utf8')
          }
        }
        const stylePath = path.join(dir, 'style.css')
        if (fs.existsSync(stylePath)) {
          const css = fs.readFileSync(stylePath, 'utf8')
          if (!css.startsWith('/*! wechat-share-ui')) {
            fs.writeFileSync(stylePath, `${banner}\n${css}`, 'utf8')
          }
        }
        fs.writeFileSync(
          path.join(dir, 'ui-build.json'),
          `${JSON.stringify({ version, builtAt: new Date().toISOString() })}\n`,
          'utf8',
        )
      } catch {
        // ignore
      }
    },
  }
}

export default viteConfig({
  vite: {
    define: {
      'import.meta.env.VITE_UI_BUILD_VERSION': JSON.stringify(UI_BUILD_VERSION),
    },
    plugins: [Icons({ compiler: 'vue3' }), uiBuildStampPlugin(UI_BUILD_VERSION)],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },
  },
})
