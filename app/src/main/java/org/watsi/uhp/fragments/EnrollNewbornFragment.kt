package org.watsi.uhp.fragments

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import dagger.android.support.DaggerFragment
import io.reactivex.Single
import kotlinx.android.synthetic.main.fragment_enroll_newborn.gender_field
import kotlinx.android.synthetic.main.fragment_enroll_newborn.name_layout
import kotlinx.android.synthetic.main.fragment_enroll_newborn.name
import kotlinx.android.synthetic.main.fragment_enroll_newborn.birthdate_dialog_field
import kotlinx.android.synthetic.main.fragment_enroll_newborn.photo_field
import kotlinx.android.synthetic.main.fragment_enroll_newborn.card_id_field
import kotlinx.android.synthetic.main.fragment_enroll_newborn.done_button
import org.watsi.device.managers.Logger
import org.watsi.domain.entities.Member
import org.watsi.uhp.R
import org.watsi.uhp.activities.SavePhotoActivity
import org.watsi.uhp.activities.ScanNewCardActivity
import org.watsi.uhp.helpers.LayoutHelper
import org.watsi.uhp.helpers.SnackbarHelper
import org.watsi.uhp.managers.KeyboardManager
import org.watsi.uhp.managers.NavigationManager
import org.watsi.uhp.viewmodels.EnrollNewbornViewModel
import org.watsi.uhp.viewmodels.EnrollNewbornViewModel.MemberStatus
import org.watsi.domain.utils.StringUtils
import java.util.UUID
import javax.inject.Inject

class EnrollNewbornFragment : DaggerFragment(), NavigationManager.HandleOnBack {

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var keyboardManager: KeyboardManager
    @Inject lateinit var navigationManager: NavigationManager
    @Inject lateinit var logger: Logger
    lateinit var viewModel: EnrollNewbornViewModel
    lateinit var parent: Member
    private val memberId = UUID.randomUUID()

    companion object {
        const val AUTO_SCROLL_PADDING_IN_DP = 60
        const val CAPTURE_PHOTO_INTENT = 1
        const val SCAN_QRCODE_INTENT = 2
        const val PARAM_MEMBER = "member"

        fun forParent(member: Member): EnrollNewbornFragment {
            val fragment = EnrollNewbornFragment()
            fragment.arguments = Bundle().apply {
                putSerializable(PARAM_MEMBER, member)
            }
            return fragment
        }
    }

    private fun setErrors(errorMap: Map<String, String>) {
        gender_field.setError(errorMap[EnrollNewbornViewModel.MEMBER_GENDER_ERROR])
        name_layout.setError(errorMap[EnrollNewbornViewModel.MEMBER_NAME_ERROR])
        birthdate_dialog_field.setErrorOnField(errorMap[EnrollNewbornViewModel.MEMBER_BIRTHDATE_ERROR])
        photo_field.setError(errorMap[EnrollNewbornViewModel.MEMBER_PHOTO_ERROR])
        card_id_field.setError(errorMap[EnrollNewbornViewModel.MEMBER_CARD_ERROR])
    }

