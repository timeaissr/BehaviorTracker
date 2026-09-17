# Behavior Tracker 开发、维护与发布标准流程

本文档是 Behavior Tracker 项目进行代码开发、日常维护、版本管理、APK 签名和 GitHub Release 发布时的标准操作手册。涉及这些事项时，应以本文档为准；如实际工作流发生变化，应在同一个 Pull Request 中同步更新本文档。

## 1. 项目基线

| 项目 | 当前约定 |
| --- | --- |
| 稳定分支 | `main` |
| Java | 21 |
| 最低 Android 版本 | Android 13（API 33） |
| 编译 SDK | API 36 |
| Gradle | 使用项目自带的 Gradle Wrapper |
| 数据库 | Room，当前 schema version 为 2 |
| 版本号来源 | `app/build.gradle` |
| 持续集成 | `.github/workflows/android-build.yaml` |
| 正式发布 | `.github/workflows/release.yaml` |
| 正式签名 | GitHub Actions Repository secrets |

`main` 应始终保持可构建、可测试、可发布。功能开发和问题修复不得直接在 `main` 上进行，应通过独立分支和 Pull Request 合并。

## 2. 版本与文件命名

项目使用以下统一格式：

| 对象 | 格式 | 示例 |
| --- | --- | --- |
| 应用版本名（`versionName`） | `X.Y.Z` | `1.1.0` |
| Android 内部版本号（`versionCode`） | 递增整数 | `4` |
| Git 标签 | `vX.Y.Z` | `v1.1.0` |
| GitHub Release 标题 | `Behavior Tracker vX.Y.Z` | `Behavior Tracker v1.1.0` |
| Release APK | `BehaviorTracker-vX.Y.Z.apk` | `BehaviorTracker-v1.1.0.apk` |

`app/build.gradle` 中的 `versionName` 和 `versionCode` 是版本信息的唯一事实来源。发布工作流只校验标签与 `versionName` 是否一致，不应在构建时修改源代码或版本号。

## 3. 开发环境

推荐安装：

- Git
- JDK 21
- Android Studio，以及 Android SDK 36
- 可选：GitHub CLI（`gh`），用于查看 Pull Request、Actions 和 Release

项目已经包含 Gradle Wrapper，因此不需要单独安装 Gradle。统一从项目根目录运行：

```bash
./gradlew <任务名>
```

本机 Android SDK 的路径写入 `local.properties`，例如：

```properties
sdk.dir=/home/用户名/Android/Sdk
```

`local.properties` 只适用于本机，不得提交。没有本地 Android SDK 时，可以依靠 GitHub Actions 完成构建，再下载 Debug APK 进行真机验证。

## 4. Git 身份与凭据

提交身份和登录凭据是两件不同的事：

- `user.name`、`user.email` 决定提交记录显示的作者。
- credential helper、SSH key 或访问令牌负责向 GitHub 认证。

每位贡献者应配置自己的提交身份，例如：

```bash
git config user.name '<你的 Git 提交用户名>'
git config user.email '<你的 Git 提交邮箱>'
git config credential.username '<你的 GitHub 用户名>'
```

检查配置：

```bash
git config --get user.name
git config --get user.email
git config --get credential.username
git config --get credential.helper
```

Linux 桌面环境可通过 Git Credential Manager、`libsecret` 或 KWallet 保存凭据。正常推送使用 `git push origin ...`，不要为了临时操作覆盖仓库已有的 credential helper。GitHub CLI 主要用于 GitHub API 操作，不应擅自替换 Git 的凭据配置。任何人的真实用户名、邮箱、令牌或密码都不应写入项目流程文档。

## 5. 分支与提交规范

### 5.1 分支命名

一个分支只处理一个明确主题。推荐前缀：

- `feat/`：新功能
- `fix/`：问题修复
- `docs/`：文档
- `refactor/`：不改变外部行为的重构
- `test/`：测试
- `chore/`：维护工作
- `ci/`：持续集成和发布工作流
- `codex/`：由 Codex 创建的工作分支

示例：

```text
fix/import-confirmation
docs/release-workflow
codex/document-development-release-workflow
```

开始工作前，从最新的 `main` 创建分支：

```bash
git switch main
git pull --ff-only origin main
git switch -c <分支名>
```

### 5.2 提交信息

使用简洁的 Conventional Commits 风格，正文可以使用中文：

