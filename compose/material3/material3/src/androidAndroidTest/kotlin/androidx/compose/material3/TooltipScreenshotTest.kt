/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.material3

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
class TooltipScreenshotTest {
    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3)

    private val tooltipState = TooltipState()

    @Test
    fun plainTooltip_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) { TestTooltips() }
        assertAgainstGolden("plainTooltip_lightTheme")
    }

    @Test
    fun plainTooltip_darkTheme() {
        rule.setMaterialContent(darkColorScheme()) { TestTooltips() }
        assertAgainstGolden("plainTooltip_darkTheme")
    }

    @Composable
    private fun TestTooltips() {
        val scope = rememberCoroutineScope()

        PlainTooltipBox(
            tooltip = { Text("Tooltip Text") },
            modifier = Modifier.testTag(TooltipTestTag),
            tooltipState = tooltipState
        ) {}

        scope.launch { tooltipState.show() }
    }

    private fun assertAgainstGolden(goldenName: String) {
        rule.onNodeWithTag(TooltipTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenName)
    }
}

private const val TooltipTestTag = "tooltip"