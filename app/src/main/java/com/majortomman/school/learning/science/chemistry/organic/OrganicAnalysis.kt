package com.majortomman.school.learning.science.chemistry.organic

import com.majortomman.school.learning.science.chemistry.ChemicalElement
import java.math.BigInteger

enum class FunctionalGroupType(val label: String) {
    CARBON_CARBON_DOUBLE_BOND("碳碳双键"),
    CARBON_CARBON_TRIPLE_BOND("碳碳三键"),
    HALOGEN("卤素原子"),
    HYDROXYL("羟基"),
    ETHER("醚键"),
    ALDEHYDE("醛基"),
    KETONE("酮羰基"),
    CARBOXYL("羧基"),
    ESTER("酯基"),
    AMINO("氨基"),
    AMIDE("酰胺基"),
    AROMATIC_RING("芳香环"),
}

data class FunctionalGroupMatch(
    val type: FunctionalGroupType,
    val atomIds: Set<OrganicAtomId>,
    val description: String,
)

object FunctionalGroupDetector {
    fun detect(molecule: OrganicMolecule): List<FunctionalGroupMatch> {
        val matches = mutableListOf<FunctionalGroupMatch>()
        val carbonylCarbons = molecule.atoms.filter { atom ->
            atom.element.symbol == "C" && molecule.neighbors(atom.id).any { (neighbor, bond) ->
                neighbor.element.symbol == "O" && bond.order == OrganicBondOrder.DOUBLE
            }
        }.map { it.id }.toSet()

        molecule.bonds.forEach { bond ->
            val left = molecule.atom(bond.atomA)
            val right = molecule.atom(bond.atomB)
            if (left.element.symbol == "C" && right.element.symbol == "C") {
                when (bond.order) {
                    OrganicBondOrder.DOUBLE -> matches += FunctionalGroupMatch(
                        FunctionalGroupType.CARBON_CARBON_DOUBLE_BOND,
                        setOf(left.id, right.id),
                        "两个碳原子之间含一个 σ 键和一个 π 键。",
                    )
                    OrganicBondOrder.TRIPLE -> matches += FunctionalGroupMatch(
                        FunctionalGroupType.CARBON_CARBON_TRIPLE_BOND,
                        setOf(left.id, right.id),
                        "两个碳原子之间含一个 σ 键和两个 π 键。",
                    )
                    else -> Unit
                }
            }
        }

        molecule.atoms.filter { it.element.symbol in setOf("F", "Cl", "Br", "I") }.forEach { halogen ->
            val carbon = molecule.neighbors(halogen.id).firstOrNull { it.first.element.symbol == "C" }?.first
            if (carbon != null) {
                matches += FunctionalGroupMatch(
                    FunctionalGroupType.HALOGEN,
                    setOf(halogen.id, carbon.id),
                    "卤素原子 ${halogen.element.symbol} 与碳骨架相连。",
                )
            }
        }

        molecule.atoms.filter { it.element.symbol == "O" }.forEach { oxygen ->
            val singleCarbonNeighbors = molecule.neighbors(oxygen.id)
                .filter { (neighbor, bond) -> neighbor.element.symbol == "C" && bond.order == OrganicBondOrder.SINGLE }
                .map { it.first }
            val attachedToCarbonyl = singleCarbonNeighbors.any { it.id in carbonylCarbons }
            when {
                molecule.implicitHydrogenCount(oxygen.id) > 0 && !attachedToCarbonyl -> {
                    matches += FunctionalGroupMatch(
                        FunctionalGroupType.HYDROXYL,
                        setOf(oxygen.id) + singleCarbonNeighbors.map { it.id },
                        "氧原子以单键连接碳，并保留一个隐式氢。",
                    )
                }
                singleCarbonNeighbors.size == 2 && !attachedToCarbonyl -> {
                    matches += FunctionalGroupMatch(
                        FunctionalGroupType.ETHER,
                        setOf(oxygen.id) + singleCarbonNeighbors.map { it.id },
                        "氧原子以单键连接两个碳原子。",
                    )
                }
            }
        }

        carbonylCarbons.forEach { carbonId ->
            val carbon = molecule.atom(carbonId)
            val neighbors = molecule.neighbors(carbonId)
            val doubleOxygen = neighbors.first { (neighbor, bond) ->
                neighbor.element.symbol == "O" && bond.order == OrganicBondOrder.DOUBLE
            }.first
            val singleOxygen = neighbors.firstOrNull { (neighbor, bond) ->
                neighbor.element.symbol == "O" && bond.order == OrganicBondOrder.SINGLE
            }?.first
            val nitrogen = neighbors.firstOrNull { (neighbor, bond) ->
                neighbor.element.symbol == "N" && bond.order == OrganicBondOrder.SINGLE
            }?.first
            val carbonNeighbors = neighbors.filter { it.first.element.symbol == "C" }.map { it.first }

            when {
                singleOxygen != null && molecule.implicitHydrogenCount(singleOxygen.id) > 0 -> {
                    matches += FunctionalGroupMatch(
                        FunctionalGroupType.CARBOXYL,
                        setOf(carbon.id, doubleOxygen.id, singleOxygen.id),
                        "同一碳原子同时连接羰基氧和羟基氧。",
                    )
                }
                singleOxygen != null && molecule.neighbors(singleOxygen.id).any {
                    it.first.element.symbol == "C" && it.first.id != carbon.id
                } -> {
                    matches += FunctionalGroupMatch(
                        FunctionalGroupType.ESTER,
                        setOf(carbon.id, doubleOxygen.id, singleOxygen.id) +
                            molecule.neighbors(singleOxygen.id).filter { it.first.element.symbol == "C" }.map { it.first.id },
                        "羰基碳通过单键氧继续连接另一个碳骨架。",
                    )
                }
                nitrogen != null -> {
                    matches += FunctionalGroupMatch(
                        FunctionalGroupType.AMIDE,
                        setOf(carbon.id, doubleOxygen.id, nitrogen.id),
                        "羰基碳以单键连接氮原子。",
                    )
                }
                molecule.implicitHydrogenCount(carbon.id) > 0 && carbonNeighbors.size <= 1 -> {
                    matches += FunctionalGroupMatch(
                        FunctionalGroupType.ALDEHYDE,
                        setOf(carbon.id, doubleOxygen.id) + carbonNeighbors.map { it.id },
                        "羰基碳保留氢，构成醛基。",
                    )
                }
                carbonNeighbors.size >= 2 -> {
                    matches += FunctionalGroupMatch(
                        FunctionalGroupType.KETONE,
                        setOf(carbon.id, doubleOxygen.id) + carbonNeighbors.map { it.id },
                        "羰基碳连接两个碳骨架。",
                    )
                }
            }
        }

        molecule.atoms.filter { it.element.symbol == "N" }.forEach { nitrogen ->
            val attachedCarbonyl = molecule.neighbors(nitrogen.id).any { it.first.id in carbonylCarbons }
            val carbonNeighbors = molecule.neighbors(nitrogen.id).filter { it.first.element.symbol == "C" }
            if (!attachedCarbonyl && carbonNeighbors.isNotEmpty()) {
                matches += FunctionalGroupMatch(
                    FunctionalGroupType.AMINO,
                    setOf(nitrogen.id) + carbonNeighbors.map { it.first.id },
                    "氮原子与碳骨架相连，且不属于酰胺结构。",
                )
            }
        }

        val aromaticAtoms = molecule.atoms.filter(OrganicAtom::aromatic)
        if (aromaticAtoms.size >= 5 && aromaticAtoms.all { atom ->
                molecule.neighbors(atom.id).count { it.first.aromatic && it.second.order == OrganicBondOrder.AROMATIC } >= 2
            }
        ) {
            matches += FunctionalGroupMatch(
                FunctionalGroupType.AROMATIC_RING,
                aromaticAtoms.map { it.id }.toSet(),
                "芳香原子通过芳香键形成闭合共轭环。",
            )
        }
        return matches.distinctBy { it.type to it.atomIds }
    }
}

