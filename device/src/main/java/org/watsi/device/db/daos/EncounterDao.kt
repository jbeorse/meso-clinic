package org.watsi.device.db.daos

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Delete
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import android.arch.persistence.room.Transaction
import android.arch.persistence.room.Update
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Single
import org.watsi.device.db.models.BillableModel
import org.watsi.device.db.models.DeltaModel
import org.watsi.device.db.models.EncounterFormModel
import org.watsi.device.db.models.EncounterItemModel
import org.watsi.device.db.models.EncounterModel
import org.watsi.device.db.models.EncounterWithItemsModel
import org.watsi.device.db.models.EncounterWithMemberAndItemsAndFormsModel
import org.watsi.device.db.models.MemberModel
import org.watsi.device.db.models.PriceScheduleModel
import java.util.UUID

@Dao
interface EncounterDao {

    @Transaction
    @Query("SELECT * FROM encounters WHERE id = :id LIMIT 1")
    fun find(id: UUID): Single<EncounterWithItemsModel>

    @Transaction
    @Query("SELECT * FROM encounters WHERE id = :id LIMIT 1")
    fun findWithMemberAndForms(id: UUID): Single<EncounterWithMemberAndItemsAndFormsModel>

    @Transaction
    @Query("SELECT * FROM encounters WHERE id IN (:ids)")
    fun find(ids: List<UUID>): Single<List<EncounterModel>>

    @Update
    fun update(encounters: List<EncounterModel>): Int

    @Insert
    fun insert(encounterModel: EncounterModel,
               encounterItemModels: List<EncounterItemModel>,
               billableModels: List<BillableModel>,
               encounterFormModels: List<EncounterFormModel>,
               deltaModels: List<DeltaModel>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(encounterModels: List<EncounterModel>,
               encounterItemModels: List<EncounterItemModel>,
               billableModels: List<BillableModel>,
               priceScheduleModels: List<PriceScheduleModel>,
               memberModels: List<MemberModel>)

    @Delete
    fun delete(encounterModel: EncounterModel,
               encounterItemModels: List<EncounterItemModel>)

    @Transaction
    @Query("SELECT * from encounters WHERE submittedAt IS NULL ORDER BY occurredAt")
    fun pending(): Flowable<List<EncounterWithMemberAndItemsAndFormsModel>>

    @Transaction
    @Query("SELECT * from encounters WHERE submittedAt IS NULL ORDER BY occurredAt LIMIT 1")
    fun loadOnePendingClaim(): Maybe<EncounterWithMemberAndItemsAndFormsModel>

    @Transaction
    @Query("SELECT COUNT(*) from encounters WHERE submittedAt IS NULL")
    fun pendingCount(): Flowable<Int>

    @Transaction
    @Query("SELECT * from encounters WHERE adjudicationState = 'RETURNED' ORDER BY occurredAt")
    fun returned(): Flowable<List<EncounterWithMemberAndItemsAndFormsModel>>

    @Transaction
    @Query("SELECT * from encounters WHERE adjudicationState = 'RETURNED' ORDER BY occurredAt LIMIT 1")
    fun loadOneReturnedClaim(): Maybe<EncounterWithMemberAndItemsAndFormsModel>

    @Transaction
    @Query("SELECT COUNT(*) from encounters WHERE adjudicationState = 'RETURNED'")
    fun returnedCount(): Flowable<Int>

    @Transaction
    @Query("SELECT id from encounters WHERE adjudicationState = 'RETURNED'")
    fun returnedIds(): Single<List<UUID>>

    @Transaction
    @Query("SELECT DISTINCT(revisedEncounterId) from encounters WHERE revisedEncounterId IS NOT NULL")
    fun revisedIds(): Single<List<UUID>>

    @Query("SELECT encounters.* FROM encounters\n" +
            "INNER JOIN deltas ON\n" +
            "(encounters.id = deltas.modelId AND\n" +
            "deltas.synced = 0 AND\n" +
            "deltas.modelName = 'ENCOUNTER')")
    fun unsynced(): Single<List<EncounterWithMemberAndItemsAndFormsModel>>
}