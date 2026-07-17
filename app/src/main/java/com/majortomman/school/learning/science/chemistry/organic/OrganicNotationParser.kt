package com.majortomman.school.learning.science.chemistry.organic

import com.majortomman.school.learning.science.chemistry.PeriodicTable

/**
 * Deterministic, deliberately bounded SMILES-like parser for textbook organic structures.
 * Supported: atoms, branches, single/double/triple bonds, aromatic lower-case atoms,
 * ring digits 0-9, disconnected components and simple bracket atoms such as [NH4+] or [O-].
 */
object OrganicNotationParser {
    fun parse(input: String): OrganicMolecule {
        val source = input.trim()
        require(source.isNotEmpty()) { "请输入有机结构简式。" }
        require(source.length <= 256) { "结构式过长。" }

        val atoms = mutableListOf<OrganicAtom>()
        val bonds = mutableListOf<OrganicBond>()
        val branchStack = ArrayDeque<OrganicAtomId?>()
        val ringAnchors = mutableMapOf<Int, RingAnchor>()
        var current: OrganicAtomId? = null
        var pendingBond: OrganicBondOrder? = null
        var index = 0

        fun addAtom(parsed: ParsedAtom) {
            val id = OrganicAtomId(atoms.size)
            val atom = OrganicAtom(
                id = id,
                element = PeriodicTable.bySymbol(parsed.symbol),
                formalCharge = parsed.formalCharge,
                aromatic = parsed.aromatic,
                explicitHydrogens = parsed.explicitHydrogens,
            )
            atoms += atom
            current?.let { previous ->
                val previousAtom = atoms[previous.value]
                val order = pendingBond ?: if (previousAtom.aromatic && atom.aromatic) {
                    OrganicBondOrder.AROMATIC
                } else {
                    OrganicBondOrder.SINGLE
                }
                bonds += OrganicBond(previous, id, order)
            }
            current = id
            pendingBond = null
        }

        while (index < source.length) {
            when (val char = source[index]) {
                ' ', '\t', '\n' -> index++
                '-' -> { pendingBond = requireNoPendingBond(pendingBond, OrganicBondOrder.SINGLE); index++ }
                '=' -> { pendingBond = requireNoPendingBond(pendingBond, OrganicBondOrder.DOUBLE); index++ }
                '#' -> { pendingBond = requireNoPendingBond(pendingBond, OrganicBondOrder.TRIPLE); index++ }
                ':' -> { pendingBond = requireNoPendingBond(pendingBond, OrganicBondOrder.AROMATIC); index++ }
                '(' -> {
                    require(current != null) { "分支括号前需要原子。" }
                    branchStack.addLast(current)
                    index++
                }
                ')' -> {
                    require(branchStack.isNotEmpty()) { "分支右括号没有对应左括号。" }
                    require(pendingBond == null) { "分支结束前缺少原子。" }
                    current = branchStack.removeLast()
                    index++
                }
                '.' -> {
                    require(pendingBond == null) { "点号前的化学键缺少原子。" }
                    current = null
                    index++
                }
                in '0'..'9' -> {
                    val atom = current ?: error("环编号前需要原子。")
                    val ring = char.digitToInt()
                    val existing = ringAnchors.remove(ring)
                    if (existing == null) {
                        ringAnchors[ring] = RingAnchor(atom, pendingBond)
                    } else {
                        require(existing.atom != atom) { "环闭合不能连接原子自身。" }
                        val left = atoms[existing.atom.value]
                        val right = atoms[atom.value]
                        val order = pendingBond ?: existing.order ?: if (left.aromatic && right.aromatic) {
                            OrganicBondOrder.AROMATIC
                        } else {
                            OrganicBondOrder.SINGLE
                        }
                        require(bonds.none { it.connects(existing.atom, atom) }) { "环闭合重复定义了已有化学键。" }
                        bonds += OrganicBond(existing.atom, atom, order)
                    }
                    pendingBond = null
                    index++
                }
                '[' -> {
                    val closing = source.indexOf(']', startIndex = index + 1)
                    require(closing >= 0) { "方括号原子没有闭合。" }
                    addAtom(parseBracketAtom(source.substring(index + 1, closing)))
                    index = closing + 1
                }
                else -> {
                    val parsed = parseBareAtom(source, index)
                        ?: error("无法识别结构式字符“$char”。")
                    addAtom(parsed.atom)
                    index = parsed.nextIndex
                }
            }
            require(atoms.size <= 128 && bonds.size <= 192) { "结构式超出教学编辑器限制。" }
        }

        require(atoms.isNotEmpty()) { "结构式没有原子。" }
        require(branchStack.isEmpty()) { "分支括号没有闭合。" }
        require(ringAnchors.isEmpty()) { "存在未闭合的环编号：${ringAnchors.keys.joinToString()}。" }
        require(pendingBond == null) { "结构式结尾的化学键缺少原子。" }
        return OrganicMolecule(atoms, bonds, sourceNotation = source)
    }

