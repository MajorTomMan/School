package com.majortomman.school.learning.science.math

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

data class Vector2(val x: Double, val y: Double) {
    operator fun plus(other: Vector2) = Vector2(x + other.x, y + other.y)
    operator fun minus(other: Vector2) = Vector2(x - other.x, y - other.y)
    operator fun times(scale: Double) = Vector2(x * scale, y * scale)
    operator fun div(scale: Double): Vector2 {
        require(abs(scale) > EPSILON) { "向量不能除以 0。" }
        return Vector2(x / scale, y / scale)
    }

    fun dot(other: Vector2): Double = x * other.x + y * other.y
    fun cross(other: Vector2): Double = x * other.y - y * other.x
    fun magnitude(): Double = hypot(x, y)
    fun normalized(): Vector2 = this / magnitude().also { require(it > EPSILON) { "零向量没有方向。" } }
    fun projectionOnto(other: Vector2): Vector2 {
        val denominator = other.dot(other)
        require(denominator > EPSILON) { "不能投影到零向量。" }
        return other * (dot(other) / denominator)
    }

    fun angleTo(other: Vector2): Double {
        val denominator = magnitude() * other.magnitude()
        require(denominator > EPSILON) { "零向量不能参与夹角计算。" }
        return acos((dot(other) / denominator).coerceIn(-1.0, 1.0))
    }

    companion object {
        const val EPSILON = 1e-10
    }
}

data class Point2(val x: Double, val y: Double) {
    operator fun plus(vector: Vector2) = Point2(x + vector.x, y + vector.y)
    operator fun minus(other: Point2) = Vector2(x - other.x, y - other.y)
    fun distanceTo(other: Point2): Double = (this - other).magnitude()
}

data class Line2(val point: Point2, val direction: Vector2) {
    init {
        require(direction.magnitude() > Vector2.EPSILON) { "直线方向向量不能为零。" }
    }

    fun contains(candidate: Point2, tolerance: Double = 1e-8): Boolean =
        abs((candidate - point).cross(direction)) <= tolerance * direction.magnitude()

    fun distanceTo(candidate: Point2): Double =
        abs((candidate - point).cross(direction)) / direction.magnitude()

    fun intersection(other: Line2): Point2? {
        val denominator = direction.cross(other.direction)
        if (abs(denominator) <= Vector2.EPSILON) return null
        val parameter = (other.point - point).cross(other.direction) / denominator
        return point + direction * parameter
    }

    fun isParallelTo(other: Line2): Boolean = abs(direction.cross(other.direction)) <= Vector2.EPSILON
    fun isPerpendicularTo(other: Line2): Boolean = abs(direction.dot(other.direction)) <= Vector2.EPSILON
}

data class Circle2(val center: Point2, val radius: Double) {
    init {
        require(radius >= 0.0) { "圆的半径不能为负数。" }
    }

    fun contains(point: Point2, tolerance: Double = 1e-8): Boolean =
        point.distanceTo(center) <= radius + tolerance

    fun isOnBoundary(point: Point2, tolerance: Double = 1e-8): Boolean =
        abs(point.distanceTo(center) - radius) <= tolerance
}

data class Transform2(
    val a: Double,
    val b: Double,
    val c: Double,
    val d: Double,
    val tx: Double,
    val ty: Double,
) {
    fun apply(point: Point2): Point2 = Point2(
        a * point.x + b * point.y + tx,
        c * point.x + d * point.y + ty,
    )

    fun then(next: Transform2): Transform2 = Transform2(
        a = next.a * a + next.b * c,
        b = next.a * b + next.b * d,
        c = next.c * a + next.d * c,
        d = next.c * b + next.d * d,
        tx = next.a * tx + next.b * ty + next.tx,
        ty = next.c * tx + next.d * ty + next.ty,
    )

    companion object {
        fun identity() = Transform2(1.0, 0.0, 0.0, 1.0, 0.0, 0.0)
        fun translation(dx: Double, dy: Double) = Transform2(1.0, 0.0, 0.0, 1.0, dx, dy)
        fun scale(sx: Double, sy: Double = sx) = Transform2(sx, 0.0, 0.0, sy, 0.0, 0.0)
        fun rotation(radians: Double) = Transform2(
            cos(radians), -sin(radians),
            sin(radians), cos(radians),
            0.0, 0.0,
        )
        fun reflectionAcrossYAxis() = Transform2(-1.0, 0.0, 0.0, 1.0, 0.0, 0.0)
    }
}

