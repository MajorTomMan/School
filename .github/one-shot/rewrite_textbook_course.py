import json
from pathlib import Path

path = Path("courses/pep-math-7-1/course.json")
data = json.loads(path.read_text(encoding="utf-8"))


def lesson(lid):
    for chapter in data["chapters"]:
        for section in chapter["sections"]:
            for item in section["lessons"]:
                if item["id"] == lid:
                    return item
    raise KeyError(lid)


def explanation(title, text):
    return {"type": "explanation", "title": title, "text": text}


def question(prompt, hint):
    return {"type": "question", "prompt": prompt, "hint": hint}


def keyidea(title, text):
    return {"type": "keyIdea", "title": title, "text": text}


def viz(renderer, parameters, texts):
    return {"type": "visualization", "renderer": renderer, "parameters": parameters, "texts": texts}


def example(title, prompt, steps, answer):
    return {"type": "example", "title": title, "prompt": prompt, "steps": steps, "answer": answer}


def checkpoint(prompt, expected, why):
    return {"type": "checkpoint", "prompt": prompt, "expectedAnswer": expected, "explanation": why}


l = lesson("ch01-intro")
l["steps"] = [
    explanation("从已经认识的数出发", "小学阶段，我们从实际生活中的计数和测量出发，认识了自然数、小数和分数，并学习了这些数的运算。进入新的问题情境后，原来熟悉的数有时还不足以把数量的方向和变化表达清楚。"),
    explanation("新的表示问题", "例如，同一天的气温可能一个高于0 ℃、一个低于0 ℃；同一家公司可能有盈利，也可能有亏损；同一种农作物的产量与上年相比可能增长，也可能减少。这些数量都围绕同一个基准，却朝着两个相反的方向变化。"),
    viz("mathematics.context.opposite-quantities", {"positive": 3, "negative": -3}, {"title": "以0 ℃为基准看两个相反方向", "positiveLabel": "零上 3 ℃", "negativeLabel": "零下 3 ℃", "baselineLabel": "0 ℃", "note": "数值同为3，但相对于同一基准的方向不同。"}),
    question("如果只写数字3，能同时把“零上3 ℃”和“零下3 ℃”的方向区别出来吗？类似地，怎样用数区分盈利与亏损、增长与减少？", "先确定共同的基准，再考虑怎样让数本身带上方向信息。"),
    keyidea("本章研究什么", "为了表示具有相反意义的量，我们将引入正数和负数，把数的范围扩大到有理数；随后借助数轴研究有理数的表示、相反数、绝对值和大小比较。"),
]
l["summary"] = [
    "实际问题会推动数的概念不断扩展；当数量除了大小还有相反方向时，需要新的表示方法。",
    "研究有理数时，既要理解一个数表示的实际意义，也要研究它在数轴上的位置以及与其他数的大小关系。",
]