data class IsomerComparison(
    val sameMolecularFormula: Boolean,
    val sameConnectivity: Boolean,
    val constitutionalIsomers: Boolean,
    val message: String,
)

object OrganicIsomerAnalyzer {
    fun compare(left: OrganicMolecule, right: OrganicMolecule): IsomerComparison {
        val sameFormula = left.elementalComposition() == right.elementalComposition() &&
            left.totalFormalCharge == right.totalFormalCharge
        val sameConnectivity = sameFormula && MoleculeIsomorphism.areIsomorphic(left, right)
        return IsomerComparison(
            sameMolecularFormula = sameFormula,
            sameConnectivity = sameConnectivity,
            constitutionalIsomers = sameFormula && !sameConnectivity,
            message = when {
                !sameFormula -> "两个结构的分子式不同，不属于同分异构体。"
                sameConnectivity -> "两个写法表示相同的原子连接关系。"
                else -> "分子式相同但原子连接关系不同，属于构造异构体。"
            },
        )
    }
}

object MoleculeIsomorphism {
    fun areIsomorphic(left: OrganicMolecule, right: OrganicMolecule): Boolean {
        if (left.atoms.size != right.atoms.size || left.bonds.size != right.bonds.size) return false
        if (left.elementalComposition() != right.elementalComposition()) return false
        if (left.totalFormalCharge != right.totalFormalCharge) return false

        val candidates = left.atoms.associate { atom ->
            atom.id to right.atoms.filter { candidate -> compatibleAtom(left, atom, right, candidate) }.map { it.id }
        }
        if (candidates.values.any(List<OrganicAtomId>::isEmpty)) return false
        val order = left.atoms.sortedWith(
            compareBy<OrganicAtom> { candidates.getValue(it.id).size }
                .thenByDescending { left.bondsOf(it.id).size },
        )
        val mapping = mutableMapOf<OrganicAtomId, OrganicAtomId>()
        val used = mutableSetOf<OrganicAtomId>()

        fun search(index: Int): Boolean {
            if (index == order.size) return true
            val sourceAtom = order[index]
            for (targetId in candidates.getValue(sourceAtom.id)) {
                if (targetId in used) continue
                if (!consistentWithMappedNeighbors(left, sourceAtom.id, right, targetId, mapping)) continue
                mapping[sourceAtom.id] = targetId
                used += targetId
                if (search(index + 1)) return true
                mapping.remove(sourceAtom.id)
                used -= targetId
            }
            return false
        }
        return search(0)
    }

