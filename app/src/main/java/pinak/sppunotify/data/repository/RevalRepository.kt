package pinak.sppunotify.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pinak.sppunotify.data.local.RevalCourseDao
import pinak.sppunotify.data.local.RevalCourseEntity
import pinak.sppunotify.data.remote.RevalCourse
import pinak.sppunotify.data.remote.RevaluationScraper
import javax.inject.Inject
import javax.inject.Singleton

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import pinak.sppunotify.data.remote.ServerStatus

@Singleton
class RevalRepository @Inject constructor(
    private val scraper: RevaluationScraper,
    private val dao: RevalCourseDao,
) {
    private val _serverStatus = MutableStateFlow<ServerStatus?>(null)
    val serverStatus = _serverStatus.asStateFlow()

    suspend fun updateServerStatus() {
        val status = scraper.checkServerHealth()
        _serverStatus.value = status
    }
    suspend fun getAllCourses(): List<RevalCourse> = withContext(Dispatchers.IO) {
        dao.getAllCourses().map { entity ->
            RevalCourse(
                course = entity.course,
                subject = entity.subject,
                eventTarget = entity.eventTarget,
            )
        }
    }

    suspend fun fetchAndCacheAllCourses(): List<RevalCourse> = withContext(Dispatchers.IO) {
        val currentCourses = scraper.scrapeCourses()

        val entities = currentCourses.map { course ->
            RevalCourseEntity(
                eventTarget = course.eventTarget,
                course = course.course,
                subject = course.subject,
            )
        }
        dao.clearAll()
        dao.insertCourses(entities)

        currentCourses
    }

    suspend fun checkForNewCourses(): List<RevalCourse> = withContext(Dispatchers.IO) {
        val currentCourses = scraper.scrapeCourses()
        val existingTargets = dao.getAllEventTargets().toSet()

        val newCourses = currentCourses.filter { it.eventTarget !in existingTargets }

        val entities = currentCourses.map { course ->
            RevalCourseEntity(
                eventTarget = course.eventTarget,
                course = course.course,
                subject = course.subject,
            )
        }
        dao.insertCourses(entities)

        newCourses
    }

    suspend fun hardRefresh(): List<RevalCourse> = withContext(Dispatchers.IO) {
        val fresh = scraper.scrapeCourses()
        val entities = fresh.map { course ->
            RevalCourseEntity(
                eventTarget = course.eventTarget,
                course = course.course,
                subject = course.subject,
            )
        }
        dao.clearAll()
        dao.insertCourses(entities)
        fresh
    }

    suspend fun searchRevaluation(
        eventTarget: String,
        searchBy: String,
        searchValue: String,
    ): String = withContext(Dispatchers.IO) {
        scraper.searchRevaluation(eventTarget, searchBy, searchValue)
    }
}
