package pinak.sppunotify.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pinak.sppunotify.data.local.RevalCourseDao
import pinak.sppunotify.data.local.RevalCourseEntity
import pinak.sppunotify.data.remote.RevalCourse
import pinak.sppunotify.data.remote.RevaluationScraper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RevalRepository @Inject constructor(
    private val scraper: RevaluationScraper,
    private val dao: RevalCourseDao,
) {
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
}