l = lesson("ch01-positive-negative")
l["steps"] = [
    explanation("数的认识随着实际需要扩展", "数不是一次形成的。为了计数和排序，人们使用1，2，3，…；为了表示“没有”和记数中的空位，引入0；分物和测量又产生了分数。实际问题和运算需要不断推动着数的概念与记号向前发展。"),
    viz("mathematics.context.number-development", {}, {
        "title": "实际需要推动数的认识不断扩展",
        "countingOrigin": "古代中国", "countingNeed": "计数、排序", "countingNumbers": "1，2，3，…",
        "zeroOrigin": "古代印度", "zeroNeed": "表示“没有”、记数中的占位", "zeroNumber": "0",
        "fractionOrigin": "古代埃及", "fractionNeed": "分物、测量", "fractionNumbers": "1/2，1/3，…",
        "note": "这里重现教材图1.1-1所表达的数学关系：新的实际需要不断推动数的范围和表示方法扩展。",
    }),
    explanation("新的需要：表示相反意义的量", "温度以0 ℃为分界，有零上和零下；记账时有盈利和亏损；统计变化时有增长和减少。这些都不是两个毫无关系的量，而是在同一对象、同一基准下方向相反的量。"),
    viz("mathematics.context.opposite-quantities", {"positive": 3, "negative": -3}, {"title": "同一基准下的两个方向", "positiveLabel": "零上 3 ℃", "negativeLabel": "零下 3 ℃", "baselineLabel": "0 ℃", "note": "先确定共同基准，再用符号区别相反方向。"}),
    question("在同一基准下，怎样让一个数既表示数量的大小，又同时说明它处在两个相反方向中的哪一边？", "可以在原有数的前面使用不同的符号来区分方向。"),
    keyidea("正数和负数", "在数学中，大于0的数叫作正数；在正数前面加上负号“−”得到的数叫作负数。为了强调与负数相反的意义，正数前有时写正号“+”，通常正号可以省略，负号不能省略。一个数前面的“+”“−”叫作这个数的符号。"),
    keyidea("0的意义", "0既不是正数，也不是负数。它常常是正、负两个方向的分界或共同基准，而且0本身可以表示一个确定的数量，例如0 ℃是一个确定的温度，海拔0 m是一个确定的海拔。"),
    example("例：用正负数表示相对标准量", "一箱橘子的标准质量为2.5 kg，规定超过标准质量的克数用正数表示。比标准质量多65 g和少30 g分别怎样表示？+50 g和−27 g又分别说明什么？", [
        "以2.5 kg为共同标准，超过标准的方向规定为正，低于标准的方向就是负。",
        "多65 g记作+65 g，少30 g记作−30 g。",
        "+50 g表示实际质量比标准质量多50 g；−27 g表示实际质量比标准质量少27 g。",
    ], "+65 g，−30 g；+50 g表示多50 g，−27 g表示少27 g"),
    checkpoint("如果把水位高于警戒水位0.4 m记作+0.4 m，那么低于警戒水位0.7 m怎样表示？水位正好等于警戒水位又怎样表示？", "−0.7 m，0 m", "警戒水位是共同基准，高于和低于是两个相反方向；正好位于基准处的变化量为0。"),
]
l["summary"] = [
    "大于0的数叫作正数；在正数前加负号“−”得到负数。正号“+”通常可以省略，负号不能省略。",
    "0既不是正数，也不是负数；在许多实际问题中，0还是正、负两个方向的分界或共同基准。",
    "用正数和负数表示实际量时，必须先说明共同基准、正方向和单位；脱离这些条件，符号本身不能完整说明实际意义。",
    "具有相反意义的量必须属于同一对象或同一数量关系，并且相对于同一个基准讨论。",
]

l = lesson("ch01-allowed-deviation")
l["steps"] = [
    explanation("工业生产中的允许偏差", "产品的尺寸、质量等通常有标准规格，但实际加工不可能每一件都与标准值完全相同。只要实际值落在规定的允许范围内，产品仍可以判为合格；因此工程参数常用正、负数同时表示标准值两侧允许出现的偏差。"),
    viz("mathematics.context.opposite-quantities", {"positive": 0.05, "negative": -0.05}, {"title": "直径40 mm的允许偏差", "positiveLabel": "+0.05 mm", "negativeLabel": "−0.05 mm", "baselineLabel": "标准直径 40 mm", "note": "允许的实际直径范围是39.95 mm到40.05 mm。"}),
    question("标注“40 mm±0.05 mm”时，±0.05 mm是在表示两个直径，还是表示实际直径相对于40 mm允许向两个方向偏离的范围？", "把40 mm看作基准值，再分别解释正偏差和负偏差。"),
    example("例：判断尺寸是否在允许范围内", "某零件标准长度为20 mm，允许偏差为±0.05 mm。实际长度20.03 mm是否合格？", [
        "实际值相对标准值的偏差为20.03−20=+0.03 mm。",
        "允许偏差范围是−0.05 mm到+0.05 mm。",
        "+0.03 mm位于允许范围内。",
    ], "合格"),
    checkpoint("同一零件测得19.93 mm是否合格？", "不合格", "偏差为−0.07 mm，已经超出−0.05 mm到+0.05 mm的允许范围。"),
]
l["summary"] = [
    "“标准值±允许偏差”描述的是实际值相对标准值可以向上或向下偏离的范围。",
    "判断是否合格时，应先求“实际值−标准值”，再检查所得偏差是否落在规定区间内。",
    "正负号说明偏差方向，偏差的绝对大小说明偏离标准的程度。",
]

