# 行为记录 (BehaviorTracker)

一款原生 Android 行为追踪应用，支持布尔值（打卡）和数值型记录，提供统计图表和数据导出/导入功能。

[![zread](https://img.shields.io/badge/Ask_Zread-_.svg?style=flat-square&color=00b0aa&labelColor=000000&logo=data%3Aimage%2Fsvg%2Bxml%3Bbase64%2CPHN2ZyB3aWR0aD0iMTYiIGhlaWdodD0iMTYiIHZpZXdCb3g9IjAgMCAxNiAxNiIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHBhdGggZD0iTTQuOTYxNTYgMS42MDAxSDIuMjQxNTZDMS44ODgxIDEuNjAwMSAxLjYwMTU2IDEuODg2NjQgMS42MDE1NiAyLjI0MDFWNC45NjAxQzEuNjAxNTYgNS4zMTM1NiAxLjg4ODEgNS42MDAxIDIuMjQxNTYgNS42MDAxSDQuOTYxNTZDNS4zMTUwMiA1LjYwMDEgNS42MDE1NiA1LjMxMzU2IDUuNjAxNTYgNC45NjAxVjIuMjQwMUM1LjYwMTU2IDEuODg2NjQgNS4zMTUwMiAxLjYwMDEgNC45NjE1NiAxLjYwMDFaIiBmaWxsPSIjZmZmIi8%2BCjxwYXRoIGQ9Ik00Ljk2MTU2IDEwLjM5OTlIMi4yNDE1NkMxLjg4ODEgMTAuMzk5OSAxLjYwMTU2IDEwLjY4NjQgMS42MDE1NiAxMS4wMzk5VjEzLjc1OTlDMS42MDE1NiAxNC4xMTM0IDEuODg4MSAxNC4zOTk5IDIuMjQxNTYgMTQuMzk5OUg0Ljk2MTU2QzUuMzE1MDIgMTQuMzk5OSA1LjYwMTU2IDE0LjExMzQgNS42MDE1NiAxMy43NTk5VjExLjAzOTlDNS42MDE1NiAxMC42ODY0IDUuMzE1MDIgMTAuMzk5OSA0Ljk2MTU2IDEwLjM5OTlaIiBmaWxsPSIjZmZmIi8%2BCjxwYXRoIGQ9Ik0xMy43NTg0IDEuNjAwMUgxMS4wMzg0QzEwLjY4NSAxLjYwMDEgMTAuMzk4NCAxLjg4NjY0IDEwLjM5ODQgMi4yNDAxVjQuOTYwMUMxMC4zOTg0IDUuMzEzNTYgMTAuNjg1IDUuNjAwMSAxMS4wMzg0IDUuNjAwMUgxMy43NTg0QzE0LjExMTkgNS42MDAxIDE0LjM5ODQgNS4zMTM1NiAxNC4zOTg0IDQuOTYwMVYyLjI0MDFDMTQuMzk4NCAxLjg4NjY0IDE0LjExMTkgMS42MDAxIDEzLjc1ODQgMS42MDAxWiIgZmlsbD0iI2ZmZiIvPgo8cGF0aCBkPSJNNCAxMkwxMiA0TDQgMTJaIiBmaWxsPSIjZmZmIi8%2BCjxwYXRoIGQ9Ik00IDEyTDEyIDQiIHN0cm9rZT0iI2ZmZiIgc3Ryb2tlLXdpZHRoPSIxLjUiIHN0cm9rZS1saW5lY2FwPSJyb3VuZCIvPgo8L3N2Zz4K&logoColor=ffffff)](https://zread.ai/TimeAIssr/BehaviorTracker)

## 功能特性

- **行为管理**：创建、编辑、删除自定义行为，支持 8 种颜色选择
- **两种记录类型**：
  - 布尔型：打卡式记录（是/否），支持选择日期时间
  - 数值型：带单位的数值记录（如"ml"、"km"），支持备注
- **统计与图表**：
  - 布尔型：连续打卡天数、最长连续、总次数
  - 数值型：日均值、总次数、总和
  - 动态图表（柱状图/折线图），支持7/30/90天时间范围
- **数据导出/导入**：通过JSON格式完整备份和恢复数据（使用SAF，无需存储权限）
- **主题设置**：系统/浅色/深色模式切换，支持Material You动态配色

## 技术栈

| 类别 | 技术 |
|------|------|
| 语言 | Java 21 |
| 最低SDK | 33 (Android 13) |
| 目标SDK | 36 |
| 架构 | MVVM (Activity + ViewModel + Repository) |
| 数据库 | Room 2.8.4 |
| UI框架 | Material Design 3, ViewBinding |
| 图表 | MPAndroidChart v3.1.0 |
| 序列化 | Gson 2.13.2 |
| 构建工具 | Gradle 9.4.0 / Android Gradle Plugin 9.1.0 |

## 项目结构

```
app/src/main/java/com/github/timeaissr/behaviortracker/
├── data/                          # 数据层
│   ├── entity/                    # Room实体
│   │   ├── Behavior.java          # 行为实体
│   │   ├── Record.java            # 记录实体
│   │   └── RecordType.java        # 记录类型枚举
│   ├── dao/                       # 数据访问对象
│   │   ├── BehaviorDao.java
│   │   └── RecordDao.java
│   ├── converter/
│   │   └── Converters.java        # Room类型转换器
│   ├── repository/
│   │   └── BehaviorRepository.java # 统一数据仓库
│   └── AppDatabase.java           # Room数据库单例
├── ui/                            # 界面层
│   ├── main/                      # 主页
│   │   ├── MainActivity.java
│   │   ├── MainViewModel.java
│   │   └── BehaviorAdapter.java
│   ├── add/                       # 添加/编辑行为
│   │   ├── AddBehaviorActivity.java
│   │   ├── AddBehaviorViewModel.java
│   │   └── ColorPickerAdapter.java
│   ├── detail/                    # 行为详情与统计
│   │   ├── BehaviorDetailActivity.java
│   │   ├── DetailViewModel.java
│   │   └── RecordAdapter.java
│   └── settings/                  # 设置
│       └── SettingsActivity.java
├── export/                        # 数据导出/导入
│   ├── DataManager.java
│   ├── ExportData.java
│   └── BackupValidator.java
├── util/
│   ├── DateUtils.java             # 日期工具类
│   └── StatsCalculator.java       # 统计计算
└── BehaviorTrackerApp.java        # Application类
```

## 构建与运行

```bash
# 构建调试版本
./gradlew assembleDebug

# 构建发布版本（含ProGuard混淆）
./gradlew assembleRelease

# 运行单元测试
./gradlew test

# 运行仪器测试（需要设备/模拟器）
./gradlew connectedAndroidTest

# 运行单个测试类
./gradlew testDebugUnitTest --tests "com.github.timeaissr.behaviortracker.util.StatsCalculatorTest"

# 清理并构建
./gradlew clean assembleDebug

# 安装到设备
./gradlew installDebug
```

## 数据库设计

### Behavior（行为）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | long (自增主键) | 唯一标识 |
| name | String | 行为名称 |
| recordType | RecordType | BOOLEAN 或 NUMERIC |
| unit | String (可空) | 数值型单位 |
| color | String | 卡片颜色（十六进制） |
| createdAt | long | 创建时间戳 |

### Record（记录）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | long (自增主键) | 唯一标识 |
| behaviorId | long (外键) | 关联行为 |
| timestamp | long | 记录时间戳 |
| value | double | 数值（布尔型固定为1.0） |
| note | String (可空) | 备注 |
