package org.watsi.uhp.fragments

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import dagger.android.support.DaggerFragment
import io.reactivex.Single
import kotlinx.android.synthetic.uganda.fragment_encounter_form.encounter_form_count
import kotlinx.android.synthetic.uganda.fragment_encounter_form.encounter_form_list
import kotlinx.android.synthetic.uganda.fragment_encounter_form.photo_btn
import kotlinx.android.synthetic.uganda.fragment_encounter_form.save_button
import org.watsi.device.managers.Logger
import org.watsi.uhp.R
import org.watsi.uhp.activities.ClinicActivity
import org.watsi.uhp.activities.SavePhotoActivity
import org.watsi.uhp.adapters.EncounterFormAdapter
import org.watsi.uhp.flowstates.EncounterFlowState
import org.watsi.uhp.helpers.RecyclerViewHelper.setRecyclerView
import org.watsi.uhp.managers.NavigationManager
import org.watsi.uhp.viewmodels.EncounterFormViewModel
import javax.inject.Inject

class EncounterFormFragment : DaggerFragment(), NavigationManager.HandleOnBack {

    @Inject lateinit var navigationManager: NavigationManager
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var logger: Logger

    private lateinit var encounterFormAdapter: EncounterFormAdapter
    lateinit var viewModel: EncounterFormViewModel
    lateinit var observable: LiveData<EncounterFormViewModel.ViewState>
    lateinit var encounterFlowState: EncounterFlowState

    companion object {
        const val CAPTURE_PHOTO_INTENT = 1
        const val PARAM_ENCOUNTER = "encounter"

        fun forEncounter(encounter: EncounterFlowState): EncounterFormFragment {
            val fragment = EncounterFormFragment()
            fragment.arguments = Bundle().apply {
                putSerializable(PARAM_ENCOUNTER, encounter)
            }
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        encounterFlowState = arguments.getSerializable(PARAM_ENCOUNTER) as EncounterFlowState
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(EncounterFormViewModel::class.java)
        observable = viewModel.getObservable()
        observable.observe(this, Observer {
            it?.let { viewState ->
                encounter_form_count.text = resources.getQuantityString(
                        R.plurals.encounter_form_count, viewState.encounterFormPhotos.size, viewState.encounterFormPhotos.size
                )

                encounterFormAdapter.setEncounterForms(viewState.encounterFormPhotos)
            }
        })

        encounterFormAdapter = EncounterFormAdapter(
                onRemoveEncounterForm = { encounterFormPhoto: EncounterFormViewModel.EncounterFormPhoto ->
                    viewModel.removeEncounterFormPhoto(encounterFormPhoto)
                }
        )

        viewModel.initEncounterFormPhotos(encounterFlowState.encounterForms).subscribe({
            /* No-Op; the forms have been loaded successfully */
        }, {
            logger.error(it)
        })
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        (activity as ClinicActivity).setToolbar(context.getString(R.string.encounter_form_fragment_label), R.drawable.ic_arrow_back_white_24dp)
        setHasOptionsMenu(true)
        return inflater?.inflate(R.layout.fragment_encounter_form, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        photo_btn.setOnClickListener {
            val intent = Intent(activity, SavePhotoActivity::class.java).apply {
                putExtra(SavePhotoActivity.FOR_FORM_KEY, true)
            }
            startActivityForResult(intent, CAPTURE_PHOTO_INTENT)
        }

        setRecyclerView(encounter_form_list, encounterFormAdapter, context)

        save_button.setOnClickListener {
            viewModel.updateEncounterWithForms(encounterFlowState)
            navigationManager.goTo(VisitResultFragment.forEncounter(encounterFlowState))
        }
    }

    override fun onBack(): Single<Boolean> {
        return Single.fromCallable {
            viewModel.updateEncounterWithForms(encounterFlowState)
            true
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val (photoIds, error) = SavePhotoActivity.parseResult(resultCode, data, logger)
        photoIds?.let {
            viewModel.addEncounterFormPhoto(it.first, it.second)
        }
        error?.let {
            logger.error(it)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                navigationManager.goBack()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
