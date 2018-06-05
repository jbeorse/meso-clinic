package org.watsi.device.db.repositories

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import okhttp3.MediaType
import okhttp3.RequestBody
import org.threeten.bp.Clock
import org.watsi.device.api.CoverageApi
import org.watsi.device.api.models.MemberApi
import org.watsi.device.db.daos.MemberDao
import org.watsi.device.db.daos.PhotoDao
import org.watsi.device.db.models.DeltaModel
import org.watsi.device.db.models.MemberModel
import org.watsi.device.db.models.PhotoModel
import org.watsi.device.managers.PreferencesManager
import org.watsi.device.managers.SessionManager
import org.watsi.domain.entities.Delta
import org.watsi.domain.entities.Member
import org.watsi.domain.entities.Photo
import org.watsi.domain.relations.MemberWithIdEventAndThumbnailPhoto
import org.watsi.domain.relations.MemberWithThumbnail
import org.watsi.domain.repositories.MemberRepository
import java.util.UUID

class MemberRepositoryImpl(private val memberDao: MemberDao,
                           private val api: CoverageApi,
                           private val sessionManager: SessionManager,
                           private val preferencesManager: PreferencesManager,
                           private val photoDao: PhotoDao,
                           private val clock: Clock) : MemberRepository {

    override fun all(): Flowable<List<Member>> {
        return memberDao.all().map { it.map { it.toMember() } }.subscribeOn(Schedulers.io())
    }

    override fun find(id: UUID): Flowable<Member> {
        return memberDao.findFlowable(id).map { it.toMember() }.subscribeOn(Schedulers.io())
    }

    override fun findMemberWithThumbnailFlowable(id: UUID): Flowable<MemberWithThumbnail> {
        return memberDao.findFlowableMemberWithThumbnail(id).map { it.toMemberWithThumbnail() }
    }

    override fun findByCardId(cardId: String): Maybe<Member> {
        return memberDao.findByCardId(cardId).map { it.toMember() }.subscribeOn(Schedulers.io())
    }

    override fun byIds(ids: List<UUID>): Single<List<MemberWithIdEventAndThumbnailPhoto>> {
        return memberDao.byIds(ids).map {
            it.map { it.toMemberWithIdEventAndThumbnailPhoto() }
        }.subscribeOn(Schedulers.io())
    }

    override fun checkedInMembers(): Flowable<List<MemberWithIdEventAndThumbnailPhoto>> {
        return memberDao.checkedInMembers().map {
            it.map { it.toMemberWithIdEventAndThumbnailPhoto() }
        }
    }

    override fun remainingHouseholdMembers(member: Member): Flowable<List<MemberWithIdEventAndThumbnailPhoto>> {
        return memberDao.remainingHouseholdMembers(member.id, member.householdId).map { memberWithIdEventAndThumbnailModels ->
            memberWithIdEventAndThumbnailModels.map { memberWithIdEventAndThumbnailModel ->
                memberWithIdEventAndThumbnailModel.toMemberWithIdEventAndThumbnailPhoto()
            }
        }
    }

    override fun save(member: Member, deltas: List<Delta>): Completable {
        return Completable.fromAction {
            val deltaModels = deltas.map { DeltaModel.fromDelta(it, clock) }
            memberDao.upsert(MemberModel.fromMember(member, clock), deltaModels)
        }.subscribeOn(Schedulers.io())
    }

    override fun fetch(): Completable {
        return sessionManager.currentToken()?.let { token ->
            Completable.fromAction {
                val fetchedMembers = api.getMembers(token.getHeaderString(), token.user.providerId)
                        .blockingGet()
                val unsyncedMembers = memberDao.unsynced().blockingGet()
                val unsyncedIds = unsyncedMembers.map { it.id }
                val persistedMembers = memberDao.all().blockingFirst()
                val membersById = persistedMembers.groupBy { it.id }
                val fetchedAndUnsyncedIds = fetchedMembers.map { it.id } + unsyncedIds
                memberDao.deleteNotInList(fetchedAndUnsyncedIds.distinct())
                val fetchedMembersWithoutUnsynced = fetchedMembers.filter {
                    !unsyncedIds.contains(it.id)
                }
                memberDao.upsert(fetchedMembersWithoutUnsynced.map { memberApi ->
                    val persistedMember = membersById[memberApi.id]?.firstOrNull()?.toMember()
                    MemberModel.fromMember(memberApi.toMember(persistedMember), clock)
                })
                preferencesManager.updateMemberLastFetched(clock.instant())
            }.subscribeOn(Schedulers.io())
        } ?: Completable.complete()
    }

    override fun downloadPhotos(): Completable {
        return memberDao.needPhotoDownload().flatMapCompletable { memberModels ->
            Completable.concat(memberModels.map { memberModel ->
                val member = memberModel.toMember()
                api.fetchPhoto(member.photoUrl!!).flatMapCompletable {
                    Completable.fromAction {
                        val photo = Photo(UUID.randomUUID(), it.bytes())
                        photoDao.insert(PhotoModel.fromPhoto(photo, clock))
                        memberDao.upsert(memberModel.copy(thumbnailPhotoId = photo.id))
                    }
                }
            })
        }.subscribeOn(Schedulers.io())
    }

    override fun withPhotosToFetchCount(): Flowable<Int> {
        return memberDao.needPhotoDownloadCount()
    }

    override fun sync(deltas: List<Delta>): Completable {
        return sessionManager.currentToken()?.let { token ->
            memberDao.find(deltas.first().modelId).flatMapCompletable { memberModel ->
                val member = memberModel.toMember()
                if (deltas.any { it.action == Delta.Action.ADD }) {
                    api.postMember(token.getHeaderString(), MemberApi(member))
                } else {
                    api.patchMember(token.getHeaderString(), member.id, MemberApi.patch(member, deltas))
                }
            }.subscribeOn(Schedulers.io())
        } ?: Completable.complete()
    }

    override fun syncPhotos(deltas: List<Delta>): Completable {
        return sessionManager.currentToken()?.let { token ->
            // the modelId in a photo delta corresponds to the member ID and not the photo ID
            // to make this querying and formatting of the sync request simpler
            val memberId = deltas.first().modelId
            photoDao.findMemberWithRawPhoto(memberId).flatMapCompletable { memberWithRawPhotoModel ->
                val memberWithRawPhoto = memberWithRawPhotoModel.toMemberWithRawPhoto()
                val requestBody = RequestBody.create(MediaType.parse("image/jpg"), memberWithRawPhoto.photo.bytes)
                api.patchPhoto(token.getHeaderString(), memberId, requestBody)
            }.subscribeOn(Schedulers.io())
        } ?: Completable.complete()
    }
}
