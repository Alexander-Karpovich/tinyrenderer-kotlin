package com.akar.tinyrenderer

import ij.IJ
import ij.ImagePlus
import ij.process.ImageProcessor
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.stream.FileImageOutputStream
import kotlin.math.*

const val IMAGE_WIDTH = 2048
const val IMAGE_HEIGHT = 2048

fun main() {
    val image = IJ.createImage("result", "RGB", IMAGE_WIDTH, IMAGE_HEIGHT, 1)
    image.processor.setColor(Color.BLACK.rgb)
    val outputStream = FileImageOutputStream(File("result.gif"))
    val writer = GifSequenceWriter(outputStream, BufferedImage.TYPE_INT_RGB, 100, true)
    val model = parseObj("/obj/african_head/african_head.obj")
    model.normalizeVertices()

    for (i in 0..35) {
        val zbuffer = Array(IMAGE_HEIGHT) { DoubleArray(IMAGE_WIDTH) { Double.NEGATIVE_INFINITY } }
        image.processor.fill()
        println(i)
        val alfa = 2 * PI / 36 * i
        val rotation = Matrix(arrayOf(doubleArrayOf(cos(alfa), 0.0, sin(alfa)),
                doubleArrayOf(0.0, 1.0, 0.0),
                doubleArrayOf(-sin(alfa), 0.0, cos(alfa))))
        val vertices = model.vertices.asSequence().map {
            (rotation * it) * (IMAGE_WIDTH / 2 - 1).toDouble() + Vec3I(IMAGE_WIDTH / 2, IMAGE_WIDTH / 2, IMAGE_WIDTH / 2)
        }.toList()

        model.triangles.forEach {
            val v0 = vertices[it[0]]
            val v1 = vertices[it[1]]
            val v2 = vertices[it[2]]

            val side1 = v1 - v0
            val side2 = v2 - v0
            val intensity = side1.cross(side2).normalize().scalar(Vec3D(0.0, 0.0, 1.0))
            if (intensity > 0) {
                val steppedIntensity = intensityRange(intensity)
                val color = Color(steppedIntensity, steppedIntensity, steppedIntensity).rgb
                image.processor.triangle(v0, v1, v2, color, zbuffer)
            }

        }
        image.processor.flipVertical()

        writer.writeToSequence(image.processor.bufferedImage)
    }

    writer.close()
}

fun intensityRange(value: Double) = when (value) {
    in 0.0..0.4 -> 0.4f
    in 0.4..0.8 -> 0.8f
    else -> 1.0f
}

fun ImagePlus.line(x0: Int, y0: Int, x1: Int, y1: Int, color: Int) {
    var steep = false
    var _x0 = x0
    var _y0 = y0
    var _x1 = x1
    var _y1 = y1

    if (abs(_x0 - _x1) < abs(_y0 - _y1)) {
        _x0 = _y0.also { _y0 = _x0 }
        _x1 = _y1.also { _y1 = _x1 }
        steep = true
    }
    if (_x0 > _x1) {
        _x0 = _x1.also { _x1 = _x0 }
        _y0 = _y1.also { _y1 = _y0 }
    }
    val dx = _x1 - _x0
    val dy = _y1 - _y0
    val derror2 = abs(dy) * 2
    var error2 = 0
    var y = _y0
    for (x in _x0.._x1) {
        if (steep) {
            this.processor[y, x] = color
        } else {
            this.processor[x, y] = color
        }
        error2 += derror2
        if (error2 > dx) {
            y += if (_y1 > _y0) 1 else -1
            error2 -= dx * 2
        }
    }
}

fun ImageProcessor.triangle(v0: Vec3D, v1: Vec3D, v2: Vec3D, color: Int, zbuffer: Array<DoubleArray>) {
    val xes = doubleArrayOf(v0.x, v1.x, v2.x)
    val xmin = xes.min()!!
    val xmax = xes.max()!!

    val ys = doubleArrayOf(v0.y, v1.y, v2.y)
    val ymin = ys.min()!!
    val ymax = ys.max()!!

    for (x in ceil(xmin).toInt()..xmax.toInt()) {
        for (y: Int in ceil(ymin).toInt()..ymax.toInt()) {
            val bary = barycentric(Vec3D(x.toDouble(), y.toDouble(), 0.0), v0, v1, v2)
            if (bary.x < 0 || bary.y < 0 || bary.z < 0) continue
            val z = v0.z * bary.x + v1.z * bary.y + v2.z * bary.z
            if (zbuffer[x][y] < z) {
                zbuffer[x][y] = z
                this[x, y] = color
            }
        }
    }

}

fun barycentric(v0: Vec3D, v1: Vec3D, v2: Vec3D, v3: Vec3D): Vec3D {
    val denominator = (v2.y - v3.y) * (v1.x - v3.x) + (v3.x - v2.x) * (v1.y - v3.y)
    val l0 = ((v2.y - v3.y) * (v0.x - v3.x) + (v3.x - v2.x) * (v0.y - v3.y)) / denominator
    val l1 = ((v3.y - v1.y) * (v0.x - v3.x) + (v1.x - v3.x) * (v0.y - v3.y)) / denominator
    val l2 = 1 - l0 - l1
    return Vec3D(l0, l1, l2)
}
