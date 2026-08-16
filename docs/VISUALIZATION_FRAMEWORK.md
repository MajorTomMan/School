# School 可视化基础设施

## 定位

可视化是独立基础设施，不是课程业务层，也不是外部数据展示层。

依赖方向固定为：

```text
course package
    ↓ renderer + parameters + texts
:app
    ↓ VisualizationInvocation
:visualization
```

`:visualization` **不能反向依赖 `:app`**。基础设施内部不读取课程包、不访问网络、不读写文件、不访问数据库、不调用 AI、不查询 Repository、不接收 URL、不接收文件路径，也不接受回调让课程执行任意逻辑。

课程包唯一允许做的事是：选择一个已经注册的 renderer，并传入该 renderer schema 明确允许的参数和文本。

## 模块隔离

可视化代码全部位于独立 Android Library：

```text
visualization/
└── src/main/java/com/majortomman/school/visualization/
    ├── VisualizationContract.kt
    ├── VisualizationRuntime.kt
    ├── VisualizationSurface.kt
    └── renderers/
        └── math/
```

`app` 只依赖 `project(":visualization")`。旧的 `app/ui/visualization`、课程专用 Canvas、`CourseSceneTemplate`、`CourseSceneData` 和兼容分发入口已经删除，不允许重新引入。

## 唯一调用契约

课程步骤使用：

```json
{
  "type": "visualization",
  "renderer": "mathematics.number-line.opposite",
  "parameters": {
    "value": 3,
    "min": -8,
    "max": 8,
    "step": 1
  },
  "texts": {
    "title": "相反数关于 0 对称",
    "leftLabel": "−3",
    "rightLabel": "3",
    "note": "两个点到 0 的距离相等、方向相反。"
  }
}
```

步骤字段必须且只能是：

- `type`
- `renderer`
- `parameters`
- `texts`

其中：

- `renderer`：稳定、显式注册的 key；
- `parameters`：只接受 JSON number、boolean、number[]；
- `texts`：只接受字符串；
- 未声明字段直接拒绝；
- 未注册 renderer 直接拒绝；
- 缺少 required 字段直接拒绝；
- 参数类型错误直接拒绝；
- `NaN`、Infinity 等非有限数在运行时契约中直接拒绝。

**参数中不允许 String、Object、任意 JSON 树、URL、文件路径、资源引用或代码。** 文本只是教学显示文本，不作为命令、路径、表达式或数据源解释。

## 校验时机

课程 JSON 解码时立即构造 `VisualizationInvocation`，并调用 `SchoolVisualizationCatalog.validate()`。

因此非法可视化不会进入课程运行页面，而是在课程包启用前失败。渲染阶段仍保留错误状态作为最后一道防线，但它不是兼容或降级机制。

## Renderer 规则

每个 renderer 必须声明：

1. 稳定 `VisualizationKey`；
2. 精确 `VisualizationSchema`；
3. 纯本地、确定性的渲染逻辑；
4. 仅使用 invocation 中的参数和文本；
5. 不持有 App 业务对象；
6. 不自行拉取任何数据。

所有 renderer 通过 `MathVisualizationRenderers` 等显式清单注册。禁止运行时反射扫描和基于课程字符串猜 renderer。

## 几何与文字分层

基础设施采用两层绘制：

```text
ZoomableVisualizationSurface
├── Geometry Layer     Canvas
└── Annotation Layer   Compose Text
```

Canvas 只画：

- 线、点、圆、矩形、路径；
- 坐标轴、刻度、箭头；
- 几何轮廓和数据几何。

教学文字、标签、说明使用 Compose `Text`。禁止恢复 `nativeCanvas.drawText()` 作为教学标签方案。

这样字体缩放、文本测量、换行、边缘避让和无障碍能力仍由 Compose 管理。

## 共享渲染器

当前数学 renderer key：

```text
mathematics.context.opposite-quantities
mathematics.classification.rational
mathematics.process.integer-to-fraction
mathematics.process.expression
mathematics.rule.sign
mathematics.process.power
mathematics.balance.equation

mathematics.number-line.basic
mathematics.number-line.construction
mathematics.number-line.points
mathematics.number-line.opposite
mathematics.number-line.absolute-value
mathematics.number-line.comparison
mathematics.number-line.movement
mathematics.number-line.root

mathematics.cartesian.point
mathematics.cartesian.linear
mathematics.cartesian.quadratic
mathematics.cartesian.inverse

mathematics.geometry.triangle
mathematics.geometry.circle
mathematics.geometry.angle
mathematics.geometry.parallel
mathematics.geometry.right-triangle
mathematics.geometry.line-ray-segment
mathematics.geometry.projection
mathematics.geometry.object-abstraction
mathematics.geometry.translation
mathematics.geometry.symmetry
mathematics.geometry.rotation

mathematics.chart.line
mathematics.chart.bar
mathematics.probability.tree
```

其中数轴、二维坐标、几何、变换和图表都是基础设施家族，同一个知识体系不得再复制另一套轴、点、标签或缩放实现。

## 数轴基础设施

所有数轴知识统一使用 `NumberLineRenderer`。不同知识只选择不同 renderer key/schema：

- 基本定位 → `mathematics.number-line.basic`
- 构造要素 → `mathematics.number-line.construction`
- 多点读取 → `mathematics.number-line.points`
- 相反数 → `mathematics.number-line.opposite`
- 绝对值 → `mathematics.number-line.absolute-value`
- 大小比较 → `mathematics.number-line.comparison`
- 加减移动 → `mathematics.number-line.movement`
- 根号/无理数定位 → `mathematics.number-line.root`

课程不再拥有“数轴模式”，也不允许通过标题、字符串或 `mode` 让 renderer 猜应该画什么。

## 新增可视化

新增 renderer 时：

1. 在 `:visualization` 中实现 renderer；
2. 定义精确 schema；
3. 注册稳定 key；
4. 为契约和关键数学行为补测试；
5. 更新本文件和课程制作校验器；
6. 课程包随后才能引用新 key。

不要在 App 页面临时画一个图，也不要为旧课程格式增加 adapter、alias 或 fallback。

## 禁止项

以下实现一律不接受：

- renderer 调 HTTP/Socket/WebView；
- renderer 读取本地文件、ContentResolver、数据库或 DataStore；
- renderer 引用课程 Repository、ViewModel、AI Client；
- 参数传 URL、路径、JSON Object、任意 Map、代码字符串；
- 课程传 Compose lambda、回调或可执行表达式；
- 通过标题关键字决定画哪一种图；
- 为 `scene/template/data` 保留兼容入口；
- 在 `app` 再建一套可视化 primitive。

如果某个教学内容必须依赖网络、文件或动态业务数据，它就不属于课程可视化基础设施，应由其他业务能力负责。
