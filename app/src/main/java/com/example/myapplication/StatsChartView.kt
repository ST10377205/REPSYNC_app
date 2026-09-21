package com.example.myapplication

import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import kotlin.math.max
import kotlin.math.min

class StatsChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var dataPoints: FloatArray = floatArrayOf()
    
    fun setData(data: List<Float>) {
        dataPoints = data.toFloatArray()
        invalidate()
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 8f
        strokeCap = Paint.Cap.ROUND
    }

    private val regressionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        pathEffect = DashPathEffect(floatArrayOf(15f, 10f), 0f)
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 28f
    }

    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (dataPoints.size < 2) {
            val text = if (dataPoints.isEmpty()) "Start training to see progress!" else "Need at least 2 sessions for trends"
            val textSecondary = context.getColor(R.color.text_secondary)
            textPaint.color = textSecondary
            canvas.drawText(text, width / 2f - 200f, height / 2f, textPaint)
            return
        }

        // Resolve theme colors dynamically
        val orangeColor = context.getColor(R.color.orange_primary)
        val textPrimary = context.getColor(R.color.text_primary)
        val textSecondary = context.getColor(R.color.text_secondary)
        val gridColor = context.getColor(R.color.app_surface_variant)

        linePaint.color = orangeColor
        regressionPaint.color = textPrimary
        gridPaint.color = gridColor
        textPaint.color = textSecondary
        pointPaint.color = orangeColor

        val paddingLeft = 100f
        val paddingRight = 40f
        val paddingTop = 40f
        val paddingBottom = 60f

        val graphWidth = width - paddingLeft - paddingRight
        val graphHeight = height - paddingTop - paddingBottom

        if (graphWidth <= 0 || graphHeight <= 0) return

        var minVal = dataPoints.minOrNull() ?: 0f
        var maxVal = dataPoints.maxOrNull() ?: 100f
        
        // Add padding to range
        if (maxVal == minVal) {
            maxVal += 10f
            minVal -= 10f
        } else {
            val buffer = (maxVal - minVal) * 0.2f
            maxVal += buffer
            minVal = max(0f, minVal - buffer)
        }
        
        val valRange = maxVal - minVal

        // Draw horizontal grid lines and labels
        val gridLines = 4
        for (i in 0..gridLines) {
            val ratio = i.toFloat() / gridLines
            val y = paddingTop + graphHeight * (1f - ratio)
            canvas.drawLine(paddingLeft, y, width - paddingRight, y, gridPaint)
            
            val labelVal = minVal + ratio * valRange
            canvas.drawText("${labelVal.toInt()}kg", 10f, y + 10f, textPaint)
        }

        // Compute point coordinates
        val pointsX = FloatArray(dataPoints.size)
        val pointsY = FloatArray(dataPoints.size)
        for (i in dataPoints.indices) {
            pointsX[i] = paddingLeft + (i.toFloat() / (dataPoints.size - 1)) * graphWidth
            val normalizedY = (dataPoints[i] - minVal) / valRange
            pointsY[i] = paddingTop + graphHeight * (1f - normalizedY)
        }

        // 1. Draw Actual Progress Line
        val progressPath = Path()
        progressPath.moveTo(pointsX[0], pointsY[0])
        for (i in 1 until dataPoints.size) {
            progressPath.lineTo(pointsX[i], pointsY[i])
        }
        canvas.drawPath(progressPath, linePaint)

        // Draw data point dots
        for (i in dataPoints.indices) {
            canvas.drawCircle(pointsX[i], pointsY[i], 10f, pointPaint)
        }

        // 2. Compute and Draw Linear Regression Line (y = mx + c)
        val n = dataPoints.size
        var sumX = 0f
        var sumY = 0f
        var sumXY = 0f
        var sumXX = 0f

        for (i in dataPoints.indices) {
            val xVal = i.toFloat()
            val yVal = dataPoints[i]
            sumX += xVal
            sumY += yVal
            sumXY += xVal * yVal
            sumXX += xVal * xVal
        }

        val denominator = (n * sumXX - sumX * sumX)
        if (denominator != 0f) {
            val m = (n * sumXY - sumX * sumY) / denominator
            val c = (sumY - m * sumX) / n

            val firstYVal = m * 0f + c
            val lastYVal = m * (n - 1).toFloat() + c

            val regStartNormalizedY = (firstYVal - minVal) / valRange
            val regStartY = paddingTop + graphHeight * (1f - regStartNormalizedY)

            val regEndNormalizedY = (lastYVal - minVal) / valRange
            val regEndY = paddingTop + graphHeight * (1f - regEndNormalizedY)

            canvas.drawLine(pointsX[0], regStartY, pointsX[n - 1], regEndY, regressionPaint)
        }

        // Draw X-axis Labels (Sessions)
        for (i in dataPoints.indices) {
            if (dataPoints.size <= 7 || i % (dataPoints.size / 5 + 1) == 0 || i == dataPoints.size - 1) {
                canvas.drawText("S${i + 1}", pointsX[i] - 20f, height - 15f, textPaint)
            }
        }
    }
}