    private fun scrollToFirstError(errorMap: Map<String, String>) {
        val validationKeysToField = linkedMapOf(
                EnrollNewbornViewModel.MEMBER_GENDER_ERROR to gender_field,
                EnrollNewbornViewModel.MEMBER_NAME_ERROR to name_layout,
                EnrollNewbornViewModel.MEMBER_BIRTHDATE_ERROR to birthdate_dialog_field,
                EnrollNewbornViewModel.MEMBER_PHOTO_ERROR to photo_field,
                EnrollNewbornViewModel.MEMBER_CARD_ERROR to card_id_field
        )

        validationKeysToField.forEach {
            val validationKey = it.key
            val layout = it.value
            errorMap[validationKey]?.let {
                (view as ScrollView).smoothScrollTo(0, layout.top - AUTO_SCROLL_PADDING_IN_DP)
                return
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(EnrollNewbornViewModel::class.java)
        viewModel.getViewStateObservable().observe(this, Observer {
            it?.let {
                setErrors(it.errors)

                if (it.name.isEmpty()) {
                    activity.title = context.getString(R.string.enroll_newborn_activity_title)
                } else {
                    activity.title = it.name
                }

                if (it.photoId != null && it.thumbnailPhoto != null) {
                    val thumbnailBitmap = BitmapFactory.decodeByteArray(
                            it.thumbnailPhoto.bytes, 0, it.thumbnailPhoto.bytes.size)
                    photo_field.setPhotoPreview(thumbnailBitmap)
                }

                it.cardId?.let {
                    card_id_field.setCardId(StringUtils.formatCardId(it))
                }
            }
        })
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstance: Bundle?): View? {
        activity.title = context.getString(R.string.enroll_newborn_activity_title)
        return inflater?.inflate(R.layout.fragment_enroll_newborn, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        gender_field.setOnGenderChange { gender -> viewModel.onGenderChange(gender) }

        name.addTextChangedListener(LayoutHelper.OnChangedListener {
            text -> viewModel.onNameChange(text)
        })

        name.onFocusChangeListener = View.OnFocusChangeListener{ view, hasFocus ->
            if (!hasFocus) { keyboardManager.hideKeyboard(view) }
        }

        photo_field.setOnClickListener{
            startActivityForResult(Intent(activity, SavePhotoActivity::class.java), CAPTURE_PHOTO_INTENT)
        }

        card_id_field.setOnClickListener {
            startActivityForResult(Intent(activity, ScanNewCardActivity::class.java), SCAN_QRCODE_INTENT)
        }

        done_button.setOnClickListener {
            parent = arguments.getSerializable(PARAM_MEMBER) as Member

            viewModel.saveMember(memberId, parent.householdId, parent.language).subscribe({
                viewModel.setStatusAsSaved()
                navigationManager.goBack()

                view?.let {
                    SnackbarHelper.show(it, context, context.getString(R.string.enrollment_completed_snackbar_message))
                }
            }, { throwable ->
                handleOnSaveError(throwable)
            })
        }
    }

    fun handleOnSaveError(throwable: Throwable) {
        var errorMessage = context.getString(R.string.generic_save_error)
        if (throwable is EnrollNewbornViewModel.ValidationException) {
            errorMessage = throwable.localizedMessage
            scrollToFirstError(throwable.errors)
        } else {
            logger.error(throwable)
        }
        view?.let {
            val snackbar = Snackbar.make(it, errorMessage, Snackbar.LENGTH_LONG)
            val textView = snackbar.view.findViewById<TextView>(android.support.design.R.id.snackbar_text)
            textView.setTextColor(context.getColor(R.color.white))
            snackbar.view.setBackgroundColor(context.getColor(R.color.red6))
            snackbar.show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            CAPTURE_PHOTO_INTENT -> {
                val (photoIds, error) = SavePhotoActivity.parseResult(resultCode, data, logger)
                photoIds?.let {
                    viewModel.onPhotoTaken(photoIds.first, photoIds.second)
                }
            }
            SCAN_QRCODE_INTENT -> {
                val (cardId, error) = ScanNewCardActivity.parseResult(resultCode, data, logger)
                cardId?.let {
                    viewModel.onCardScan(cardId)
                }
            }
            else -> {
                logger.error("EnrollNewbornFragment.onActivityResult received unknown request code $resultCode")
            }
        }
    }

    override fun onBack(): Single<Boolean> {
        val currentViewState = viewModel.getViewStateObservable().value

        return if (currentViewState == EnrollNewbornViewModel.ViewState() || currentViewState?.status == MemberStatus.SAVED) {
            Single.just(true)
        } else {
            Single.create<Boolean> { single ->
                AlertDialog.Builder(activity)
                        .setTitle(context.getString(R.string.exit_form_alert))
                        .setMessage(context.getString(R.string.exit_edit_member_form_message))
                        .setPositiveButton(context.getString(R.string.discard)) { _, _ -> single.onSuccess(true) }
                        .setNegativeButton(context.getString(R.string.cancel)) { _, _ -> single.onSuccess(false) }
                        .setOnDismissListener { single.onSuccess(false) }
                        .show()
            }
        }
    }
}
