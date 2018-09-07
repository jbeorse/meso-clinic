package org.watsi.domain.factories

import org.watsi.domain.entities.EncounterItem
import org.watsi.domain.relations.BillableWithPriceSchedule
import org.watsi.domain.relations.EncounterItemWithBillableAndPrice

object EncounterItemWithBillableAndPriceFactory {
    fun build(
        billableWithPrice: BillableWithPriceSchedule = BillableWithPriceScheduleFactory.build(),
        encounterItem: EncounterItem = EncounterItemFactory.build(
            billableId = billableWithPrice.billable.id,
            priceScheduleId = billableWithPrice.priceSchedule.id
        )
    ): EncounterItemWithBillableAndPrice {
        return EncounterItemWithBillableAndPrice(encounterItem, billableWithPrice)
    }
}
