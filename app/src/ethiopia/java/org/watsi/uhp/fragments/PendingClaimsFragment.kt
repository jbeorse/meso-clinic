package org.watsi.uhp.fragments

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.ethiopia.fragment_claims_list.claims_list
import kotlinx.android.synthetic.ethiopia.fragment_claims_list.submit_all_button
import kotlinx.android.synthetic.ethiopia.fragment_claims_list.total_claims_label
import kotlinx.android.synthetic.ethiopia.fragment_claims_list.total_price_label
import org.watsi.device.managers.Logger
import org.watsi.domain.relations.EncounterWithExtras
import org.watsi.uhp.R
import org.watsi.uhp.activities.ClinicActivity
import org.watsi.uhp.adapters.ClaimListItemAdapter
import org.watsi.uhp.flowstates.EncounterFlowState
import org.watsi.uhp.helpers.RecyclerViewHelper
import org.watsi.uhp.helpers.SnackbarHelper
import org.watsi.uhp.managers.NavigationManager
import org.watsi.uhp.utils.CurrencyUtil
import org.watsi.uhp.viewmodels.PendingClaimsViewModel
import javax.inject.Inject

class PendingClaimsFragment : DaggerFragment() {
    @Inject lateinit var navigationManager: NavigationManager
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var logger: Logger

    lateinit var viewModel: PendingClaimsViewModel
    lateinit var claimsAdapter: ClaimListItemAdapter

    private var snackbarMessageToShow: String? = null

    companion object {
        const val PARAM_SNACKBAR_MESSAGE = "snackbar_message"

        fun withSnackbarMessage(message: String): PendingClaimsFragment {
            val fragment = PendingClaimsFragment()
            fragment.arguments = Bundle().apply {
                putString(PARAM_SNACKBAR_MESSAGE, message)
            }
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(PendingClaimsViewModel::class.java)
        val observable = viewModel.getObservable()
        observable.observe(this, Observer {
            it?.let { viewState ->
                updateClaims(viewState.claims)

                if (viewState.claims.count() > 0) {
                    submit_all_button.visibility = View.VISIBLE
                } else {
                    submit_all_button.visibility = View.GONE
                }
            }
        })

        snackbarMessageToShow = arguments?.getString(PARAM_SNACKBAR_MESSAGE)
    }

    private fun updateClaims(pendingClaims: List<EncounterWithExtras>) {
        total_claims_label.text = if (pendingClaims.isEmpty()) {
            getString(R.string.pending_claims_count_empty)
        } else {
            resources.getQuantityString(
                R.plurals.pending_claims_count, pendingClaims.size, pendingClaims.size
            )
        }

        total_price_label.text = CurrencyUtil.formatMoney(pendingClaims.sumBy { it.price() })

        claimsAdapter.setClaims(pendingClaims)
        RecyclerViewHelper.setRecyclerView(claims_list, claimsAdapter, context, true)
    }

    private fun submitAll() {
        viewModel.submitAll().subscribe({
            SnackbarHelper.show(claims_list, context, getString(R.string.all_encounter_submitted))
        }, {
            logger.error(it)
        })
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        (activity as ClinicActivity).setToolbar(
            context.getString(org.watsi.uhp.R.string.pending_claims_fragment_label),
            org.watsi.uhp.R.drawable.ic_arrow_back_white_24dp
        )
        setHasOptionsMenu(false)
        return inflater?.inflate(org.watsi.uhp.R.layout.fragment_claims_list, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        claimsAdapter = ClaimListItemAdapter(
            onClaimSelected = { encounterRelation ->
                viewModel.getClaims()?.let { claims ->
                    val remainingClaimIds = if (claims.size > 1) {
                        ArrayList(claims.subList(claims.indexOf(encounterRelation) + 1, claims.size).map { it.encounter.id })
                    } else {
                        null
                    }

                    navigationManager.goTo(ReceiptFragment.forEncounterAndRemainingEncountersAndSnackbar(
                        EncounterFlowState.fromEncounterWithExtras(encounterRelation),
                        remainingClaimIds
                    ))
                }
            }
        )

        submit_all_button.setOnClickListener {
            AlertDialog.Builder(activity)
                    .setTitle(resources.getQuantityString(
                        R.plurals.submit_all_form_alert, claimsAdapter.itemCount, claimsAdapter.itemCount)
                    )
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.submit_encounter_button) { _, _ ->
                        submitAll()
                    }.create().show()
        }

        snackbarMessageToShow?.let { snackbarMessage ->
            SnackbarHelper.show(claims_list, context, snackbarMessage)
            snackbarMessageToShow = null
        }
    }

    override fun onResume() {
        super.onResume()

        // this is required for when the user back navigates into this screen
        // the observable does not trigger, so we need to set the adapter from the viewModel
        viewModel.getClaims()?.let { updateClaims(it) }
    }
}
