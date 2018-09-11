package org.watsi.uhp.views

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.view.View
import kotlinx.android.synthetic.main.item_receipt_list.view.receipt_billable_dosage
import kotlinx.android.synthetic.main.item_receipt_list.view.receipt_billable_name
import kotlinx.android.synthetic.main.item_receipt_list.view.receipt_billable_price
import kotlinx.android.synthetic.main.item_receipt_list.view.receipt_billable_quantity
import org.watsi.domain.relations.EncounterItemWithBillableAndPrice
import java.text.NumberFormat

class ReceiptListItem @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    fun setEncounterItem(encounterItemRelations: EncounterItemWithBillableAndPrice) {
        receipt_billable_quantity.text = NumberFormat.getInstance().format(encounterItemRelations.encounterItem.quantity)
        receipt_billable_name.text = encounterItemRelations.billableWithPriceSchedule.billable.name
        // TODO: pass previous price if one exists
        receipt_billable_price.setPrice(encounterItemRelations.price())
        encounterItemRelations.billableWithPriceSchedule.billable.details()?.let { details ->
            receipt_billable_dosage.visibility = View.VISIBLE
            receipt_billable_dosage.text = details
        }
    }
}
