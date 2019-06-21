package org.watsi.uhp.di.modules

import android.arch.persistence.room.Room
import android.content.Context
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import org.threeten.bp.Clock
import org.watsi.device.api.CoverageApi
import org.watsi.device.db.AppDatabase
import org.watsi.device.db.daos.BillableDao
import org.watsi.device.db.daos.DeltaDao
import org.watsi.device.db.daos.DiagnosisDao
import org.watsi.device.db.daos.EncounterDao
import org.watsi.device.db.daos.EncounterFormDao
import org.watsi.device.db.daos.EncounterItemDao
import org.watsi.device.db.daos.IdentificationEventDao
import org.watsi.device.db.daos.MemberDao
import org.watsi.device.db.daos.PhotoDao
import org.watsi.device.db.daos.PriceScheduleDao
import org.watsi.device.db.daos.ReferralDao
import org.watsi.device.db.repositories.BillableRepositoryImpl
import org.watsi.device.db.repositories.DeltaRepositoryImpl
import org.watsi.device.db.repositories.DiagnosisRepositoryImpl
import org.watsi.device.db.repositories.EncounterFormRepositoryImpl
import org.watsi.device.db.repositories.EncounterRepositoryImpl
import org.watsi.device.db.repositories.IdentificationEventRepositoryImpl
import org.watsi.device.db.repositories.MainRepositoryImpl
import org.watsi.device.db.repositories.MemberRepositoryImpl
import org.watsi.device.db.repositories.PhotoRepositoryImpl
import org.watsi.device.db.repositories.PriceScheduleRepositoryImpl
import org.watsi.device.db.repositories.ReferralRepositoryImpl
import org.watsi.device.managers.PreferencesManager
import org.watsi.device.managers.SessionManager
import org.watsi.domain.repositories.BillableRepository
import org.watsi.domain.repositories.DeltaRepository
import org.watsi.domain.repositories.DiagnosisRepository
import org.watsi.domain.repositories.EncounterFormRepository
import org.watsi.domain.repositories.EncounterRepository
import org.watsi.domain.repositories.IdentificationEventRepository
import org.watsi.domain.repositories.MainRepository
import org.watsi.domain.repositories.MemberRepository
import org.watsi.domain.repositories.PhotoRepository
import org.watsi.domain.repositories.PriceScheduleRepository
import org.watsi.domain.repositories.ReferralRepository
import javax.inject.Singleton

@Module
class DbModule {

    @Singleton
    @Provides
    fun provideDatabase(context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "submission")
                .fallbackToDestructiveMigration()
                .build()
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
    fun provideEncounterItemDao(db: AppDatabase): EncounterItemDao = db.encounterItemDao()

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

    @Singleton
    @Provides
    fun providePriceScheduleDao(db: AppDatabase): PriceScheduleDao = db.priceScheduleDao()

    @Singleton
    @Provides
    fun provideReferralDao(db: AppDatabase): ReferralDao = db.referralDao()

    @Provides
    fun provideBillableRepository(billableDao: BillableDao,
                                  api: CoverageApi,
                                  sessionManager: SessionManager,
                                  preferencesManager: PreferencesManager,
                                  clock: Clock): BillableRepository =
            BillableRepositoryImpl(billableDao, api, sessionManager, preferencesManager, clock)

    @Provides
    fun providePriceScheduleRepository(priceScheduleDao: PriceScheduleDao,
                                       api: CoverageApi,
                                       sessionManager: SessionManager,
                                       clock: Clock): PriceScheduleRepository {
        return PriceScheduleRepositoryImpl(priceScheduleDao, api, sessionManager, clock)
    }

    @Provides
    fun provideDeltaRepository(deltaDao: DeltaDao, clock: Clock): DeltaRepository {
        return DeltaRepositoryImpl(deltaDao, clock)
    }

    @Provides
    fun provideDiagnosisRepository(diagnosisDao: DiagnosisDao,
                                   api: CoverageApi,
                                   sessionManager: SessionManager,
                                   preferencesManager: PreferencesManager,
                                   clock: Clock): DiagnosisRepository =
            DiagnosisRepositoryImpl(diagnosisDao, api, sessionManager, preferencesManager, clock)

    @Provides
    fun provideEncounterRepository(encounterDao: EncounterDao,
                                   encounterItemDao: EncounterItemDao,
                                   diagnosisDao: DiagnosisDao,
                                   memberDao: MemberDao,
                                   api: CoverageApi,
                                   sessionManager: SessionManager,
                                   preferencesManager: PreferencesManager,
                                   clock: Clock): EncounterRepository =
            EncounterRepositoryImpl(encounterDao, encounterItemDao, diagnosisDao, memberDao, api, sessionManager, preferencesManager, clock)

    @Provides
    fun provideEncounterFormRepository(encounterFormDao: EncounterFormDao,
                                       api: CoverageApi,
                                       sessionManager: SessionManager,
                                       clock: Clock): EncounterFormRepository =
            EncounterFormRepositoryImpl(encounterFormDao, api, sessionManager, clock)

    @Provides
    fun provideIdentificationEventRepository(identificationEventDao: IdentificationEventDao,
                                             encounterDao: EncounterDao,
                                             api: CoverageApi,
                                             sessionManager: SessionManager,
                                             preferencesManager: PreferencesManager,
                                             clock: Clock): IdentificationEventRepository {
        return IdentificationEventRepositoryImpl(identificationEventDao, encounterDao, api, sessionManager, preferencesManager, clock)
    }

    @Provides
    fun provideMemberRepository(memberDao: MemberDao,
                                api: CoverageApi,
                                sessionManager: SessionManager,
                                preferencesManager: PreferencesManager,
                                photoDao: PhotoDao,
                                clock: Clock): MemberRepository =
            MemberRepositoryImpl(memberDao, api, sessionManager, preferencesManager, photoDao, clock)

    @Provides
    fun providePhotoRepository(photoDao: PhotoDao, clock: Clock): PhotoRepository {
        return PhotoRepositoryImpl(photoDao, clock)
    }

    @Provides
    fun provideReferralRepository(referralDao: ReferralDao): ReferralRepository {
        return ReferralRepositoryImpl(referralDao)
    }

    @Provides
    fun provideMainRepository(appDatabase: AppDatabase, okHttpClient: OkHttpClient): MainRepository {
        return MainRepositoryImpl(appDatabase, okHttpClient)
    }
}