```text
feat: 增加导入预览
fix: 修复统计结果重复计算
docs: 添加开发维护与发布流程
ci: 校验发布 APK 签名
```

提交前确认：

- 没有混入无关改动。
- 没有提交密钥、密码、令牌、`secrets.properties`、`local.properties`、APK 或构建目录。
- `git diff --check` 没有空白符错误。

## 6. 标准开发流程

### 6.1 明确变更范围

编码前先写清楚：

- 用户可见的预期行为。
- 不应改变的现有行为。
- 是否涉及数据库、备份格式、版本号、权限、后台任务或签名。
- 验证成功的条件。

不同主题应拆成不同 Pull Request。测试中发现但不准备在当前 Pull Request 修复的问题，应记录到 Issue、待办事项或本文档的“已知问题”中。

### 6.2 数据库和记录逻辑

修改 Room 实体或表结构时必须：

1. 增加数据库 schema version。
2. 编写明确、可重复执行的 Migration。
3. 保留用户原有数据及原有可见性语义。
4. 同时测试全新安装和从上一正式版本升级。
5. 禁止使用破坏性迁移代替正式迁移。

删除提醒、闹钟或通知等系统功能时，还要清理：

- 数据库字段和迁移逻辑。
- 已注册的 Alarm、Worker、Receiver 或 Service。
- 通知权限、通知渠道及残留设置入口。

当前两种记录类型都允许同一天添加多条记录；不要重新引入“同一天只允许一条”的限制。

### 6.3 备份与导入

备份文件应在写入数据库前完成格式和必填数据校验。只有校验成功后才能进入事务，以避免不完整文件造成部分数据被修改。

当前导入语义是“使用备份中的完整数据替换现有数据”，不是增量合并。任何改变导入语义的工作都应单独设计交互、冲突规则、事务和测试。

### 6.4 异步任务和界面状态

- 数据库和文件操作不得阻塞主线程。
- 成功提示、导航和界面刷新只能在实际操作成功后执行。
- 异步回调应遵循界面生命周期，避免在页面销毁后更新界面。
- 失败必须向用户提供明确反馈，不能静默吞掉异常。
- 行为名称、类型等关键字段在编辑记录时不得被意外修改。

### 6.5 日期、时间和统计

- 时间戳解析必须明确接受的单位和范围。
- 涉及“某天”的逻辑要覆盖本地时区、午夜边界和夏令时。
- 聚合统计应直接按记录计算，避免先生成巨大日期集合。
- 新增日期工具时补充相应导入和单元测试。

## 7. 构建与验证

### 7.1 本地最低验证

在项目根目录运行：

```bash
./gradlew --no-daemon assembleDebug
./gradlew --no-daemon testDebugUnitTest
git diff --check
```

高风险改动还应酌情运行：

```bash
./gradlew --no-daemon lintDebug
./gradlew --no-daemon connectedDebugAndroidTest
```

`connectedDebugAndroidTest` 需要已连接的 Android 设备或模拟器。

### 7.2 没有本地 Android 工具时

可以按以下方式验证：

1. 推送功能分支。
2. 创建 Pull Request。
3. 等待 Android Build 工作流完成。
4. 从 Actions 下载 Debug APK artifact。
5. 在手机上安装并执行对应场景的手工测试。

CI 构建通过不能代替真机验证，特别是数据库迁移、系统权限、主题切换、文件导入导出和升级安装。

## 8. Pull Request 流程

推送工作分支：

```bash
git push -u origin <分支名>
```

Pull Request 至少说明：

- 修改了什么。
- 为什么要修改。
- 如何验证。
- 是否包含数据库迁移、备份格式变化或版本变化。
- 已知限制和暂不处理的问题。

合并前必须满足：

1. Android Build 工作流通过。
2. 代码审查意见已处理或明确说明不采纳原因。
3. 相关 Debug APK 已完成真机测试。
4. 没有秘密信息和无关文件。
5. 版本和文档在需要时已经同步更新。

推荐使用 **Squash and merge**。最终合并标题应概括用户可见结果，Extended description 应总结主要变化，不要直接堆叠所有临时提交信息。

合并后同步本地仓库：

```bash
git switch main
git pull --ff-only origin main
```

同时确认 `main` 上的 Actions 也已成功。

## 9. 版本号管理

项目采用语义化版本：

- `MAJOR`：存在不兼容变化或大规模重构。
- `MINOR`：增加向后兼容的新功能或明显的行为变化。
- `PATCH`：向后兼容的问题修复和小型维护。

