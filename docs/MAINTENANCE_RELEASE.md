# 维护与发布手册

本文负责版本、签名、正式发布和日常维护，由维护者执行交付操作。开发协作遵循[贡献指南](../CONTRIBUTING.md)，实现约束与测试方法见[开发指南](DEVELOPMENT.md)。

<a id="versioning"></a>

## 版本管理

[app/build.gradle](../app/build.gradle) 是源码版本的唯一事实来源。确定下一版本时，同时核对当前源码和所有历史正式 Release，不在文档中另设固定版本基线。

| 对象 | 格式或规则 |
| --- | --- |
| `versionName` | `X.Y.Z`，遵循下表的语义化版本规则 |
| `versionCode` | 高于所有历史正式版本的整数 |
| Git 标签 | `vX.Y.Z` |
| GitHub Release 标题 | `X.Y.Z` |
| Release APK | `BehaviorTracker-vX.Y.Z.apk` |

| 变更范围 | 递增部分 |
| --- | --- |
| 不兼容变化或大规模重构 | MAJOR |
| 向后兼容的新功能或明显行为变化 | MINOR |
| 向后兼容的问题修复、小型维护 | PATCH |

准备正式发布的 PR 必须：

1. 按上述规则更新两个版本号，并在说明中写出各自的更新前后值。
2. 在版本更新提交后确认最新提交的 Actions 通过，不能沿用之前提交的检查结果。
3. 通过审查并合并后才创建标签；发布工作流仅校验和构建，不得临时改写源码版本号。

开发者和自动化代理应主动完成这些要求，不能仅凭 Actions 通过就合并发布，也不能等用户提醒才补版本号。纯文档或不准备发布的改动无需单独增加应用版本号。

<a id="signing"></a>

## 签名与升级

### 配置

[Release 工作流](../.github/workflows/release.yaml) 从 GitHub Actions Repository secrets 读取：

- `KEYSTORE_BASE64`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

工作流还原临时 keystore 并生成 `secrets.properties`。本地签名构建将 keystore 放在 `app/behaviortracker.jks`，在项目根目录配置：

```properties
storeFile=app/behaviortracker.jks
storePassword=<本地密码>
keyAlias=<别名>
keyPassword=<本地密码>
```

然后运行：

```bash
./gradlew --no-daemon assembleRelease
```

### 保管与安装约束

