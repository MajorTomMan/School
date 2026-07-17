package com.majortomman.school.learning.science.chemistry.organic

import com.majortomman.school.learning.science.chemistry.ChemicalElement
import com.majortomman.school.learning.science.chemistry.PeriodicTable
import java.math.BigInteger
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@JvmInline
value class OrganicAtomId(val value: Int) : Comparable<OrganicAtomId> {
    init { require(value >= 0) }
    override fun compareTo(other: OrganicAtomId): Int = value.compareTo(other.value)
}

enum class OrganicBondOrder(val valenceUnits: Int, val symbol: String) {
    SINGLE(2, "—"),
    DOUBLE(4, "="),
    TRIPLE(6, "≡"),
    AROMATIC(3, "⌁"),
}

data class OrganicAtom(
    val id: OrganicAtomId,
    val element: ChemicalElement,
    val formalCharge: Int = 0,
    val aromatic: Boolean = false,
    val explicitHydrogens: Int = 0,
) {
    init {
        require(explicitHydrogens >= 0)
        require(formalCharge in -4..4)
    }
}

data class OrganicBond(
    val atomA: OrganicAtomId,
    val atomB: OrganicAtomId,
    val order: OrganicBondOrder,
) {
    init { require(atomA != atomB) { "化学键不能连接同一个原子。" } }

    fun connects(left: OrganicAtomId, right: OrganicAtomId): Boolean =
        (atomA == left && atomB == right) || (atomA == right && atomB == left)

    fun other(atom: OrganicAtomId): OrganicAtomId = when (atom) {
        atomA -> atomB
        atomB -> atomA
        else -> error("原子 $atom 不在这条键上。")
    }
}

data class ValenceIssue(
    val atomId: OrganicAtomId,
    val message: String,
)