l = lesson("ch01-rational-concept")
l["steps"] = [
    explanation("把已经认识的数重新整理", "引入负数后，我们已经接触正整数、0、负整数，也接触正分数和负分数。有限小数以及无限循环小数都能够写成分数形式；整数也同样能够写成分母为1等形式的分数。这样就可以从“能否写成分数形式”来统一认识这一大类数。"),
    question("正整数、0、负整数、正分数、负分数以及能够化成分数的有限小数和循环小数，有没有一个统一的数学名称？", "尝试把这些数都写成两个整数之比的形式。"),
    keyidea("有理数", "可以写成分数形式的数称为有理数。正整数、0、负整数统称整数；正分数和负分数统称分数。整数都可以写成分数形式，因此整数和分数都属于有理数。有限小数和无限循环小数也都可以化成分数，所以它们也是有理数。"),
    viz("mathematics.classification.rational", {}, {
        "title": "从两个角度整理有理数", "columnInteger": "整数", "columnFraction": "分数", "rowPositive": "正有理数", "rowZero": "0", "rowNegative": "负有理数",
        "positiveInteger": "1，2，3，…", "negativeInteger": "−1，−2，−3，…", "positiveFraction": "1/2，0.25，…", "negativeFraction": "−2/3，−1.2，…", "zero": "0",
        "note": "同一个有理数既可以按正、零、负分类，也可以看它是不是整数。",
    }),
    example("例：辨认有理数中的整数与分数", "在−7、0、0.25、−2/3、5中，哪些是整数？哪些可以归入分数？", [
        "−7、0、5本身是整数。",
        "0.25=1/4，−2/3本身就是分数形式。",
        "这些数都可以写成分数形式，因此都属于有理数。",
    ], "整数：−7、0、5；分数：0.25、−2/3；它们都属于有理数"),
    checkpoint("−7、0、0.25、−2/3中，哪些是整数？", "−7和0", "整数包括正整数、0和负整数；0.25与−2/3可归入分数。"),
]
l["summary"] = [
    "可以写成分数形式的数称为有理数；整数、有限小数和无限循环小数都能够写成分数形式。",
    "正整数、0、负整数统称整数；正分数和负分数统称分数。",
    "有理数既可以按正有理数、0、负有理数分类，也可以从整数与分数的角度整理；分类时要先明确采用的标准。",
]

l = lesson("ch01-number-line")
l["steps"] = [
    explanation("先把实际位置画在一条直线上", "设一条东西向马路旁有一个汽车站牌，以站牌为基准：东侧3 m和7.5 m处有两个物体，西侧3 m和4.8 m处也有两个物体。只写距离还不能说明物体位于站牌哪一侧；如果把方向和距离同时放到一条有方向的直线上，位置关系就会变得清楚。"),
    viz("mathematics.number-line.points", {"min": -5, "max": 8, "step": 1, "values": [-4.8, -3, 0, 3, 7.5]}, {"title": "以站牌为0表示马路两侧位置", "label0": "西4.8 m", "label1": "西3 m", "label2": "站牌", "label3": "东3 m", "label4": "东7.5 m", "note": "规定向东为正后，西侧位置用负数表示。"}),
    question("怎样只用一个数，就同时说明一个物体在站牌的哪一侧，以及离站牌有多远？", "把站牌作为0，规定一个正方向和统一的单位长度。"),
    explanation("从位置图抽象成数轴", "在直线上任取一点表示0，这个点叫原点；规定一个方向为正方向，反方向为负方向；再选取适当的长度作为单位长度。满足这三个条件后，直线上的点就可以按方向和距离表示正数、0和负数。"),
    viz("mathematics.number-line.construction", {"min": -5, "max": 5, "step": 1}, {"title": "数轴的三个要素", "originLabel": "原点", "positiveDirectionLabel": "正方向", "negativeDirectionLabel": "负方向", "unitLabel": "1个单位长度", "note": "原点、正方向和单位长度缺一不可。"}),
    keyidea("数轴", "规定了原点、正方向和单位长度的直线叫作数轴。原点把数轴分成正半轴和负半轴；一般地，正数表示在正半轴上，负数表示在负半轴上，数值的绝对大小与点到原点的距离有关。"),
    example("例：在数轴上表示数", "在数轴上表示−3、−1.5、0、2。", [
        "先确定原点、正方向和单位长度。", "负数放在原点负方向一侧，正数放在正方向一侧。", "按每个数到0的距离确定具体位置。",
    ], "从左到右依次为−3、−1.5、0、2"),
    checkpoint("数轴上原点右侧4个单位长度的点表示什么数？", "4", "通常规定向右为正方向，因此原点右侧4个单位长度表示正数4。"),
]
l["summary"] = [
    "规定了原点、正方向和单位长度的直线叫作数轴，这三个要素缺一不可。",
    "数轴上的一个点既体现方向，也体现到原点的距离；因此它把“数”和“位置”建立起对应关系。",
    "在通常的水平数轴上，原点右侧为正半轴，左侧为负半轴；表示数时必须先看单位长度。",
]

