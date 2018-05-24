package org.watsi.uhp.fragments

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.VectorDrawable
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import com.simprints.libsimprints.Constants
import com.simprints.libsimprints.Tier
import dagger.android.support.DaggerFragment
import io.reactivex.Completable
import kotlinx.android.synthetic.main.fragment_member_detail.absentee_notification
import kotlinx.android.synthetic.main.fragment_member_detail.household_members_label
import kotlinx.android.synthetic.main.fragment_member_detail.household_members_list
import kotlinx.android.synthetic.main.fragment_member_detail.member_action_button
import kotlinx.android.synthetic.main.fragment_member_detail.member_age_and_gender
import kotlinx.android.synthetic.main.fragment_member_detail.member_card_id_detail_fragment
import kotlinx.android.synthetic.main.fragment_member_detail.member_name_detail_fragment
import kotlinx.android.synthetic.main.fragment_member_detail.member_phone_number
import kotlinx.android.synthetic.main.fragment_member_detail.member_photo
import kotlinx.android.synthetic.main.fragment_member_detail.replace_card_notification
import kotlinx.android.synthetic.main.fragment_member_detail.scan_fingerprints_btn
import kotlinx.android.synthetic.main.fragment_member_detail.scan_result
import org.threeten.bp.Clock
import org.watsi.device.managers.Logger
import org.watsi.device.managers.SessionManager
import org.watsi.domain.entities.IdentificationEvent
import org.watsi.domain.entities.Member
import org.watsi.domain.relations.MemberWithIdEventAndThumbnailPhoto
import org.watsi.domain.repositories.PhotoRepository
import org.watsi.domain.usecases.CreateIdentificationEventUseCase
import org.watsi.uhp.BuildConfig
import org.watsi.uhp.R
import org.watsi.uhp.adapters.MemberAdapter
import org.watsi.uhp.helpers.PhotoLoader
import org.watsi.uhp.helpers.SimprintsHelper
import org.watsi.uhp.managers.KeyboardManager
import org.watsi.uhp.managers.NavigationManager
import org.watsi.uhp.viewmodels.CheckInMemberDetailViewModel
import java.util.UUID
import javax.inject.Inject

class CheckInMemberDetailFragment : DaggerFragment() {

    @Inject lateinit var clock: Clock
    @Inject lateinit var navigationManager: NavigationManager
    @Inject lateinit var sessionManager: SessionManager
    @Inject lateinit var createIdentificationEventUseCase: CreateIdentificationEventUseCase
    @Inject lateinit var photoRepository: PhotoRepository
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var logger: Logger

    lateinit var viewModel: CheckInMemberDetailViewModel
    lateinit var member: Member
    lateinit var simprintsHelper: SimprintsHelper
    lateinit var memberAdapter: MemberAdapter
    private var verificationDetails: FingerprintVerificationDetails? = null