    private fun compatibleAtom(
        left: OrganicMolecule,
        leftAtom: OrganicAtom,
        right: OrganicMolecule,
        rightAtom: OrganicAtom,
    ): Boolean =
        leftAtom.element == rightAtom.element &&
            leftAtom.formalCharge == rightAtom.formalCharge &&
            leftAtom.aromatic == rightAtom.aromatic &&
            leftAtom.explicitHydrogens == rightAtom.explicitHydrogens &&
            left.implicitHydrogenCount(leftAtom.id) == right.implicitHydrogenCount(rightAtom.id) &&
            left.bondsOf(leftAtom.id).map { it.order }.sortedBy { it.ordinal } ==
            right.bondsOf(rightAtom.id).map { it.order }.sortedBy { it.ordinal }

    private fun consistentWithMappedNeighbors(
        left: OrganicMolecule,
        leftId: OrganicAtomId,
        right: OrganicMolecule,
        rightId: OrganicAtomId,
        mapping: Map<OrganicAtomId, OrganicAtomId>,
    ): Boolean {
        return mapping.all { (mappedLeft, mappedRight) ->
            val leftBond = left.bondBetween(leftId, mappedLeft)
            val rightBond = right.bondBetween(rightId, mappedRight)
            when {
                leftBond == null && rightBond == null -> true
                leftBond == null || rightBond == null -> false
                else -> leftBond.order == rightBond.order
            }
        }
    }
}

data class OrganicReactionTemplate(
    val id: String,
    val reactionClass: String,
    val reactants: List<OrganicMolecule>,
    val products: List<OrganicMolecule>,
    val conditions: List<String>,
) {
    init {
        require(id.isNotBlank())
        require(reactants.isNotEmpty() && products.isNotEmpty())
        require(conservation().balanced) { "有机反应模板必须满足元素和电荷守恒。" }
    }

    fun conservation(): OrganicReactionConservation {
        val left = totalComposition(reactants)
        val right = totalComposition(products)
        val leftCharge = reactants.sumOf(OrganicMolecule::totalFormalCharge)
        val rightCharge = products.sumOf(OrganicMolecule::totalFormalCharge)
        return OrganicReactionConservation(
            balanced = left == right && leftCharge == rightCharge,
            reactantComposition = left,
            productComposition = right,
            reactantCharge = leftCharge,
            productCharge = rightCharge,
        )
    }

    fun matchesReactants(actual: List<OrganicMolecule>): Boolean =
        matchMoleculeMultiset(reactants, actual)

    private fun matchMoleculeMultiset(expected: List<OrganicMolecule>, actual: List<OrganicMolecule>): Boolean {
        if (expected.size != actual.size) return false
        val used = BooleanArray(actual.size)
        fun search(index: Int): Boolean {
            if (index == expected.size) return true
            actual.indices.forEach { actualIndex ->
                if (!used[actualIndex] && MoleculeIsomorphism.areIsomorphic(expected[index], actual[actualIndex])) {
                    used[actualIndex] = true
                    if (search(index + 1)) return true
                    used[actualIndex] = false
                }
            }
            return false
        }
        return search(0)
    }

    private fun totalComposition(molecules: List<OrganicMolecule>): Map<ChemicalElement, BigInteger> {
        val result = linkedMapOf<ChemicalElement, BigInteger>()
        molecules.forEach { molecule ->
            molecule.elementalComposition().forEach { (element, count) ->
                result[element] = (result[element] ?: BigInteger.ZERO) + count
            }
        }
        return result.toSortedMap(compareBy { it.atomicNumber })
    }
}

data class OrganicReactionConservation(
    val balanced: Boolean,
    val reactantComposition: Map<ChemicalElement, BigInteger>,
    val productComposition: Map<ChemicalElement, BigInteger>,
    val reactantCharge: Int,
    val productCharge: Int,
)

object TextbookOrganicReactionTemplates {
    val etheneHydrogenation: OrganicReactionTemplate by lazy {
        OrganicReactionTemplate(
            id = "ethene_hydrogenation",
            reactionClass = "加成反应",
            reactants = listOf(
                OrganicNotationParser.parse("C=C"),
                OrganicNotationParser.parse("H-H"),
            ),
            products = listOf(OrganicNotationParser.parse("CC")),
            conditions = listOf("催化剂", "适当温度"),
        )
    }
}
