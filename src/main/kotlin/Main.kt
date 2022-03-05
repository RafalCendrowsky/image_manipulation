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
    val function: (BufferedImage) -> Any
    var resize = false
    try {
        when (args[0]) {
            "--negative" -> function = ::negateColours
            "--energy" -> function = { generateImageEnergy(it) }
            "--no-op" -> function = { _ ->  }
            "--seam-v" -> function = { generateVerticalSeam(it) }
            "--seam-h" -> function = { generateHorizontalSeam(it) }
            "--seam-c" -> {
                resize = true
                val width = args[3].toInt()
                val height = args[4].toInt()
                function = { resizeImage(it, width, height) }
            }
            else -> throw IllegalArgumentException("Invalid argument for function")
        }
        inputFile = File(args[1])
        outputFile = File(args[2])
    } catch (e: IndexOutOfBoundsException) {
        throw IllegalArgumentException("Invalid command line argument count")
    } catch (e: NumberFormatException) {
        throw IllegalArgumentException("Invalid argument for width or height")
    }
    var image: BufferedImage = ImageIO.read(inputFile)
    if (resize) {
        image = function(image) as BufferedImage
    } else {
        function(image)
    }
    ImageIO.write(image, inputFile.extension, outputFile)
}

fun resizeImage(image: BufferedImage, width: Int, height: Int): BufferedImage {
    var mutableImage = image
    mutableImage = resizeVertical(mutableImage, height)
    mutableImage = resizeHorizontal(mutableImage, width)
    return mutableImage
}

fun resizeVertical(image: BufferedImage, height: Int): BufferedImage{
    var mutableImage = image
    while (mutableImage.height > image.height - height && mutableImage.height > 3) {
        val seamArray = generateHorizontalSeam(mutableImage)
        val newImage = BufferedImage(mutableImage.width, mutableImage.height - 1, BufferedImage.TYPE_INT_ARGB)
        for (x in 0 until mutableImage.width) {
            var shift = 0
            for (y in 0 until mutableImage.height) {
                if (seamArray[x] == y) {
                    shift = 1
                    continue
                }
                newImage.setRGB(x, y - shift, mutableImage.getRGB(x, y))
            }
        }
        mutableImage = newImage
    }
    return mutableImage
}

fun resizeHorizontal(image: BufferedImage, width: Int): BufferedImage {
    var mutableImage = image
    while (mutableImage.width > image.width - width && mutableImage.width > 3) {
        val seamArray = generateVerticalSeam(mutableImage)
        val newImage = BufferedImage(mutableImage.width - 1, mutableImage.height, BufferedImage.TYPE_INT_RGB)
        for (y in 0 until mutableImage.height) {
            var shift = 0
            for (x in 0 until mutableImage.width) {
                if (seamArray[y] == x) {
                    shift = 1
                    continue
                }
                newImage.setRGB(x - shift, y, mutableImage.getRGB(x, y))
            }
        }
        mutableImage = newImage
    }
    return mutableImage
}

fun negateColours(image: BufferedImage) {
    for (y in 0 until image.height) {
        for (x in 0 until image.width) {
            val color = Color(image.getRGB(x, y))
            image.setRGB(x, y, Color(255 - color.red, 255 - color.green, 255 - color.blue).rgb)
        }
    }
}

fun generateVerticalSeam(image: BufferedImage, modify: Boolean = true): Array<Int> {
    val seamArray = getVerticalSeamPathArray(image)
    val lastRowIndex = seamArray.size - 1
    var x = seamArray[lastRowIndex].indexOf(seamArray[lastRowIndex].minOrNull()) // smallest element in last row
    val smallestSeamArray = Array(seamArray.size) { 0 }
    if (modify) image.setRGB(x, lastRowIndex, Color.red.rgb)
    smallestSeamArray[lastRowIndex] = x
    for (y in lastRowIndex downTo 1) {
        var smallestIndex = x
        if (x != 0) {
            smallestIndex = if (seamArray[y-1][smallestIndex] > seamArray[y-1][x-1]) x - 1 else smallestIndex
        }
        if (x != seamArray[y].size - 1) {
            smallestIndex = if (seamArray[y-1][smallestIndex] > seamArray[y-1][x+1]) x + 1 else smallestIndex
        }
        x = smallestIndex
        if (modify) image.setRGB(x, y - 1, Color.red.rgb)
        smallestSeamArray[y-1] = x
    }
    return smallestSeamArray
}

fun generateHorizontalSeam(image: BufferedImage, modify: Boolean = true): Array<Int> {
    val seamArray = getHorizontalSeamPathArray(image)
    val lastColIndex = seamArray[0].size - 1
    val lastColumnArray = Array(seamArray.size) { seamArray[it][seamArray[it].size - 1]}
    var y = lastColumnArray.indexOf(lastColumnArray.minOrNull()) // smallest element in last col
    val smallestSeamArray = Array(seamArray[0].size) { 0 }
    if (modify) image.setRGB(lastColIndex, y, Color.red.rgb)
    smallestSeamArray[lastColIndex] = y
    for (x in lastColIndex downTo 1) {
        var smallestIndex = y
        if (y != 0) {
            smallestIndex = if (seamArray[smallestIndex][x-1] > seamArray[y-1][x-1]) y - 1 else smallestIndex
        }
        if (y != seamArray.size - 1) {
            smallestIndex = if (seamArray[smallestIndex][x-1] > seamArray[y+1][x-1]) y + 1 else smallestIndex
        }
        y = smallestIndex
        if (modify) image.setRGB(x - 1, y, Color.red.rgb)
        smallestSeamArray[x-1] = y
    }
    return smallestSeamArray
}

private fun getVerticalSeamPathArray(image: BufferedImage): Array<Array<Double>>  {
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

private fun getHorizontalSeamPathArray(image: BufferedImage): Array<Array<Double>> {
    val intensityArray = generateImageEnergy(image, false)
    for (x in 1 until intensityArray[0].size) {
        for (y in intensityArray.indices) {
            var smallest = intensityArray[y][x - 1]
            if (y != 0) smallest = min(smallest, intensityArray[y - 1][x - 1])
            if (y != intensityArray.size - 1) smallest = min(smallest, intensityArray[y + 1][x - 1])
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
            val intensity = round(intensityDouble).toInt()
            if(modify) image.setRGB(x, y, Color(intensity, intensity, intensity).rgb)
            energyArray[y][x] = intensityDouble
        }
    }
    return energyArray
}

fun getPixelEnergy(image: BufferedImage, x: Int, y: Int): Double {
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
