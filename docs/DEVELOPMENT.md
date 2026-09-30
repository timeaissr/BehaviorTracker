# 开发指南

本文负责开发环境、技术结构、实现约束和验证方法。分支、提交和 PR 规则见[贡献指南](../CONTRIBUTING.md)，版本、签名及交付操作见[维护与发布手册](MAINTENANCE_RELEASE.md)。

## 环境与技术栈

安装 Git、JDK 21、Android Studio 和 Android SDK 36。可选安装 GitHub CLI（`gh`）查看 PR、Actions 和 Release。使用仓库自带的 Gradle Wrapper，从项目根目录运行命令，无需单独安装 Gradle。

| 类别 | 技术 | 配置来源 |
| --- | --- | --- |
| 语言与 Android SDK | Java 21；最低 API 33（Android 13）；编译和目标 API 36 | [app/build.gradle](../app/build.gradle) |
| 架构 | MVVM：Activity + ViewModel + Repository | 下文的代码结构 |
| 数据库 | Room | [app/build.gradle](../app/build.gradle) |
| UI | Material Design 3、ViewBinding | [app/build.gradle](../app/build.gradle) |
| 图表与序列化 | MPAndroidChart、Gson | [app/build.gradle](../app/build.gradle) |
| 构建工具 | Gradle Wrapper、Android Gradle Plugin | [Wrapper 配置](../gradle/wrapper/gradle-wrapper.properties)、[根构建配置](../build.gradle) |

依赖的具体版本以链接中的配置为准。本机 SDK 路径写入根目录的 `local.properties`：

```properties
sdk.dir=/home/用户名/Android/Sdk
```

## 代码结构

应用代码位于 [`app/src/main/java/com/github/timeaissr/behaviortracker/`](../app/src/main/java/com/github/timeaissr/behaviortracker)：

| 路径 | 职责与主要入口 |
| --- | --- |
| `data/entity/`、`data/converter/` | Behavior、Record、RecordType 及 Room 类型转换 |
| `data/dao/`、`data/model/` | 数据查询接口和 NumericStats 聚合结果 |
| `data/repository/` | BehaviorRepository，统一数据访问 |
| `data/AppDatabase.java` | 数据库单例、schema 版本及 Migration |
| `ui/main/` | 主页 Activity、ViewModel、行为列表适配器 |
| `ui/add/` | 行为添加/编辑、颜色选择 |
| `ui/detail/` | 行为详情、记录列表和统计 |
| `ui/settings/` | 主题及导入导出设置 |
| `export/` | DataManager 文件操作、ExportData 格式、BackupValidator 校验 |
| `util/` | DateUtils 日期处理、StatsCalculator 统计计算 |
| `BehaviorTrackerApp.java` | Application 初始化 |

## 数据模型与兼容性

数据库名为 `behavior_tracker.db`，当前 schema version 为 **3**；定义和迁移以 [AppDatabase.java](../app/src/main/java/com/github/timeaissr/behaviortracker/data/AppDatabase.java) 为准。

### 行为与记录

[Behavior](../app/src/main/java/com/github/timeaissr/behaviortracker/data/entity/Behavior.java) 对应 `behaviors` 表：

| 字段 | Java 类型 | 含义 |
| --- | --- | --- |
| `id` | long | 自增主键 |
| `name` | String | 行为名称 |
| `recordType` | RecordType | BOOLEAN 或 NUMERIC |
| `detailedTime` | boolean | 是否记录并显示小时和分钟 |
| `unit` | String | 数值单位，可空 |
| `color` | String | 卡片颜色，十六进制字符串 |
| `createdAt` | long | 创建时间戳 |
| `archived` | boolean | 是否从活跃行为列表隐藏 |

[Record](../app/src/main/java/com/github/timeaissr/behaviortracker/data/entity/Record.java) 对应 `records` 表：

| 字段 | Java 类型 | 含义 |
| --- | --- | --- |
| `id` | long | 自增主键 |
| `behaviorId` | long | 指向行为的外键，删除行为时级联删除记录 |
| `timestamp` | long | 记录时间戳 |
| `value` | double | 数值；布尔型固定为 1.0 |
| `note` | String | 备注，可空 |

记录表在 `behaviorId` 和 `timestamp` 上建立索引。两种记录类型均允许同一天添加多条记录，不得重新引入一天一条的限制。

### 数据库变更

修改表结构时必须增加 schema version，编写明确、可重复执行的 Migration，保留原有数据和可见性语义，同时验证全新安装与从上一正式版本升级。禁止使用破坏性迁移替代正式迁移。