保留至少一份 keystore、别名和密码的离线加密备份，定期验证可恢复性。除[禁止提交的内容](../CONTRIBUTING.md#提交要求)外，也不得把签名材料粘贴到日志、截图或聊天中。

正式版覆盖升级必须保持签名证书一致，并满足[版本管理](#versioning)要求。找不到 keystore 时不得为同一个应用临时生成新密钥。

Debug APK 使用调试签名，通常不能覆盖正式版。需要切换测试时，先导出数据，再卸载正式版、安装 Debug 版并导入；完成后反向操作恢复正式版。

## 正式发布流程

### 1. 准备发布

- [ ] 发布范围已按[PR 流程](../CONTRIBUTING.md#pull-requests)合并，`main` 的 Actions 通过。
- [ ] 已完成[版本管理](#versioning)和[签名配置](#signing)要求，签名 secrets 及离线备份可用。
- [ ] 候选正式 APK 已在真机通过从上一正式版本覆盖升级的测试，原数据保留、数据库迁移、核心记录和备份导入导出正常。
- [ ] 已准备面向用户的发行说明，并记录已知问题。

工作区干净时，按贡献指南同步 `main`，再检查：

```bash
git status --short
rg 'versionCode|versionName' app/build.gradle
```

`git status --short` 应无输出。

### 2. 创建标签

以下命令中的 `X.Y.Z` 必须替换为计划发布的版本。先创建并核对带说明的标签，确认其指向计划发布的 `main` 提交：

```bash
git tag -a vX.Y.Z -m 'Behavior Tracker vX.Y.Z'
git show-ref --tags vX.Y.Z
git rev-list -n 1 vX.Y.Z
```

核对无误后推送，触发自动发布：

```bash
git push origin vX.Y.Z
```

已发布的标签和版本不得移动、强制覆盖或复用，也不得用新文件覆盖已发布的同名版本。需要修复时发布新的补丁版本。

### 3. 检查工作流与发行说明

工作流检出标签、准备构建环境、校验标签与源码版本、还原签名材料、构建并重命名 APK，最后创建 GitHub Release 并上传产物。命名采用[版本管理](#versioning)中的统一格式。

当前工作流自动生成标题和通用正文。完成后核对标签、标题和文件名，并将正文替换为面向用户的发行说明，包含：

- 主要变化与问题修复。
- 升级说明、数据兼容性和已知问题。
- 安装方法、APK 文件名和 SHA-256 摘要。

说明应描述最终用户能感知的结果，避免直接使用内部提交列表。

### 4. 验证实际发布产物

从 Release 页面下载 APK 后执行：

```bash
sha256sum BehaviorTracker-vX.Y.Z.apk
apksigner verify --verbose --print-certs BehaviorTracker-vX.Y.Z.apk
```

- [ ] 记录并公开 SHA-256 摘要。
- [ ] 核对签名证书与上一正式版本一致。
- [ ] 使用下载的 APK 复核发布前的升级和核心功能场景，并确认应用显示的版本正确。
- [ ] 更新相关 Issue、里程碑和文档，确认无严重回归后再清理功能分支。

<a id="branch-cleanup"></a>

## 分支清理

仅在 PR 已合并、正式版本已发布且验证通过后删除功能分支。Squash merge 会生成新提交，Git 可能仍认为原分支未合并；先记录分支末端，确认其内容已进入 `main`：

```bash
git branch --show-current
git log -1 --oneline <分支名>
git ls-remote --heads origin <分支名>
```

确认当前位于 `main` 且目标分支没有独有工作成果后，删除远端和本地分支：

```bash
git push origin --delete <分支名>
git branch -D <分支名>
```

## 故障处理

热修复按[贡献流程](../CONTRIBUTING.md)建立独立分支，只处理本次故障并补充回归测试，再按本手册完成版本更新和发布。

严重故障时，可在 Release 页面标注警告、撤下有问题的资产或标记为预发布，并说明影响。后续修复仍通过新版本交付。

| 故障 | 排查与处理 |
| --- | --- |
| 标签与源码版本不一致 | 核对版本管理规则，修正源码并使用正确的新标签 |
| 签名失败 | 检查签名 secrets、Base64 内容、别名和密码是否匹配 |
| APK 无法覆盖安装 | 检查签名证书和内部版本号 |
| 构建失败 | 先在 PR 或本地复现，避免在正式标签上试错 |
| 发布工作流中断 | 先确认 Release 是否已对用户可见；未发布时谨慎判断能否重试，已发布时遵守标签不可变规则 |

## 日常维护

- 依赖和 GitHub Actions 升级尽量使用独立 PR，验证运行时兼容性并保持工作流最小权限。
- 持续维护[数据兼容性](DEVELOPMENT.md#数据模型与兼容性)测试。
- 定期执行[签名备份恢复](#signing)演练。
- 功能删除遵循[系统功能清理要求](DEVELOPMENT.md#移除系统功能)。

<a id="known-issues"></a>

## 已知问题与技术债

| 项目 | 后续工作 |
| --- | --- |
| 导入交互缺少覆盖前确认，也不支持合并 | 设计导入预览、确认及冲突处理，现有语义见[备份与导入](DEVELOPMENT.md#备份与导入) |
| 动态颜色切换会重载整个界面 | 评估局部更新和界面状态保留 |
| Release 正文仍需手动完善 | 改善发行说明生成流程 |
| 曾出现第三方 Action 的 Node.js 运行时升级提示 | 下次维护时核对 Actions 日志，单独升级验证 |
| 工作流尚未自动校验 APK 签名和对比上一版本证书 | 将发布产物验证步骤纳入自动化 |

处理上述事项时分别建立明确的 Issue 或 PR，完成后更新此表。
