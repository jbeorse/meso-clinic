package org.watsi.domain.entities

import org.threeten.bp.Instant
import java.io.Serializable
import java.util.UUID

data class Encounter(
    val id: UUID,
    val memberId: UUID,
    val identificationEventId: UUID?,
    val occurredAt: Instant,
    val patientOutcome: PatientOutcome?,
    val preparedAt: Instant? = null,
    val backdatedOccurredAt: Boolean = false,
    val copaymentPaid: Boolean? = true,
    val diagnoses: List<Int> = emptyList(),
    val visitType: String? = null,
    val claimId: String = id.toString(),
    val adjudicationState: Encounter.AdjudicationState = Encounter.AdjudicationState.PENDING,
    val adjudicatedAt: Instant? = null,
    val adjudicationReason: String? = null,
    val revisedEncounterId: UUID? = null,
    val providerComment: String? = null,
    val submittedAt: Instant? = null
) : Serializable {

    fun shortenedClaimId(): String {
        return claimId.toUpperCase().substring(CLAIM_ID_RANGE)
    }

    enum class AdjudicationState { PENDING, RETURNED, REVISED, APPROVED }

    enum class EncounterAction { PREPARE, SUBMIT, RESUBMIT }

    enum class PatientOutcome { CURED_OR_DISCHARGED, REFERRED, FOLLOW_UP, REFUSED_SERVICE, DECEASED, OTHER }

    companion object {
        val VISIT_TYPE_CHOICES = listOf(
            "OPD - New Visit",
            "OPD - Repeat Visit",
            "Youth Friendly Services (YFS) - New Visit",
            "Youth Friendly Services (YFS) - Repeat Visit",
            "<5 OPD - New Visit",
            "<5 OPD - Repeat Visit",
            "Emergency OPD",
            "Inpatient (IPD)",
            "ART - New Patient",
            "ART - Repeat Visit",
            "TB - New Patient",
            "TB - Repeat Visit",
            "Family Planning (FP) - New Visit",
            "Family Planning (FP) - Repeat Visit",
            "Antenatal Care (ANC)  - 1st Visit",
            "Antenatal Care (ANC)  - 2nd Visit",
            "Antenatal Care (ANC)  - 3rd Visit",
            "Antenatal Care (ANC)  - 4th Visit",
            "Postnatal Care (PNC) - 1st Visit",
            "Postnatal Care (PNC) - 2nd Visit",
            "EPI",
            "Delivery (DR)",
            "Abortion",
            "Growth Monitoring & Promotion (GMP) - New Visit",
            "Growth Monitoring & Promotion (GMP) - Repeat Visit",
            "Dental"
        )

        val CLAIM_ID_RANGE = (0..7)
    }
}
