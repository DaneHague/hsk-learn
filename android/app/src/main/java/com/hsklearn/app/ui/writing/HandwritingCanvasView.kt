package com.hsklearn.app.ui.writing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import java.io.ByteArrayOutputStream

class HandwritingCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val strokes = mutableListOf<StrokeData>()
    private var currentPoints = mutableListOf<PointF>()
    private var currentPressures = mutableListOf<Float>()
    private val currentPath = Path()

    private val density = resources.displayMetrics.density

    private val inkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#222222")
        style = Paint.Style.STROKE
        strokeWidth = 3f * density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#D0D0D0")
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }

    private val gridDashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E8E8E8")
        style = Paint.Style.STROKE
        strokeWidth = 1f
        pathEffect = android.graphics.DashPathEffect(floatArrayOf(8f, 8f), 0f)
    }

    private val templatePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E8E8E8")
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT
    }

    private var templateChar: String? = null
    private var isTemplateMode = false
    private var hasStylusPressure = false
    private var isLinedMode = false

    /** Line height in dp for composition lined mode. */
    private val lineHeightDp = 48f
    private val lineHeightPx get() = lineHeightDp * density

    data class StrokeData(
        val path: Path,
        val points: List<PointF>,
        val pressures: List<Float>,
    )

    fun setTemplateCharacter(char: String?) {
        templateChar = char
        invalidate()
    }

    fun setTemplateMode(enabled: Boolean) {
        isTemplateMode = enabled
        invalidate()
    }

    fun setLinedMode(enabled: Boolean) {
        isLinedMode = enabled
        invalidate()
    }

    fun clearCanvas() {
        strokes.clear()
        currentPath.reset()
        currentPoints.clear()
        currentPressures.clear()
        invalidate()
    }

    fun undoLastStroke() {
        if (strokes.isNotEmpty()) {
            strokes.removeAt(strokes.lastIndex)
            invalidate()
        }
    }

    fun exportAsPng(): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        draw(canvas)
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        bitmap.recycle()
        return stream.toByteArray()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // White background
        canvas.drawColor(Color.WHITE)

        // Draw grid
        drawGrid(canvas)

        // Draw template character if in template mode
        if (isTemplateMode && templateChar != null) {
            drawTemplate(canvas)
        }

        // Draw completed strokes
        for (stroke in strokes) {
            drawStroke(canvas, stroke)
        }

        // Draw current in-progress stroke
        if (currentPoints.size >= 2) {
            val tempStroke = StrokeData(Path(currentPath), currentPoints.toList(), currentPressures.toList())
            drawStroke(canvas, tempStroke)
        }
    }

    private fun drawGrid(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()

        // Outer border
        canvas.drawRect(0f, 0f, w, h, gridPaint)

        if (isLinedMode) {
            // Lined notebook style for composition writing
            val spacing = lineHeightPx
            var y = spacing
            while (y < h) {
                // Solid baseline
                canvas.drawLine(0f, y, w, y, gridPaint)
                // Dashed midline for character proportion guidance
                val mid = y - spacing / 2
                canvas.drawLine(0f, mid, w, mid, gridDashPaint)
                y += spacing
            }
        } else {
            // 田字格 character grid
            canvas.drawLine(w / 2, 0f, w / 2, h, gridDashPaint)
            canvas.drawLine(0f, h / 2, w, h / 2, gridDashPaint)
            canvas.drawLine(0f, 0f, w, h, gridDashPaint)
            canvas.drawLine(w, 0f, 0f, h, gridDashPaint)
        }
    }

    private fun drawTemplate(canvas: Canvas) {
        val char = templateChar ?: return
        val size = (minOf(width, height) * 0.8f)
        templatePaint.textSize = size

        val x = width / 2f
        val metrics = templatePaint.fontMetrics
        val y = height / 2f - (metrics.ascent + metrics.descent) / 2f

        canvas.drawText(char, x, y, templatePaint)
    }

    private fun drawStroke(canvas: Canvas, stroke: StrokeData) {
        if (stroke.points.size < 2) return

        val baseWidth = 3f * density

        if (hasStylusPressure && stroke.pressures.isNotEmpty()) {
            // Draw segments with variable width based on pressure
            for (i in 1 until stroke.points.size) {
                val p0 = stroke.points[i - 1]
                val p1 = stroke.points[i]
                val pressure = stroke.pressures.getOrElse(i) { 0.5f }
                inkPaint.strokeWidth = baseWidth * pressure.coerceIn(0.4f, 1.8f)
                canvas.drawLine(p0.x, p0.y, p1.x, p1.y, inkPaint)
            }
            inkPaint.strokeWidth = baseWidth // reset
        } else {
            inkPaint.strokeWidth = baseWidth
            canvas.drawPath(stroke.path, inkPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        // Detect stylus pressure
        if (event.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS) {
            hasStylusPressure = true
        }

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Prevent parent ScrollView from stealing touch events while drawing
                parent?.requestDisallowInterceptTouchEvent(true)

                currentPath.reset()
                currentPoints.clear()
                currentPressures.clear()
                currentPath.moveTo(x, y)
                currentPoints.add(PointF(x, y))
                currentPressures.add(event.pressure)
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val prev = currentPoints.lastOrNull() ?: return true
                currentPath.quadTo(prev.x, prev.y, (prev.x + x) / 2, (prev.y + y) / 2)
                currentPoints.add(PointF(x, y))
                currentPressures.add(event.pressure)
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)

                if (event.action == MotionEvent.ACTION_UP) {
                    currentPath.lineTo(x, y)
                    currentPoints.add(PointF(x, y))
                    currentPressures.add(event.pressure)

                    strokes.add(
                        StrokeData(
                            path = Path(currentPath),
                            points = currentPoints.toList(),
                            pressures = currentPressures.toList(),
                        ),
                    )
                }

                currentPath.reset()
                currentPoints.clear()
                currentPressures.clear()
                invalidate()
                return true
            }
        }

        return super.onTouchEvent(event)
    }
}
