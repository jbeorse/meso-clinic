package org.watsi.device.db.models

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import org.threeten.bp.Clock
import org.threeten.bp.Instant
import org.watsi.domain.entities.EncounterItem
import java.util.UUID

@Entity(tableName = "encounter_items")
data class EncounterItemModel(@PrimaryKey val id: UUID,
                              val createdAt: Instant,
                              val updatedAt: Instant,
                              val encounterId: UUID,
                              val billableId: UUID,
                              val quantity: Int,
                              val priceScheduleId: UUID,
                              val priceScheduleIssued: Boolean) {

    fun toEncounterItem(): EncounterItem {
        return EncounterItem(
            id = id,
            encounterId = encounterId,
            billableId = billableId,
            quantity = quantity,
            priceScheduleId = priceScheduleId,
            priceScheduleIssued = priceScheduleIssued
        )
    }

    companion object {
        fun fromEncounterItem(encounterItem: EncounterItem, clock: Clock): EncounterItemModel {
            val now = clock.instant()
            return EncounterItemModel(
                id = encounterItem.id,
                createdAt = now,
                updatedAt = now,
                encounterId = encounterItem.encounterId,
                billableId = encounterItem.billableId,
                quantity = encounterItem.quantity,
                priceScheduleId = encounterItem.priceScheduleId,
                priceScheduleIssued = encounterItem.priceScheduleIssued
            )
        }
    }
}