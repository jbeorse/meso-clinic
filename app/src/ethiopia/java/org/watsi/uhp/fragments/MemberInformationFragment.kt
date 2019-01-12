package org.watsi.uhp.fragments

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dagger.android.support.DaggerFragment
import io.reactivex.Single
import kotlinx.android.synthetic.ethiopia.fragment_member_information.age_input
import kotlinx.android.synthetic.ethiopia.fragment_member_information.age_input_layout
import kotlinx.android.synthetic.ethiopia.fragment_member_information.age_unit_spinner
import kotlinx.android.synthetic.ethiopia.fragment_member_information.check_in_button
import kotlinx.android.synthetic.ethiopia.fragment_member_information.gender_field
import kotlinx.android.synthetic.ethiopia.fragment_member_information.medical_record_number
import kotlinx.android.synthetic.ethiopia.fragment_member_information.medical_record_number_layout
import kotlinx.android.synthetic.ethiopia.fragment_member_information.member_name
import kotlinx.android.synthetic.ethiopia.fragment_member_information.membership_number
import kotlinx.android.synthetic.ethiopia.fragment_member_information.name_layout
import org.threeten.bp.Clock
import org.watsi.device.managers.Logger
import org.watsi.domain.utils.AgeUnit
import org.watsi.uhp.R
import org.watsi.uhp.activities.ClinicActivity
import org.watsi.uhp.helpers.LayoutHelper
import org.watsi.uhp.helpers.SnackbarHelper
import org.watsi.uhp.managers.KeyboardManager
import org.watsi.uhp.managers.NavigationManager
import org.watsi.uhp.viewmodels.MemberInformationViewModel
import org.watsi.uhp.viewmodels.MemberInformationViewModel.Companion.MEMBER_AGE_ERROR
import org.watsi.uhp.viewmodels.MemberInformationViewModel.Companion.MEMBER_GENDER_ERROR
import org.watsi.uhp.viewmodels.MemberInformationViewModel.Companion.MEMBER_MEDICAL_RECORD_NUMBER_ERROR
import org.watsi.uhp.viewmodels.MemberInformationViewModel.Companion.MEMBER_NAME_ERROR
import javax.inject.Inject

class MemberInformationFragment : DaggerFragment(), NavigationManager.HandleOnBack {

    @Inject lateinit var clock: Clock
    @Inject lateinit var navigationManager: NavigationManager
    @Inject lateinit var keyboardManager: KeyboardManager
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var logger: Logger

    lateinit var viewModel: MemberInformationViewModel
    lateinit var observable: LiveData<MemberInformationViewModel.ViewState>
    lateinit var membershipNumber: String

