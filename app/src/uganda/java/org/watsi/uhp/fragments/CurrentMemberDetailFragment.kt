package org.watsi.uhp.fragments

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.uganda.fragment_current_member_detail.absentee_notification
import kotlinx.android.synthetic.uganda.fragment_current_member_detail.member_action_button
import kotlinx.android.synthetic.uganda.fragment_current_member_detail.member_detail
import kotlinx.android.synthetic.uganda.fragment_current_member_detail.notification_container
import kotlinx.android.synthetic.uganda.fragment_current_member_detail.replace_card_notification
import org.threeten.bp.Clock
import org.watsi.device.managers.Logger
import org.watsi.domain.entities.IdentificationEvent
import org.watsi.domain.entities.Member
import org.watsi.uhp.R
import org.watsi.uhp.activities.ClinicActivity
import org.watsi.uhp.managers.NavigationManager
import org.watsi.uhp.viewmodels.CurrentMemberDetailViewModel
import javax.inject.Inject

class CurrentMemberDetailFragment : DaggerFragment() {

    @Inject lateinit var clock: Clock
    @Inject lateinit var navigationManager: NavigationManager
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var logger: Logger

    lateinit var member: Member
    lateinit var identificationEvent: IdentificationEvent
    lateinit var viewModel: CurrentMemberDetailViewModel

    companion object {
        const val PARAM_MEMBER = "member"
        const val PARAM_IDENTIFICAITON_EVENT = "identification_event"

        fun forMemberAndIdEvent(member: Member, identificationEvent: IdentificationEvent): CurrentMemberDetailFragment {
            val fragment = CurrentMemberDetailFragment()
            fragment.arguments = Bundle().apply {
                putSerializable(PARAM_MEMBER, member)
                putSerializable(PARAM_IDENTIFICAITON_EVENT, identificationEvent)
            }
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        member = arguments.getSerializable(PARAM_MEMBER) as Member
        identificationEvent = arguments.getSerializable(PARAM_IDENTIFICAITON_EVENT) as IdentificationEvent

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(CurrentMemberDetailViewModel::class.java)
        viewModel.getObservable(member, identificationEvent).observe(this, Observer {
            it?.member?.let { member ->
                this.member = member

                if (member.isAbsentee() || member.cardId == null) {
                    notification_container.visibility = View.VISIBLE

                    if (member.isAbsentee()) {
                        absentee_notification.visibility = View.VISIBLE
                        absentee_notification.setOnClickListener {
                            navigationManager.goTo(EditMemberFragment.forMember(member.id))
                        }
                    }

                    if (member.cardId == null) {
                        replace_card_notification.visibility = View.VISIBLE
                        replace_card_notification.setOnClickListener {
                            navigationManager.goTo(EditMemberFragment.forMember(member.id))
                        }
                    }
                }

                member_detail.setMember(member, it.memberThumbnail, clock)
            }
        })
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        (activity as ClinicActivity).setToolbar(context.getString(R.string.current_member_fragment_label), R.drawable.ic_arrow_back_white_24dp)
        setHasOptionsMenu(true)
        return inflater?.inflate(R.layout.fragment_current_member_detail, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        member_action_button.text = getString(R.string.detail_create_encounter)
        member_action_button.setOnClickListener {
            navigationManager.goTo(HealthIndicatorsFragment.forEncounter(
                    viewModel.buildEncounter(identificationEvent, member)))
        }
        member_detail.setIdentificationEvent(identificationEvent)
    }

    override fun onPrepareOptionsMenu(menu: Menu?) {
        menu?.let {
            it.findItem(R.id.menu_member_edit).isVisible = true
            it.findItem(R.id.menu_enroll_newborn).isVisible = true
            it.findItem(R.id.menu_dismiss_member).isVisible = true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> {
                navigationManager.goBack()
            }
            R.id.menu_member_edit -> {
                navigationManager.goTo(EditMemberFragment.forMember(member.id))
            }
            R.id.menu_enroll_newborn -> {
                navigationManager.goTo(EnrollNewbornFragment.forParent(member))
            }
            R.id.menu_dismiss_member -> {
                viewModel.dismiss(identificationEvent).subscribe({
                    navigationManager.popTo(CurrentPatientsFragment.withSnackbarMessage(
                            getString(R.string.checked_out_snackbar_message, member.name)))
                }, {
                    logger.error(it)
                })
            }
            else -> super.onOptionsItemSelected(item)
        }
        return true
    }
}
