package com.example.testbuildervm.action

import android.graphics.Point
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers
import android.view.View
import org.hamcrest.Matcher

class PinchZoom : ViewAction {
    override fun getDescription(): String = "Pinch zoom"

    override fun getConstraints(): Matcher<View> = ViewMatchers.isEnabled()

    override fun perform(uiController: UiController?, view: View?) {
        uiController ?: return
        view ?: return
        val middlePosition = PerformPinch.getCenterPointer(view)

        val startDelta = 500
        val endDelta = 0

        val startPoint1 = Point(middlePosition.x - startDelta, middlePosition.y)
        val startPoint2 = Point(middlePosition.x + startDelta, middlePosition.y)
        val endPoint1 = Point(middlePosition.x - endDelta, middlePosition.y)
        val endPoint2 = Point(middlePosition.x + endDelta, middlePosition.y)

        PerformPinch.pinch(uiController, startPoint1, startPoint2, endPoint1, endPoint2)

    }
}