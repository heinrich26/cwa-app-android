package de.rki.coronawarnapp.ui.launcher

import android.app.Activity
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.install.model.ActivityResult
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import de.rki.coronawarnapp.environment.BuildConfigWrap
import de.rki.coronawarnapp.main.CWASettings
import de.rki.coronawarnapp.storage.OnboardingSettings
import de.rki.coronawarnapp.update.getUpdateInfo
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.instanceOf
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import testhelpers.BaseTest
import testhelpers.TestDispatcherProvider
import testhelpers.extensions.InstantExecutorExtension
import testhelpers.preferences.mockFlowPreference

@ExtendWith(InstantExecutorExtension::class)
class LauncherActivityViewModelTest : BaseTest() {

    @MockK lateinit var appUpdateManager: AppUpdateManager
    @MockK lateinit var cwaSettings: CWASettings
    @MockK lateinit var onboardingSettings: OnboardingSettings

    @BeforeEach
    fun setupFreshViewModel() {
        MockKAnnotations.init(this)
        mockkStatic("de.rki.coronawarnapp.update.InAppUpdateKt")

        every { onboardingSettings.isOnboarded } returns false

        mockkObject(BuildConfigWrap)
        every { BuildConfigWrap.VERSION_CODE } returns 10L

        coEvery { appUpdateManager.getUpdateInfo() } returns
            mockk<AppUpdateInfo>().apply {
                every { updateAvailability() } returns UpdateAvailability.UPDATE_NOT_AVAILABLE
            }

        every {
            appUpdateManager.startUpdateFlowForResult(
                any(),
                AppUpdateType.IMMEDIATE,
                any<Activity>(),
                any()
            )
        } returns true
    }

    private fun createViewModel() = LauncherActivityViewModel(
        appUpdateManager = appUpdateManager,
        dispatcherProvider = TestDispatcherProvider(),
        cwaSettings = cwaSettings,
        onboardingSettings = onboardingSettings
    )

    @Test
    fun `update is available and checked in init`() = runBlockingTest {
        coEvery { appUpdateManager.getUpdateInfo() } returns
            mockk<AppUpdateInfo>().apply {
                every { updateAvailability() } returns UpdateAvailability.UPDATE_AVAILABLE
            }
        val vm = createViewModel()

        vm.events.value shouldBe instanceOf(LauncherEvent.ForceUpdate::class)
    }

    @Test
    fun `Force update event triggers update`() {
        coEvery { appUpdateManager.getUpdateInfo() } returns
            mockk<AppUpdateInfo>().apply {
                every { updateAvailability() } returns UpdateAvailability.UPDATE_AVAILABLE
            }
        val vm = createViewModel()

        (vm.events.value as LauncherEvent.ForceUpdate).apply {
            forceUpdate(mockk())
            verify {
                appUpdateManager.startUpdateFlowForResult(
                    any(),
                    AppUpdateType.IMMEDIATE,
                    any<Activity>(),
                    any()
                )
            }
        }
    }

    @Test
    fun `On resume update is resumed`() {
        coEvery { appUpdateManager.getUpdateInfo() } returns
            mockk<AppUpdateInfo>().apply {
                every { updateAvailability() } returns UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
            }
        val vm = createViewModel()
        vm.onResume()
        (vm.events.value as LauncherEvent.ForceUpdate).apply {
            forceUpdate(mockk())
            verify {
                appUpdateManager.startUpdateFlowForResult(
                    any(),
                    AppUpdateType.IMMEDIATE,
                    any<Activity>(),
                    any()
                )
            }
        }
    }

    @Test
    fun `onResult nothing happens if requestCode is different`() {
        val vm = createViewModel()
        vm.onResult(1000, Activity.RESULT_OK)
        vm.events.value shouldBe LauncherEvent.GoToOnboarding // From initialization
    }

    @Test
    fun `onResult nothing happens if resultCode is OK`() {
        val vm = createViewModel()
        vm.onResult(LauncherActivityViewModel.UPDATE_CODE, Activity.RESULT_OK)
        vm.events.value shouldBe LauncherEvent.GoToOnboarding // From initialization
    }

    @Test
    fun `onResult ask user to update if resultCode is CANCLED`() {
        val vm = createViewModel()
        vm.onResult(LauncherActivityViewModel.UPDATE_CODE, Activity.RESULT_CANCELED)
        vm.events.value shouldBe LauncherEvent.ShowUpdateDialog
    }

    @Test
    fun `onResult ask user to update if resultCode is RESULT_IN_APP_UPDATE_FAILED`() {
        val vm = createViewModel()
        vm.onResult(LauncherActivityViewModel.UPDATE_CODE, ActivityResult.RESULT_IN_APP_UPDATE_FAILED)
        vm.events.value shouldBe LauncherEvent.ShowUpdateDialog
    }

    @Test
    fun `request update event triggers update`() {
        coEvery { appUpdateManager.getUpdateInfo() } returns
            mockk<AppUpdateInfo>().apply {
                every { updateAvailability() } returns UpdateAvailability.UPDATE_AVAILABLE
            }
        val vm = createViewModel()

        (vm.events.value as LauncherEvent.ForceUpdate).apply {
            forceUpdate(mockk())
            verify {
                appUpdateManager.startUpdateFlowForResult(
                    any(),
                    AppUpdateType.IMMEDIATE,
                    any<Activity>(),
                    any()
                )
            }
        }
    }

    @Test
    fun `fresh install no update needed`() {
        val vm = createViewModel()

        vm.events.value shouldBe LauncherEvent.GoToOnboarding
    }

    @Test
    fun `onboarding finished`() {
        every { onboardingSettings.isOnboarded } returns true
        every { cwaSettings.wasInteroperabilityShownAtLeastOnce } returns true
        every { cwaSettings.lastChangelogVersion } returns mockFlowPreference(10L)

        val vm = createViewModel()

        vm.events.value shouldBe LauncherEvent.GoToMainActivity
    }
}
