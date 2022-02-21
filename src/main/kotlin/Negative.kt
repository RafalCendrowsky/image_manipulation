import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

fun main(args: Array<String>) {
    val inputFile: File
    val outputFile: File
    try {
        val inputFileName = args[args.indexOf("-in") + 1]
        val outputFileName = args[args.indexOf("-out") + 1]
        inputFile = File(inputFileName)
        outputFile = File(outputFileName)
    } catch (e: IndexOutOfBoundsException) {
        throw IllegalArgumentException("Invalid command line argument(s)")
    }
    val image: BufferedImage = ImageIO.read(inputFile)
    negateColours(image)
    ImageIO.write(image, "png", outputFile)
}

fun negateColours(image: BufferedImage) {
    for (x in 0 until image.width) {
        for (y in 0 until image.height) {
            val color = Color(image.getRGB(x, y))
            image.setRGB(x, y, Color(255 - color.red, 255 - color.green, 255 - color.blue).rgb)
        }
    }
}