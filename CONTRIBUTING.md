# 贡献指南

本文负责开发协作规则。技术实现和验证方法见[开发指南](docs/DEVELOPMENT.md)，正式交付见[维护与发布手册](docs/MAINTENANCE_RELEASE.md)。完整文档入口见 [README](README.md#文档导航)。

## Git 身份与凭据

提交身份决定作者信息，登录凭据负责向 GitHub 认证。每位贡献者应配置自己的身份：

```bash
git config user.name '<你的 Git 提交用户名>'
git config user.email '<你的 Git 提交邮箱>'
git config credential.username '<你的 GitHub 用户名>'
```

使用 `git config --get <配置项>` 检查上述配置及 `credential.helper`。Linux 桌面可通过 Git Credential Manager、libsecret 或 KWallet 保存凭据。推送使用仓库已有的 credential helper；GitHub CLI 用于 API 操作，不应擅自替换 Git 的凭据配置。

## 分支与变更范围

`main` 应保持可构建、可测试、可发布。修改通过独立分支和 Pull Request 合并。工作区干净时，从最新的 `main` 开始：

```bash
git switch main
git pull --ff-only origin main
git switch -c <分支名>
```

一个分支只处理一个明确主题。编码前确定用户可见的预期行为、需保留的现有行为、验证成功的条件，以及是否涉及数据库、备份、版本号、权限、后台任务或签名。

| 前缀 | 用途 |
| --- | --- |
| `feat/` | 新功能 |
| `fix/` | 问题修复 |
| `docs/` | 文档 |
| `refactor/` | 不改变外部行为的重构 |
| `test/` | 测试 |
| `chore/` | 日常维护 |
| `ci/` | 持续集成与发布工作流 |
| `codex/` | Codex 创建的工作分支 |

## 提交要求

提交信息使用 `<类型>: <中文说明>`，常用类型为 `feat`、`fix`、`docs`、`refactor`、`test`、`chore`、`ci`：

```text
fix: 修复导入校验失败后数据被修改的问题
feat: 增加备份导入预览
docs: 更新发布流程
```

提交前完成[构建与验证](docs/DEVELOPMENT.md#validation)，检查修改范围并执行：

```bash
git diff --check
```

不得提交或公开以下内容：

- keystore、签名密码、访问令牌。
- `secrets.properties`、`local.properties` 和其他本机配置。
- APK、构建目录和含真实用户数据的备份。

文档示例使用占位符，不写入个人真实用户名、邮箱或凭据。

<a id="pull-requests"></a>

## Pull Request 与合并

推送工作分支后创建 PR：

```bash
git push -u origin <分支名>
```

PR 说明应包含修改内容、原因、验证方法及结果、数据库迁移或备份格式变化，以及已知限制。准备正式发布时，先完成[发布 PR 的版本要求](docs/MAINTENANCE_RELEASE.md#versioning)。

合并前检查：

- [ ] 修改范围单一，没有无关文件或禁止提交的内容。
- [ ] 最新提交的 Android Build 工作流通过，审查意见已处理或明确说明不采纳原因。
- [ ] 涉及应用的修改已按[验证要求](docs/DEVELOPMENT.md#validation)完成相关 APK 的真机测试；纯文档修改通过[文档检查](docs/DEVELOPMENT.md#ci-documentation)。
- [ ] 需要的版本更新和对应文档已同步。
- [ ] 本次不处理的问题已记录到 Issue、待办事项或[技术债](docs/MAINTENANCE_RELEASE.md#known-issues)。

推荐使用 **Squash and merge**。合并标题概括用户可见结果，Extended description 总结主要变化，避免直接堆叠临时提交信息。

合并后同步本地仓库，并确认 `main` 的 Actions 成功：

```bash
git switch main
git pull --ff-only origin main
```

功能分支按[分支清理规则](docs/MAINTENANCE_RELEASE.md#branch-cleanup)处理。
