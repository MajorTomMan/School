from __future__ import annotations

import base64
import copy
import gzip
import hashlib
import json
from pathlib import Path

EXPECTED_SIZE = 263601
EXPECTED_SHA256 = "e0250eb6d638d322f589fc1f3d13224bf226af19f65a25e189e98e518f68100f"

FORMULAS = {
    "c1l4": ("a+(-a)=0", "a 与 -a 互为相反数；0 的相反数仍是 0。"),
    "c1l5": (r"|a|=\begin{cases}a,&a>0\\0,&a=0\\-a,&a<0\end{cases}", "绝对值表示数轴上对应点到原点的距离。"),
    "c2l2": (r"a+b=b+a,\qquad (a+b)+c=a+(b+c)", "分别表示加法交换律和结合律。"),
    "c2l3": (r"a-b=a+(-b)", "减去一个数，等于加上这个数的相反数。"),
    "c2l6": (r"a(b+c)=ab+ac", "乘法分配律。"),
    "c2l7": (r"a\div b=a\cdot\frac{1}{b},\qquad b\ne 0", "除以一个非零数，等于乘以这个数的倒数。"),
    "c2l9": (r"a^n=a\cdot a\cdots a", "右边共有 n 个相同因数 a，n 为正整数。"),
    "c2l11": (r"N=a\times 10^n,\qquad 1\le |a|<10", "科学记数法的标准形式。"),
    "c2l13": (r"(a_n\cdots a_1a_0)_b=\sum_{k=0}^{n}a_kb^k", "b 是进位制的基数。"),
    "c3l3": (r"s=vt,\qquad S=ab", "字母可以把同一类数量关系写成一般形式。"),
    "c4l4": (r"ax+bx=(a+b)x", "合并同类项可看作分配律的逆用。"),
    "c4l5": (r"a(b+c)=ab+ac,\qquad -(a+b)=-a-b", "去括号时，括号外的因数要作用到括号内每一项。"),
    "c5l3": (r"a=b\Rightarrow a\pm c=b\pm c,\qquad a=b\Rightarrow ac=bc,\qquad c\ne0\Rightarrow \frac{a}{c}=\frac{b}{c}", "等式两边进行相同的合法运算，等式仍成立。"),
    "c5l4": (r"ax=b,\quad a\ne0\Rightarrow x=\frac{b}{a}", "把未知数的系数化为 1。"),
    "c5l13": (r"x=0.\overline{3}\Rightarrow 10x-x=3\Rightarrow x=\frac{1}{3}", "利用循环部分相同，相减后可以消去无限循环尾部。"),
    "c5l10": (r"P=S-C,\qquad S=C(1\pm r)", "P 表示利润，S 表示售价，C 表示进价，r 表示利润率；盈利取 +，亏损取 -。"),
    "c6l8": (r"AM=MB=\frac{1}{2}AB", "M 是线段 AB 的中点时成立。"),
    "c6l10": (r"1^\circ=60^\prime,\qquad 1^\prime=60^{\prime\prime}", "角的度、分、秒之间按六十进制换算。"),
    "c6l12": (r"\angle AOC=\angle COB=\frac{1}{2}\angle AOB", "OC 平分 ∠AOB 时成立。"),
    "c6l13": (r"\alpha+\beta=90^\circ,\qquad \alpha+\beta=180^\circ", "前式表示互余，后式表示互补。"),
}

EXPLANATIONS = {
    "c1l1": "在许多实际问题中，同一数量会出现意义相反的两种情况。选定一个共同基准，并规定其中一种意义为正，另一种意义就用负数表示；0表示正好处在这个基准上。",
    "c1l2": "正整数、0和负整数统称为整数；正分数和负分数统称为分数。整数也能写成分母为1的分数形式，因此这些数都可以纳入同一个数的范围来研究，这就是有理数。",
    "c1l3": "用一条直线表示数时，需要先确定基准点、方向和长度标准。规定原点、正方向和单位长度以后，正数、0和负数就都能用直线上的点表示。",
    "c1l4": "像3和-3这样，只有符号不同的两个数互为相反数。在数轴上，它们位于原点两侧并且到原点的距离相等；0的相反数仍是0。",
    "c1l5": "数轴上表示数a的点到原点的距离叫作a的绝对值。距离总是不小于0，所以一对相反数的绝对值相等，而0的绝对值是0。",
    "c1l6": "把有理数表示在水平数轴上，越靠右的点表示的数越大。两个负数都在原点左侧，绝对值较大的数离原点更远，也就更靠左，因此反而更小。",
    "c2l1": "可以借助数轴理解有理数加法：符号表示运动方向，绝对值表示运动距离。连续完成两次运动后所在的位置，对应的数就是两个有理数的和。",
    "c2l3": "有理数范围内仍把减法看作加法的逆运算。由这种关系可以得到：减去一个数，与加上这个数的相反数结果相同。",
    "c2l7": "有理数除法仍是乘法的逆运算。除以一个非零数，可以转化为乘以它的倒数；商的符号判断与乘法相同。",
    "c3l1": "用字母表示数以后，同一种数量关系可以不依赖某一组具体数值来表达。把实际问题中的数量及其关系写成含字母的式子，就得到代数式。",
    "c5l1": "解决含有未知量的问题时，可以先设未知量为字母，再根据题目中的相等关系列出等式。含有未知数的等式叫作方程。",
    "c6l1": "研究物体的形状、大小和相互位置时，可以暂时忽略颜色、材料等属性，只保留与几何有关的特征。由此得到的长方体、圆柱、三角形、圆等，就是几何图形。",
}