    companion object {
        const val PARAM_MEMBERSHIP_NUMBER = "membership_number"

        fun withMembershipNumber(membershipNumber: String): MemberInformationFragment {
            val fragment = MemberInformationFragment()
            fragment.arguments = Bundle().apply {
                putString(PARAM_MEMBERSHIP_NUMBER, membershipNumber)
            }
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        membershipNumber = arguments.getString(PARAM_MEMBERSHIP_NUMBER)

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(MemberInformationViewModel::class.java)
        viewModel.getObservable().observe(this, Observer {
            it?.let { viewState ->
                setErrors(viewState.errors)

                gender_field.setGender(viewState.gender)

                member_name.setText(viewState.name)
                member_name.setSelection(member_name.text.length)

                age_input.setText(if (viewState.age == null) "" else viewState.age.toString())
                age_input.setSelection(age_input.text.length)

                medical_record_number.setText(viewState.medicalRecordNumber)
                medical_record_number.setSelection(medical_record_number.text.length)
            }
        })
    }

    private fun setErrors(errorMap: Map<String, Int>) {
        errorMap[MEMBER_GENDER_ERROR].let { errorResourceId ->
            if (errorResourceId == null) {
                gender_field.setError(null)
            } else {
                gender_field.setError(getString(errorResourceId))
            }
        }

        errorMap[MEMBER_NAME_ERROR].let { errorResourceId ->
            if (errorResourceId == null) {
                name_layout.error = null
            } else {
                name_layout.error = getString(errorResourceId)
            }
        }

        errorMap[MEMBER_AGE_ERROR].let { errorResourceId ->
            if (errorResourceId == null) {
                age_input_layout.error = null
            } else {
                age_input_layout.error = getString(errorResourceId)
            }
        }

        errorMap[MEMBER_MEDICAL_RECORD_NUMBER_ERROR].let { errorResourceId ->
            if (errorResourceId == null) {
                medical_record_number_layout.error = null
            } else {
                medical_record_number_layout.error = getString(errorResourceId)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        (activity as ClinicActivity).setToolbar(getString(R.string.member_information_fragment_label), R.drawable.ic_clear_white_24dp)
        return inflater?.inflate(R.layout.fragment_member_information, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        membership_number.setText(membershipNumber)

        gender_field.setOnGenderChange { gender -> viewModel.onGenderChange(gender) }

        member_name.addTextChangedListener(LayoutHelper.OnChangedListener {
                text -> viewModel.onNameChange(text)
        })

        age_input.addTextChangedListener(LayoutHelper.OnChangedListener { text ->
            viewModel.onAgeChange(text.toIntOrNull())
        })

        age_unit_spinner.setUpSpinner(
            listOf(AgeUnit.years, AgeUnit.months, AgeUnit.days).map {
                AgeUnitPresenter.toDisplayedString(it, context)
            },
            AgeUnitPresenter.toDisplayedString(AgeUnit.years, context),
            { selectedString: String? ->
                if (selectedString == null) {
                    logger.error("selectedString is null when onItemSelected is called in MemberInformationFragment")
                } else {
                    viewModel.onAgeUnitChange(AgeUnitPresenter.fromDisplayedString(selectedString, context))
                }
            }
        )

        medical_record_number.addTextChangedListener(LayoutHelper.OnChangedListener {
                text -> viewModel.onMedicalRecordNumberChange(text)
        })

        check_in_button.setOnClickListener {
            viewModel.createAndCheckInMember(membershipNumber).subscribe({
                navigationManager.popTo(HomeFragment.withSnackbarMessage(
                    getString(R.string.checked_in_snackbar_message, viewModel.getName())
                ))
            }, { throwable ->
                handleOnSaveError(throwable)
            })
        }

        /* Hide keyboard if no text inputs have focus */
        val textFields = listOf(age_input, medical_record_number)
        textFields.forEach {
            it.onFocusChangeListener = View.OnFocusChangeListener { view, hasFocus ->
                if (textFields.all { !it.hasFocus() }) {
                    keyboardManager.hideKeyboard(view)
                }
            }
        }
    }

    object AgeUnitPresenter {
        fun toDisplayedString(ageUnit: AgeUnit, context: Context): String {
            return when (ageUnit) {
                AgeUnit.years -> {
                    context.getString(R.string.years)
                }
                AgeUnit.months -> {
                    context.getString(R.string.months)
                }
                AgeUnit.days -> {
                    context.getString(R.string.days)
                }
                else -> {
                    throw IllegalStateException("AgeUnitPresenter.toDisplayedString called with invalid AgeUnit: $ageUnit")
                }
            }
        }

        fun fromDisplayedString(string: String, context: Context): AgeUnit {
            return when (string) {
                context.getString(R.string.years) -> {
                    AgeUnit.years
                }
                context.getString(R.string.months) -> {
                    AgeUnit.months
                }
                context.getString(R.string.days) -> {
                    AgeUnit.days
                }
                else -> {
                    throw IllegalStateException("AgeUnitPresenter.fromDisplayedString called with invalid string: $string")
                }
            }
        }
    }

    override fun onBack(): Single<Boolean> {
        return Single.create<Boolean> { single ->
            AlertDialog.Builder(activity)
                    .setMessage(R.string.exit_form_alert)
                    .setPositiveButton(R.string.yes) { _, _ ->
                        single.onSuccess(true)
                    }
                    .setNegativeButton(R.string.cancel) { _, _ -> single.onSuccess(false) }
                    .setOnDismissListener { single.onSuccess(false) }
                    .show()
        }
    }

    private fun handleOnSaveError(throwable: Throwable) {
        var errorMessage = context.getString(R.string.generic_save_error)
        if (throwable is MemberInformationViewModel.ValidationException) {
            errorMessage = context.getString(R.string.missing_fields_validation_error)
        } else {
            logger.error(throwable)
        }
        SnackbarHelper.showError(check_in_button, context, errorMessage)
    }
}