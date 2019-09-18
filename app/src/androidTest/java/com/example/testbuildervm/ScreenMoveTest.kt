package com.example.testbuildervm

import android.content.Intent
import android.graphics.Rect
import android.support.constraint.ConstraintLayout
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.matcher.ViewMatchers.withId
import android.support.test.filters.LargeTest
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import com.photocard.wallpaper.WallPaperView
import junit.framework.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


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

    @Test
    fun initScreen() {
        activityRule.launchActivity(Intent())

        activityRule.activity.runOnUiThread {
            wallpaperImageView.getWallPaperImageView()
                .setImageResource(R.drawable.ic_launcher_background)
        }

        Thread.sleep(1000)


        val overlayInsideCheck = imageBoxRectChecker()

        if (overlayInsideCheck.isEmpty().not()) {
            fail("$overlayInsideCheck over")
        }

    }


    @Test
    fun screenMove() {

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