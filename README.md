# BiFilm

胶片多重曝光 (Multiple Exposure) 安卓 App。所见即所得：在按胶片快门之前，
先在手机上看到 2~9 张叠加后的构图。

## 已实现

| 阶段 | 状态 | 内容 |
| --- | --- | --- |
| M1 | ✅ | 项目骨架 + 主题(胶片色板) + Navigation + Room + DataStore |
| M2 | ✅ | `blend_agsl` AGSL shader + `AgslBlendHostImpl` (ping-pong) + `BlendComposer` + `ComposeLayersUseCase` + `BlendModePicker` |
| M3 | ✅ | CameraX `CameraFrameBridge` (Preview + ImageAnalysis + ImageCapture) + `CaptureScreen` + 权限 |
| M4 | ✅ | 拍照累积、状态机 (`Idle/LivePreview/Capturing/Error`)、`FrameCountPicker`、`ExposurePicker` |
| M5 | ✅ | `SoftMaskBrush` (高斯羽化) + `MaskRenderer` (ALPHA_8) + `SoftMaskCanvas` (Compose) |
| M6 | ✅ | `GlEsBlendHost` (OpenGL ES 2.0 + GLSL ES `blend_frag`) + `BlendHostFactory` 按 SDK 路由 |
| M7 | ✅ | `ExportProjectUseCase` (MediaStore) + `ExportScreen` + 分享 Intent + `ImportNegativeFrameUseCase` |
| M8 | ✅ | SettingsScreen、单元测试 (`BlendMode`, `BlendModeRegistry`, `ExposureApplier`)、README、本地化 strings |

## 编译

依赖代理 (HTTP/SOCKS, 端口 2080) 已通过 `~/.gradle/init.gradle` 配置。

```bash
./gradlew :app:assembleDebug
```

如需离线模式：使用本机已缓存的 Gradle 8.4 (`gradle/wrapper/gradle-wrapper.properties` 中指定) 与 Maven 缓存.

## 渲染管线

```
                ┌──────────┐    ┌──────────┐    ┌──────────────┐
bitmaps   ───▶ │ BlendHost │ ─▶│  AGSL    │ ──▶ │ output bitmap │
(front/back)   │  (M2/M6)  │    │ shader   │    │  (ARGB_8888)  │
                └──────────┘    └──────────┘    └──────────────┘
                        ▲
                        │ ping-pong
                        ▼
                  ┌──────────────────┐
                  │ BlendComposer    │
                  │ (engine/composer)│
                  └──────────────────┘
```

`BlendHostFactory.create(context)` 自动选:
- **AGSL RuntimeShader** (API 33+) — 完整 6 种混合模式
- **OpenGL ES 2.0** (API 26-32) — 在 `GlEsBlendHost` 中用 GLSurfaceView + GLSL ES

## 6 种混合模式 (`BlendMode.kt`)

| mode | uniform | 中文 | 胶片语境 |
| --- | --- | --- | --- |
| SCREEN | 0 | 叠加 | **最贴近胶片负片多次曝光** (默认) |
| ADDITIVE | 1 | 加法 | 纯亮度累加; N 张时需 `-log2(N)` stops |
| MULTIPLY | 2 | 正片叠底 | 适合暗背景上的亮物 |
| LIGHTEN | 3 | 变亮 | 取较亮像素 |
| DARKEN | 4 | 变暗 | 取较暗像素 |
| AVERAGE | 5 | 平均 | 自动平衡; N 张时无需补偿 |

## 关键文件

- `app/src/main/res/raw/blend_agsl` — AGSL 核心 fragment shader
- `app/src/main/res/raw/blend_frag` / `blend_vert` — OpenGL ES fallback
- `app/src/main/java/com/bifilm/app/render/compose/AgslBlendHostImpl.kt`
- `app/src/main/java/com/bifilm/app/render/compose/GlEsBlendHost.kt`
- `app/src/main/java/com/bifilm/app/render/engine/BlendComposer.kt`
- `app/src/main/java/com/bifilm/app/render/camera/CameraFrameBridge.kt`
- `app/src/main/java/com/bifilm/app/ui/capture/CaptureScreen.kt`
- `app/src/main/java/com/bifilm/app/ui/compose/ComposeScreen.kt`

## 状态

调试版的 APK 已经能构建 (17+ MB)。
所有非错误未实现项已标注明确 TODO 注释或留 hook
(如 `MaskRenderer.setHardness` 的动态笔刷切换).

## 不在本计划内

云同步、滤镜商店、视频多重曝光、内嵌社交、HDR/包围曝光。
