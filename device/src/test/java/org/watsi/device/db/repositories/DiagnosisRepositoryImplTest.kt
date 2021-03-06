package org.watsi.device.db.repositories

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Single
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.threeten.bp.Clock
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.watsi.device.api.CoverageApi
import org.watsi.device.api.models.DiagnosisApi
import org.watsi.device.db.daos.DiagnosisDao
import org.watsi.device.factories.DiagnosisModelFactory
import org.watsi.device.managers.PreferencesManager
import org.watsi.device.managers.SessionManager
import org.watsi.domain.factories.AuthenticationTokenFactory

@RunWith(MockitoJUnitRunner::class)
class DiagnosisRepositoryImplTest {

    @Mock lateinit var mockDao: DiagnosisDao
    @Mock lateinit var mockApi: CoverageApi
    @Mock lateinit var mockSessionManager: SessionManager
    @Mock lateinit var mockPreferencesManager: PreferencesManager
    val clock = Clock.fixed(Instant.now(), ZoneId.of("UTC"))
    lateinit var repository: DiagnosisRepositoryImpl

    @Before
    fun setup() {
        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }

        repository = DiagnosisRepositoryImpl(
                mockDao, mockApi, mockSessionManager, mockPreferencesManager, clock)
    }

    @Test
    fun all() {
        val diagnosesModels = listOf(DiagnosisModelFactory.build(), DiagnosisModelFactory.build())
        whenever(mockDao.allActive()).thenReturn(Single.just(diagnosesModels))

        repository.allActive().test().assertValue(diagnosesModels.map { it.toDiagnosis() })
    }

    @Test
    fun delete() {
        val ids = (1..1001).map { it }

        repository.delete(ids).test().assertComplete()

        verify(mockDao).delete(ids.take(999))
        verify(mockDao).delete(ids.takeLast(2))
    }

    @Test
    fun fetch_noCurrentAuthenticationToken_errors() {
        whenever(mockSessionManager.currentAuthenticationToken()).thenReturn(null)

        repository.fetch().test().assertError(Exception::class.java)
    }

    @Test
    fun fetch_hasToken_savesResponse() {
        val authToken = AuthenticationTokenFactory.build()
        val noChange = DiagnosisModelFactory.build(id = 1, clock = clock)
        val noChangeApi = DiagnosisApi(noChange.toDiagnosis())
        val serverEdited = DiagnosisModelFactory.build(id = 2, description = "maleria", clock = clock)
        val serverEditedApi = DiagnosisApi(serverEdited.copy(description = "malaria").toDiagnosis())
        val serverAdded = DiagnosisModelFactory.build(id = 4, clock = clock)
        val serverAddedApi = DiagnosisApi(serverAdded.toDiagnosis())
        val serverRemoved = DiagnosisModelFactory.build(id = 3, clock = clock)

        whenever(mockSessionManager.currentAuthenticationToken()).thenReturn(authToken)
        whenever(mockApi.getDiagnoses(any())).thenReturn(Single.just(listOf(
                noChangeApi,
                serverEditedApi,
                serverAddedApi
        )))
        whenever(mockDao.allActive()).thenReturn(Single.just(listOf(
                noChange,
                serverEdited,
                serverRemoved
        )))
        whenever(mockDao.find(listOf(serverRemoved.id))).thenReturn(Single.just(listOf(
            serverRemoved
        )))

        repository.fetch().test().assertComplete()

        verify(mockApi).getDiagnoses(authToken.getHeaderString())
        verify(mockDao).upsert(listOf(serverRemoved.copy(active = false)))
        verify(mockDao).upsert(listOf(
                noChange,
                serverEdited.copy(description = "malaria"),
                serverAdded
        ))
        verify(mockPreferencesManager).updateDiagnosesLastFetched(clock.instant())
    }
}
