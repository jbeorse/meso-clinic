package org.watsi.uhp.viewmodels

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import org.threeten.bp.Clock
import org.threeten.bp.Instant
import org.watsi.device.managers.Logger
import org.watsi.domain.entities.Encounter.EncounterAction
import org.watsi.domain.usecases.CreateEncounterUseCase
import org.watsi.domain.usecases.ReviseClaimUseCase
import org.watsi.domain.usecases.SubmitClaimUseCase
import org.watsi.domain.usecases.UpdateEncounterUseCase
import org.watsi.uhp.flowstates.EncounterFlowState
import javax.inject.Inject

class ReceiptViewModel @Inject constructor(
    private val createEncounterUseCase: CreateEncounterUseCase,
    private val updateEncounterUseCase: UpdateEncounterUseCase,
    private val submitClaimUseCase: SubmitClaimUseCase,
    private val reviseClaimUseCase: ReviseClaimUseCase,
    private val logger: Logger,
    private val clock: Clock
) : ViewModel() {

    private val observable = MutableLiveData<ViewState>()

    fun getObservable(occurredAt: Instant, backdatedOccurredAt: Boolean, comment: String?): LiveData<ViewState> {
        observable.value = ViewState(occurredAt, backdatedOccurredAt, comment)
        return observable
    }

    fun updateBackdatedOccurredAt(instant: Instant) {
        observable.value = observable.value?.copy(occurredAt = instant, backdatedOccurredAt = true)
    }

    fun updateEncounterWithDateAndComment(encounterFlowState: EncounterFlowState) {
        observable.value?.let { viewState ->
            encounterFlowState.encounter = encounterFlowState.encounter.copy(
                occurredAt = viewState.occurredAt,
                backdatedOccurredAt =  viewState.backdatedOccurredAt
            )
            encounterFlowState.newProviderComment = viewState.comment
        }
    }

    fun updateComment(comment: String) {
        observable.value = observable.value?.copy(comment = comment)
    }

    fun finishEncounter(
        encounterFlowState: EncounterFlowState,
        encounterAction: EncounterAction
    ): Completable {
        return observable.value?.let { viewState ->
            encounterFlowState.encounter = encounterFlowState.encounter.copy(
                occurredAt = viewState.occurredAt,
                backdatedOccurredAt =  viewState.backdatedOccurredAt,
                providerComment = viewState.comment,
                copaymentPaid = null    // no copayment in ethiopia system
            )
            Completable.fromCallable {
                if (encounterFlowState.member == null) {
                    logger.error("Member cannot be null")
                }

                when (encounterAction) {
                    EncounterAction.PREPARE -> {
                        createEncounterUseCase.execute(
                            encounterFlowState.toEncounterWithItemsAndForms(), false, clock
                        ).blockingAwait()
                    }
                    EncounterAction.SUBMIT -> {
                        Completable.concatArray(
                            updateEncounterUseCase.execute(
                                encounterFlowState.toEncounterWithItemsAndForms()
                            ),
                            submitClaimUseCase.execute(
                                encounterFlowState.toEncounterWithItemsAndForms(), clock
                            )
                        ).blockingAwait()
                    }
                    EncounterAction.RESUBMIT -> {
                        reviseClaimUseCase.execute(
                            encounterFlowState.toEncounterWithItemsAndForms(), clock
                        ).blockingAwait()
                    }
                }
            }.observeOn(AndroidSchedulers.mainThread())
        } ?: Completable.never()
    }

    fun occurredAt(): Instant? {
        return observable.value?.occurredAt
    }

    fun backdatedOccurredAt(): Boolean? {
        return observable.value?.backdatedOccurredAt
    }

    fun comment(): String? {
        return observable.value?.comment
    }

    data class ViewState(val occurredAt: Instant,
                         val backdatedOccurredAt: Boolean,
                         val comment: String? = null)
}
