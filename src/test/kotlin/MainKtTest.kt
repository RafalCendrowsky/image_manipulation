import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.awt.Color
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO
import kotlin.test.assertEquals

const val OUTPUT_FILE_NAME = "src/test/resources/img-negative.png" // taken from a generic image negation tool
const val INPUT_FILE_NAME = "src/test/resources/img.png"
const val RESULT_FILE_NAME = "src/test/resources/img-result.png"

internal class MainKtTest {

    @ParameterizedTest
    @MethodSource("invalidCLArgFactory")
    fun `Test main with invalid command line args`(args: Array<String>) {
        assertThrows<IllegalArgumentException> { main(args) }
    }

    @ParameterizedTest
    @MethodSource("invalidFileArgFactory")
    fun `Test main with invalid file args` (args: Array<String>) {
        assertThrows<IOException> { main(args) }
    }

    @ParameterizedTest
    @MethodSource("functionNameFactory")
    fun `Test functions with valid args` (functionName: String) {
        main(arrayOf(functionName, INPUT_FILE_NAME, RESULT_FILE_NAME))
        File(RESULT_FILE_NAME)
        File(RESULT_FILE_NAME).delete()
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
    @MethodSource("pixelCoordinateFactory")
    fun `Test pixel energy`(x: Int, y: Int, energy: Double) {
        val image = ImageIO.read(File(INPUT_FILE_NAME))
        assertEquals(getPixelEnergy(image, x, y), energy, 0.5)

    }

    @ParameterizedTest
    @MethodSource("pixelCoordinateImageFactory")
    fun `Test image energy`(x: Int, y: Int, energy: Int) {
        val image = ImageIO.read(File(INPUT_FILE_NAME))
        generateImageEnergy(image)
        assert(image.getRGB(x, y) == Color(energy, energy, energy).rgb)
    }

    @Test
    fun `Test vertical seam`() {
        val image = ImageIO.read(File(INPUT_FILE_NAME))
        generateSeam(image)
        assert(image.getRGB(8, 3) == Color.red.rgb)
        assert(image.getRGB(9, 6) == Color.red.rgb)
    }

    companion object {
        @JvmStatic
        fun invalidCLArgFactory(): Array<Arguments> {
            return arrayOf(
                Arguments.arguments(emptyArray<String>()),
                Arguments.arguments(arrayOf("")),
                Arguments.arguments(arrayOf("one")),
                Arguments.arguments(arrayOf("one", "two")),
                Arguments.arguments(arrayOf("one", "two", "three"))
            )
        }

        @JvmStatic
        fun invalidFileArgFactory(): Array<Arguments> {
            return arrayOf(
                Arguments.arguments(arrayOf("--no-op", "file", "src/test/resources/img.png")),
                Arguments.arguments(arrayOf("--no-op", "", "src/test/resources/img.png"))
            )
        }

        @JvmStatic
        fun functionNameFactory(): Array<String> {
            return arrayOf("--no-op", "--negative", "--seam", "--energy")
        }

        @JvmStatic
        fun pixelCoordinateFactory(): Array<Arguments> {
            return arrayOf(
                Arguments.arguments(6, 0, 12),
                Arguments.arguments(2, 3, 16),
                Arguments.arguments(9, 6, 85)
            )
        }

        @JvmStatic
        fun pixelCoordinateImageFactory(): Array<Arguments> {
            return arrayOf(
                Arguments.arguments(6, 0, 8),
                Arguments.arguments(2, 3, 11),
                Arguments.arguments(9, 6, 59)
            )

        }
    }
}