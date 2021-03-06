package org.watsi.domain.usecases

import io.reactivex.Single
import org.watsi.domain.entities.IdentificationEvent
import org.watsi.domain.relations.BillableWithPriceSchedule
import org.watsi.domain.repositories.BillableRepository

class LoadDefaultBillablesUseCase(private val billableRepository: BillableRepository) {
    fun execute(identificationEvent: IdentificationEvent): Single<List<BillableWithPriceSchedule>> {
        return if (identificationEvent.clinicNumberType == IdentificationEvent.ClinicNumberType.OPD) {
            billableRepository.opdDefaults()
        } else {
            Single.just(emptyList())
        }
    }
}
