# Behavior Tracker 贡献指南

感谢参与 Behavior Tracker 的开发和维护。完整的开发、测试、数据库、版本、签名和发布规则，请先阅读 [开发、维护与发布标准流程](docs/DEVELOPMENT_MAINTENANCE_RELEASE.md)。该文档是本项目流程的权威说明。

## 基本流程

1. 从最新的 `main` 创建独立分支。
2. 完成单一主题的修改和测试。
3. 使用清晰的提交信息提交。
4. 推送分支并创建 Pull Request。
5. 等待 Actions、代码审查和真机验证通过后合并。

```bash
git switch main
git pull --ff-only origin main
git switch -c <分支名>

./gradlew --no-daemon assembleDebug
./gradlew --no-daemon testDebugUnitTest
git diff --check

git push -u origin <分支名>
```

## 分支命名

- `feat/`：新功能
- `fix/`：问题修复
- `docs/`：文档
- `refactor/`：重构
- `test/`：测试
- `chore/`：维护
- `ci/`：持续集成和发布工作流
- `codex/`：由 Codex 创建的工作分支

## 提交信息

使用 `<类型>: <中文说明>` 的形式，例如：

```text
fix: 修复导入校验失败后数据被修改的问题
feat: 增加备份导入预览
docs: 更新发布流程
ci: 增加 APK 签名校验
```

常用类型包括 `feat`、`fix`、`docs`、`refactor`、`test`、`chore` 和 `ci`。

## Pull Request 要求

Pull Request 应说明：

- 修改内容和原因。
- 验证方法及结果。
- 是否涉及数据库迁移、备份格式或版本号。
- 已知限制和暂不处理的问题。

合并前必须确认 Actions 通过，审查意见已处理，相关 APK 已按风险完成真机测试。

## 禁止提交的内容

不得提交或公开：

- keystore、签名密码和访问令牌。
- `secrets.properties`、`local.properties`。
- APK、构建目录和本机配置。
- 备份中包含的真实用户数据。

## 正式发布

正式发布由维护者执行。版本号必须先在 `app/build.gradle` 中更新，通过 Pull Request 审查并合并，再创建对应的 `vX.Y.Z` 标签。发布工作流只校验和构建版本，不在构建时修改源码版本号。

详细检查清单、签名保管要求、标签命令和 Release 验证步骤见 [开发、维护与发布标准流程](docs/DEVELOPMENT_MAINTENANCE_RELEASE.md)。
