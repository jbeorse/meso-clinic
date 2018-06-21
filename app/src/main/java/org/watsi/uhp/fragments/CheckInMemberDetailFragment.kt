package org.watsi.uhp.fragments

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import dagger.android.support.DaggerFragment
import io.reactivex.Completable
import kotlinx.android.synthetic.main.fragment_checkin_member_detail.absentee_notification
import kotlinx.android.synthetic.main.fragment_checkin_member_detail.household_members_list
import kotlinx.android.synthetic.main.fragment_checkin_member_detail.household_panel_summary
import kotlinx.android.synthetic.main.fragment_checkin_member_detail.member_action_button
import kotlinx.android.synthetic.main.fragment_checkin_member_detail.member_detail
import kotlinx.android.synthetic.main.fragment_checkin_member_detail.notification_container
import kotlinx.android.synthetic.main.fragment_checkin_member_detail.replace_card_notification
import kotlinx.android.synthetic.main.fragment_checkin_member_detail.scan_fingerprints_btn
import org.threeten.bp.Clock
import org.watsi.device.managers.FingerprintManager
import org.watsi.device.managers.Logger
import org.watsi.device.managers.SessionManager
import org.watsi.domain.entities.IdentificationEvent
import org.watsi.domain.entities.IdentificationEvent.SearchMethod
import org.watsi.domain.entities.Member
import org.watsi.domain.relations.MemberWithIdEventAndThumbnailPhoto
import org.watsi.domain.repositories.PhotoRepository
import org.watsi.domain.usecases.CreateIdentificationEventUseCase
import org.watsi.uhp.R
import org.watsi.uhp.adapters.MemberAdapter
import org.watsi.uhp.helpers.RecyclerViewHelper
import org.watsi.uhp.helpers.SnackbarHelper
import org.watsi.uhp.managers.KeyboardManager
import org.watsi.uhp.managers.NavigationManager
import org.watsi.uhp.viewmodels.CheckInMemberDetailViewModel
import java.io.Serializable
import java.util.UUID
import javax.inject.Inject

class CheckInMemberDetailFragment : DaggerFragment() {

    @Inject lateinit var clock: Clock
    @Inject lateinit var navigationManager: NavigationManager
    @Inject lateinit var sessionManager: SessionManager
    @Inject lateinit var createIdentificationEventUseCase: CreateIdentificationEventUseCase
    @Inject lateinit var photoRepository: PhotoRepository
    @Inject lateinit var fingerprintManager: FingerprintManager
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var logger: Logger
    @Inject lateinit var keyboardManager: KeyboardManager

    lateinit var viewModel: CheckInMemberDetailViewModel
    lateinit var member: Member
    lateinit var memberAdapter: MemberAdapter
    lateinit var searchFields: SearchFields
    private var verificationDetails: FingerprintVerificationDetails? = null

    data class SearchFields(val searchMethod: SearchMethod, var throughMemberId: UUID? = null) : Serializable

