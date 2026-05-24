package pinak.sppunotify.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import pinak.sppunotify.data.local.ResultDatabase
import pinak.sppunotify.data.remote.ResultScraper
import pinak.sppunotify.data.repository.ResultRepository

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun scraper(): ResultScraper
    fun database(): ResultDatabase
    fun repository(): ResultRepository
}
