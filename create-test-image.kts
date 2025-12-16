import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

// Create a simple test image
val width = 300
val height = 300
val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
val graphics = image.createGraphics()

// Fill with a gradient
for (y in 0 until height) {
    for (x in 0 until width) {
        val r = (x * 255) / width
        val g = (y * 255) / height
        val b = 128
        image.setRGB(x, y, Color(r, g, b).rgb)
    }
}

graphics.dispose()

// Save as JPEG
ImageIO.write(image, "jpg", File("test-image.jpg"))
println("Test image created: test-image.jpg (${width}x${height})")
