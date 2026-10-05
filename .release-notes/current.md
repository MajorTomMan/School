## 变更点

- 课程分发默认仅将 `course.json` 放入完整 ZIP；`assessments.json`、`knowledge-points.json`、教材 PDF 与题目资源推荐作为 `bundled=false` 独立文件，由同一 immutable release 的 manifest 锁定版本关系。
- 设置页重新整理为“应用 / 课程 / 显示 / 网络 / AI / 数据”，默认进入应用页，代理设置移动到网络页；“我的”中的下载与存储、显示模式可直接跳转到对应设置分页。
- 学习数据从 AI 设置中拆出为独立数据页，明确区分学习记录与课程资源缓存。
- 课程设置页新增资源拆分统计，可按课程查看结构、题库、教材与题目资源的本地占用情况。
- 本地课程存储快照新增资源分类统计，同时保持课程总占用与下载暂存占用的原有计算语义。
- 修正课程 release 管理器的 URL 校验：package 与外置文件必须指向当前 immutable release，bundled 文件允许不提供独立 URL。