    companion object {
        const val PARAM_MEMBER = "member"
        const val PARAM_SEARCH_FIELDS = "search_fields"
        const val VERIFY_FINGERPRINT_INTENT = 1

        fun forMemberWithSearchMethod(member: Member, searchMethod: SearchMethod): CheckInMemberDetailFragment {
            return forMemberWithSearchFields(member, SearchFields(searchMethod))
        }

        private fun forMemberWithSearchFields(member: Member, searchFields: SearchFields): CheckInMemberDetailFragment{
            val fragment = CheckInMemberDetailFragment()
            fragment.arguments = Bundle().apply {
                putSerializable(PARAM_MEMBER, member)
                putSerializable(PARAM_SEARCH_FIELDS, searchFields)
            }
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        member = arguments.getSerializable(PARAM_MEMBER) as Member
        searchFields = arguments.getSerializable(PARAM_SEARCH_FIELDS) as SearchFields

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(CheckInMemberDetailViewModel::class.java)
        viewModel.getObservable(member).observe(this, Observer {
            it?.member?.let { member ->
                this.member = member

                if (member.isAbsentee(clock) || member.cardId == null) {
                    notification_container.visibility = View.VISIBLE

                    if (member.isAbsentee(clock)) {
                        absentee_notification.visibility = View.VISIBLE
                    }

                    if (member.cardId == null) {
                        replace_card_notification.visibility = View.VISIBLE
                    }
                }

                member_detail.setMember(member, it.memberThumbnail, clock)
            }

            it?.isMemberCheckedIn?.let { isMemberCheckedIn ->
                member_action_button.visibility = View.VISIBLE

                if (isMemberCheckedIn) {
                    member_action_button.isEnabled = false
                    member_action_button.text = getString(R.string.checked_in)
                } else {
                    member_action_button.isEnabled = true
                    member_action_button.text = getString(R.string.check_in)
                    member_action_button.setOnClickListener {
                        launchClinicNumberDialog()
                    }
                }
            }

            it?.householdMembers?.let { householdMembers ->
                memberAdapter.setMembers(householdMembers)
                household_panel_summary.text = context.resources.getQuantityString(
                        R.plurals.household_members_label, householdMembers.size, householdMembers.size)
            }
        })

        memberAdapter = MemberAdapter(
                onItemSelect = { memberRelation: MemberWithIdEventAndThumbnailPhoto ->
                    val throughMemberId = searchFields.throughMemberId ?: memberRelation.member.id
                    navigationManager.goTo(CheckInMemberDetailFragment.forMemberWithSearchFields(
                            memberRelation.member,
                            SearchFields(searchFields.searchMethod, throughMemberId)))
                },
                clock = clock
        )
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity.setTitle(R.string.detail_fragment_label)
        setHasOptionsMenu(true)
        return inflater?.inflate(R.layout.fragment_checkin_member_detail, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        member.fingerprintsGuid?.let { guid ->
            scan_fingerprints_btn.setOnClickListener {view ->
                if (!fingerprintManager.verifyFingerprint(guid.toString(), this, VERIFY_FINGERPRINT_INTENT)) {
                    SnackbarHelper.show(view, context, R.string.fingerprints_not_installed_error_message)
                }
            }
        }

        absentee_notification.setOnClickListener {
            navigationManager.goTo(EditMemberFragment.forMember(member.id))
        }

        replace_card_notification.setOnClickListener {
            navigationManager.goTo(EditMemberFragment.forMember(member.id))
        }

        RecyclerViewHelper.setRecyclerView(household_members_list, memberAdapter, context, false)
    }

    private fun launchClinicNumberDialog() {
        val builder = AlertDialog.Builder(activity)
        builder.setView(R.layout.dialog_clinic_number)
                .setMessage(R.string.clinic_number_prompt)
                .setPositiveButton(R.string.clinic_number_button) { dialog, _ ->
                    createIdentificationEvent(dialog as AlertDialog).subscribe({
                        view?.let {
                            SnackbarHelper.show(it, context, getString(R.string.checked_in_snackbar_message, member.name))
                        }
                        navigationManager.popTo(CurrentPatientsFragment())
                    }, {
                        logger.error(it)
                    })
                }

        val dialog = builder.create()

        dialog.setOnShowListener {
            val clinicNumberField = dialog.findViewById<EditText>(R.id.clinic_number_field)
            clinicNumberField?.let { keyboardManager.showKeyboard(it) }
        }

        dialog.show()
    }

    private fun createIdentificationEvent(dialog: AlertDialog): Completable {
        val radioGroupView = dialog.findViewById<RadioGroup>(R.id.radio_group_clinic_number)
        val selectedRadioButton = dialog.findViewById<RadioButton>(radioGroupView?.checkedRadioButtonId!!)
        val clinicNumberField = dialog.findViewById<EditText>(R.id.clinic_number_field)

        val clinicNumberType = IdentificationEvent.ClinicNumberType.valueOf(
                selectedRadioButton?.text.toString().toUpperCase())
        val clinicNumber = Integer.valueOf(clinicNumberField?.text.toString())

        val idEvent = IdentificationEvent(id = UUID.randomUUID(),
                                          memberId = member.id,
                                          occurredAt = clock.instant(),
                                          searchMethod = searchFields.searchMethod,
                                          throughMemberId = searchFields.throughMemberId,
                                          clinicNumber = clinicNumber,
                                          clinicNumberType = clinicNumberType,
                                          fingerprintsVerificationTier =
                                             verificationDetails?.tier.toString(),
                                          fingerprintsVerificationConfidence =
                                            verificationDetails?.confidence,
                                          fingerprintsVerificationResultCode =
                                            verificationDetails?.resultCode)
        return createIdentificationEventUseCase.execute(idEvent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            VERIFY_FINGERPRINT_INTENT -> {
                val fingerprintResponse = fingerprintManager.parseResponseForVerification(resultCode, data)
                when (fingerprintResponse.status) {
                    FingerprintManager.FingerprintStatus.SUCCESS -> {
                        verificationDetails = FingerprintVerificationDetails(
                                fingerprintResponse.tier,
                                fingerprintResponse.confidence,
                                resultCode
                        )

                        val badScan = fingerprintResponse.badScan

                        when (badScan) {
                            true -> {
                                setScanResultProperties(ContextCompat.getColor(context, R.color.indicatorRed), R.string.bad_scan_indicator)
                                view?.let { SnackbarHelper.show(it, context, R.string.fingerprint_scan_successful) }
                            } false -> {
                                setScanResultProperties(ContextCompat.getColor(context, R.color.indicatorGreen), R.string.good_scan_indicator)
                                view?.let { SnackbarHelper.show(it, context, R.string.fingerprint_scan_successful) }
                            } null -> {
                                view?.let { SnackbarHelper.show(it, context, R.string.fingerprint_scan_failed) }
                                logger.error("FingerprintManager returned null badScan on Success $fingerprintResponse")
                            }
                        }

                    }
                    FingerprintManager.FingerprintStatus.FAILURE -> {
//                        setScanResultProperties(ContextCompat.getColor(context, R.color.indicatorNeutral), R.string.no_scan_indicator)
                        view?.let { SnackbarHelper.show(it, context, R.string.fingerprint_scan_failed) }
                    }
                    FingerprintManager.FingerprintStatus.CANCELLED -> { /* No-op */ }
                }
            }
            else -> {
                logger.error("Unknown requestCode called from CheckInMemberDetailFragment: $requestCode")
            }
        }
    }

    private fun setScanResultProperties(color: Int, textId: Int) {
//        scan_result.invalidate()
//        scan_result.setText(textId)
//        scan_result.setTextColor(color)
//        val border = scan_result.background as GradientDrawable
//        border.setStroke(2, color)
//        val fingerprintIcon = scan_result.compoundDrawables[0] as VectorDrawable
//        //mutate() allows us to modify only this instance of the drawable without affecting others
//        fingerprintIcon.mutate().setTint(color)
//
//        scan_fingerprints_btn.visibility = View.GONE
//        scan_result.visibility = View.VISIBLE
    }

    private data class FingerprintVerificationDetails(val tier: String?,
                                                      val confidence: Float?,
                                                      val resultCode: Int)

    override fun onPrepareOptionsMenu(menu: Menu?) {
        menu?.let {
            it.findItem(R.id.menu_member_edit).isVisible = true
            it.findItem(R.id.menu_enroll_newborn).isVisible = true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_member_edit -> {
                navigationManager.goTo(EditMemberFragment.forMember(member.id))
            }
            R.id.menu_enroll_newborn -> {
                val member = arguments?.getSerializable(PARAM_MEMBER) as Member
                navigationManager.goTo(EnrollNewbornInfoFragment.forParent(member))
            }
            else -> super.onOptionsItemSelected(item)
        }
        return true
    }
}