    private fun requireNoPendingBond(
        current: OrganicBondOrder?,
        next: OrganicBondOrder,
    ): OrganicBondOrder {
        require(current == null) { "不能连续书写两个化学键符号。" }
        return next
    }

    private fun parseBareAtom(source: String, index: Int): ParsedBareAtom? {
        val first = source[index]
        if (first in listOf('c', 'n', 'o', 's', 'p')) {
            return ParsedBareAtom(
                ParsedAtom(first.uppercase(), aromatic = true),
                index + 1,
            )
        }
        if (!first.isUpperCase()) return null
        val nextIndex = if (index + 1 < source.length && source[index + 1].isLowerCase()) index + 2 else index + 1
        val symbol = source.substring(index, nextIndex)
        runCatching { PeriodicTable.bySymbol(symbol) }.getOrElse { return null }
        return ParsedBareAtom(ParsedAtom(symbol), nextIndex)
    }

    private fun parseBracketAtom(content: String): ParsedAtom {
        require(content.isNotBlank()) { "方括号中缺少原子。" }
        var index = 0
        val aromatic = content[index] in listOf('c', 'n', 'o', 's', 'p')
        val symbol = if (aromatic) {
            content[index++].uppercase()
        } else {
            require(content[index].isUpperCase()) { "方括号原子必须以元素符号开始。" }
            val start = index++
            if (index < content.length && content[index].isLowerCase()) index++
            content.substring(start, index)
        }
        PeriodicTable.bySymbol(symbol)

        var explicitHydrogens = 0
        if (index < content.length && content[index] == 'H') {
            index++
            val start = index
            while (index < content.length && content[index].isDigit()) index++
            explicitHydrogens = if (start == index) 1 else content.substring(start, index).toInt()
        }

        var charge = 0
        if (index < content.length) {
            val remainder = content.substring(index)
            charge = parseBracketCharge(remainder)
            index = content.length
        }
        require(index == content.length)
        return ParsedAtom(symbol, aromatic, explicitHydrogens, charge)
    }

    private fun parseBracketCharge(text: String): Int {
        Regex("^([+-])(\\d*)$").matchEntire(text)?.let { match ->
            val magnitude = match.groupValues[2].ifBlank { "1" }.toInt()
            return if (match.groupValues[1] == "+") magnitude else -magnitude
        }
        Regex("^(\\d+)([+-])$").matchEntire(text)?.let { match ->
            val magnitude = match.groupValues[1].toInt()
            return if (match.groupValues[2] == "+") magnitude else -magnitude
        }
        error("暂不支持方括号电荷“$text”。")
    }

    private data class RingAnchor(
        val atom: OrganicAtomId,
        val order: OrganicBondOrder?,
    )

    private data class ParsedAtom(
        val symbol: String,
        val aromatic: Boolean = false,
        val explicitHydrogens: Int = 0,
        val formalCharge: Int = 0,
    )

    private data class ParsedBareAtom(
        val atom: ParsedAtom,
        val nextIndex: Int,
    )
}
