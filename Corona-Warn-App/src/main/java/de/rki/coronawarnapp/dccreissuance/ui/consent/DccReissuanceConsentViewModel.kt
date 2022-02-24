package de.rki.coronawarnapp.dccreissuance.ui.consent

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import de.rki.coronawarnapp.ccl.ui.text.CclTextFormatter
import de.rki.coronawarnapp.covidcertificate.common.certificate.DccQrCodeExtractor
import de.rki.coronawarnapp.covidcertificate.common.certificate.DccV1
import de.rki.coronawarnapp.covidcertificate.common.certificate.DccV1Parser
import de.rki.coronawarnapp.covidcertificate.person.core.PersonCertificatesProvider
import de.rki.coronawarnapp.util.ui.SingleLiveEvent
import de.rki.coronawarnapp.util.viewmodel.CWAViewModel
import de.rki.coronawarnapp.util.viewmodel.SimpleCWAViewModelFactory
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull

class DccReissuanceConsentViewModel @AssistedInject constructor(
    personCertificatesProvider: PersonCertificatesProvider,
    dccQrCodeExtractor: DccQrCodeExtractor,
    format: CclTextFormatter,
) : CWAViewModel() {

    @AssistedFactory
    interface Factory : SimpleCWAViewModelFactory<DccReissuanceConsentViewModel>

    internal val stateLiveData: MutableLiveData<State> = MutableLiveData()
    internal val certificateLiveData: MutableLiveData<DccV1.MetaData> = MutableLiveData()

    internal val event = SingleLiveEvent<Event>()

    init {
        personCertificatesProvider.personCertificates.mapNotNull {
            val walletInfo = it.first().dccWalletInfo
            if (walletInfo?.certificateReissuance?.reissuanceDivision?.visible == true) {
                val division = walletInfo.certificateReissuance.reissuanceDivision
                val state = State(
                    title = format(division.titleText),
                    subtitle = format(division.subtitleText),
                    content = format(division.longText),
                    url = division.faqAnchor
                )
                stateLiveData.postValue(state)
            }

            walletInfo?.certificateReissuance?.certificateToReissue?.certificateRef?.barcodeData?.let {
                val certificate = dccQrCodeExtractor.extract(
                    it,
                    DccV1Parser.Mode.CERT_SINGLE_STRICT
                )
                certificateLiveData.postValue(certificate.data.certificate)
            }
        }.launchIn(viewModelScope)
    }

    internal fun startReissuance() {
        event.postValue(ReissuanceInProgress)
        // call api
    }

    internal data class State(
        val title: String,
        val subtitle: String,
        val content: String,
        val url: String?,
    )

    internal sealed class Event
    internal object ReissuanceInProgress : Event()
    internal object ReissuanceSuccess : Event()
    internal object ReissuanceError : Event()
}
