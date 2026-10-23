package com.example

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.ui.AppViewModel
import com.example.ui.screens.SettingsScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("ClassHeure", appName)
  }

  @Test
  fun `test settings screen composition`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val database = AppDatabase.getDatabase(context)
    val repository = AppRepository(database.appDao())
    val viewModel = AppViewModel(repository)

    composeTestRule.setContent {
      SettingsScreen(viewModel = viewModel)
    }
    composeTestRule.waitForIdle()
  }

  @Test
  fun `test navigate to settings screen`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val database = AppDatabase.getDatabase(context)
    val repository = AppRepository(database.appDao())
    val viewModel = AppViewModel(repository)

    composeTestRule.setContent {
      MainAppLayout(viewModel = viewModel)
    }
    composeTestRule.waitForIdle()

    // Find settings button and click it
    composeTestRule.onNodeWithContentDescription("Settings").performClick()
    composeTestRule.waitForIdle()
  }
}