现有迁移包括：1 → 2 清理旧提醒并移除提醒表及旧图标字段；2 → 3 增加 `detailedTime`，默认开启以保留旧记录的时间语义。

### 备份与导入

备份格式由 [ExportData.java](../app/src/main/java/com/github/timeaissr/behaviortracker/export/ExportData.java) 定义，当前格式版本为 **3**，包含版本、导出时间、行为列表和记录列表。该版本独立于数据库 schema 管理。

[DataManager.java](../app/src/main/java/com/github/timeaissr/behaviortracker/export/DataManager.java) 使用 SAF 读写 JSON。导入前完成格式与必填数据校验，校验成功后才进入事务，使用备份中的完整数据替换现有数据。旧格式备份导入时开启 `detailedTime`，保留旧版本的时间语义。

修改格式需维护兼容性测试；若改为增量合并，应单独设计交互、冲突规则、事务和测试。

## 实现约束

### 异步与界面状态

- 数据库和文件操作不得阻塞主线程。
- 成功提示、导航和刷新只能在操作实际成功后执行。
- 异步回调遵循界面生命周期，避免页面销毁后更新界面。
- 失败必须向用户提供明确反馈，不得静默吞掉异常。
- 编辑记录时，不得意外修改行为名称、类型等关键字段。

### 日期与统计

- 时间戳解析明确接受的单位和范围。
- 涉及“某天”的逻辑覆盖本地时区、午夜边界和夏令时。
- 聚合统计直接按记录计算，避免先生成巨大日期集合。
- 新增日期工具时补充相应导入和单元测试。

### 移除系统功能

删除提醒、闹钟或通知功能时，同步清理数据库字段及迁移、已注册的 Alarm/Worker/Receiver/Service、权限、通知渠道、设置入口和相关文档；升级验证需确认不会重新创建旧通知渠道或后台任务。

<a id="validation"></a>

## 构建与验证

应用代码修改的最低本地验证：

```bash
./gradlew --no-daemon assembleDebug
./gradlew --no-daemon testDebugUnitTest
```

其他常用命令：

```bash
# 单个测试类
./gradlew --no-daemon testDebugUnitTest --tests "com.github.timeaissr.behaviortracker.util.StatsCalculatorTest"

# 清理构建并安装到已连接设备
./gradlew --no-daemon clean assembleDebug
./gradlew --no-daemon installDebug

# 高风险修改的补充检查
./gradlew --no-daemon lintDebug
./gradlew --no-daemon connectedDebugAndroidTest
```

仪器测试需要设备或模拟器；当前仓库只有 `app/src/test/` 下的单元测试，尚无 `app/src/androidTest/` 测试用例。重要逻辑修复应补充回归测试，不能只依赖人工验证。

修改应用时，如果本地没有 Android SDK，可按[贡献流程](../CONTRIBUTING.md#pull-requests)推送分支并创建 PR，等待 [Android Build](../.github/workflows/android-build.yaml) 完成，再下载 `app-debug-apk` artifact 进行真机验证。触发条件和变更分类见下文的 [CI 与文档检查](#ci-documentation)。

CI 通过不能替代真机验证，尤其是数据库迁移、权限、主题切换、文件导入导出和升级安装。Debug 与正式版切换测试的安装要求见[签名与升级](MAINTENANCE_RELEASE.md#signing)。

<a id="ci-documentation"></a>

### CI 与文档检查

[Android Build](../.github/workflows/android-build.yaml) 在面向 `main` 的 PR、`main` 推送及手动触发时运行，保留同一个 `build` 检查名。

- PR 检查从共同祖先到 PR 最新提交的完整差异；`main` 推送检查推送前后提交的完整差异，使用相同的文件分类规则。
- 只有 `.md`、`.markdown` 或 `docs/` 下文件变化时，只执行文档检查，跳过 APK 构建、应用单元测试和 artifact 上传。`app/`、`gradle/`、`.github/`、`scripts/` 下的变更始终执行完整构建；其他不在文档范围内的文件（包括构建和依赖配置）也执行完整构建。
- 混合变更、空差异或无法确认变更范围时执行完整构建。手动触发始终执行完整构建。
- 每次运行都检查差异的空白格式，以及仓库 Markdown 中的本地文件链接和章节锚点；外部网址不做联网检查。

纯文档修改无需构建 APK，本地执行：

```bash
python3 .github/scripts/check_docs.py
git diff --check
```

修改 CI 检查脚本时，执行其回归测试；完整构建路径也会执行这些测试：

```bash
python3 -m unittest discover -s .github/scripts/tests -v
```