l = lesson("ch01-opposite-number")
l["steps"] = [
    explanation("从数轴上的等距离点开始", "在数轴上，一个正数和一个负数可能位于原点两侧，但到原点的距离完全相同。例如表示3和−3的两个点都离原点3个单位长度；表示1/2和−1/2的两个点也具有同样的特点。"),
    viz("mathematics.number-line.opposite", {"value": 3, "min": -5, "max": 5, "step": 1}, {"title": "原点两侧的等距离点", "leftLabel": "−3", "rightLabel": "3", "note": "两个点到原点距离相等，方向相反。"}),
    question("3和−3、1/2和−1/2这几组数有什么共同特点？在数轴上的位置又有什么共同特点？", "比较它们的符号以及两个点相对于原点的位置。"),
    keyidea("相反数", "只有符号不同的两个数互为相反数。一般地，a和−a互为相反数；0的相反数仍是0。数轴上互为相反数的两个数所对应的点关于原点对称。"),
    keyidea("符号“−”的另一层意义", "在任意一个数前面添上“−”号，表示求这个数的相反数。因此−a表示a的相反数，而不能脱离a的取值直接断定−a一定是负数。"),
    example("例：求相反数", "分别求−7和4/3的相反数；若a的相反数是2.4，求a。", [
        "−7的相反数是7；4/3的相反数是−4/3。", "a与2.4互为相反数，因此a=−2.4。",
    ], "7，−4/3；a=−2.4"),
    checkpoint("若a=−4，那么−a等于多少？", "4", "−a表示a的相反数，所以−(−4)=4。"),
]
l["summary"] = [
    "只有符号不同的两个数互为相反数；0的相反数是0。",
    "数轴上表示互为相反数的两个点关于原点对称，它们到原点的距离相等。",
    "−a表示a的相反数，a可以是正数、负数或0，因此−a不一定是负数。",
]

l = lesson("ch01-absolute-value")
l["steps"] = [
    explanation("相反数中“相同”的是什么", "10和−10只有符号不同，在数轴上分居原点两侧，但表示它们的两个点到原点的距离都等于10。相反数的方向不同，而这个共同的距离不带正负方向。"),
    viz("mathematics.number-line.absolute-value", {"value": -4, "min": -6, "max": 6, "step": 1}, {"title": "绝对值表示到原点的距离", "valueLabel": "−4", "absoluteLabel": "距离为4", "note": "点在负半轴上，但距离本身是非负的。"}),
    question("一个数在数轴上的点到原点的距离，与这个数本身有什么关系？正数、0、负数三种情况分别会怎样？", "距离没有方向，因此结果不会是负数。"),
    keyidea("绝对值", "数轴上表示数a的点与原点的距离叫作数a的绝对值，记作|a|。正数的绝对值是它本身；0的绝对值是0；负数的绝对值是它的相反数。任何数的绝对值都大于或等于0。"),
    example("例：从数轴理解绝对值", "求|1|、|−0.5|和|−7/4|。", [
        "1是正数，所以|1|=1。", "−0.5是负数，它的相反数是0.5，所以|−0.5|=0.5。", "−7/4是负数，所以|−7/4|=7/4。",
    ], "1，0.5，7/4"),
    checkpoint("|−5|、|0|、|3.2|分别是多少？", "5、0、3.2", "分别按负数、0、正数三种情况处理。"),
]
l["summary"] = [
    "数轴上表示数a的点与原点的距离叫作a的绝对值，记作|a|。",
    "若a>0，则|a|=a；若a=0，则|a|=0；若a<0，则|a|=−a。",
    "绝对值表示距离，所以|a|≥0；互为相反数的两个数绝对值相等。",
]