Android 的 `versionCode` 每次正式发布必须严格增加，即使 `versionName` 的变化很小也不能复用。实际值始终以 `app/build.gradle` 和最新正式 Release 为准。本文档建立时的正式基线是：

```text
versionName = 1.1.0
versionCode = 4
```

所以下一个正式版本的 `versionCode` 至少是 `5`。

版本号应在发布 Pull Request 中修改、审查并合并。不得等到创建标签时再由工作流临时改写版本号。

## 10. APK 签名

### 10.1 正式签名配置

GitHub Actions 使用以下 Repository secrets：

- `KEYSTORE_BASE64`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

发布工作流把 Base64 内容还原为临时 keystore，并生成临时 `secrets.properties` 完成 Release 签名。

本地构建正式 APK 时，keystore 通常放在：

```text
app/behaviortracker.jks
```

本地 `secrets.properties` 只可包含实际机器所需的信息，例如：

```properties
storeFile=app/behaviortracker.jks
storePassword=<本地密码>
keyAlias=<别名>
keyPassword=<本地密码>
```

该文件、keystore 和密码不得提交、粘贴到日志、截图或聊天中。

### 10.2 签名保管规则

- 对 keystore、别名和密码做至少一份离线加密备份。
- 定期确认备份可读取，但不要在公共环境暴露密码。
- 已发布应用不能随意改用新签名；签名不同的 APK 无法直接覆盖升级原应用。
- 不得因为找不到 keystore 就为同一个应用临时生成新密钥。
- 正式发布升级必须同时满足：签名证书一致、`versionCode` 更高。

Debug APK 使用调试签名，通常不能直接覆盖正式签名版本。需要测试 Debug APK 时，应先导出数据，再卸载正式版、安装 Debug 版并导入；完成后反向操作恢复正式版。

## 11. 正式发布流程

### 11.1 发布前检查

- 发布范围已通过 Pull Request 合并到 `main`。
- `main` 的 Actions 已通过。
- 真机测试通过，包括从上一正式版本覆盖升级。
- 数据库迁移、备份导入导出和核心记录功能正常。
- `versionName` 符合计划，`versionCode` 高于所有历史正式版本。
- Release notes 已准备好。
- 四个签名 secrets 存在且未过期。
- 已知问题已记录。

同步并核对版本：

```bash
git switch main
git pull --ff-only origin main
git status --short
rg 'versionCode|versionName' app/build.gradle
```

`git status --short` 应无输出。

### 11.2 创建和推送标签

使用带说明的标签：

```bash
git tag -a vX.Y.Z -m 'Behavior Tracker vX.Y.Z'
git show-ref --tags vX.Y.Z
git rev-list -n 1 vX.Y.Z
git push origin vX.Y.Z
```

确认标签指向计划发布的 `main` 提交。已经发布的标签不得移动、强制覆盖或复用；需要修复时发布新的补丁版本。

### 11.3 自动发布工作流

推送 `v*` 标签后，Release 工作流会：

1. 检出该标签对应的源码。
2. 安装 JDK 21 和 Gradle 环境。
3. 校验标签版本与 `app/build.gradle` 的 `versionName` 一致。
4. 从 GitHub Secrets 还原签名材料。
5. 构建 Release APK。
6. 把文件重命名为 `BehaviorTracker-vX.Y.Z.apk`。
7. 创建 GitHub Release 并上传 APK。

工作流失败时，不要反复移动同一个已发布标签。先判断 Release 是否已经对用户可见；未发布且确需重试时也应谨慎处理，已发布则必须使用新版本。

### 11.4 Release 标题和说明

统一标题：

```text
Behavior Tracker vX.Y.Z
```

当前工作流自动生成的标题和正文较通用，工作流完成后应手动检查并编辑为统一标题和面向用户的说明。Release notes 推荐包含：

- 主要变化
- 问题修复
- 升级说明和数据兼容性
- 已知问题
- 安装方法
- APK 文件名和 SHA-256 摘要

内容应描述最终用户能感知的结果，不要把内部提交列表直接当作发行说明。

### 11.5 验证正式 APK

下载 Release 页面中的 APK，并检查：

```bash
sha256sum BehaviorTracker-vX.Y.Z.apk
apksigner verify --verbose --print-certs BehaviorTracker-vX.Y.Z.apk
```

至少确认：

