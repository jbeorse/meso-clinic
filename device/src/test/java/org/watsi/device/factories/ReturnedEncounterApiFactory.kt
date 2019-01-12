package org.watsi.device.factories

import org.threeten.bp.Clock
import org.threeten.bp.Instant
import org.watsi.device.api.models.BillableApi
import org.watsi.device.api.models.EncounterItemApi
import org.watsi.device.api.models.MemberApi
import org.watsi.device.api.models.PriceScheduleApi
import org.watsi.device.api.models.ReturnedEncounterApi
import org.watsi.domain.relations.EncounterWithExtras
import java.util.UUID

object ReturnedEncounterApiFactory {

    fun build(
        encounterWithExtras: EncounterWithExtras,
        clock: Clock = Clock.systemUTC()
    ) : ReturnedEncounterApi {
        val now = Instant.now(clock)
        return ReturnedEncounterApi(
            id = encounterWithExtras.encounter.id,
            memberId = encounterWithExtras.encounter.memberId,
            identificationEventId = encounterWithExtras.encounter.identificationEventId,
            occurredAt = encounterWithExtras.encounter.occurredAt,
            backdatedOccurredAt = encounterWithExtras.encounter.backdatedOccurredAt,
            diagnoses = encounterWithExtras.encounter.diagnoses,
            visitType = encounterWithExtras.encounter.visitType,
            claimId = encounterWithExtras.encounter.claimId,
            adjudicationState = encounterWithExtras.encounter.adjudicationState.toString(),
            adjudicatedAt = encounterWithExtras.encounter.adjudicatedAt ?: now,
            returnReason = encounterWithExtras.encounter.returnReason ?: "return reason",
            revisedEncounterId = encounterWithExtras.encounter.revisedEncounterId ?: UUID.randomUUID(),
            providerComment = encounterWithExtras.encounter.providerComment,
            preparedAt = encounterWithExtras.encounter.preparedAt ?: now,
            submittedAt = encounterWithExtras.encounter.submittedAt ?: now,
            memberApi = MemberApi(encounterWithExtras.member),
            billablesApi = encounterWithExtras.encounterItemRelations.map {
                BillableApi(it.billableWithPriceSchedule.billable)
            },
            priceSchedulesApi = encounterWithExtras.encounterItemRelations.map {
                PriceScheduleApi(it.billableWithPriceSchedule.priceSchedule)
            },
            encounterItemsApi = encounterWithExtras.encounterItemRelations.map {
                EncounterItemApi(it.encounterItem)
            }
        )
    }
}