    companion object {
        const val PARAM_MEMBER = "member"

        fun forMember(member: Member): CheckInMemberDetailFragment {
            val fragment = CheckInMemberDetailFragment()
            fragment.arguments = Bundle().apply {
                putSerializable(PARAM_MEMBER, member)
            }
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        member = arguments.getSerializable(PARAM_MEMBER) as Member
        simprintsHelper = SimprintsHelper(sessionManager.currentToken()?.user?.username, this)

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(CheckInMemberDetailViewModel::class.java)
        viewModel.getObservable(member).observe(this, Observer {
            it?.member?.let { member ->
                this.member = member

                if (member.isAbsentee()) {
                    absentee_notification.visibility = View.VISIBLE
                    absentee_notification.setOnActionClickListener {
                        navigationManager.goTo(CompleteEnrollmentFragment.forMember(member.id))
                    }
                }

                if (member.cardId == null) {
                    replace_card_notification.visibility = View.VISIBLE
                    replace_card_notification.setOnClickListener {
                        navigationManager.goTo(MemberEditFragment.forMember(member))
                    }
                }

                member_name_detail_fragment.text = member.name
                member_age_and_gender.text = member.formatAgeAndGender(clock)
                member_card_id_detail_fragment.text = member.cardId
                member_phone_number.text = member.phoneNumber
            }

            it?.householdMembers?.let { householdMembers ->
                memberAdapter.setMembers(householdMembers)
                household_members_label.text = context.resources.getQuantityString(
                        R.plurals.household_label, householdMembers.size, householdMembers.size)
            }

            it?.memberThumbnail?.bytes?.let { bytes ->
                PhotoLoader.loadMemberPhoto(bytes, member_photo, context)
            }
        })
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity.setTitle(R.string.detail_fragment_label)
        setHasOptionsMenu(true)
        return inflater?.inflate(R.layout.fragment_member_detail, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        member_action_button.text = getString(R.string.check_in)
        member_action_button.setOnClickListener {
            launchClinicNumberDialog()
        }

        if (member.fingerprintsGuid != null) {
            scan_fingerprints_btn.visibility = View.VISIBLE
            scan_fingerprints_btn.setOnClickListener {
                try {
                    simprintsHelper.verify(BuildConfig.PROVIDER_ID.toString(), member.fingerprintsGuid)
                } catch (e: SimprintsHelper.SimprintsInvalidIntentException) {
                    Toast.makeText(context, R.string.fingerprints_not_installed_error_message, Toast.LENGTH_LONG).show()
                }
            }
        }

        memberAdapter = MemberAdapter(
                showClinicNumber = false,
                showPhoneNumber = true,
                onItemSelect = { memberRelation: MemberWithIdEventAndThumbnailPhoto ->
                    if (memberRelation.identificationEvent != null) {
                        memberRelation.identificationEvent?.let {
                            navigationManager.goTo(CurrentMemberDetailFragment.forMemberAndIdEvent(
                                    memberRelation.member, it))
                        }
                    } else {
                        navigationManager.goTo(CheckInMemberDetailFragment.forMember(memberRelation.member))
                    }
                })
        household_members_list.adapter = memberAdapter
        household_members_list.layoutManager = LinearLayoutManager(activity)
        household_members_list.isNestedScrollingEnabled = false
    }

    private fun launchClinicNumberDialog() {
        val builder = AlertDialog.Builder(activity)
        builder.setView(R.layout.dialog_clinic_number)
                .setMessage(R.string.clinic_number_prompt)
                .setPositiveButton(R.string.clinic_number_button) { dialog, _ ->
                    createIdentificationEvent(dialog as AlertDialog).subscribe({
                        navigationManager.popTo(CurrentPatientsFragment())
                    }, {
                        // TODO: handle error
                    })
                }

        val dialog = builder.create()

        dialog.setOnShowListener {
            val clinicNumberField = dialog.findViewById<EditText>(R.id.clinic_number_field)
            KeyboardManager.focusAndForceShowKeyboard(clinicNumberField, context)
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
                                          accepted = true,
                                          searchMethod =
                                            IdentificationEvent.SearchMethod.SCAN_BARCODE, // TODO
                                          throughMemberId = null,
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
        try {
            val verification = simprintsHelper.onActivityResultFromVerify(requestCode, resultCode, data)
            if (verification != null) {
                val fingerprintTier = verification.tier
                val fingerprintConfidence = verification.confidence

                verificationDetails = FingerprintVerificationDetails(
                        fingerprintTier, fingerprintConfidence, resultCode)

                if (fingerprintTier == Tier.TIER_5) {
                    setScanResultProperties(ContextCompat.getColor(context, R.color.indicatorRed), R.string.bad_scan_indicator)
                } else {
                    setScanResultProperties(ContextCompat.getColor(context, R.color.indicatorGreen), R.string.good_scan_indicator)
                }
                Toast.makeText(context, R.string.fingerprint_scan_successful, Toast.LENGTH_LONG).show()
                return
            } else {
                Toast.makeText(context, R.string.fingerprint_scan_failed, Toast.LENGTH_LONG).show()
            }
        } catch (e: SimprintsHelper.SimprintsHelperException) {
            logger.error(e)
            Toast.makeText(context, R.string.fingerprint_scan_failed, Toast.LENGTH_LONG).show()
        }

        if (resultCode != Constants.SIMPRINTS_CANCELLED) {
            setScanResultProperties(ContextCompat.getColor(context, R.color.indicatorNeutral), R.string.no_scan_indicator)
        }
    }

    private fun setScanResultProperties(color: Int, textId: Int) {
        scan_result.invalidate()
        scan_result.setText(textId)
        scan_result.setTextColor(color)
        val border = scan_result.background as GradientDrawable
        border.setStroke(2, color)
        val fingerprintIcon = scan_result.compoundDrawables[0] as VectorDrawable
        //mutate() allows us to modify only this instance of the drawable without affecting others
        fingerprintIcon.mutate().setTint(color)

        scan_fingerprints_btn.visibility = View.GONE
        scan_result.visibility = View.VISIBLE
    }

    private data class FingerprintVerificationDetails(val tier: Tier?,
                                                      val confidence: Float?,
                                                      val resultCode: Int)

    override fun onPrepareOptionsMenu(menu: Menu?) {
        menu!!.findItem(R.id.menu_member_edit).isVisible = true
        menu.findItem(R.id.menu_enroll_newborn).isVisible = true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_member_edit -> {
                navigationManager.goTo(MemberEditFragment.forMember(member))
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