- APK 可以安装。
- 应用显示的版本正确。
- 签名证书与上一正式版本一致。
- 可以从上一正式版本直接覆盖升级。
- 升级后原有数据仍然存在。
- 核心新增功能和修复有效。

## 12. 发布后工作

发布完成后：

1. 检查 GitHub Release 的标签、标题和 APK 文件名。
2. 记录并公开 SHA-256 摘要。
3. 在真实设备上从上一正式版本升级安装。
4. 检查版本显示、数据库迁移、记录增删改、统计和导入导出。
5. 确认已移除的提醒功能不会重新创建通知渠道或后台任务。
6. 确认无严重回归后再清理功能分支。
7. 更新 Issue、里程碑或项目文档。

## 13. 分支清理

只有在 Pull Request 已合并、正式版本已发布且验证通过后，才删除功能分支。

由于 Squash merge 会在 `main` 上生成新的提交，Git 可能认为原功能分支尚未合并。删除前先记录分支末端并确认 Pull Request 的内容已经进入 `main`：

```bash
git branch --show-current
git log -1 --oneline <分支名>
git ls-remote --heads origin <分支名>
```

然后删除远端和本地分支：

```bash
git push origin --delete <分支名>
git branch -D <分支名>
```

不要删除尚未合并或仍包含独有工作成果的分支。

## 14. 热修复与回退

已发布版本出现问题时：

1. 从最新 `main` 创建 `fix/` 或 `codex/` 分支。
2. 只处理本次故障，补充回归测试。
3. 增加 PATCH 版本和 `versionCode`。
4. 通过正常 Pull Request 合并。
5. 创建新标签并发布新的正式版本。

严重故障可暂时在 Release 页面标注警告、撤下有问题的资产或标记为预发布，但应清楚说明影响。不要强制移动旧标签、复用旧版本号或用新文件覆盖已经发布的同名版本。

常见发布失败排查：

- 标签和 `versionName` 不一致：修正源码版本并使用正确的新标签。
- 签名失败：检查四个 Repository secrets、Base64 内容、别名和密码是否匹配。
- APK 无法覆盖安装：检查签名证书和 `versionCode`。
- 构建失败：先在 Pull Request 或本地复现，不要在正式标签上试错。

## 15. 日常维护

- 依赖升级尽量单独创建 Pull Request，避免和业务修改混合。
- Room schema、Migration 和备份格式应保持兼容性测试。
- 定期更新 GitHub Actions 版本，并保持最小权限。
- 定期执行签名材料备份恢复演练。
- 删除功能时同步删除数据、权限、后台任务、设置入口和文档。
- 重要逻辑修复要补充自动化测试，不能只依赖人工验证。

## 16. 当前已知问题和技术债

以下问题已经确认，但不属于 v1.1.0 的修复范围：

- 导入数据会完整覆盖现有数据，尚无合并或覆盖前确认流程。
- 开关动态颜色时会重载整个界面。
- Release 标题和说明仍需发布后手动统一。
- Release 工作流使用的第三方 Action 存在 Node.js 运行时升级提示，应单独更新验证。
- Release 工作流尚未自动执行 `apksigner verify` 和与上一版本的证书对比。

处理这些问题时应分别创建清晰的 Issue 或 Pull Request，避免混入无关版本。

## 17. 快速检查清单

### 开发提交前

- [ ] 工作位于独立分支
- [ ] 修改范围单一
- [ ] 数据库和备份兼容性已考虑
- [ ] Debug 构建和单元测试通过
- [ ] `git diff --check` 通过
- [ ] 没有秘密信息或构建产物

### Pull Request 合并前

- [ ] 说明包含修改、原因和验证方式
- [ ] Actions 全部通过
- [ ] 审查意见已处理
- [ ] Debug APK 已真机测试
- [ ] 版本号和文档已按需更新

### 发布前

- [ ] `main` 已同步且工作区干净
- [ ] `versionName`、`versionCode` 正确
- [ ] 从上一正式版本升级测试通过
- [ ] 签名 secrets 和离线备份可用
- [ ] Release notes 已准备
- [ ] 标签准确指向计划发布提交

### 发布后

- [ ] Release 标题为 `Behavior Tracker vX.Y.Z`
- [ ] APK 名称为 `BehaviorTracker-vX.Y.Z.apk`
- [ ] SHA-256 已记录
- [ ] APK 签名、安装、升级和数据保留已验证
- [ ] 已知问题已记录
- [ ] 已合并分支已安全清理
