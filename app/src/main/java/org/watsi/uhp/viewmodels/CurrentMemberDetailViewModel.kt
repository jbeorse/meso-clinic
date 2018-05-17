package org.watsi.uhp.viewmodels

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.LiveDataReactiveStreams
import android.arch.lifecycle.ViewModel
import io.reactivex.Completable
import org.watsi.domain.entities.IdentificationEvent
import org.watsi.domain.relations.MemberWithThumbnail
import org.watsi.domain.repositories.MemberRepository
import org.watsi.domain.usecases.DismissIdentificationEventUseCase
import java.util.UUID
import javax.inject.Inject

class CurrentMemberDetailViewModel @Inject constructor(
        private val memberRepository: MemberRepository,
        private val dismissIdentificationEventUseCase: DismissIdentificationEventUseCase
) : ViewModel() {

    fun getObservable(memberId: UUID): LiveData<ViewState> {
        val transformedFlowable = memberRepository.findMemberWithThumbnailFlowable(memberId).map { ViewState(it) }
        return LiveDataReactiveStreams.fromPublisher(transformedFlowable)
    }

    fun dismiss(identificationEvent: IdentificationEvent): Completable {
        return dismissIdentificationEventUseCase.execute(identificationEvent)
    }

    data class ViewState(val memberWIthThumbnail: MemberWithThumbnail)
}
