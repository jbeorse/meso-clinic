package org.watsi.uhp.di.modules

import android.arch.persistence.room.Room
import android.content.Context
import dagger.Module
import dagger.Provides
import org.threeten.bp.Clock
import org.watsi.device.api.CoverageApi
import org.watsi.device.db.AppDatabase
import org.watsi.device.db.daos.BillableDao
import org.watsi.device.db.daos.DeltaDao
import org.watsi.device.db.daos.DiagnosisDao
import org.watsi.device.db.daos.EncounterDao
import org.watsi.device.db.daos.EncounterFormDao
import org.watsi.device.db.daos.IdentificationEventDao
import org.watsi.device.db.daos.MemberDao
import org.watsi.device.db.daos.PhotoDao
import org.watsi.device.db.repositories.BillableRepositoryImpl
import org.watsi.device.db.repositories.DeltaRepositoryImpl
import org.watsi.device.db.repositories.DiagnosisRepositoryImpl
import org.watsi.device.db.repositories.EncounterFormRepositoryImpl
import org.watsi.device.db.repositories.EncounterRepositoryImpl
import org.watsi.device.db.repositories.IdentificationEventRepositoryImpl
import org.watsi.device.db.repositories.MemberRepositoryImpl
import org.watsi.device.db.repositories.PhotoRepositoryImpl
import org.watsi.device.managers.PreferencesManager
import org.watsi.device.managers.SessionManager
import org.watsi.domain.repositories.BillableRepository
import org.watsi.domain.repositories.DeltaRepository
import org.watsi.domain.repositories.DiagnosisRepository
import org.watsi.domain.repositories.EncounterFormRepository
import org.watsi.domain.repositories.EncounterRepository
import org.watsi.domain.repositories.IdentificationEventRepository
import org.watsi.domain.repositories.MemberRepository
import org.watsi.domain.repositories.PhotoRepository
import javax.inject.Singleton

@Module
class DbModule {

    @Singleton
    @Provides
    fun provideDatabase(context: Context): AppDatabase {
        val builder = Room.databaseBuilder(context, AppDatabase::class.java, "submission")
        builder.allowMainThreadQueries()
        return builder.build()
    }

    @Singleton
    @Provides
    fun provideBillableDao(db: AppDatabase): BillableDao = db.billableDao()

    @Singleton
    @Provides
    fun provideDeltaDao(db: AppDatabase): DeltaDao = db.deltaDao()

    @Singleton
    @Provides
    fun provideDiagonsisDao(db: AppDatabase): DiagnosisDao = db.diagnosisDao()

    @Singleton
    @Provides
    fun provideEncounterDao(db: AppDatabase): EncounterDao = db.encounterDao()

    @Singleton
    @Provides
    fun provideEncounterFormDao(db: AppDatabase): EncounterFormDao = db.encounterFormDao()

    @Singleton
    @Provides
    fun provideIdentificationEventDao(db: AppDatabase): IdentificationEventDao =
            db.identificationEventDao()

    @Singleton
    @Provides
    fun provideMemberDao(db: AppDatabase): MemberDao = db.memberDao()

    @Singleton
    @Provides
    fun providePhotoDao(db: AppDatabase): PhotoDao = db.photoDao()

    @Provides
    fun provideBillableRepository(billableDao: BillableDao, clock: Clock): BillableRepository =
            BillableRepositoryImpl(billableDao, clock)

    @Provides
    fun provideDeltaRepository(deltaDao: DeltaDao, clock: Clock): DeltaRepository {
        return DeltaRepositoryImpl(deltaDao, clock)
    }

    @Provides
    fun provideDiagnosisRepository(diagnosisDao: DiagnosisDao, clock: Clock): DiagnosisRepository =
            DiagnosisRepositoryImpl(diagnosisDao, clock)

    @Provides
    fun provideEncounterRepository(encounterDao: EncounterDao, clock: Clock): EncounterRepository =
            EncounterRepositoryImpl(encounterDao, clock)

    @Provides
    fun provideEncounterFormRepository(encounterFormDao: EncounterDao,
                                       clock: Clock): EncounterFormRepository =
            EncounterFormRepositoryImpl()

    @Provides
    fun provideIdentificationEventRepository(identificationEventDao: IdentificationEventDao,
                                             clock: Clock): IdentificationEventRepository {
        return IdentificationEventRepositoryImpl(identificationEventDao, clock)
    }

    @Provides
    fun provideMemberRepository(memberDao: MemberDao,
                                api: CoverageApi,
                                sessionManager: SessionManager,
                                preferencesManager: PreferencesManager,
                                clock: Clock): MemberRepository =
            MemberRepositoryImpl(memberDao, api, sessionManager, preferencesManager, clock)

    @Provides
    fun providePhotoRepository(photoDao: PhotoDao,
                               clock: Clock,
                               context: Context): PhotoRepository {
        return PhotoRepositoryImpl(photoDao, clock, context.contentResolver)
    }
}
