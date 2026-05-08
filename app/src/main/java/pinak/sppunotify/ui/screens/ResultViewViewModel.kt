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
import pinak.sppunotify.data.local.ResultEntity
import pinak.sppunotify.data.remote.ResultScraper
import pinak.sppunotify.data.repository.ResultRepository
import javax.inject.Inject

data class ResultViewState(
    val result: ResultEntity? = null,
    val captchaBitmap: Bitmap? = null,
    val orgCaptchaText: String = "",
    val captchaImageStr: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val resultBytes: ByteArray? = null,
    val resultMimeType: String = "",
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
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val resultId: String = checkNotNull(savedStateHandle["resultId"])

    private val _state = MutableStateFlow(ResultViewState())
    val state = _state.asStateFlow()

    private val _events = MutableSharedFlow<ResultViewEvent>()
    val events = _events.asSharedFlow()

    init {
        loadResult()
    }

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
            _state.value = _state.value.copy(isLoading = true, error = null)
            val captcha = scraper.fetchCaptcha()
            if (captcha != null) {
                val bytes = Base64.decode(captcha.imageBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                _state.value = _state.value.copy(
                    captchaBitmap = bitmap,
                    orgCaptchaText = captcha.orgCaptchaText,
                    captchaImageStr = captcha.imageBase64,
                    isLoading = false,
                )
            } else {
                _state.value = _state.value.copy(isLoading = false)
                _events.emit(ResultViewEvent.ShowErrorDialog(
                    "Captcha Error",
                    "Failed to load captcha from SPPU server. Check your connection or try again later."
                ))
            }
        }
    }

    fun submitForm(
        seatNo: String,
        motherName: String,
        captchaText: String,
    ) {
        val s = _state.value
        val result = s.result ?: return

        if (seatNo.isBlank()) {
            _state.value = s.copy(error = "Please enter Seat No")
            return
        }
        if (motherName.isBlank()) {
            _state.value = s.copy(error = "Please enter Mother Name")
            return
        }
        if (captchaText.length != 5) {
            _state.value = s.copy(error = "Captcha text must be 5 characters")
            return
        }
        if (s.orgCaptchaText.isEmpty()) {
            _state.value = s.copy(error = "Captcha not loaded. Tap refresh.")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            val valid = scraper.validateCaptcha(captchaText, s.orgCaptchaText)
            if (!valid) {
                _state.value = _state.value.copy(isLoading = false)
                _events.emit(ResultViewEvent.ShowErrorDialog(
                    "Invalid Captcha",
                    "The captcha text you entered was incorrect. A new captcha has been loaded — try again."
                ))
                loadCaptcha()
                return@launch
            }

            val submitResult = scraper.submitResult(
                patternName = result.patternName,
                patternId = result.patternId,
                seatNo = seatNo,
                motherName = motherName,
                captchaText = captchaText,
                orgCaptchaText = s.orgCaptchaText,
                captchaImageStr = s.captchaImageStr,
            )

            if (submitResult != null) {
                val mt = submitResult.mimeType.lowercase()
                val ext = if (mt.contains("pdf")) "pdf" else "html"
                val safeName = result.title
                    .replace(Regex("[^a-zA-Z0-9_\\- ]"), "")
                    .take(80)
                    .trim()
                val suggestedName = "${safeName}_${result.publishedDate}.$ext"
                _state.value = _state.value.copy(
                    isLoading = false,
                    resultBytes = submitResult.bytes,
                    resultMimeType = submitResult.mimeType,
                )
                _events.emit(ResultViewEvent.SaveResult(submitResult.bytes, submitResult.mimeType, suggestedName))
            } else {
                _state.value = _state.value.copy(isLoading = false)
                _events.emit(ResultViewEvent.ShowErrorDialog(
                    "Server Busy",
                    "Failed to fetch result. The SPPU server may be busy or down (502/503). Please try again later."
                ))
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
