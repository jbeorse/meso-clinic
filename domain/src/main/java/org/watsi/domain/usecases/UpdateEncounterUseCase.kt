package org.watsi.domain.usecases

import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import org.watsi.domain.entities.Delta
import org.watsi.domain.entities.PriceSchedule
import org.watsi.domain.relations.EncounterWithExtras
import org.watsi.domain.repositories.EncounterRepository
import org.watsi.domain.repositories.PriceScheduleRepository
import org.watsi.domain.repositories.ReferralRepository

class UpdateEncounterUseCase(
    private val encounterRepository: EncounterRepository,
    private val referralRepository: ReferralRepository,
    private val priceScheduleRepository: PriceScheduleRepository
) {

    fun execute(encounterWithExtras: EncounterWithExtras): Completable {
        return Completable.fromAction {
            val savedEncounter = encounterRepository.find(encounterWithExtras.encounter.id).blockingGet()
            val removedEncounterItemIds = savedEncounter.encounterItemRelations.map { it.encounterItem.id }
                    .minus(encounterWithExtras.encounterItemRelations.map { it.encounterItem.id })
            val newPriceSchedules = encounterWithExtras.encounterItemRelations
                    .filter { it.encounterItem.priceScheduleIssued }
                    .map { it.billableWithPriceSchedule.priceSchedule }
                    .filter { priceScheduleRepository.find(it.id).blockingGet() == null }

            // Makes sure if facility head deletes a referral, it is deleted from the database.
            savedEncounter.referral?.let { oldReferral ->
                if (oldReferral.id != encounterWithExtras.referral?.id) {
                    referralRepository.delete(oldReferral.id).blockingAwait()
                }
            }
            // Upsert so that newly added encounterItems are saved
            createPriceSchedules(newPriceSchedules)
            encounterRepository.upsert(encounterWithExtras).blockingAwait()
            encounterRepository.deleteEncounterItems(removedEncounterItemIds).blockingAwait()
        }.subscribeOn(Schedulers.io())
    }

    // Always create price schedule with Delta for immediate syncing since we are never deleting
    // price schedules (see https://watsi.slack.com/archives/C03T9TUT1/p1538150314000100)
    private fun createPriceSchedules(newPriceSchedules: List<PriceSchedule>) {
        newPriceSchedules.forEach { priceSchedule ->
            val priceScheduleDelta = Delta(
                action = Delta.Action.ADD,
                modelName = Delta.ModelName.PRICE_SCHEDULE,
                modelId = priceSchedule.id
            )
            priceScheduleRepository.create(priceSchedule, priceScheduleDelta).blockingAwait()
        }
    }
}