l = lesson("ch01-rational-comparison")
l["steps"] = [
    explanation("从温度的高低得到数的顺序", "比较温度时，越低的温度对应越小的数。把−4，−3，−2，−1，0，1，2依次放到水平数轴上，表示它们的点正好从左到右排列。这个事实把日常的高低关系转化为数轴上的位置关系。"),
    viz("mathematics.number-line.points", {"min": -5, "max": 3, "step": 1, "values": [-4, -3, -2, -1, 0, 1, 2]}, {"title": "数轴上的顺序就是数的大小顺序", "label0": "−4", "label1": "−3", "label2": "−2", "label3": "−1", "label4": "0", "label5": "1", "label6": "2", "note": "在水平数轴上，右边的数总比左边的数大。"}),
    question("从数轴上的位置看，正数、0、负数之间有什么大小关系？两个负数之间又该怎样比较？", "先比较点在数轴上的左右位置，再联系它们到原点的距离。"),
    keyidea("有理数的大小比较", "在水平数轴上，右边的数总比左边的数大。由此可知：正数大于0，0大于负数，正数大于负数；两个负数比较大小时，绝对值大的反而小。"),
    viz("mathematics.number-line.comparison", {"left": -5, "right": -2, "min": -6, "max": 2, "step": 1}, {"title": "两个负数的比较", "leftLabel": "−5", "rightLabel": "−2", "note": "−2在−5的右边，所以−2>−5。"}),
    example("例：比较两个负数", "比较−3.5与−2.8的大小。", [
        "两个数都是负数。", "比较绝对值：3.5>2.8。", "两个负数中绝对值大的反而小。",
    ], "−3.5<−2.8"),
    checkpoint("把−3、1、0、−1.5按从小到大排列。", "−3<−1.5<0<1", "把这些数放在同一条数轴上，从左到右读取即可。"),
]
l["summary"] = [
    "在水平数轴上，右边的数总比左边的数大。",
    "正数大于0，0大于负数，因此正数一定大于负数。",
    "两个负数比较大小时，绝对值大的数反而小；这是数轴位置关系的直接结果。",
]

l = lesson("ch01-activity")
if l["steps"] and l["steps"][0]["type"] == "question":
    idx = next((i for i, step in enumerate(l["steps"][1:4], 1) if step["type"] == "explanation"), None)
    if idx is not None:
        context = l["steps"].pop(idx)
        l["steps"].insert(0, context)
l["summary"] = [
    "整理真实数据时必须先说明单位和共同基准，再规定正方向，最后才能解释正负号。",
    "数轴或表格可以把数据的方向、大小和顺序放在同一个表示体系中。",
]

# 其余章节先做结构性第一遍：凡是已有说明性材料的课时，先把材料放到问题之前。
for chapter in data["chapters"][1:]:
    for section in chapter["sections"]:
        for item in section["lessons"]:
            steps = item.get("steps", [])
            if steps and steps[0].get("type") == "question":
                idx = next((i for i, step in enumerate(steps[1:4], 1) if step.get("type") == "explanation"), None)
                if idx is not None:
                    context = steps.pop(idx)
                    steps.insert(0, context)

contexts = {
    "ch02-subtraction": ("从已有加法回看减法", "上一课已经能够计算有理数加法。减法与加法互为逆运算，因此研究有理数减法时，可以先比较一个减法算式与相应加法算式的结果，再寻找把减法统一到加法的方法。"),
    "ch03-direct-proportion": ("先看工作效率保持不变的数量关系", "在“工作量=工作效率×工作时间”中，如果工作效率保持不变，工作时间变化时工作量也随之变化。把几组相对应的工作量和时间写出来，就可以进一步研究它们的比值。"),
    "ch03-inverse-proportion": ("再看总量保持不变的数量关系", "如果总工作量或总路程保持不变，提高效率或速度通常会缩短所需时间。此时两个量一增一减，但判断它们的关系不能只看变化方向，还要检查相对应数值之间是否存在稳定的数量关系。"),
    "ch06-intro": ("几何研究从现实物体开始", "建筑、包装、交通设施和日常用品都具有形状、大小和位置关系。研究几何时，我们暂时忽略材质、颜色等与当前问题无关的属性，把现实物体中需要研究的空间特征抽象出来。"),
}
for lid, (title, text) in contexts.items():
    l = lesson(lid)
    if l["steps"] and l["steps"][0]["type"] == "question":
        l["steps"].insert(0, explanation(title, text))

remaining = []
for chapter in data["chapters"]:
    for section in chapter["sections"]:
        for item in section["lessons"]:
            if item.get("steps") and item["steps"][0].get("type") == "question":
                remaining.append(item["id"])
if remaining:
    raise SystemExit(f"仍有无背景直接提问的课时：{remaining}")

path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print("course rewritten:", path)
