package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        androidx.compose.material3.Card(
          modifier = androidx.compose.ui.Modifier.padding(16.dp),
          colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = androidx.compose.ui.graphics.Color(0xFFEFEFF4)
          )
        ) {
          androidx.compose.foundation.layout.Column(
            modifier = androidx.compose.ui.Modifier.padding(24.dp)
          ) {
            androidx.compose.material3.Text(
              text = "ZÜRICH FLOW",
              style = androidx.compose.material3.MaterialTheme.typography.displayMedium,
              color = androidx.compose.ui.graphics.Color(0xFFE30613)
            )
            androidx.compose.material3.Text(
              text = "Precision in Motion.",
              style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
              color = androidx.compose.ui.graphics.Color(0xFF1C1C1E)
            )
          }
        }
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
