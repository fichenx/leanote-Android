# Leanote Next

一个用 **Kotlin + Jetpack Compose + Material 3** 从零重写的 [蚂蚁笔记 / Leanote](https://github.com/leanote/leanote) Android 客户端。

原官方客户端 [leanote-android](https://github.com/leanote/leanote-android) 已多年未更新，界面停留在 Material Design 早期，功能也落后于桌面版。本项目参考其协议实现与交互逻辑，重新设计了界面与架构，目标是做一个现代、顺滑、可离线使用的第三方客户端。

> 本项目是社区第三方客户端，与 Leanote 官方无关。数据通过 Leanote 的开放 REST API 与你的服务器同步，支持官方托管服务与自建服务器。

## 功能

- **账户**：邮箱登录、自建服务器地址、多账户保存与一键切换
- **笔记浏览**：笔记本列表、标签筛选、标题/全文搜索、回收站、多种排序
- **阅读**：Markdown 渲染（表格、任务列表、删除线、自动链接）、富文本 HTML 渲染、图片自适应
- **编辑**：Markdown 编辑与实时预览、富文本所见即所得编辑、格式工具栏、标题/标签/笔记本/博客设置
- **图片**：从相册插入图片，自动上传并替换为服务器地址，本地缓存优先加载
- **同步**：基于 USN 的增量双向同步，手动同步 + 6 小时后台定时同步；冲突时本地版本自动另存为「冲突副本」
- **离线**：Room 本地缓存，无网络时仍可阅读与编辑，联网后自动补传
- **外观**：Material 3 设计、深色模式、可选动态取色（Android 12+）
- **系统集成**：接收其他应用的文本分享、桌面「最近笔记」小组件、分享笔记文本/博客链接

## 技术栈

| 层 | 选型 |
| --- | --- |
| UI | Jetpack Compose、Material 3、Navigation Compose、Edge-to-Edge |
| 架构 | MVVM + Repository，单 Activity |
| 依赖注入 | Hilt（KSP） |
| 本地存储 | Room、DataStore Preferences |
| 网络 | Retrofit + OkHttp + kotlinx.serialization |
| 图片 | Coil |
| Markdown | Markwon |
| 后台任务 | WorkManager |
| 桌面小组件 | Glance |

## 构建

环境要求：JDK 17、Android SDK 35。

```bash
# 1. 配置 SDK 路径（或在 Android Studio 中打开项目）
echo "sdk.dir=/path/to/Android/Sdk" > local.properties

# 2. 构建 Debug 包
./gradlew assembleDebug

# 产物：app/build/outputs/apk/debug/app-debug.apk
```

也可以直接用 Android Studio（Ladybug 及以上）打开本目录运行。

## 连接自建服务器

登录页展开「服务器地址」，填入你的 Leanote 实例地址即可，例如：

```
https://notes.example.com
```

地址会自动补全为 HTTPS 并去掉多余的 `/api` 后缀。若使用自签名证书，请自行配置网络安全策略。

## 项目结构

```
app/src/main/java/com/leanote/next
├── data
│   ├── local        # Room 实体与 DAO
│   ├── model        # 账户、设置、筛选条件
│   ├── prefs        # 账户与设置的 DataStore
│   ├── remote       # Leanote REST API 与 DTO
│   ├── repo         # 账户、笔记本、笔记、图片仓库
│   └── sync         # 增量同步引擎
├── di               # Hilt 模块
├── sync             # WorkManager 定时同步
├── ui
│   ├── home         # 主界面、抽屉、笔记卡片
│   ├── detail       # 阅读页
│   ├── editor       # Markdown / 富文本编辑器
│   ├── login        # 登录与账户切换
│   ├── settings     # 设置
│   ├── share        # 接收系统分享
│   ├── web          # Markdown / HTML 渲染
│   └── theme        # Material 3 主题
├── util             # 时间、HTML、图片引用工具
└── widget           # Glance 桌面小组件
```

## 已知限制

- 暂未实现附件（非图片文件）的上传与管理
- 笔记本以平铺列表展示，暂不支持拖拽调整层级
- 富文本编辑基于 WebView 的 `contenteditable`，复杂排版与原桌面版可能略有差异
- 仅支持 Leanote 官方 API，未适配第三方魔改服务端

## 许可证

[GPL-3.0](LICENSE)。本项目参考了 [leanote-android](https://github.com/leanote/leanote-android)（GPL-3.0）的协议实现与交互设计，因此沿用同一许可证。

Leanote 本身由 leanote 团队开发，本项目仅为其 Android 客户端的一个独立实现。
