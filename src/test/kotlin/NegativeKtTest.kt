import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO

const val OUTPUT_FILE_NAME = "src/test/resources/img-negative.png" // taken from a generic image negation tool
const val INPUT_FILE_NAME = "src/test/resources/img.png"
const val RESULT_FILE_NAME = "src/test/resources/img-result.png"

internal class NegativeKtTest {

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

    @Test
    fun `Test main with valid args, not testing negation` () {
        main(arrayOf("-in", INPUT_FILE_NAME, "-out", RESULT_FILE_NAME))
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

    companion object {
        @JvmStatic
        fun invalidCLArgFactory(): Array<Arguments> {
            return arrayOf(
                Arguments.arguments(emptyArray<String>()),
                Arguments.arguments(arrayOf("-in")),
                Arguments.arguments(arrayOf("-out")),
                Arguments.arguments(arrayOf("-in", "-out")),
                Arguments.arguments(arrayOf("-in", "file", "-out"))
            )
        }

        @JvmStatic
        fun invalidFileArgFactory(): Array<Arguments> {
            return arrayOf(
                Arguments.arguments(arrayOf("-in", "file", "-out", "src/test/resources/img.png")),
                // also throws FileNotFoundException, can't manage to encase it in try/catch block though, dunno why
                Arguments.arguments(arrayOf("-in", "src/test/resources/img.png", "-out", "")),
                Arguments.arguments(arrayOf("-in", "", "-out", "src/test/resources/img.png"))
            )
        }
    }
}