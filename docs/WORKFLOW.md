# 开发流程

一次改动要走的所有步骤。规则细节见[贡献指南](../CONTRIBUTING.md)、[开发指南](DEVELOPMENT.md)和[维护与发布手册](MAINTENANCE_RELEASE.md)。

## 日常开发

### 1. 同步 main

```bash
git switch main
git pull --ff-only origin main
```

### 2. 建分支

```bash
git switch -c <类型>/<短描述>
```

类型见[贡献指南](../CONTRIBUTING.md#分支与变更范围)的前缀表，短描述用英文小写加连字符。一个分支只做一件事。

### 3. 改代码

发布版本的 PR 还要做两件事：更新 [app/build.gradle](../app/build.gradle) 里的 `versionCode` 和 `versionName`（递增规则见[版本管理](MAINTENANCE_RELEASE.md#versioning)），并新增 `.github/release-notes/vX.Y.Z.md` 发行说明。

### 4. 本地验证

```bash
./gradlew --no-daemon assembleDebug
./gradlew --no-daemon testDebugUnitTest
git diff --check
```

只改了 `.md` 或 `docs/` 下的文件时：

```bash
python3 .github/scripts/check_docs.py
git diff --check
```

### 5. 提交

```bash
git commit -m "<类型>: <中文说明>"
```

遵循 Conventional Commits，描述部分用中文。

### 6. 推送并创建 PR

```bash
git push -u origin <分支名>
```

标题按同一规范写，它将成为 `main` 上的提交标题；说明按[贡献指南](../CONTRIBUTING.md#pull-requests)的五项写。

### 7. 合并

确认最新提交的 Android Build 通过、涉及应用的改动已在真机验证，然后 Squash and merge。

### 8. 合并后同步

```bash
git switch main
git pull --ff-only origin main
```

## 发布（维护者）

前置条件：发布 PR 已合并，`main` 的版本号就是要发的版本，发行说明已在 `.github/release-notes/` 里。

### 1. 打标签

```bash
git tag -a vX.Y.Z -m 'Behavior Tracker vX.Y.Z'
git push origin vX.Y.Z
```

### 2. 校验产物

从 Release 页面下载 APK：

```bash
apksigner verify --verbose --print-certs BehaviorTracker-vX.Y.Z.apk
```

核对签名证书与上一正式版本一致，并确认 Release 标题为 `X.Y.Z`、正文是发行说明。

### 3. 清理分支

```bash
git push origin --delete <分支名>
git branch -D <分支名>
```
