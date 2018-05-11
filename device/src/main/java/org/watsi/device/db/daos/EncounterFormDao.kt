package org.watsi.device.db.daos

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Query
import io.reactivex.Single
import org.watsi.device.db.models.EncounterFormModel
import org.watsi.device.db.models.EncounterFormWithEncounterAndPhotoModel
import java.util.UUID

@Dao
interface EncounterFormDao {

    @Insert
    fun insert(model: EncounterFormModel)

    @Query("SELECT * FROM encounter_forms WHERE id = :id LIMIT 1")
    fun find(id: UUID): Single<EncounterFormWithEncounterAndPhotoModel>
}