KEY_IDEAS = {
    "c1l1": "正数和负数表示的是相对于同一基准的两种相反意义；0既不是正数也不是负数。",
    "c1l2": "整数和分数统称为有理数。按符号还可以把有理数分为正有理数、0和负有理数。",
    "c1l3": "数轴由原点、正方向和单位长度三个要素确定；每一个有理数都可以用数轴上的点表示。",
    "c1l4": "-a表示a的相反数。a可以是正数、0或负数，所以-a并不一定是负数。",
    "c1l5": "正数的绝对值是它本身，负数的绝对值是它的相反数，0的绝对值是0。",
    "c1l6": "正数大于0，0大于负数；两个负数比较大小时，绝对值大的反而小。",
    "c2l1": "同号两数相加，取相同的符号并把绝对值相加；异号两数相加，取绝对值较大加数的符号，并用较大的绝对值减去较小的绝对值。",
    "c2l3": "有理数减法可以统一转化为加法，转化时减数要同时变成它的相反数。",
    "c2l7": "除数不能为0；除法转化为乘法后，再按乘法的符号法则和绝对值进行计算。",
    "c3l1": "列代数式时，先明确各数量之间的关系，再按照运算关系写出式子。",
    "c5l1": "列方程时，应先找出题目中能够用等号连接的两个量，再把它们分别表示出来。",
    "c6l1": "从实际物体中抽取与形状、大小和位置有关的特征，是研究几何图形的基本方法。",
}

PROCESS_TEXT = "解含分母的一元一次方程时，通常先去分母，再去括号、移项、合并同类项，最后把未知数的系数化为 1。"


def formalize(text: str) -> str:
    for old, new in (
        ("核心是", "可以归结为"),
        ("关键不是", "需要注意的不是"),
        ("本质是", "实质上是"),
        ("最重要的边界条件", "需要特别注意的条件"),
        ("“统一语言”是", "可以统一写成"),
        ("翻译成", "写成"),
        ("翻译回", "解释回"),
        ("套路", "方法"),
    ):
        text = text.replace(old, new)
    return text


source = Path("courses/pep-math-7-1")
encoded = "".join((source / f"course.json.gz.b64.part{index:02d}").read_text(encoding="utf-8") for index in range(1, 8))
course = json.loads(gzip.decompress(base64.b64decode(encoded)).decode("utf-8"))

for chapter in course["chapters"]:
    for section in chapter["sections"]:
        for lesson in section["lessons"]:
            lesson_id = lesson["id"]
            rewritten = []
            for step in lesson["steps"]:
                step_type = step["type"]
                if step_type == "sourceLink":
                    continue
                step = copy.deepcopy(step)
                if step_type == "explanation":
                    step["title"] = None
                    step["text"] = EXPLANATIONS.get(lesson_id, formalize(step["text"]))
                elif step_type == "keyIdea":
                    step["title"] = None
                    step["text"] = KEY_IDEAS.get(lesson_id, formalize(step["text"]))
                elif step_type == "formula":
                    if lesson_id == "c5l7":
                        rewritten.append({"type": "explanation", "text": PROCESS_TEXT, "title": None})
                        continue
                    expression, note = FORMULAS[lesson_id]
                    step["expression"] = expression
                    step["note"] = note
                elif step_type == "question":
                    step["prompt"] = formalize(step["prompt"])
                elif step_type == "summary":
                    step["text"] = formalize(step["text"])
                elif step_type == "checkpoint":
                    step["prompt"] = formalize(step["prompt"])
                    step["explanation"] = formalize(step["explanation"])
                elif step_type == "example":
                    step["prompt"] = formalize(step["prompt"])
                    step["steps"] = [formalize(item) for item in step["steps"]]
                    step["answer"] = formalize(step["answer"])
                rewritten.append(step)
            lesson["steps"] = rewritten
            lesson["goals"] = [formalize(item) for item in lesson["goals"]]
            lesson["summary"] = [formalize(item) for item in lesson["summary"]]
            for practice in lesson["practice"]:
                practice["prompt"] = formalize(practice["prompt"])
                practice["answer"] = formalize(practice["answer"])
                practice["analysis"] = [formalize(item) for item in practice["analysis"]]

raw = (json.dumps(course, ensure_ascii=False, indent=2) + "\n").encode("utf-8")
if len(raw) != EXPECTED_SIZE:
    raise RuntimeError(f"unexpected course size: {len(raw)}")
actual_sha = hashlib.sha256(raw).hexdigest()
if actual_sha != EXPECTED_SHA256:
    raise RuntimeError(f"unexpected course SHA-256: {actual_sha}")

archive = gzip.compress(raw, compresslevel=9, mtime=0)
new_encoded = base64.b64encode(archive).decode("ascii")
parts = [new_encoded[index:index + 8000] for index in range(0, len(new_encoded), 8000)]
if len(parts) != 7:
    raise RuntimeError(f"expected 7 course parts, got {len(parts)}")
for index, part in enumerate(parts, start=1):
    (source / f"course.json.gz.b64.part{index:02d}").write_text(part, encoding="utf-8")
