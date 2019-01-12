package org.watsi.device.db.daos

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import io.reactivex.Single
import org.watsi.device.db.models.DiagnosisModel

@Dao
interface DiagnosisDao {

    @Insert
    fun insert(model: DiagnosisModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(models: List<DiagnosisModel>)

    @Query("SELECT * FROM diagnoses")
    fun all(): Single<List<DiagnosisModel>>

    @Query("SELECT * FROM diagnoses WHERE id IN (:ids)")
    fun findAll(ids: List<Int>): Single<List<DiagnosisModel>>

    @Query("DELETE FROM diagnoses WHERE id IN (:ids)")
    fun delete(ids: List<Int>)

    @Query("SELECT count(*) from diagnoses")
    fun count(): Single<Int>
}