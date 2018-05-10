package org.watsi.device.db.repositories

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import okhttp3.MediaType
import okhttp3.RequestBody
import org.threeten.bp.Clock
import org.watsi.device.api.CoverageApi
import org.watsi.device.db.daos.PhotoDao
import org.watsi.device.db.models.PhotoModel
import org.watsi.device.managers.SessionManager
import org.watsi.domain.entities.Delta
import org.watsi.domain.entities.Photo
import org.watsi.domain.repositories.PhotoRepository
import java.util.UUID

class PhotoRepositoryImpl(private val photoDao: PhotoDao,
                          private val api: CoverageApi,
                          private val sessionManager: SessionManager,
                          private val clock: Clock) : PhotoRepository {

    override fun find(id: UUID): Single<Photo> {
        return photoDao.find(id).map { it.toPhoto() }.subscribeOn(Schedulers.io())
    }

    override fun create(photo: Photo): Completable {
        return Completable.fromAction {
            photoDao.insert(PhotoModel.fromPhoto(photo, clock))
        }.subscribeOn(Schedulers.io())
    }

    override fun sync(deltas: List<Delta>): Completable {
        val authToken = sessionManager.currentToken()!!
        val memberId = deltas.first().modelId

        // the modelId in a photo delta corresponds to the member ID and not the photo ID
        // to make this querying and formatting of the sync request simpler
        return photoDao.findMemberWithRawPhoto(memberId).flatMapCompletable { memberWithRawPhotoModel ->
            val memberWithRawPhoto = memberWithRawPhotoModel.toMemberWithRawPhoto()
            val requestBody = RequestBody.create(
                    MediaType.parse("image/jpg"), memberWithRawPhoto.photo.bytes)
            api.patchPhoto(authToken.getHeaderString(), memberId, requestBody)
        }
    }

    override fun deleteSynced(): Completable {
        return photoDao.canBeDeleted().flatMapCompletable {
            Completable.fromAction {
                it.forEach { model ->
                    photoDao.destroy(model)
                }
            }
        }
    }
}
