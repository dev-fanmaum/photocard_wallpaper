package com.example.testbuildervm.action

import android.app.Instrumentation
import android.graphics.Point
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.espresso.UiController
import android.view.MotionEvent
import android.view.View
import junit.framework.Assert.fail

object PerformPinch {

    fun getCenterPointer(view: View): Point {
        val locationOnScreen = IntArray(2) { 0 }
        view.getLocationOnScreen(locationOnScreen)
        val viewWidth = view.width * view.scaleX
        val viewHeight = view.height * view.scaleY
        return Point(
            (locationOnScreen[0] + viewWidth / 2).toInt(),
            (locationOnScreen[1] + viewHeight / 2).toInt()
        )
    }

    fun pinch(
        controller: UiController,
        startPoint1: Point,
        startPoint2: Point,
        endPoint1: Point,
        endPoint2: Point
    ) {
        val inst = InstrumentationRegistry.getInstrumentation()
        val duringTime = 500
        val intervalTime = 10
        val startTime = SystemClock.uptimeMillis()
        var eventTime = startTime

        var event: MotionEvent

        var eventX1 = startPoint1.x.toFloat()
        var eventY1 = startPoint1.y.toFloat()
        var eventX2 = startPoint2.x.toFloat()
        var eventY2 = startPoint2.y.toFloat()


        val pp1 = MotionEvent.PointerProperties().apply {
            id = 0
            toolType = MotionEvent.TOOL_TYPE_FINGER
        }
        val pp2 = MotionEvent.PointerProperties().apply {
            id = 1
            toolType = MotionEvent.TOOL_TYPE_FINGER
        }
        val properties = arrayOf(pp1, pp2)

        val pc1 = MotionEvent.PointerCoords().apply {
            x = eventX1
            y = eventY1
            pressure = 1f
            size = 1f
        }
        val pc2 = MotionEvent.PointerCoords().apply {
            x = eventX2
            y = eventY2
            pressure = 1f
            size = 1f
        }
        val pointerCoords = arrayOf(pc1, pc2)


        try{
            event = MotionEvent.obtain(
                startTime,
                eventTime,
                MotionEvent.ACTION_DOWN,
                1, properties,
                pointerCoords,
                0, 0, 1f, 1f, 0, 0, 0, 0
            )
            injectMotionEventToUiController(controller, event)

            event = MotionEvent.obtain(
                startTime,
                eventTime,
                MotionEvent.ACTION_POINTER_DOWN + (pp2.id shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                2,
                properties,
                pointerCoords,
                0, 0, 1f, 1f, 0, 0, 0, 0
            )
            injectMotionEventToUiController(controller, event)


            val moveEventNum = duringTime / intervalTime

            val stepX1 = (endPoint1.x - startPoint1.x) / moveEventNum
            val stepy1 = (endPoint1.y - startPoint1.y) / moveEventNum
            val stepX2 = (endPoint1.x - startPoint1.x) / moveEventNum
            val stepY2 = (endPoint1.y - startPoint1.y) / moveEventNum

            for (i in 0..moveEventNum) {
                eventTime += intervalTime
                eventX1 += stepX1
                eventY1 += stepy1
                eventX2 += stepX2
                eventY2 += stepY2

                pc1.x = eventX1
                pc1.y = eventY1
                pc2.x = eventX2
                pc2.y = eventY2

                pointerCoords[0] = pc1
                pointerCoords[1] = pc2

                event = MotionEvent.obtain(
                    startTime, eventTime,
                    MotionEvent.ACTION_MOVE, 2, properties,
                    pointerCoords, 0, 0, 1f, 1f, 0, 0, 0, 0
                )

                injectMotionEventToUiController(controller, event)

                pc1.x = endPoint1.x.toFloat()
                pc1.y = endPoint1.y.toFloat()
                pc2.x = endPoint2.x.toFloat()
                pc2.y = endPoint2.y.toFloat()
                pointerCoords[0] = pc1
                pointerCoords[1] = pc2


                eventTime += intervalTime
                event = MotionEvent.obtain(
                    startTime,
                    eventTime,
                    MotionEvent.ACTION_POINTER_UP + (pp2.id shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                    2,
                    properties,
                    pointerCoords,
                    0, 0, 1f, 1f, 0, 0, 0, 0
                )
                injectMotionEventToUiController(controller, event)

                eventTime += intervalTime
                event = MotionEvent.obtain(
                    startTime, eventTime,
                    MotionEvent.ACTION_UP, 1, properties,
                    pointerCoords, 0, 0, 1f, 1f, 0, 0, 0, 0
                )
                injectMotionEventToUiController(controller, event)
            }
        }catch (e : Exception){}
    }

    private fun injectMotionEventToUiController(controller: UiController, event: MotionEvent) {

        val eventResult = controller.injectMotionEvent(event)
        if (!eventResult) {
            fail("performing event Error $event")
        }
    }

}