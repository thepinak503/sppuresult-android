package pinak.sppunotify.util

import android.content.Context
import android.graphics.*
import android.net.Uri
import androidx.core.content.FileProvider
import pinak.sppunotify.data.local.ResultEntity
import java.io.File
import java.io.FileOutputStream

object ResultImageGenerator {

    fun generateAndShare(context: Context, result: ResultEntity): Uri? {
        val width = 1080
        val height = 1350 // 4:5 aspect ratio
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Background
        val paint = Paint()
        paint.color = Color.parseColor("#1A5276") // SppuBlue
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        
        // Card Background
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        val margin = 80f
        val rect = RectF(margin, margin, width - margin, height - margin)
        canvas.drawRoundRect(rect, 40f, 40f, paint)
        
        // Header
        paint.color = Color.parseColor("#1A5276")
        paint.textSize = 60f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("SPPU Result Notify", 150f, 220f, paint)
        
        // Divider
        paint.strokeWidth = 3f
        canvas.drawLine(150f, 280f, width - 150f, 280f, paint)
        
        // Result Title
        paint.color = Color.BLACK
        paint.textSize = 48f
        val textPaint = android.text.TextPaint(paint)
        val textLayout = android.text.StaticLayout.Builder.obtain(
            result.title, 0, result.title.length, textPaint, width - 300
        ).setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL).build()
        
        canvas.save()
        canvas.translate(150f, 350f)
        textLayout.draw(canvas)
        canvas.restore()
        
        // Details
        paint.textSize = 40f
        paint.typeface = Typeface.DEFAULT
        paint.color = Color.GRAY
        canvas.drawText("Published Date: ${result.publishedDate}", 150f, 850f, paint)
        canvas.drawText("Department: ${result.department}", 150f, 920f, paint)
        
        // Footer (Call to action)
        paint.color = Color.parseColor("#F39C12")
        paint.textSize = 36f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        canvas.drawText("Download SPPU Result Notify on Play Store", 150f, 1200f, paint)

        // Save to cache
        val imagesFolder = File(context.cacheDir, "shared_images")
        imagesFolder.mkdirs()
        val file = File(imagesFolder, "result_${result.id}.png")
        val stream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.flush()
        stream.close()
        
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
}
