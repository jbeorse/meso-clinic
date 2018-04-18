package org.watsi.uhp.repositories

import org.watsi.domain.entities.Delta
import org.watsi.uhp.models.Encounter

interface EncounterRepository {
    fun create(encounter: Encounter)
    fun sync(deltas: List<Delta>)
}