data class Vector3(val x: Double, val y: Double, val z: Double) {
    operator fun plus(other: Vector3) = Vector3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vector3) = Vector3(x - other.x, y - other.y, z - other.z)
    operator fun times(scale: Double) = Vector3(x * scale, y * scale, z * scale)
    operator fun div(scale: Double): Vector3 {
        require(abs(scale) > EPSILON) { "向量不能除以 0。" }
        return Vector3(x / scale, y / scale, z / scale)
    }

    fun dot(other: Vector3): Double = x * other.x + y * other.y + z * other.z
    fun cross(other: Vector3): Vector3 = Vector3(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x,
    )
    fun magnitude(): Double = sqrt(dot(this))
    fun normalized(): Vector3 = this / magnitude().also { require(it > EPSILON) { "零向量没有方向。" } }
    fun projectionOnto(other: Vector3): Vector3 {
        val denominator = other.dot(other)
        require(denominator > EPSILON) { "不能投影到零向量。" }
        return other * (dot(other) / denominator)
    }

    companion object {
        const val EPSILON = 1e-10
    }
}

data class Point3(val x: Double, val y: Double, val z: Double) {
    operator fun plus(vector: Vector3) = Point3(x + vector.x, y + vector.y, z + vector.z)
    operator fun minus(other: Point3) = Vector3(x - other.x, y - other.y, z - other.z)
    fun distanceTo(other: Point3): Double = (this - other).magnitude()
}

data class Plane3(val point: Point3, val normal: Vector3) {
    init {
        require(normal.magnitude() > Vector3.EPSILON) { "平面法向量不能为零。" }
    }

    fun signedDistanceTo(candidate: Point3): Double =
        (candidate - point).dot(normal.normalized())

    fun projectionOf(candidate: Point3): Point3 =
        candidate + normal.normalized() * -signedDistanceTo(candidate)
}

enum class ProofStepKind {
    GIVEN,
    DEFINITION,
    THEOREM,
    INFERENCE,
    EQUIVALENT_TRANSFORMATION,
    CONCLUSION,
}

data class ProofStep(
    val kind: ProofStepKind,
    val statement: String,
    val reason: String,
    val dependencies: Set<String> = emptySet(),
    val producedFactId: String? = null,
)

data class ProofValidation(
    val valid: Boolean,
    val missingDependencies: Set<String>,
    val duplicateFacts: Set<String>,
    val message: String,
)

object ProofStructureValidator {
    fun validate(steps: List<ProofStep>): ProofValidation {
        val known = mutableSetOf<String>()
        val missing = mutableSetOf<String>()
        val duplicates = mutableSetOf<String>()
        steps.forEach { step ->
            missing += step.dependencies - known
            step.producedFactId?.let { fact ->
                if (!known.add(fact)) duplicates += fact
            }
        }
        val valid = missing.isEmpty() && duplicates.isEmpty()
        return ProofValidation(
            valid = valid,
            missingDependencies = missing,
            duplicateFacts = duplicates,
            message = when {
                missing.isNotEmpty() -> "证明步骤引用了尚未得到的条件：${missing.joinToString()}。"
                duplicates.isNotEmpty() -> "证明中重复定义了事实：${duplicates.joinToString()}。"
                else -> "证明步骤依赖关系完整。"
            },
        )
    }
}
