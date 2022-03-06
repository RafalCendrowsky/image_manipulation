import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO
import kotlin.test.AfterTest
import kotlin.test.assertEquals

const val OUTPUT_FILE_NAME = "src/test/resources/img-negative.png" // taken from a generic image negation tool
const val INPUT_FILE_NAME = "src/test/resources/img.png"
const val RESULT_FILE_NAME = "src/test/resources/img-result.png"

internal class MainKtTest {

    @AfterTest
    fun deleteFile() {
        File(RESULT_FILE_NAME).delete()
    }

    @ParameterizedTest
    @MethodSource("invalidCLArgSource")
    fun `Test main with invalid command line args`(args: Array<String>) {
        assertThrows<IllegalArgumentException> { main(args) }
    }

    @ParameterizedTest
    @MethodSource("invalidFileArgSource")
    fun `Test main with invalid file args` (args: Array<String>) {
        assertThrows<IOException> { main(args) }
    }

    @ParameterizedTest
    @ValueSource(strings = ["--no-op", "--energy", "--seam-v", "--seam-h", "--negative"])
    fun `Test functions with valid args` (functionName: String) {
        main(arrayOf(functionName, INPUT_FILE_NAME, RESULT_FILE_NAME))
    }

    @Test
    fun `Test valid args for seam carve`() {
        main(arrayOf("--seam-c", INPUT_FILE_NAME, RESULT_FILE_NAME, "2", "2"))
    }

    @Test
    fun `Test for invalid args for seam carve height or width`() {
        main(arrayOf("--seam-c", INPUT_FILE_NAME, RESULT_FILE_NAME, "200", "200"))
    }

    @Test
    fun `Test colour negation`() {
        val outputImage = ImageIO.read(File(OUTPUT_FILE_NAME))
        val inputImage = ImageIO.read(File(INPUT_FILE_NAME))
        negateColours(inputImage)
        for (x in 0 until inputImage.width - 1) {
            for (y in 0 until inputImage.height - 1) {
                if (inputImage.getRGB(x, y) != outputImage.getRGB(x, y)) assert(false)
            }
        }
        assert(true)
    }

    @ParameterizedTest
    @MethodSource("pixelCoordinateSource")
    fun `Test pixel energy`(x: Int, y: Int, energy: Double) {
        val image = ImageIO.read(File(INPUT_FILE_NAME))
        assertEquals(getPixelEnergy(image, x, y), energy, 0.5)

    }

    @ParameterizedTest
    @MethodSource("pixelCoordinateImageSource")
    fun `Test image energy`(x: Int, y: Int, energy: Int, image: BufferedImage) {
        assert(image.getRGB(x, y) == Color(energy, energy, energy).rgb)
    }

    @Test
    fun `Test vertical seam`() {
        val image = ImageIO.read(File(INPUT_FILE_NAME))
        val seamArray = generateVerticalSeam(image, false)
        val resultArray = arrayOf(6,7,7,8,8,8,9,8,9,10) // picture is small enough that it's possible to enumerate the pixels
        seamArray.indices.forEach {assert(seamArray[it] == resultArray[it])}
    }

    @Test
    fun `Test horizontal seam`() {
        val image = ImageIO.read(File(INPUT_FILE_NAME))
        val seamArray = generateHorizontalSeam(image, false)
        val resultArray = arrayOf(7,7,8,8,9,9,8,7,8,9,9,9,8,8,8) // again, small enough that it's possible to enumerate the pixels
        seamArray.indices.forEach {assert(seamArray[it] == resultArray[it])}
    }

    @Test
    fun `Test seam carve`() {
        var image = ImageIO.read(File(INPUT_FILE_NAME))
        image = resizeImage(image, 5, 5)
        assert(image.height == 5 && image.width == 10)
    }

    companion object {
        @JvmStatic
        fun invalidCLArgSource(): Array<Arguments> {
            return arrayOf(
                Arguments.arguments(emptyArray<String>()),
                Arguments.arguments(arrayOf("")),
                Arguments.arguments(arrayOf("one")),
                Arguments.arguments(arrayOf("one", "two")),
                Arguments.arguments(arrayOf("one", "two", "three")),
                Arguments.arguments(arrayOf("--seam-c", "src/test/resources/img.png", "src/test/resources/img.png")),
                Arguments.arguments(arrayOf("--seam-c", "src/test/resources/img.png", "src/test/resources/img.png", "4")),
                Arguments.arguments(arrayOf("--seam-c", "src/test/resources/img.png", "src/test/resources/img.png", "4", "a"))
            )
        }

        @JvmStatic
        fun invalidFileArgSource(): Array<Arguments> {
            return arrayOf(
                Arguments.arguments(arrayOf("--no-op", "file", "src/test/resources/img.png")),
                Arguments.arguments(arrayOf("--no-op", "", "src/test/resources/img.png")),
            )
        }

        @JvmStatic
        fun pixelCoordinateSource(): Array<Arguments> {
            return arrayOf(
                Arguments.arguments(6, 0, 12),
                Arguments.arguments(2, 3, 16),
                Arguments.arguments(9, 6, 85)
            )
        }

        @JvmStatic
        fun pixelCoordinateImageSource(): Array<Arguments> {
            val image = ImageIO.read(File(INPUT_FILE_NAME))
            generateImageEnergy(image)
            return arrayOf(
                Arguments.arguments(6, 0, 8, image),
                Arguments.arguments(2, 3, 11, image),
                Arguments.arguments(9, 6, 59, image)
            )
        }
    }
}