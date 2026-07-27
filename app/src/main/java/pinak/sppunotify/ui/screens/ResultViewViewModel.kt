package pinak.sppunotify.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.net.Uri
import pinak.sppunotify.data.local.DownloadedResultEntity
import pinak.sppunotify.data.local.PreferenceManager
import pinak.sppunotify.data.local.ResultEntity
import pinak.sppunotify.data.remote.ResultScraper
import pinak.sppunotify.data.repository.ResultRepository
import pinak.sppunotify.data.repository.VaultRepository
import javax.inject.Inject

data class ResultViewState(
    val result: ResultEntity? = null,
    val captchaBitmap: Bitmap? = null,
    val orgCaptchaText: String = "",
    val captchaImageStr: String = "",
    val isLoading: Boolean = false,
    val isLoadingCaptcha: Boolean = false,
    val error: String? = null,
    val resultBytes: ByteArray? = null,
    val resultMimeType: String = "",
    val seatNo: String = "",
    val motherName: String = "",
    val captchaText: String = "",
    val activeProfileName: String = "Default",
    val profiles: List<pinak.sppunotify.data.local.UserProfile> = emptyList()
)

sealed class ResultViewEvent {
    data class ShowSnackbar(val message: String) : ResultViewEvent()
    data class ShowErrorDialog(val title: String, val message: String) : ResultViewEvent()
    data class SaveResult(val bytes: ByteArray, val mimeType: String, val suggestedName: String) : ResultViewEvent()
}

@HiltViewModel
class ResultViewViewModel @Inject constructor(
    private val scraper: ResultScraper,
    private val repository: ResultRepository,
    private val vaultRepository: VaultRepository,
    private val preferenceManager: PreferenceManager,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val resultId: String = checkNotNull(savedStateHandle["resultId"])

    private val _state = MutableStateFlow(ResultViewState())
    val state = _state.asStateFlow()

    private val _events = MutableSharedFlow<ResultViewEvent>()
    val events = _events.asSharedFlow()

    init {
        loadResult()
        loadPreferences()
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            preferenceManager.preferencesFlow.collectLatest { prefs ->
                val activeProfile = prefs.profiles.find { it.id == prefs.activeProfileId }
                _state.value = _state.value.copy(
                    seatNo = activeProfile?.seatNo ?: _state.value.seatNo,
                    motherName = activeProfile?.motherName ?: _state.value.motherName,
                    activeProfileName = activeProfile?.name ?: "Default",
                    profiles = prefs.profiles
                )
            }
        }
    }

    fun switchProfile(profile: pinak.sppunotify.data.local.UserProfile) {
        _state.value = _state.value.copy(
            seatNo = profile.seatNo,
            motherName = profile.motherName,
            activeProfileName = profile.name
        )
    }

    fun updateSeatNo(value: String) { _state.value = _state.value.copy(seatNo = value) }
    fun updateMotherName(value: String) { _state.value = _state.value.copy(motherName = value) }
    fun updateCaptchaText(value: String) { _state.value = _state.value.copy(captchaText = value.take(5)) }

    private fun loadResult() {
        viewModelScope.launch {
            repository.results
                .map { results -> results.find { it.id == resultId } }
                .first { it != null }
                .let { result ->
                    _state.value = _state.value.copy(result = result)
                    loadCaptcha()
                }
        }
    }

    fun loadCaptcha() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingCaptcha = true, error = null)
            val captcha = scraper.fetchCaptcha()
            if (captcha != null) {
                val bytes = Base64.decode(captcha.imageBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                _state.value = _state.value.copy(
                    captchaBitmap = bitmap,
                    orgCaptchaText = captcha.orgCaptchaText,
                    captchaImageStr = captcha.imageBase64,
                    isLoadingCaptcha = false,
                )
            } else {
                _state.value = _state.value.copy(isLoadingCaptcha = false)
                _events.emit(ResultViewEvent.ShowErrorDialog(
                    "Captcha Error",
                    "Failed to load captcha from SPPU server. Check your connection or try again later."
                ))
            }
        }
    }

    fun submitForm() {
        val s = _state.value
        val result = s.result ?: return

        if (s.seatNo.isBlank()) {
            _state.value = s.copy(error = "Please enter Seat No")
            return
        }
        if (s.motherName.isBlank()) {
            _state.value = s.copy(error = "Please enter Mother Name")
            return
        }
        if (s.captchaText.length != 5) {
            _state.value = s.copy(error = "Captcha text must be 5 characters")
            return
        }
        if (s.orgCaptchaText.isEmpty()) {
            _state.value = s.copy(error = "Captcha not loaded. Tap refresh.")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            val isDummy = s.seatNo.equals("DUMMY", ignoreCase = true)
            
            if (!isDummy) {
                val valid = scraper.validateCaptcha(s.captchaText, s.orgCaptchaText)
                if (!valid) {
                    _state.value = _state.value.copy(isLoadingCaptcha = false)
                    _events.emit(ResultViewEvent.ShowErrorDialog(
                        "Invalid Captcha",
                        "The captcha text you entered was incorrect. A new captcha has been loaded — try again."
                    ))
                    loadCaptcha()
                    return@launch
                }
            }

            val submitResult = scraper.submitResult(
                patternName = result.patternName,
                patternId = result.patternId,
                seatNo = s.seatNo,
                motherName = s.motherName,
                captchaText = s.captchaText,
                orgCaptchaText = s.orgCaptchaText,
                captchaImageStr = s.captchaImageStr,
            )

            if (submitResult != null) {
                val mt = submitResult.mimeType.lowercase()
                val ext = if (mt.contains("pdf")) "pdf" else "html"
                
                // ULTRA DYNAMIC NAMING
                val deptTag = if (result.department.isNotBlank() && result.department != "Other UG") "[${result.department}]" else ""
                val seatTag = "(${s.seatNo})"
                val titleSlug = result.title
                    .replace(Regex("[^a-zA-Z0-9]"), " ")
                    .replace(Regex("\\s+"), "_")
                    .take(50)
                    .trim('_')
                
                val suggestedName = "${deptTag}${titleSlug}_${seatTag}_${result.publishedDate.replace(" ", "-")}.$ext"

                _state.value = _state.value.copy(
                    isLoading = false,
                    resultBytes = submitResult.bytes,
                    resultMimeType = submitResult.mimeType,
                )
                _events.emit(ResultViewEvent.SaveResult(submitResult.bytes, submitResult.mimeType, suggestedName))
            } else {
                _state.value = _state.value.copy(isLoadingCaptcha = false)
                _events.emit(ResultViewEvent.ShowErrorDialog(
                    "Server Busy",
                    "Failed to fetch result. The SPPU server may be busy or down (502/503). Please try again later."
                ))
            }
        }
    }

    fun onResultSaved(uri: Uri, suggestedName: String) {
        val s = _state.value
        val result = s.result ?: return
        
        viewModelScope.launch {
            vaultRepository.saveDownloadedResult(
                DownloadedResultEntity(
                    resultId = result.id,
                    title = result.title,
                    profileName = s.activeProfileName,
                    fileName = suggestedName,
                    filePath = uri.toString(),
                    mimeType = s.resultMimeType
                )
            )
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
