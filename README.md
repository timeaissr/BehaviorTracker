# 行为记录 (BehaviorTracker)

一款原生 Android 行为追踪应用，可分别设置是否记录详细时间和数值，提供统计图表和数据导出/导入功能。

[![zread](https://img.shields.io/badge/Ask_Zread-_.svg?style=flat-square&color=00b0aa&labelColor=000000&logo=data%3Aimage%2Fsvg%2Bxml%3Bbase64%2CPHN2ZyB3aWR0aD0iMTYiIGhlaWdodD0iMTYiIHZpZXdCb3g9IjAgMCAxNiAxNiIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHBhdGggZD0iTTQuOTYxNTYgMS42MDAxSDIuMjQxNTZDMS44ODgxIDEuNjAwMSAxLjYwMTU2IDEuODg2NjQgMS42MDE1NiAyLjI0MDFWNC45NjAxQzEuNjAxNTYgNS4zMTM1NiAxLjg4ODEgNS42MDAxIDIuMjQxNTYgNS42MDAxSDQuOTYxNTZDNS4zMTUwMiA1LjYwMDEgNS42MDE1NiA1LjMxMzU2IDUuNjAxNTYgNC45NjAxVjIuMjQwMUM1LjYwMTU2IDEuODg2NjQgNS4zMTUwMiAxLjYwMDEgNC45NjE1NiAxLjYwMDFaIiBmaWxsPSIjZmZmIi8%2BCjxwYXRoIGQ9Ik00Ljk2MTU2IDEwLjM5OTlIMi4yNDE1NkMxLjg4ODEgMTAuMzk5OSAxLjYwMTU2IDEwLjY4NjQgMS42MDE1NiAxMS4wMzk5VjEzLjc1OTlDMS42MDE1NiAxNC4xMTM0IDEuODg4MSAxNC4zOTk5IDIuMjQxNTYgMTQuMzk5OUg0Ljk2MTU2QzUuMzE1MDIgMTQuMzk5OSA1LjYwMTU2IDE0LjExMzQgNS42MDE1NiAxMy43NTk5VjExLjAzOTlDNS42MDE1NiAxMC42ODY0IDUuMzE1MDIgMTAuMzk5OSA0Ljk2MTU2IDEwLjM5OTlaIiBmaWxsPSIjZmZmIi8%2BCjxwYXRoIGQ9Ik0xMy43NTg0IDEuNjAwMUgxMS4wMzg0QzEwLjY4NSAxLjYwMDEgMTAuMzk4NCAxLjg4NjY0IDEwLjM5ODQgMi4yNDAxVjQuOTYwMUMxMC4zOTg0IDUuMzEzNTYgMTAuNjg1IDUuNjAwMSAxMS4wMzg0IDUuNjAwMUgxMy43NTg0QzE0LjExMTkgNS42MDAxIDE0LjM5ODQgNS4zMTM1NiAxNC4zOTg0IDQuOTYwMVYyLjI0MDFDMTQuMzk4NCAxLjg4NjY0IDE0LjExMTkgMS42MDAxIDEzLjc1ODQgMS42MDAxWiIgZmlsbD0iI2ZmZiIvPgo8cGF0aCBkPSJNNCAxMkwxMiA0TDQgMTJaIiBmaWxsPSIjZmZmIi8%2BCjxwYXRoIGQ9Ik00IDEyTDEyIDQiIHN0cm9rZT0iI2ZmZiIgc3Ryb2tlLXdpZHRoPSIxLjUiIHN0cm9rZS1saW5lY2FwPSJyb3VuZCIvPgo8L3N2Zz4K&logoColor=ffffff)](https://zread.ai/TimeAIssr/BehaviorTracker)

## 功能特性

- **行为管理**：创建、编辑、删除自定义行为，支持 8 种颜色选择
- **两个独立记录选项**：
  - 记录详细时间：开启后选择并显示小时和分钟；关闭时只记录日期
  - 记录数值：开启后输入带单位的数值（如"ml"、"km"）
- **统计与图表**：
  - 布尔型：连续打卡天数、最长连续、总次数
  - 数值型：日均值、总次数、总和
  - 动态图表（柱状图/折线图），支持7/30/90天时间范围
- **首页今日次数**：所有行为右侧在今日无记录时显示“×”，有记录时显示今日记录次数；点击可继续新增记录
- **数据导出/导入**：通过JSON格式完整备份和恢复数据（使用SAF，无需存储权限）
- **主题设置**：系统/浅色/深色模式切换，支持Material You动态配色

## 文档导航

| 文档 | 职责 | 适合查阅的内容 |
| --- | --- | --- |
| [贡献指南](CONTRIBUTING.md) | 开发协作 | Git 配置、分支、提交、PR 和合并要求 |
| [开发指南](docs/DEVELOPMENT.md) | 技术实现与验证 | 环境、架构、数据模型、编码约束、构建与测试 |
| [维护与发布手册](docs/MAINTENANCE_RELEASE.md) | 维护与交付 | 版本号、签名、发布、故障处理、分支清理和技术债 |

各主题的具体规则只在对应文档中维护，其他文档通过链接引用；代码或流程变化时，在同一个 PR 中更新对应章节。
