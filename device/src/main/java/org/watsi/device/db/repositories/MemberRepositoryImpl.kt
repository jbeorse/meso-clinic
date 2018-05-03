package org.watsi.device.db.repositories

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.schedulers.Schedulers
import org.threeten.bp.Clock
import org.watsi.device.api.CoverageApi
import org.watsi.device.db.daos.MemberDao
import org.watsi.device.db.models.DeltaModel
import org.watsi.device.db.models.MemberModel
import org.watsi.device.managers.PreferencesManager
import org.watsi.device.managers.SessionManager
import org.watsi.domain.entities.Delta
import org.watsi.domain.entities.Member
import org.watsi.domain.repositories.MemberRepository
import java.util.UUID

class MemberRepositoryImpl(private val memberDao: MemberDao,
                           private val api: CoverageApi,
                           private val sessionManager: SessionManager,
                           private val preferencesManager: PreferencesManager,
                           private val clock: Clock) : MemberRepository {

    override fun all(): Flowable<List<Member>> {
        return memberDao.all().map { it.map { it.toMember() } }.subscribeOn(Schedulers.io())
    }

    override fun find(id: UUID): Flowable<Member> {
        return memberDao.find(id).map { it.toMember() }
    }

    override fun create(member: Member, deltas: List<Delta>): Completable {
        return Completable.fromAction {
            val deltaModels = deltas.map { DeltaModel.fromDelta(it, clock) }
            memberDao.insertWithDeltas(MemberModel.fromMember(member, clock), deltaModels)
        }.subscribeOn(Schedulers.io())
    }

    override fun update(member: Member, deltas: List<Delta>): Completable {
        return Completable.fromAction {
            val deltaModels = deltas.map { DeltaModel.fromDelta(it, clock) }
            memberDao.updateWithDeltas(MemberModel.fromMember(member, clock), deltaModels)
        }.subscribeOn(Schedulers.io())
    }

    override fun fetch(): Completable {
        val token = sessionManager.currentToken()
        return if (token == null) {
            Completable.complete()
        } else {
            api.members(token.getHeaderString(),
                        token.user.providerId).flatMapCompletable { updatedMembers ->
                // TODO: more efficient way of saving?
                // TODO: clean up any members not returned in the fetch
                // TODO: do not overwrite unsynced member data
                Completable.concat(updatedMembers.map { saveAfterFetch(it.toMember()) })
            }.andThen {
                preferencesManager.updateMemberLastFetched(clock.instant())
            }.subscribeOn(Schedulers.io())
        }
    }

    override fun saveAfterFetch(member: Member): Completable {
        return Completable.fromAction {
            if (memberDao.exists(member.id) != null) {
                memberDao.update(MemberModel.fromMember(member, clock))
            } else {
                memberDao.insert(MemberModel.fromMember(member, clock))
            }
        }.subscribeOn(Schedulers.io())
    }

    override fun findByCardId(cardId: String): Maybe<Member> {
        return memberDao.findByCardId(cardId).map { it.toMember() }.subscribeOn(Schedulers.io())
    }

    override fun checkedInMembers(): Flowable<List<Member>> {
        return memberDao.checkedInMembers().map { it.map { it.toMember() } }
    }

    override fun remainingHouseholdMembers(householdId: UUID,
                                           memberId: UUID): Flowable<List<Member>> {
        return memberDao.remainingHouseholdMembers(householdId, memberId)
                .map { it.map { it.toMember() } }
    }

    override fun fetchPhotos(): Completable {
        // TODO: implement
        return Completable.complete()
    }

    override fun sync(deltas: List<Delta>): Completable {
        // TODO: implement
        return Completable.complete()
    }
}
