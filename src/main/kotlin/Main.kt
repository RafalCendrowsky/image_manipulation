import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

fun main(args: Array<String>) {
    val inputFile: File
    val outputFile: File
    var function: (BufferedImage) -> Unit = {}
    try {
        function = when (args[0]) {
            "--negative" -> ::negateColours
            "--no-op" -> { _ ->  }
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
    for (x in 0 until image.width) {
        for (y in 0 until image.height) {
            val color = Color(image.getRGB(x, y))
            image.setRGB(x, y, Color(255 - color.red, 255 - color.green, 255 - color.blue).rgb)
        }
    }
}
