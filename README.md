<div align="center">
    <img alt="logo" width="106px" src="./src/main/resources/logo.png" style="border-radius: 16px;">
    <h1>Halo - Wechat Share（自定义微信分享卡片）</h1>
    <p>为你的网址在微信中“穿上衣服”</p>
    <p align="center">
        <a href="https://github.com/Avrinbai/halo-plugin-wechat-share/releases"><img alt="GitHub Release" src="https://img.shields.io/github/v/release/Avrinbai/halo-plugin-wechat-share?style=flat-square&logo=github" /></a>
        <a href="https://www.gnu.org/licenses/gpl-3.0.html"><img alt="License GPL-3.0" src="https://img.shields.io/badge/License-GPL%20v3-blue.svg?style=flat-square" /></a>
        <img alt="Halo" src="https://img.shields.io/badge/Halo-%3E%3D2.23.0-0A81F5?style=flat-square&logo=halo&logoColor=white" />
    </p>

</div>

## 概览

`Wechat Share` 是一款面向 **Halo 2.23+** 的微信自定义分享卡片插件：

- **控制台**：在「系统 → 工具」下维护分享卡片（标题、摘要、封面、跳转链接），集中配置公众号凭据与公开路径等。
- **站点前台**：按配置的公开路径前缀提供 **`/share`**（微信内分享预览页 + JSSDK 注入）与 **`/go`**（带 `sid` 的 302 跳转）能力；

## 目录

| 章节 | 说明 |
|------|------|
| [功能亮点](#功能亮点) | 能力列表 |
| [预览图](#预览图) | 界面截图（可选） |
| [快速开始](#快速开始) | 安装、配置、使用 |
| [公开 URL 说明](#公开-url-说明) | 分享页、跳转、路径前缀 |
| [本地开发](#本地开发) | 构建、调试 |
| [许可证](#许可证) | 协议 |

## 功能亮点

- 分享卡片 CRUD：标题/摘要/封面/跳转 URL，独立 **SID** 与扩展存储
- 列表内复制分享链接、编辑、删除；支持二维码预览
- 插件设置：公众号 **AppId / AppSecret**（服务端换票与 `wx.config` 签名）、**公开路径前缀**（默认 `/wechat-share`）、**二维码生成服务** 等
- 分享页 HTML 在微信内置浏览器中注入 **jweixin**，更新朋友圈 / 会话分享数据

## 预览图

<div align="center">

<img src="./images/admin-cards.png" alt="控制台-卡片管理" title="卡片管理" width="45%" />
<img src="./images/admin-settings.png" alt="控制台-插件配置" title="插件配置" width="45%" />

</div>

## 快速开始

### 1) 安装插件

在 Halo 应用市场安装，或在本仓库 [Releases](https://github.com/Avrinbai/halo-plugin-wechat-share/releases) 下载构建产物后 **手动上传** 安装。

### 2) 打开控制台

进入 Halo 控制台：**系统 → 工具 → 自定义微信分享卡片**。

### 3) 插件配置（右上角「插件配置」）

按需填写：

| 配置项 | 说明 |
|--------|------|
| 公众号 AppId / AppSecret | 用于服务端换取 `access_token`、`jsapi_ticket`，并为分享页生成 `wx.config` 签名 |
| 公开路径前缀 | 默认 `/wechat-share`；保存后与站点根拼接生成对外路径 |
| 二维码上游接口 | 控制台列表中二维码预览所用 HTTP 接口（需与你的网关约定一致） |

分享链接、跳转地址中的 **站点根 URL** 使用 Halo 全局 **外部访问地址**，无需在插件内单独填写「站点域名」。

### 4) 新建卡片

填写标题、摘要、封面图 URL、跳转链接（`http/https`）。保存后可复制分享链接或预览二维码。

### 5) 在微信中使用

将 **`{外部访问地址}{公开路径前缀}/share?sid=…`** 在微信内打开，按页面提示从右上角菜单发起分享；点击跳转类链接时使用 **`…/go?sid=…`**。

---

## 公开 URL 说明

假定 Halo 外部访问地址为 `https://example.com`，公开路径前缀为 `/wechat-share`（默认）：

| 路径 | 作用 |
|------|------|
| `https://example.com/wechat-share/share?sid={sid}` | 微信内分享落地页，注入 JSSDK 分享参数 |
| `https://example.com/wechat-share/go?sid={sid}` | 302 跳转到卡片配置的落地 URL |

修改「公开路径前缀」并保存后，对外路径随之变化（别忘了更新已发出的推广链接）。

---

## 本地开发

环境要求：**JDK 21**、**Node 20+**（推荐使用 **pnpm** 构建控制台）。

```bash
# 构建控制台前端（产物由 Gradle 拷贝进插件 resources）
cd ui
pnpm install
pnpm run build
cd ..

# 编译并打包插件 JAR
./gradlew.bat clean build -x test
# Linux / macOS: ./gradlew clean build -x test
```

使用 Halo 官方 **插件开发** 工具链时，可在项目根目录执行 `./gradlew.bat haloServer`（或文档推荐命令）启动带插件装载的开发环境；详见 [Halo 插件开发文档](https://docs.halo.run/developer-guide/plugin/introduction)。

---

## 许可证

本项目采用 [**GNU General Public License v3.0**](https://www.gnu.org/licenses/gpl-3.0.html)（与 `src/main/resources/plugin.yaml` 声明一致）。