data class OrganicMolecule(
    val atoms: List<OrganicAtom>,
    val bonds: List<OrganicBond>,
    val sourceNotation: String? = null,
) {
    init {
        require(atoms.isNotEmpty()) { "分子至少需要一个原子。" }
        require(atoms.size <= 128) { "分子超出教学编辑器的原子数限制。" }
        require(atoms.map { it.id }.toSet().size == atoms.size) { "原子 ID 不能重复。" }
        val ids = atoms.map { it.id }.toSet()
        require(bonds.all { it.atomA in ids && it.atomB in ids }) { "化学键引用了不存在的原子。" }
        require(bonds.map { normalizedBondKey(it) }.toSet().size == bonds.size) { "两个原子之间不能重复定义化学键。" }
    }

    private val atomById: Map<OrganicAtomId, OrganicAtom> = atoms.associateBy { it.id }

    fun atom(id: OrganicAtomId): OrganicAtom = atomById[id] ?: error("找不到原子 $id。")

    fun bondsOf(id: OrganicAtomId): List<OrganicBond> = bonds.filter { it.atomA == id || it.atomB == id }

    fun neighbors(id: OrganicAtomId): List<Pair<OrganicAtom, OrganicBond>> =
        bondsOf(id).map { bond -> atom(bond.other(id)) to bond }

    fun bondBetween(left: OrganicAtomId, right: OrganicAtomId): OrganicBond? =
        bonds.firstOrNull { it.connects(left, right) }

    fun connectedComponents(): List<Set<OrganicAtomId>> {
        val remaining = atoms.map { it.id }.toMutableSet()
        val components = mutableListOf<Set<OrganicAtomId>>()
        while (remaining.isNotEmpty()) {
            val start = remaining.first()
            val visited = mutableSetOf(start)
            val queue = ArrayDeque<OrganicAtomId>()
            queue += start
            while (queue.isNotEmpty()) {
                val current = queue.removeFirst()
                neighbors(current).forEach { (neighbor, _) ->
                    if (visited.add(neighbor.id)) queue += neighbor.id
                }
            }
            remaining.removeAll(visited)
            components += visited
        }
        return components
    }

    fun implicitHydrogenCount(atomId: OrganicAtomId): Int {
        val atom = atom(atomId)
        if (atom.element.symbol == "H") return 0
        val target = targetValenceUnits(atom)
        val occupied = bondsOf(atomId).sumOf { it.order.valenceUnits } + atom.explicitHydrogens * 2
        return ((target - occupied).coerceAtLeast(0) / 2)
    }

    fun elementalComposition(includeImplicitHydrogens: Boolean = true): Map<ChemicalElement, BigInteger> {
        val counts = linkedMapOf<ChemicalElement, BigInteger>()
        atoms.forEach { atom ->
            counts[atom.element] = (counts[atom.element] ?: BigInteger.ZERO) + BigInteger.ONE
            val hydrogens = atom.explicitHydrogens + if (includeImplicitHydrogens) implicitHydrogenCount(atom.id) else 0
            if (hydrogens > 0) {
                val hydrogen = PeriodicTable.bySymbol("H")
                counts[hydrogen] = (counts[hydrogen] ?: BigInteger.ZERO) + BigInteger.valueOf(hydrogens.toLong())
            }
        }
        return counts.toSortedMap(compareBy { it.atomicNumber })
    }

    val totalFormalCharge: Int get() = atoms.sumOf(OrganicAtom::formalCharge)

    fun molecularFormula(): String {
        val counts = elementalComposition()
        val carbon = PeriodicTable.bySymbol("C")
        val hydrogen = PeriodicTable.bySymbol("H")
        val ordered = buildList {
            counts[carbon]?.let { add(carbon to it) }
            counts[hydrogen]?.let { add(hydrogen to it) }
            addAll(counts.entries.filter { it.key != carbon && it.key != hydrogen }.sortedBy { it.key.symbol }.map { it.toPair() })
        }
        return ordered.joinToString("") { (element, count) ->
            element.symbol + if (count == BigInteger.ONE) "" else count.toString()
        } + when {
            totalFormalCharge > 0 -> "^${if (totalFormalCharge == 1) "" else totalFormalCharge}+"
            totalFormalCharge < 0 -> "^${if (totalFormalCharge == -1) "" else -totalFormalCharge}-"
            else -> ""
        }
    }

    fun validateValence(): List<ValenceIssue> = atoms.mapNotNull { atom ->
        val target = targetValenceUnits(atom)
        val occupied = bondsOf(atom.id).sumOf { it.order.valenceUnits } + atom.explicitHydrogens * 2
        when {
            occupied > target -> ValenceIssue(
                atom.id,
                "${atom.element.symbol} 原子 ${atom.id.value} 的键级总和超过当前简化价态允许值。",
            )
            (target - occupied) % 2 != 0 -> ValenceIssue(
                atom.id,
                "${atom.element.symbol} 原子 ${atom.id.value} 无法用整数个氢原子补足当前价态。",
            )
            else -> null
        }
    }

    fun carbonSkeleton(): CarbonSkeleton {
        val carbonIds = atoms.filter { it.element.symbol == "C" }.map { it.id }.toSet()
        if (carbonIds.isEmpty()) return CarbonSkeleton(emptyList(), emptyList(), emptyList())
        var longest = emptyList<OrganicAtomId>()

        fun dfs(current: OrganicAtomId, visited: MutableList<OrganicAtomId>) {
            if (visited.size > longest.size) longest = visited.toList()
            neighbors(current)
                .map { it.first.id }
                .filter { it in carbonIds && it !in visited }
                .forEach { next ->
                    visited += next
                    dfs(next, visited)
                    visited.removeAt(visited.lastIndex)
                }
        }
        carbonIds.forEach { dfs(it, mutableListOf(it)) }
        val positions = longest.withIndex().associate { it.value to it.index + 1 }
        val doubleBonds = bonds.filter { it.order == OrganicBondOrder.DOUBLE && it.atomA in positions && it.atomB in positions }
            .map { minOf(positions.getValue(it.atomA), positions.getValue(it.atomB)) }
            .sorted()
        val tripleBonds = bonds.filter { it.order == OrganicBondOrder.TRIPLE && it.atomA in positions && it.atomB in positions }
            .map { minOf(positions.getValue(it.atomA), positions.getValue(it.atomB)) }
            .sorted()
        return CarbonSkeleton(longest, doubleBonds, tripleBonds)
    }

    private fun targetValenceUnits(atom: OrganicAtom): Int = when (atom.element.symbol) {
        "H", "F", "Cl", "Br", "I" -> 2
        "C" -> if (atom.aromatic) 8 else 8
        "N" -> when {
            atom.formalCharge > 0 -> 8
            atom.aromatic -> 6
            else -> 6
        }
        "O" -> if (atom.formalCharge > 0) 6 else 4
        "S" -> if (bondsOf(atom.id).sumOf { it.order.valenceUnits } > 4) 12 else 4
        "P" -> 10
        else -> 8
    }

    companion object {
        private fun normalizedBondKey(bond: OrganicBond): Triple<Int, Int, OrganicBondOrder> =
            Triple(minOf(bond.atomA.value, bond.atomB.value), maxOf(bond.atomA.value, bond.atomB.value), bond.order)
    }
}

