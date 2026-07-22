package pinak.sppunotify.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import pinak.sppunotify.data.local.ExamDateDao
import pinak.sppunotify.data.local.ExamDateEntity
import pinak.sppunotify.data.remote.ExamDateDto
import pinak.sppunotify.data.remote.ExamDateScraper
import javax.inject.Inject
import javax.inject.Singleton

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class ExamDateRepository @Inject constructor(
    private val scraper: ExamDateScraper,
    private val dao: ExamDateDao
) {
    val examDates: Flow<List<ExamDateEntity>> = dao.getAllExamDates()

    private val _serverStatus = MutableStateFlow<pinak.sppunotify.data.remote.ServerStatus?>(null)
    val serverStatus = _serverStatus.asStateFlow()

    suspend fun updateServerStatus() {
        val status = scraper.checkServerHealth()
        _serverStatus.value = status
    }

    suspend fun hardRefresh(): List<ExamDateDto> = withContext(Dispatchers.IO) {
        dao.clearAll()
        refreshExamDatesInternal()
    }

    suspend fun refreshExamDates(): List<ExamDateDto> = refreshExamDatesInternal()

    private suspend fun refreshExamDatesInternal(): List<ExamDateDto> = withContext(Dispatchers.IO) {
        val dtos = scraper.scrapeExamDates()
        
        val existingNames = dao.getAllCourseNames().toSet()
        val newDates = dtos.filter { it.courseName !in existingNames }

        if (dtos.isNotEmpty()) {
            val entities = dtos.map { it.toEntity() }
            dao.insertExamDates(entities)
        }
        newDates
    }

    private fun ExamDateDto.toEntity() = ExamDateEntity(
        courseName = courseName,
        status = status,
        startDate = startDate,
        endDateWithoutLateFee = endDateWithoutLateFee,
        endDateWithLateFee = endDateWithLateFee
    )
}
