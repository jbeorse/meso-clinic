package org.watsi.domain.usecases

import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import org.watsi.domain.entities.Delta
import org.watsi.domain.repositories.DeltaRepository
import org.watsi.domain.repositories.EncounterFormRepository

class SyncEncounterFormUseCase(
        private val encounterFormRepository: EncounterFormRepository,
        private val deltaRepository: DeltaRepository
) {
    fun execute(onError: (throwable: Throwable) -> Boolean): Completable {
        return Completable.fromAction {
            val unsyncedEncounterFormDeltas = deltaRepository.unsynced(
                    Delta.ModelName.ENCOUNTER_FORM).blockingGet()
            val unsyncedEncounterIds = deltaRepository.unsyncedModelIds(
                    Delta.ModelName.ENCOUNTER, Delta.Action.ADD).blockingGet()

            unsyncedEncounterFormDeltas.map { encounterFormDelta ->
                val encounterForm = encounterFormRepository.find(encounterFormDelta.modelId).blockingGet()
                val hasUnsyncedEncounter = unsyncedEncounterIds.contains(encounterForm.encounterForm.encounterId)

                if (!hasUnsyncedEncounter) {
                    Completable.concatArray(
                        encounterFormRepository.sync(encounterFormDelta),
                        deltaRepository.markAsSynced(listOf(encounterFormDelta))
                    ).onErrorComplete {
                        onError(it)
                    }.blockingAwait()
                }
            }
        }.subscribeOn(Schedulers.io())
    }
}