data class CarbonSkeleton(
    val longestChain: List<OrganicAtomId>,
    val doubleBondPositions: List<Int>,
    val tripleBondPositions: List<Int>,
)

data class MoleculeLayoutPoint(
    val atomId: OrganicAtomId,
    val x: Double,
    val y: Double,
)

object OrganicMoleculeLayout {
    fun layout(molecule: OrganicMolecule, iterations: Int = 90): List<MoleculeLayoutPoint> {
        val atoms = molecule.atoms.sortedBy { it.id.value }
        if (atoms.size == 1) return listOf(MoleculeLayoutPoint(atoms.single().id, 0.5, 0.5))
        val positions = atoms.associate { atom ->
            val angle = 2.0 * PI * atom.id.value / atoms.size
            atom.id to doubleArrayOf(cos(angle), sin(angle))
        }.toMutableMap()
        repeat(iterations.coerceIn(10, 200)) {
            val forces = atoms.associate { it.id to doubleArrayOf(0.0, 0.0) }.toMutableMap()
            for (leftIndex in atoms.indices) {
                for (rightIndex in leftIndex + 1 until atoms.size) {
                    val left = positions.getValue(atoms[leftIndex].id)
                    val right = positions.getValue(atoms[rightIndex].id)
                    val dx = left[0] - right[0]
                    val dy = left[1] - right[1]
                    val distance = hypot(dx, dy).coerceAtLeast(0.05)
                    val force = 0.018 / (distance * distance)
                    forces.getValue(atoms[leftIndex].id)[0] += dx / distance * force
                    forces.getValue(atoms[leftIndex].id)[1] += dy / distance * force
                    forces.getValue(atoms[rightIndex].id)[0] -= dx / distance * force
                    forces.getValue(atoms[rightIndex].id)[1] -= dy / distance * force
                }
            }
            molecule.bonds.forEach { bond ->
                val left = positions.getValue(bond.atomA)
                val right = positions.getValue(bond.atomB)
                val dx = right[0] - left[0]
                val dy = right[1] - left[1]
                val distance = hypot(dx, dy).coerceAtLeast(0.05)
                val preferred = if (bond.order == OrganicBondOrder.TRIPLE) 0.75 else 0.9
                val force = (distance - preferred) * 0.05
                forces.getValue(bond.atomA)[0] += dx / distance * force
                forces.getValue(bond.atomA)[1] += dy / distance * force
                forces.getValue(bond.atomB)[0] -= dx / distance * force
                forces.getValue(bond.atomB)[1] -= dy / distance * force
            }
            atoms.forEach { atom ->
                val position = positions.getValue(atom.id)
                val force = forces.getValue(atom.id)
                position[0] += force[0].coerceIn(-0.08, 0.08)
                position[1] += force[1].coerceIn(-0.08, 0.08)
            }
        }
        val xs = positions.values.map { it[0] }
        val ys = positions.values.map { it[1] }
        val minX = xs.minOrNull()!!
        val maxX = xs.maxOrNull()!!
        val minY = ys.minOrNull()!!
        val maxY = ys.maxOrNull()!!
        val width = (maxX - minX).coerceAtLeast(1e-6)
        val height = (maxY - minY).coerceAtLeast(1e-6)
        return atoms.map { atom ->
            val position = positions.getValue(atom.id)
            MoleculeLayoutPoint(
                atomId = atom.id,
                x = 0.1 + 0.8 * (position[0] - minX) / width,
                y = 0.1 + 0.8 * (position[1] - minY) / height,
            )
        }
    }
}
