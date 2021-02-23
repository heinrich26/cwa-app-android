package de.rki.coronawarnapp.ui.settings.analytics

import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap
import de.rki.coronawarnapp.util.viewmodel.CWAViewModel
import de.rki.coronawarnapp.util.viewmodel.CWAViewModelFactory
import de.rki.coronawarnapp.util.viewmodel.CWAViewModelKey

@Module
abstract class SettingsPrivacyPreservingAnalyticsFragmentModule {

    @Binds
    @IntoMap
    @CWAViewModelKey(SettingsPrivacyPreservingAnalyticsViewModel::class)
    abstract fun settingsScreenVM(
        factory: SettingsPrivacyPreservingAnalyticsViewModel.Factory
    ): CWAViewModelFactory<out CWAViewModel>

    @ContributesAndroidInjector
    abstract fun privacyPreservingAnalyticsScreen(): SettingsPrivacyPreservingAnalyticsFragment
}
