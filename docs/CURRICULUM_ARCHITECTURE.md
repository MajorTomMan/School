# School 课程与知识架构

## 目标

School 的学习内容不再由某一本教材决定。0.19.0 起，核心关系被拆分为：

```text
学科定义 SubjectDefinition
    ↓
课程体系 Curriculum
    ↓
课程目录树 CurriculumNode
    ↕ 多对多
知识点 KnowledgePoint
    ↕ 图关系
知识关系 KnowledgeRelation
    ↕
学习掌握度 KnowledgeMastery

教材、预制课程、题库、音视频等
    ↓
学习资源 LearningResource
    ↓
资源绑定 ResourceBinding
```

基本原则：

- 学科是稳定定义，不等于教材。
- 课程目录是树，允许不同学科使用不同层级。
- 知识依赖是图，一个知识点可以有多个前置知识。
- 教材是可选资源，没有 PDF 时课程仍然存在。
- 掌握度属于知识点，不属于某一本教材。
- 题目尝试和错题仍保留教材来源，用于回到原页和生成同源变式。

## 学科定义

`SubjectDefinition` 保存：

- 稳定学科 ID
- 显示名称
- 学科类别
- 适用教育阶段
- 可组合学习能力

例如数学可以组合数值判定、表达式判定、分步判定、图形和几何能力；英语可以组合词汇、语法、听力和发音能力。功能代码应优先检查能力，不应持续增加 `subjectId == "..."` 分支。

内置目录覆盖语文、数学、英语、日语、科学、物理、化学、生物、历史、地理、思想政治、计算机、编程、经济学、法学、音乐、美术、体育与健康。未知学科也可以在教材同步时动态加入。

## 课程目录树

`CurriculumNode` 使用 `parentId` 邻接表。支持以下节点类型：

```text
ROOT
STAGE
LEVEL
TERM
COURSE
MODULE
UNIT
CHAPTER
LESSON
TOPIC
```

所有层级均为可选。基础教育可以使用：

```text
课程体系
└─ 年级
   └─ 学期或册次
      └─ 教材或课程
         └─ 课程节点
```

大学课程、语言等级或自学路线可以跳过年级和学期：

```text
计算机科学
├─ 数据结构
│  ├─ 树
│  └─ 图
├─ 操作系统
└─ 计算机网络
```

树必须满足：

- 节点 ID 全局唯一。
- 父节点必须存在。
- 父子节点必须属于同一课程体系。
- 不允许循环。
- 同一父节点下通过 `orderIndex` 稳定排序。

## 知识图

`KnowledgePoint` 与课程节点分离。课程节点通过 `NodeKnowledgeRef` 绑定一个或多个知识点。

支持关系：

- `PREREQUISITE`：前置知识
- `PART_OF`：组成关系
- `RELATED`：相关知识
- `EQUIVALENT`：等价概念
- `EXTENDS`：扩展或后续内容

当前数学目录预置正数和负数、数轴、相反数、绝对值、有理数比较、整式等价变形和一元一次方程的前置关系。其他课程在导入时生成稳定知识点 ID，并用低权重 `EXTENDS` 表达教材顺序；后续可以由人工课程包替换为更准确的前置关系。

## 资源绑定

`LearningResource` 表示：

- PDF 教材
- 未绑定原书的预制课程
- 文档、图片、音频、视频
- 题库
- Web 资源

`ResourceBinding` 可以绑定课程节点或知识点，并保存页码范围与用途：

- `PRIMARY`
- `REFERENCE`
- `EVIDENCE`
- `PRACTICE`

当前旧 `TextbookSlot` 继续作为导入、WorkManager 和文件目录的兼容键；同步器会将其转换为课程树与资源绑定。后续新增课程不需要使用 `TextbookSlot`。

## 学习状态

### 节点进度

`curriculum_node_progress` 保存课程节点的：

- 未开始
- 学习中
- 已掌握
- 需要复习
- 最近访问时间
- 完成时间

旧 DataStore 中的 `lesson_status_*` 在过渡期继续保留，并与节点进度双写。

### 知识掌握度

`knowledge_mastery` 的主键为：

```text
subjectId + knowledgePointId
```

不再包含教材键。因此用户在不同版本教材、复习路径或课程体系中遇到同一知识点时，共享掌握度、连续正确次数和到期复习时间。

旧 `math_knowledge_mastery` 在 Room 2 → 3 迁移时按知识点聚合：

- 分数取最高值
- 尝试次数求和
- 连续正确、连续错误取最大值
- 到期时间取最早值
- 更新时间取最新值

旧数学尝试、错题和 JSON 问题记录不删除，仍保留 `textbookKey` 作为来源。

### 掌握度历史与趋势

0.19.1 起，当前状态和历史分析分开保存：

```text
school.db
└─ knowledge_mastery             当前掌握度真相

school-analytics.db
├─ knowledge_mastery_history     知识点逐次变化
└─ subject_mastery_daily         学科每日快照
```

`MasteryTrendRepository` 在应用进程启动时监听通用 `knowledge_mastery`。任何学科只要更新通用掌握度表，都会自动生成趋势，不需要在数学、英语、物理等评价器中分别复制记录代码。

知识点历史保存：

- 学科与知识点 ID
- 掌握度分数
- 作答次数
- 连续正确与连续错误次数
- 最近一次是否正确
- 首次基线、答对或答错事件类型
- 原掌握度更新时间与实际记录时间

`subject_mastery_daily` 每个学科每天只保留一个最新点，包含：

- 当天最新综合掌握度
- 已有掌握记录的知识点数
- 当天非基线练习次数
- 已到复习时间的知识点数

图表语义固定为：

- X 轴：日期
- Y 轴：基于练习结果估算的掌握度
- 黄色点：答对
- 红色点：答错
- 蓝色点：首次基线

系统不伪造历史数据。升级后第一次运行只根据当前状态建立基线，后续再记录真实变化。清空通用掌握度时，分析历史与每日快照也同步清空。

## 教材同步

应用观察教材库变化，并执行：

1. 建立或更新学科定义。
2. 按学科和教育阶段建立课程体系。
3. 建立年级、学期、教材和课程节点。
4. 将旧 `GeneratedLesson.id` 写入 `legacyLessonId`。
5. 推断或生成知识点。
6. 写入节点—知识点引用。
7. 将 PDF 或预制课程写成资源。
8. 将页码范围写成资源绑定。
9. 校验课程树和知识引用。
10. 原子替换由教材生成的图数据。

节点和知识点 ID 均为确定性 ID，重复同步不会制造新记录。

## 数据库

Room 核心数据库版本升级为 3，新增：

```text
subjects
learning_level_systems
learning_levels
curricula
curriculum_nodes
knowledge_points
knowledge_relations
node_knowledge_refs
learning_resources
resource_bindings
knowledge_mastery
curriculum_node_progress
```

掌握度时间序列使用独立 Room 分析库版本 1。独立存储可以避免高频追加历史影响核心课程数据库迁移，也允许以后单独压缩、导出或重建统计数据。

教材生成的数据使用 `origin = MATERIAL`，同步时只替换这一部分；内置知识和未来用户自定义课程不会被教材刷新删除。

## 兼容边界

0.19.x 暂时保留：

- `EducationStage`
- `TextbookVolume`
- `SubjectTemplate`
- `TextbookSlot`
- 旧数学尝试、掌握度和错题表
- DataStore 课程状态

这些结构作为导入和迁移适配层存在，不再是新功能的领域根。后续删除前必须先完成数据导出兼容和至少一个稳定版本周期。
