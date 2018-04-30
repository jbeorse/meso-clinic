package org.watsi.uhp.services

import org.watsi.domain.entities.Delta
import org.watsi.domain.repositories.DeltaRepository
import org.watsi.domain.repositories.EncounterFormRepository
import org.watsi.domain.repositories.EncounterRepository
import org.watsi.domain.repositories.IdentificationEventRepository
import org.watsi.domain.repositories.MemberRepository

import javax.inject.Inject

class SyncService : AbstractSyncJobService() {

    @Inject lateinit var identificationEventRepository: IdentificationEventRepository
    @Inject lateinit var encounterRepository: EncounterRepository
    @Inject lateinit var encounterFormRepository: EncounterFormRepository
    @Inject lateinit var memberRepository: MemberRepository
    @Inject lateinit var deltaRepository: DeltaRepository

    override fun performSync(): Boolean {
        val unsyncedMembers = deltaRepository.unsynced(Delta.ModelName.MEMBER).blockingGet()
        unsyncedMembers.groupBy { it.modelId }.forEach { _, deltas ->
            memberRepository.sync(deltas)
        }

        val unsyncedIdentificationEvents = deltaRepository
                .unsynced(Delta.ModelName.IDENTIFICATION_EVENT).blockingGet()
        unsyncedIdentificationEvents.groupBy { it.modelId }.forEach { _, deltas ->
            identificationEventRepository.sync(deltas)
        }

        val unsyncedEncounters = deltaRepository.unsynced(Delta.ModelName.ENCOUNTER).blockingGet()
        unsyncedEncounters.groupBy { it.modelId }.forEach { _, deltas ->
            encounterRepository.sync(deltas)
        }

        val unsyncedEncounterForms = deltaRepository.unsynced(Delta.ModelName.ENCOUNTER_FORM).blockingGet()
        unsyncedEncounterForms.groupBy { it.modelId }.forEach { _, deltas ->
            encounterFormRepository.sync(deltas)
        }

        return true
    }
}
