import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.math.sqrt

fun main(args: Array<String>) {
    val inputFile: File
    val outputFile: File
    var function: (BufferedImage) -> Unit = {}
    try {
        function = when (args[0]) {
            "--negative" -> ::negateColours
            "--energy" -> ::generateImageEnergy
            "--no-op" -> { _ ->  }
            "--seam" -> ::generateSeam
            else -> throw IllegalArgumentException("Invalid argument for function")
        }
        inputFile = File(args[1])
        outputFile = File(args[2])
    } catch (e: IndexOutOfBoundsException) {
        throw IllegalArgumentException("Invalid command line argument(s)")
    }
    val image: BufferedImage = ImageIO.read(inputFile)
    function(image)
    ImageIO.write(image, inputFile.extension, outputFile)
}

fun negateColours(image: BufferedImage) {
    for (y in 0 until image.height) {
        for (x in 0 until image.width) {
            val color = Color(image.getRGB(x, y))
            image.setRGB(x, y, Color(255 - color.red, 255 - color.green, 255 - color.blue).rgb)
        }
    }
}

fun generateSeam(image: BufferedImage, vertical: Boolean = true) {
    val seamArray = getSeamPathArray(image)
    val lastRowIndex = seamArray.size - 1
    var x = seamArray[lastRowIndex].indexOf(seamArray[lastRowIndex].minOrNull())
    image.setRGB(x, lastRowIndex, Color.red.rgb)
    for (y in lastRowIndex downTo 1) {
        var smallestIndex = x
        if (x != 0) {
            smallestIndex = if (seamArray[y-1][smallestIndex] > seamArray[y-1][x-1]) x - 1 else smallestIndex
        }
        if (x != seamArray[y].size - 1) {
            smallestIndex = if (seamArray[y-1][smallestIndex] > seamArray[y-1][x+1]) x + 1 else smallestIndex
        }
        x = smallestIndex
        image.setRGB(x, y - 1, Color.red.rgb)
    }
}

private fun getSeamPathArray(image: BufferedImage): Array<Array<Double>>{
    val intensityArray = generateImageEnergy(image, false)
    for (y in 1 until intensityArray.size) {
        for (x in 0 until intensityArray[y].size) {
            var smallest = intensityArray[y - 1][x]
            if (x != 0) smallest = min(smallest, intensityArray[y - 1][x - 1])
            if (x != intensityArray[y].size - 1) smallest = min(smallest, intensityArray[y - 1][x + 1])
            intensityArray[y][x] += smallest
        }
    }
    return intensityArray
}

fun generateImageEnergy(image: BufferedImage, modify: Boolean = true): Array<Array<Double>> {
    val energyArray = Array(image.height) { Array(image.width) { 0.0 } }
    var maxEnergy = 0.0
    for (y in 0 until image.height) {
        for (x in 0 until image.width) {
            energyArray[y][x] = getPixelEnergy(image, x, y)
            maxEnergy = max(maxEnergy, energyArray[y][x])
        }
    }
    for (y in 0 until image.height) {
        for (x in 0 until image.width) {
            val intensityDouble = (255.0 * energyArray[y][x] / maxEnergy)
            val intensity = intensityDouble.toInt()
            if(modify) image.setRGB(x, y, Color(intensity, intensity, intensity).rgb)
            energyArray[y][x] = intensityDouble
        }
    }
    return energyArray
}

private fun getPixelEnergy(image: BufferedImage, x: Int, y: Int): Double {
    val shiftedX = when (x) {
        0 -> 1
        image.width - 1 -> x - 1
        else -> x
    }
    val shiftedY = when (y) {
        0 -> 1
        image.height - 1 -> y - 1
        else -> y
    }
    val leftPixelColor = Color(image.getRGB(shiftedX - 1, y))
    val rightPixelColor = Color(image.getRGB(shiftedX + 1, y))
    val deltaX = getColorDelta(leftPixelColor, rightPixelColor)

    val upPixelColor = Color(image.getRGB(x, shiftedY - 1))
    val downPixelColor = Color(image.getRGB(x, shiftedY + 1))
    val deltaY = getColorDelta(upPixelColor, downPixelColor)

    return sqrt((deltaX + deltaY).toDouble())
}

private fun getColorDelta(leftBoundaryColor: Color, rightBoundaryColor: Color): Int {
    val redDelta = leftBoundaryColor.red - rightBoundaryColor.red
    val greenDelta = leftBoundaryColor.green - rightBoundaryColor.green
    val blueDelta = leftBoundaryColor.blue - rightBoundaryColor.blue
    return redDelta * redDelta + greenDelta * greenDelta + blueDelta * blueDelta
}