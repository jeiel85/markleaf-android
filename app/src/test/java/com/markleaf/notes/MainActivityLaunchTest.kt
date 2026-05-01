package com.markleaf.notes

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MainActivityLaunchTest {
    @Test
    fun create_doesNotCrash() {
        Robolectric.buildActivity(MainActivity::class.java).setup().close()
    }
}
