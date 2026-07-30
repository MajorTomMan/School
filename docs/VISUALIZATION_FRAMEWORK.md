# School 跨学科可视化框架

## 目标

可视化实现必须保持以下约束：

- 普通信息不使用卡片堆叠，以连续画布、留白、细线和语义色组织内容。
- Canvas 只绘制几何图形，不直接绘制教学文字；标签使用 Compose `Text`，支持全局字号缩放。
- 所有交互图默认支持双指缩放、拖动和双击复位。
- 每个场景拥有稳定 key、参数 schema、学科归属和显式注册记录。
- 不使用运行时反射扫描，避免 Android 启动不确定性和混淆问题。

## 目录结构

```text
ui/visualization/
├── core/
│   ├── VisualizationFramework.kt      # 父类、参数、schema、能力、主题和注册表
│   ├── VisualizationPrimitives.kt     # 坐标映射、网格、基准线、点和向量箭头
│   └── TechnicalLineChart.kt          # 通用折线趋势图
├── subjects/
│   ├── SubjectVisualizationRenderers.kt
│   └── math/
│       └── OppositeQuantityTrendRenderers.kt
└── SchoolVisualizationCatalog.kt      # 学科模块和统一目录
```

## 新增一个物理可视化

具体实现只继承对应学科父类并实现 `RenderContent`；父类自动提供参数校验、主题、错误降级和生命周期。

```kotlin
object FreeFallVisualizationRenderer : PhysicsVisualizationRenderer() {
    override val key = VisualizationKey("physics.mechanics.free-fall")
    override val schema = VisualizationSchema(
        listOf(
            VisualizationFieldSpec("height", VisualizationValueType.NUMBER),
            VisualizationFieldSpec("gravity", VisualizationValueType.NUMBER),
        ),
    )

    @Composable
    override fun RenderContent(context: VisualizationRenderContext, modifier: Modifier) {
        val height = context.arguments.float("height")
        val gravity = context.arguments.float("gravity", 9.8f)
        // 使用 ZoomableVisualizationCanvas、drawVectorArrow、drawReferenceLine 等通用工具。
    }
}
```

然后在 `PhysicsVisualizationModule.renderers` 中增加该对象。目录会检查学科归属和重复 key。

## 化学和生物

- 化学场景继承 `ChemistryVisualizationRenderer`，适合粒子、分子、反应过程、装置、溶液和定量关系。
- 生物场景继承 `BiologyVisualizationRenderer`，适合细胞、组织、遗传、生态关系和实验过程。
- 多学科共用场景继承 `GeneralVisualizationRenderer`。

## 优先复用的工具

- `ZoomableVisualizationCanvas`：跨学科缩放、平移与双击复位。
- `VisualizationPlotArea`：统一绘图区和坐标映射。
- `drawTechnicalGrid`：极简技术网格。
- `drawReferenceLine`：零点、海平面、平衡点、阈值等基准线。
- `drawDataMarker`：数据点和当前强调点。
- `drawVectorArrow`：力、速度、电场、流向、反应方向和物质迁移。
- `TechnicalLineChart`：时间、阶段或顺序数据的折线趋势。

## 何时创建专用场景

具象隐喻能明显提升理解时，应创建专用 renderer，例如温度计、山脉与海平面、天平、杠杆、光路、分子结构、细胞结构。没有必要具象化时，优先复用折线图、坐标系、基准轴等通用工具，避免装饰压过知识本身。
