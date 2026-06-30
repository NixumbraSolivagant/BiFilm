# BiFilm

> 把胶片的"多重曝光"装进 Android 手机。
> 所见即所得 — 在按下快门之前，先在屏幕上看到 2~9 张叠加之后的最终效果。

[![Android](https://img.shields.io/badge/Android-26%2B-3DDC84?logo=android)](#)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9-7F52FF?logo=kotlin)](#)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-2024.02-4285F4?logo=jetpackcompose)](#)
[![AGSL](https://img.shields.io/badge/AGSL-API%2033%2B-34A853)](#)
[![License](https://img.shields.io/badge/license-MIT-lightgrey)](#license)

---

## 一、这是什么

**BiFilm**（Bichromatic Film 的缩写）是一款**胶片多重曝光**相机 App。
对真实胶片来说，多次曝光意味着把同一张底片曝光多次 — 每一次曝光都和前面的结果叠加。
在暗房之前你**永远不知道最终长什么样**，靠经验和运气。

BiFilm 用 GPU 把这个过程完全搬到屏幕上：
- 用 **CameraX 实时预览**作为底图
- 把已拍过的图层**实时合成**到预览流上
- 按下快门之前，就能在取景框里看到 4 张叠好之后的样子

> 核心价值：**不浪费胶片，不浪费灵感**。

---

## 二、特性一览

### 拍摄
- 🔭 实时取景 + **所见即所得**的多重曝光合成预览
- 📷 CameraX `Preview` + `ImageAnalysis` + `ImageCapture` 三路并行
- 🎯 焦段切换（0.5× / 1× / 2× 数码变焦）
- ⚙️ 曝光补偿 `-3EV ~ +3EV`，0.3 EV 步进
- 🎬 张数规划 2 / 3 / 4 / 5 / 6 / 7 / 8 / 9 张
- 🧱 状态机 `Idle / LivePreview / Capturing / Error`，避免连拍错位

### 合成
- 🎨 **6 种混合模式**，全部走 GPU：
  - `SCREEN` 叠加（默认，最贴近胶片负片多次曝光）
  - `ADDITIVE` 加法
  - `MULTIPLY` 正片叠底
  - `LIGHTEN` 变亮
  - `DARKEN` 变暗
  - `AVERAGE` 平均
- 🖌 **软笔刷蒙版**：高斯羽化 `SoftMaskBrush` + `ALPHA_8` 蒙版缓存 + Compose `Canvas`
- 🔁 渲染引擎 `BlendComposer` 走 **ping-pong FBO** 流水线，结果可逐张拆解
- 📜 **场景预设**（`ScenePreset`）：内置经典多曝场景参数，胶片感一键到位

### 后期 / 输出
- 🗂 项目化管理：每个胶卷项目独立保存，列表首页直接挑
- 📥 从相册导入已有照片作为图层（`ImportNegativeFrameUseCase`）
- 📤 一键导出到系统相册（`MediaStore` + `ExportProjectUseCase`）
- ↗ 分享 Intent
- 🎞 黑白 / 彩色切换
- 🧪 单元测试覆盖 `BlendMode`、`BlendModeRegistry`、`ExposureApplier`

### 系统 / 体验
- 🎨 暗色为主 + 暖色胶片调色板（`ink_primary` / `film_warm` / `film_glow` / `paper`）
- 🧩 Material 3 + Jetpack Compose 全栈 UI
- 🏷 自适应图标（adaptive icon，含 monochrome 主题图标，Android 13+ 跟随系统色）
- 🌐 完整中文化文案
- ⚙️ 设置页：默认混合模式、默认张数等

---

## 三、渲染管线

```
                ┌──────────┐    ┌──────────┐    ┌──────────────┐
  bitmaps  ──▶  │ BlendHost │ ─▶│  shader  │ ──▶ │ output bitmap │
  (front/back)  │  (工厂)   │    │ AGSL/GLSL│    │  (ARGB_8888)  │
                └──────────┘    └──────────┘    └──────────────┘
                        ▲
                        │ ping-pong FBO
                        ▼
                  ┌──────────────────┐
                  │ BlendComposer    │
                  │ (engine/composer)│
                  └──────────────────┘
```

`BlendHostFactory.create(context)` 根据运行设备**自动选**：

| 设备 API | 渲染后端 | 着色器 | 性能 |
| --- | --- | --- | --- |
| **≥ 33** | `AgslBlendHost` | AGSL `RuntimeShader` | 完整 6 种模式，零拷贝 |
| **26 ~ 32** | `GlEsBlendHost` | GLSL ES 2.0 (`blend_frag`) | 同 6 种模式，GLSurfaceView |
| **fallback** | `SoftwareBlendHost` | CPU | 仅 `SCREEN`，降级保证可用 |

蒙版模块独立：

```
   笔触 ──▶ SoftMaskBrush (高斯核) ──▶ ALPHA_8 Bitmap ──▶ SoftMaskCanvas
                                                            ▲
                                                            │ Compose Pointer
                                                            ▼
                                                     MaskRenderer
```

---

## 四、技术栈

| 类别 | 选型 |
| --- | --- |
| 语言 | Kotlin 1.9 |
| UI | Jetpack Compose + Material 3 |
| 架构 | MVVM + UseCase + Repository |
| DI | 手写 `AppContainer`（轻量、无 Hilt 依赖） |
| 异步 | Kotlin Coroutines + Flow |
| 数据库 | Room (KSP) |
| 偏好 | DataStore Preferences |
| 相机 | CameraX (core / camera2 / lifecycle / view) |
| 渲染 | AGSL `RuntimeShader` / OpenGL ES 2.0 |
| 图像 | `Bitmap` + `RenderScript`-free 高斯（自定义） |
| 构建 | Gradle 8.4 + Version Catalog (`libs.versions.toml`) |
| 最低 SDK | 26 (Android 8.0) |
| 目标 SDK | 34 (Android 14) |
| JDK | 17 |

---

## 五、目录结构

```
BiFilm/
├── app/
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/bifilm/app/
│       │   ├── BiFilmApp.kt              # Application 入口
│       │   ├── MainActivity.kt           # 单 Activity 宿主
│       │   ├── di/                       # 简易 DI 容器
│       │   ├── data/
│       │   │   ├── db/                   # Room (Entities/Daos/Database)
│       │   │   ├── image/                # ImageStore
│       │   │   └── prefs/                # DataStore 设置
│       │   ├── domain/
│       │   │   ├── model/                # Project / BlendMode / FilmStock
│       │   │   │                         # ExposureStops / ScenePreset
│       │   │   └── usecase/              # AddLayer / ComposeLayers /
│       │   │                             # RemoveLayer / ReorderLayers /
│       │   │                             # ExportProject / ImportNegativeFrame
│       │   ├── render/
│       │   │   ├── camera/               # CameraX 桥接
│       │   │   ├── compose/              # BlendHost (AGSL/GLES/Software)
│       │   │   ├── engine/               # BlendComposer / Registry / Exposure
│       │   │   └── mask/                 # SoftMaskBrush / MaskRenderer
│       │   ├── ui/
│       │   │   ├── theme/                # Color / Type / Theme
│       │   │   ├── common/               # UiStates 公共状态
│       │   │   ├── home/                 # 项目列表
│       │   │   ├── capture/              # 取景 + 拍摄
│       │   │   ├── compose/              # 后期合成
│       │   │   ├── export/               # 导出
│       │   │   └── settings/             # 设置
│       │   ├── navigation/               # BiFilmNavHost + Routes
│       │   └── util/                     # Dispatcher / Logger / Result
│       └── res/
│           ├── drawable/                 # 启动图标前景/背景矢量
│           ├── mipmap-anydpi-v26/        # 自适应图标
│           ├── raw/                      # blend.agsl / blend_frag / blend_vert
│           ├── values/                   # 颜色 / 字符串 / 主题
│           └── xml/                      # 备份规则
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/libs.versions.toml             # 版本目录
└── gradle/wrapper/                       # Gradle Wrapper
```

---

## 六、编译与运行

### 6.1 环境要求
- Android Studio Hedgehog (2023.1) 或更新
- JDK 17
- Android SDK Platform 34 + Build-Tools 34
- 一台运行 **Android 8.0 (API 26) 及以上**的真机（相机功能无法在模拟器完整运行）

### 6.2 克隆
```bash
git clone https://github.com/NixumbraSolivagant/BiFilm.git
cd BiFilm
```

### 6.3 配置
- 在项目根的 `local.properties` 里指定 `sdk.dir`（Android Studio 首次同步会自动写入）
- 若使用代理 / 镜像：在 `~/.gradle/init.gradle` 里覆盖仓库地址

### 6.4 构建 Debug APK
```bash
./gradlew :app:assembleDebug
```
产物：`app/build/outputs/apk/debug/app-debug.apk`（约 17 MB）

### 6.5 安装到设备
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.bifilm.app.debug/com.bifilm.app.MainActivity
```

### 6.6 跑单元测试
```bash
./gradlew :app:testDebugUnitTest
```

---

## 七、使用流程

1. **首页** → 点击右下角 **+** 新建项目（自动跳到取景页）
2. **取景** → 选焦段 / 调曝光 / 调张数（2~9） → 看实时叠加 → 按下快门
   - 每拍一张，画面里已经能看到当前 **N 张叠加**的效果
   - 拍满 `frameCount` 后自动结束，可去合成页继续
3. **合成** → 选混合模式 / 调曝光 / 用软笔刷擦蒙版 / 黑白切换 → 实时预览
4. **导出** → 一键保存到系统相册 `Pictures/BiFilm/`，或用分享 Intent 发出去
5. **首页** → 长按项目卡片删除；点卡片进合成；点卡片缩略图右上角相机按钮回取景

---

## 八、6 种混合模式

| 模式 | uniform | 中文 | 胶片语境 |
| --- | --- | --- | --- |
| `SCREEN` | 0 | 叠加 | **最贴近胶片负片多次曝光**（默认） |
| `ADDITIVE` | 1 | 加法 | 纯亮度累加；N 张时建议 `-log2(N)` stops 补偿 |
| `MULTIPLY` | 2 | 正片叠底 | 暗背景上的亮物 |
| `LIGHTEN` | 3 | 变亮 | 取较亮像素 |
| `DARKEN` | 4 | 变暗 | 取较暗像素 |
| `AVERAGE` | 5 | 平均 | 自动平衡，N 张时无需补偿 |

---

## 九、关键文件速查

| 用途 | 路径 |
| --- | --- |
| AGSL 着色器（API 33+） | `app/src/main/res/raw/blend.agsl` |
| OpenGL ES 兜底着色器 | `app/src/main/res/raw/blend_frag` / `blend_vert` |
| 渲染主机工厂 | `app/src/main/java/com/bifilm/app/render/compose/BlendHostFactory.kt` |
| AGSL 渲染主机 | `app/src/main/java/com/bifilm/app/render/compose/AgslBlendHostImpl.kt` |
| OpenGL ES 渲染主机 | `app/src/main/java/com/bifilm/app/render/compose/GlEsBlendHost.kt` |
| 合成引擎 | `app/src/main/java/com/bifilm/app/render/engine/BlendComposer.kt` |
| 混合模式注册表 | `app/src/main/java/com/bifilm/app/render/engine/BlendModeRegistry.kt` |
| 曝光档应用器 | `app/src/main/java/com/bifilm/app/render/engine/ExposureApplier.kt` |
| 相机桥接 | `app/src/main/java/com/bifilm/app/render/camera/CameraFrameBridge.kt` |
| 软笔刷蒙版 | `app/src/main/java/com/bifilm/app/render/mask/SoftMaskBrush.kt` |
| 取景页 | `app/src/main/java/com/bifilm/app/ui/capture/CaptureScreen.kt` |
| 合成页 | `app/src/main/java/com/bifilm/app/ui/compose/ComposeScreen.kt` |
| 场景预设 | `app/src/main/java/com/bifilm/app/domain/model/ScenePreset.kt` |
| 胶片规格 | `app/src/main/java/com/bifilm/app/domain/model/FilmStock.kt` |
| 导航 | `app/src/main/java/com/bifilm/app/navigation/BiFilmNavHost.kt` |

---

## 十、版本历史

### v0.1.0 (2026-06-30)
首次公开版本。

- ✅ M1 项目骨架 + 主题(胶片色板) + Navigation + Room + DataStore
- ✅ M2 `blend_agsl` AGSL shader + `AgslBlendHostImpl` (ping-pong) + `BlendComposer` + `ComposeLayersUseCase`
- ✅ M3 CameraX `CameraFrameBridge` + 取景页 + 权限
- ✅ M4 拍照累积、状态机、`FrameCountPicker`、`ExposurePicker`
- ✅ M5 `SoftMaskBrush` + `MaskRenderer` + `SoftMaskCanvas`
- ✅ M6 `GlEsBlendHost` (OpenGL ES 2.0) + `BlendHostFactory` SDK 路由
- ✅ M7 `ExportProjectUseCase` + 导出页 + 分享 Intent + `ImportNegativeFrameUseCase`
- ✅ M8 设置页 + 单元测试 + 本地化 + 自适应启动图标
- ✅ 后期合成：场景预设 / 焦段切换 / 黑白切换 / 软笔刷蒙版实时调整
- ✅ 项目卡片：缩略图右上角加回拍摄入口

---

## 十一、Roadmap（不在当前 release）

- 云同步 / 多设备
- 滤镜商店 / 第三方胶片 LUT
- 视频多重曝光
- 包围曝光 / HDR
- 内置社区分享
- iOS 移植（理论上 Compose Multiplatform 可行，但相机栈要重写）

---

## 十二、不在 BiFilm 的设计目标内

- 滤镜花哨的"美颜相机"
- 一键 AI 修图
- 社交属性

BiFilm 只做一件事 — **把胶片多重曝光的暗房惊喜提前到按下快门前**。

---

## License

MIT — 见 [LICENSE](LICENSE)。
