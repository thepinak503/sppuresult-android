package pinak.sppunotify.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import java.io.File
import java.io.FileOutputStream

object ResultImageShare {

    private const val WIDTH = 1080
    private const val HEIGHT = 400
    private const val PADDING = 48
    private const val CARD_RADIUS = 40f
    private const val ACCENT_STRIP_WIDTH = 12f
    private const val BADGE_RADIUS = 12f

    fun shareAsImage(
        context: Context,
        title: String,
        department: String,
        publishedDate: String,
        patternName: String
    ) {
        val bitmap = generateResultBitmap(context, title, department, publishedDate, patternName)
            ?: return

        try {
            val cacheDir = File(context.cacheDir, "shared_images")
            cacheDir.mkdirs()
            val file = File(cacheDir, "result_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "SPPU Result: $title")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Result as Image"))
        } catch (e: Exception) {
            // Fall back to text share
            val intent = Intent(Intent.ACTION_SEND).apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, "Check out this SPPU Result: $title")
                type = "text/plain"
            }
            context.startActivity(Intent.createChooser(intent, null))
        }
    }

    private fun generateResultBitmap(
        context: Context,
        title: String,
        department: String,
        publishedDate: String,
        patternName: String
    ): Bitmap? {
        val density = context.resources.displayMetrics.density
        val w = (WIDTH * density).toInt()
        val h = (HEIGHT * density).toInt()
        val p = (PADDING * density).toInt()
        val cr = CARD_RADIUS * density
        val asw = ACCENT_STRIP_WIDTH * density
        val br = BADGE_RADIUS * density

        val bitmap = createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background
        val bgPaint = Paint().apply {
            color = 0xFF1C1B1F.toInt() // dark surface color
            isAntiAlias = true
        }
        canvas.drawPaint(bgPaint)

        // Card background
        val cardRect = RectF(p.toFloat(), p.toFloat(), (w - p).toFloat(), (h - p).toFloat())
        val cardPaint = Paint().apply {
            color = 0xFF2B2930.toInt() // surface container high
            isAntiAlias = true
        }
        canvas.drawRoundRect(cardRect, cr, cr, cardPaint)

        // Accent strip
        val stripRect = RectF(
            cardRect.left,
            cardRect.top,
            cardRect.left + asw,
            cardRect.bottom
        )
        val stripPaint = Paint().apply {
            color = 0xFF4F378B.toInt() // primary color approximate
            isAntiAlias = true
        }
        canvas.drawRoundRect(stripRect, cr, cr, stripPaint)
        // Clip the right side of the strip to make it rectangular
        val stripClipRect = RectF(
            cardRect.left,
            cardRect.top + 8f * density,
            cardRect.left + asw,
            cardRect.bottom - 8f * density
        )
        val stripClipPaint = Paint().apply {
            color = 0xFF2B2930.toInt() // match card background
            isAntiAlias = true
        }
        canvas.drawRect(stripClipRect, stripClipPaint)

        // Title text
        val titlePaint = Paint().apply {
            color = 0xFFE6E1E5.toInt() // on-surface
            isAntiAlias = true
            textSize = 34f * density
            typeface = Typeface.DEFAULT_BOLD
            isLinearText = true
        }

        val textX = p + asw + 32f * density
        var textY = p + 120f * density

        val maxTextWidth = w - textX - p
        val processedTitle = if (title.length > 80) title.take(77) + "..." else title
        val titleLines = breakText(processedTitle, titlePaint, maxTextWidth)

        for (line in titleLines) {
            canvas.drawText(line, textX, textY, titlePaint)
            textY += 44f * density
        }

        // Department badge
        if (department.isNotEmpty() && department != "Other UG") {
            val badgePaint = Paint().apply {
                color = 0x294F378B.toInt() // primary with alpha
                isAntiAlias = true
            }
            val badgeTextPaint = Paint().apply {
                color = 0xFFD0BCFF.toInt() // primary color
                isAntiAlias = true
                textSize = 20f * density
                typeface = Typeface.DEFAULT_BOLD
            }
            val badgeText = department
            val badgeWidth = badgeTextPaint.measureText(badgeText) + 32f * density
            val badgeRect = RectF(
                textX, textY - 8f * density,
                textX + badgeWidth, textY + 32f * density
            )
            canvas.drawRoundRect(badgeRect, br, br, badgePaint)
            canvas.drawText(
                badgeText,
                textX + 16f * density,
                textY + 22f * density,
                badgeTextPaint
            )
            textY += 60f * density
        } else {
            textY += 24f * density
        }

        // Pattern name
        if (patternName.isNotBlank() && patternName.contains(' ')) {
            val patternPaint = Paint().apply {
                color = 0xFFD0BCFF.toInt() // primary
                isAntiAlias = true
                textSize = 22f * density
            }
            canvas.drawText(patternName, textX, textY, patternPaint)
            textY += 38f * density
        }

        // Date
        val datePaint = Paint().apply {
            color = 0xFF938F99.toInt() // outline
            isAntiAlias = true
            textSize = 24f * density
        }
        canvas.drawText("Published: $publishedDate", textX, textY, datePaint)

        // Footer text
        val footerPaint = Paint().apply {
            color = 0x66938F99.toInt() // outline with alpha
            isAntiAlias = true
            textSize = 18f * density
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            "SPPU Result Notify",
            w / 2f,
            h - p.toFloat() + 40f * density,
            footerPaint
        )

        return bitmap
    }

    private fun breakText(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (paint.measureText(text) <= maxWidth) return listOf(text)

        val lines = mutableListOf<String>()
        val words = text.split(" ")
        var currentLine = StringBuilder()

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) <= maxWidth) {
                currentLine = StringBuilder(testLine)
            } else {
                lines.add(currentLine.toString())
                currentLine = StringBuilder(word)
            }
        }
        if (currentLine.isNotEmpty()) lines.add(currentLine.toString())
        return lines
    }
}
