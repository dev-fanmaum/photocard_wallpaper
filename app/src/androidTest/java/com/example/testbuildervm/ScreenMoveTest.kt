package com.example.testbuildervm

import android.content.Intent
import android.graphics.Rect
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions

import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.testbuildervm.action.PinchZoom
import com.photocard.wallpaper.WallPaperView
import junit.framework.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random


@RunWith(AndroidJUnit4::class)
@LargeTest
class ScreenMoveTest {

    @Rule
    @JvmField
    val activityRule = ActivityTestRule(MainActivity::class.java)

    val rootView by lazy { activityRule.activity.findViewById<ConstraintLayout>(R.id.root_view) }
    val wallpaperImageView by lazy { activityRule.activity.findViewById<WallPaperView>(R.id.wallPaperSettingView) }
    val wallpaperImageViewEspresso by lazy { onView(withId(R.id.wallPaperSettingView)) }
    val button by lazy { activityRule.activity.findViewById<WallPaperView>(R.id.save_button) }
    val buttonEspresso by lazy { onView(withId(R.id.save_button)) }

    private val moveMap = mapOf<Int, ViewAction>(
        0 to ViewActions.swipeLeft(),
        1 to ViewActions.swipeRight(),
        2 to ViewActions.swipeUp(),
        3 to ViewActions.swipeDown()
    )

    @Before
    fun init() {
        activityRule.launchActivity(Intent())

        activityRule.activity.runOnUiThread {
            wallpaperImageView.getWallPaperImageView()
                .setImageResource(R.drawable.ic_launcher_background)
        }

        Thread.sleep(1000)
    }

    @Test
    fun initScreen() {

        val overlayInsideCheck = imageBoxRectChecker()

        if (overlayInsideCheck.isEmpty().not()) {
            fail("$overlayInsideCheck over")
        }

    }

    @Test
    fun screenMove() {

        randomMoveTaskList(10).asSequence()
            .forEach {
                wallpaperImageViewEspresso.perform(it)
                Thread.sleep(500)
                imageBoxRectChecker()
            }
    }

    private fun randomMoveTaskList(i: Int) =
        List(i) { moveMap[Random.nextInt(0, moveMap.size)] }


    @Test
    fun screenScale(){
        activityRule.activity.runOnUiThread { wallpaperImageViewEspresso.perform(PinchZoom()) }
//        activityRule.activity.
        // scale test code

        Thread.sleep(500)
        wallpaperImageViewEspresso.perform(ViewActions.click())
        wallpaperImageViewEspresso.perform(ViewActions.swipeDown())


        Thread.sleep(1000)
    }

    private fun imageBoxRectChecker(): String {
        val rect = Rect(
            wallpaperImageView.getWallPaperImageView().left,
            wallpaperImageView.getWallPaperImageView().top,
            wallpaperImageView.getWallPaperImageView().right,
            wallpaperImageView.getWallPaperImageView().bottom
        )

        val overlayRect = wallpaperImageView.deviceViewBox

        val leftCheck =
            if (rect.left <= overlayRect.left.toInt()) null else "left ${rect.left}<= ${overlayRect.left}\n"
        val topCheck =
            if (rect.top <= overlayRect.top.toInt()) null else "top ${rect.top}  <=  ${overlayRect.top}\n"
        val rightCheck =
            if (rect.right >= overlayRect.right.toInt()) null else "right ${rect.right} >= ${overlayRect.right}\n"
        val bottomCheck =
            if (rect.bottom >= overlayRect.bottom.toInt()) null else "bottom ${rect.bottom} >= ${overlayRect.bottom}"

        return arrayOf(
            leftCheck,
            topCheck,
            rightCheck,
            bottomCheck
        ).filterNotNull()
            .joinToString { it }
    }


}