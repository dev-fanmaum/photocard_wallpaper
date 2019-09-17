package com.photocard.wallpaper

import org.junit.Test

import org.junit.Assert.*
import kotlin.reflect.KProperty0

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {

    private lateinit var initVariable: String

    @Test
    fun addition_isCorrect() {

        val initCheckNot = if (this::initVariable.isInitialized) {
            "init"
        } else {
            "not init"
        }

        println(initCheckNot)

        initVariable = "Setting"
        val initCheckOkay = if (this::initVariable.isInitialized) {
            "init"
        } else {
            "not init"
        }

        println(initCheckOkay)

    }

}
