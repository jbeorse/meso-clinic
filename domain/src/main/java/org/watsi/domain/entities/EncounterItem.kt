package org.watsi.domain.entities

import java.util.UUID

data class EncounterItem(val id: UUID,
                         val encounterId: UUID,
                         val billableId: UUID,
                         val quantity: Int)
