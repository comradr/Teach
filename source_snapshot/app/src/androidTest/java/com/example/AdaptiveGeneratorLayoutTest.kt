package com.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import androidx.compose.runtime.CompositionLocalProvider
import com.example.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test

class AdaptiveGeneratorLayoutTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test fun settingsFitAt320Dp() = verifyWidth(320)
    @Test fun settingsFitAt360Dp() = verifyWidth(360)
    @Test fun settingsFitAt393Dp() = verifyWidth(393)
    @Test fun settingsFitAt412Dp() = verifyWidth(412)
    @Test fun settingsFitAt600Dp() = verifyWidth(600)
    @Test fun settingsFitAt320DpWithLargeText() = verifyWidth(320, 1.3f)

    private fun verifyWidth(width: Int, fontScale: Float = 1f) {
        composeRule.setContent {
            MyApplicationTheme {
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
                    Box(Modifier.requiredWidth(width.dp)) {
                        GeneralSettingsCard(
                            useSecondClass = true,
                            onSecondClassChange = {},
                            planMode = "Официальный",
                            onPlanModeChange = {},
                            lessonDuration = "45",
                            onDurationChange = {}
                        )
                    }
                }
            }
        }
        composeRule.onNodeWithText("Два класса / МКШ").assertIsDisplayed()
        composeRule.onNodeWithText("Официальный").assertIsDisplayed()
        composeRule.onNodeWithText("45 мин").assertIsDisplayed()
    }
}
