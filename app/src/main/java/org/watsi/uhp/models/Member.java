package org.watsi.uhp.models;

import android.content.Context;

import com.google.common.io.ByteStreams;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

import org.watsi.uhp.BuildConfig;
import org.watsi.uhp.api.ApiService;
import org.watsi.uhp.database.IdentificationEventDao;
import org.watsi.uhp.database.MemberDao;
import org.watsi.uhp.managers.Clock;
import org.watsi.uhp.managers.ExceptionManager;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import retrofit2.Call;

@DatabaseTable(tableName = Member.TABLE_NAME)
public class Member extends SyncableModel {

    public static final String TABLE_NAME = "members";

    public static final String FIELD_NAME_CARD_ID = "card_id";
    public static final String FIELD_NAME_FULL_NAME = "full_name";
    public static final String FIELD_NAME_AGE = "age";
    public static final String FIELD_NAME_GENDER = "gender";
    public static final String FIELD_NAME_CROPPED_PHOTO_BYTES = "photo";
    public static final String FIELD_NAME_REMOTE_MEMBER_PHOTO_URL = "photo_url";
    public static final String FIELD_NAME_REMOTE_NATIONAL_ID_PHOTO_URL = "national_id_photo_url";
    public static final String FIELD_NAME_LOCAL_MEMBER_PHOTO_ID = "local_member_photo_id";
    public static final String FIELD_NAME_LOCAL_NATIONAL_ID_PHOTO_ID = "local_national_id_photo_id";
    public static final String FIELD_NAME_HOUSEHOLD_ID = "household_id";
    public static final String FIELD_NAME_FINGERPRINTS_GUID = "fingerprints_guid";
    public static final String FIELD_NAME_PHONE_NUMBER = "phone_number";
    public static final String FIELD_NAME_BIRTHDATE = "birthdate";
    public static final String FIELD_NAME_BIRTHDATE_ACCURACY = "birthdate_accuracy";
    public static final String FIELD_NAME_ENROLLED_AT = "enrolled_at";
    public static final String API_NAME_MEMBER_PHOTO = "photo";
    public static final String API_NAME_NATIONAL_ID_PHOTO = "national_id_photo";

    public static final int MINIMUM_FINGERPRINT_AGE = 6;
    public static final int MINIMUM_NATIONAL_ID_AGE = 18;

    public enum GenderEnum { M, F }

    public enum BirthdateAccuracyEnum { D, M, Y }

    @Expose
    @SerializedName(FIELD_NAME_CARD_ID)
    @DatabaseField(columnName = FIELD_NAME_CARD_ID)
    protected String mCardId;

    @Expose
    @SerializedName(FIELD_NAME_FULL_NAME)
    @DatabaseField(columnName = FIELD_NAME_FULL_NAME, canBeNull = false)
    protected String mFullName;

    @Expose
    @SerializedName(FIELD_NAME_AGE)
    @DatabaseField(columnName = FIELD_NAME_AGE)
    protected int mAge;

    @Expose
    @SerializedName(FIELD_NAME_GENDER)
    @DatabaseField(columnName = FIELD_NAME_GENDER)
    protected GenderEnum mGender;

    @DatabaseField(columnName = FIELD_NAME_CROPPED_PHOTO_BYTES, dataType = DataType.BYTE_ARRAY)
    protected byte[] mCroppedPhotoBytes;

    @Expose
    @SerializedName(FIELD_NAME_REMOTE_MEMBER_PHOTO_URL)
    @DatabaseField(columnName = FIELD_NAME_REMOTE_MEMBER_PHOTO_URL)
    protected String mRemoteMemberPhotoUrl;

    @Expose(serialize = false)
    @SerializedName(FIELD_NAME_REMOTE_NATIONAL_ID_PHOTO_URL)
    protected String mRemoteNationalIdPhotoUrl;

    @Expose(deserialize = false)
    @DatabaseField(columnName = FIELD_NAME_LOCAL_MEMBER_PHOTO_ID, foreign = true, foreignAutoRefresh = true)
    protected Photo mLocalMemberPhoto;

    @Expose(deserialize = false)
    @DatabaseField(columnName = FIELD_NAME_LOCAL_NATIONAL_ID_PHOTO_ID, foreign = true, foreignAutoRefresh = true)
    protected Photo mLocalNationalIdPhoto;

    @Expose
    @SerializedName(FIELD_NAME_HOUSEHOLD_ID)
    @DatabaseField(columnName = FIELD_NAME_HOUSEHOLD_ID)
    protected UUID mHouseholdId;

    @Expose
    @SerializedName(FIELD_NAME_FINGERPRINTS_GUID)
    @DatabaseField(columnName = FIELD_NAME_FINGERPRINTS_GUID)
    protected UUID mFingerprintsGuid;

    @Expose
    @SerializedName(FIELD_NAME_BIRTHDATE)
    @DatabaseField(columnName = FIELD_NAME_BIRTHDATE)
    private Date mBirthdate;

    @Expose
    @SerializedName(FIELD_NAME_BIRTHDATE_ACCURACY)
    @DatabaseField(columnName = FIELD_NAME_BIRTHDATE_ACCURACY)
    private BirthdateAccuracyEnum mBirthdateAccuracy;

    @Expose
    @SerializedName(FIELD_NAME_PHONE_NUMBER)
    @DatabaseField(columnName = FIELD_NAME_PHONE_NUMBER)
    protected String mPhoneNumber;

    @Expose
    @SerializedName(FIELD_NAME_ENROLLED_AT)
    @DatabaseField(columnName = FIELD_NAME_ENROLLED_AT)
    private Date mEnrolledAt;

    @ForeignCollectionField(orderColumnName = IdentificationEvent.FIELD_NAME_CREATED_AT)
    private final Collection<IdentificationEvent> mIdentificationEvents = new ArrayList<>();

    public Member() {
        super();
    }

    public void setFullName(String fullName) {
        this.mFullName = fullName;
    }

    public String getFullName() {
        return this.mFullName;
    }

    @Override
    public void validate() throws ValidationException {
        validateFullName();
        validateCardId();
        validatePhoneNumber();
        validateBirthdate();
        validateGender();
    }

    public boolean validFullName() {
        return mFullName != null && !mFullName.isEmpty();
    }

    public boolean validCardId() {
        return Member.validCardId(mCardId);
    }

    public boolean validPhoneNumber() {
        return mPhoneNumber == null || mPhoneNumber.matches("0?[1-9]\\d{8}");
    }

    public boolean validGender() {
        return mGender != null;
    }

    public boolean validBirthdate() {
        return mBirthdate != null && mBirthdateAccuracy != null;
    }

    public void validateFullName() throws ValidationException {
        if (!validFullName()) {
            throw new ValidationException(FIELD_NAME_FULL_NAME, "Name cannot be blank");
        }
    }

    public void validateCardId() throws ValidationException {
        if (!validCardId()) {
            throw new ValidationException(FIELD_NAME_CARD_ID, "Card must be 3 letters followed by 6 numbers");
        }
    }

    public void validatePhoneNumber() throws ValidationException {
        if (!validPhoneNumber()) {
            throw new ValidationException(FIELD_NAME_PHONE_NUMBER, "Phone number is invalid.");
        }
    }

    public void validateGender() throws ValidationException {
        if (!validGender()) {
            throw new ValidationException(FIELD_NAME_GENDER, "Gender cannot be blank.");
        }
    }

    public void validateBirthdate() throws ValidationException {
        if (!validBirthdate()) {
            throw new ValidationException(FIELD_NAME_BIRTHDATE, "Birthdate or birthdate accuracy is invalid.");
        }
    }

    @Override
    public void handleUpdateFromSync(SyncableModel response) throws SQLException, IOException {
        Member memberResponse = (Member) response;
        String photoUrlFromResponse = memberResponse.getRemoteMemberPhotoUrl();
        String nationalIdPhotoUrlFromResponse = memberResponse.getRemoteNationalIdPhotoUrl();

        if (photoUrlFromResponse != null) {
            getLocalMemberPhoto().markAsSynced();
            setRemoteMemberPhotoUrl(photoUrlFromResponse);
            fetchAndSetPhotoFromUrl(new OkHttpClient()); // async?
            memberResponse.setLocalMemberPhoto(getLocalMemberPhoto());
        }

        // TODO need to handle edge case where the response has a URL for a photo that is different then the locally stored photo
        if (nationalIdPhotoUrlFromResponse != null && getLocalNationalIdPhoto() != null) {
            getLocalNationalIdPhoto().markAsSynced();
            memberResponse.setLocalNationalIdPhoto(getLocalNationalIdPhoto());
        }
    }

    public void updateFromFetch() throws SQLException {
        Member persistedMember = MemberDao.findById(getId());
        if (persistedMember != null) {
            // if the persisted member has not been synced to the back-end, assume it is
            // the most up-to-date and do not update it with the fetched member attributes
            if (!persistedMember.isSynced()) {
                return;
            }

            // if the existing member record has a photo and the fetched member record has
            // the same photo url as the existing record, copy the photo to the new record
            // so we do not have to re-download it
            if (persistedMember.getCroppedPhotoBytes() != null &&
                    persistedMember.getRemoteMemberPhotoUrl() != null &&
                    persistedMember.getRemoteMemberPhotoUrl().equals(getRemoteMemberPhotoUrl())) {
                setCroppedPhotoBytes(persistedMember.getCroppedPhotoBytes());
            }

            if (!getRemoteMemberPhotoUrl().equals(persistedMember.getRemoteMemberPhotoUrl())) {
                setCroppedPhotoBytes(null);
            }
        }
        getDao().createOrUpdate(this);
    }

    @Override
    protected Call postApiCall(Context context) throws SQLException {
        return ApiService.requestBuilder(context).enrollMember(
                getTokenAuthHeaderString(), formatPostRequest(context));
    }

    @Override
    protected void persistAssociations() {
        // no-op
    }

    @Override
    protected Call patchApiCall(Context context) throws SQLException {
        return ApiService.requestBuilder(context).syncMember(
                getTokenAuthHeaderString(), getId(), formatPatchRequest(context));
    }

    public String getCardId() {
        return mCardId;
    }

    public String getFormattedCardId() {
        if (getCardId() == null) {
            return null;
        } else {
            return getCardId().substring(0,3) + " " + getCardId().substring(3,6) + " " + getCardId().substring(6);
        }
    }

    public void setCardId(String cardId) {
        cardId = cardId.replaceAll(" ","");
        this.mCardId = cardId;
    }

    public int getAge() {
        return mAge;
    }

    public void setAge(int age) {
        this.mAge = age;
    }

    public String getFormattedAge() {
        if (getAge() == 1) {
            return "1 year";
        } else {
            return getAge() + " years";
        }
    }

    public GenderEnum getGender() {
        return mGender;
    }

    public void setGender(GenderEnum gender) {
        this.mGender = gender;
    }

    public String getFormattedGender() {
        if (getGender() == GenderEnum.M) {
            return "M";
        } else {
            return "F";
        }
    }

    public String getFormattedAgeAndGender() {
        return getFormattedAge() + " / " + getFormattedGender();
    }

    public byte[] getCroppedPhotoBytes() {
        return mCroppedPhotoBytes;
    }

    public void setCroppedPhotoBytes(byte[] photoBytes) {
        this.mCroppedPhotoBytes = photoBytes;
    }

    public Photo getLocalMemberPhoto() {
        return mLocalMemberPhoto;
    }

    public void setLocalMemberPhoto(Photo photo) {
        this.mLocalMemberPhoto = photo;
    }

    public String getRemoteMemberPhotoUrl() {
        return mRemoteMemberPhotoUrl;
    }

    public String getRemoteMemberPhotoUrlForFetch() {
        return BuildConfig.USING_LOCAL_SERVER ?
                BuildConfig.API_HOST + mRemoteMemberPhotoUrl :
                mRemoteMemberPhotoUrl;
    }

    public void setRemoteMemberPhotoUrl(String photoUrl) {
        this.mRemoteMemberPhotoUrl = photoUrl;
    }

    public Photo getLocalNationalIdPhoto() {
        return mLocalNationalIdPhoto;
    }

    public void setLocalNationalIdPhoto(Photo photo) {
        this.mLocalNationalIdPhoto = photo;
    }

    public String getRemoteNationalIdPhotoUrl() {
        return mRemoteNationalIdPhotoUrl;
    }

    public void setRemoteNationalIdPhotoUrl(String nationalIdPhotoUrl) {
        this.mRemoteNationalIdPhotoUrl = nationalIdPhotoUrl;
    }

    public void setHouseholdId(UUID householdId) {
        this.mHouseholdId = householdId;
    }

    public UUID getHouseholdId() {
        return mHouseholdId;
    }

    public Collection<IdentificationEvent> getIdentificationEvents() {
        return mIdentificationEvents;
    }

    public UUID getFingerprintsGuid() {
        return mFingerprintsGuid;
    }

    public void setFingerprintsGuid(UUID fingerprintsGuid) {
        this.mFingerprintsGuid = fingerprintsGuid;
    }

    public String getPhoneNumber() {
        return mPhoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            this.mPhoneNumber = null;
        } else {
            this.mPhoneNumber = phoneNumber;
        }
    }

    public BirthdateAccuracyEnum getBirthdateAccuracy() {
        return mBirthdateAccuracy;
    }

    public void setBirthdateAccuracy(BirthdateAccuracyEnum birthdateAccuracy) {
        this.mBirthdateAccuracy = birthdateAccuracy;
    }

    public Date getBirthdate() {
        return mBirthdate;
    }

    public void setBirthdate(Date birthdate) {
        this.mBirthdate = birthdate;
    }

    public Date getEnrolledAt() {
        return mEnrolledAt;
    }

    public void setEnrolledAt(Date enrolledAt) {
        this.mEnrolledAt = enrolledAt;
    }

    public boolean isAbsentee() {
        return (getLocalMemberPhoto() == null && getRemoteMemberPhotoUrl() == null) ||
                (getAge() >= MINIMUM_FINGERPRINT_AGE && getFingerprintsGuid() == null);
    }

    public void fetchAndSetPhotoFromUrl(OkHttpClient okHttpClient) throws IOException {
        if (getRemoteMemberPhotoUrl() == null) return;
        Request request = new Request.Builder().url(getRemoteMemberPhotoUrlForFetch()).build();
        okhttp3.Response response = okHttpClient.newCall(request).execute();

        try {
            if (response.isSuccessful()) {
                InputStream is = response.body().byteStream();
                setCroppedPhotoBytes(ByteStreams.toByteArray(is));
            } else {
                Map<String,String> params = new HashMap<>();
                params.put("member.id", getId().toString());
                ExceptionManager.requestFailure(
                        "Failed to fetch member photo",
                        request,
                        response,
                        params
                );
            }
        } finally {
            response.close();
        }
    }

    public boolean shouldCaptureFingerprint() {
        return getAge() >= Member.MINIMUM_FINGERPRINT_AGE;
    }

    public boolean shouldCaptureNationalIdPhoto() {
        return getAge() >= Member.MINIMUM_NATIONAL_ID_AGE;
    }

    public Map<String, RequestBody> formatPatchRequest(Context context) {
        Map<String, RequestBody> requestPartMap = new HashMap<>();

        if (getLocalMemberPhoto() != null && !getLocalMemberPhoto().getSynced()) {
            byte[] image = getLocalMemberPhoto().bytes(context);
            if (image != null) {
                requestPartMap.put(
                        API_NAME_MEMBER_PHOTO,
                        RequestBody.create(MediaType.parse("image/jpg"), image)
                );
            }
        }

        // only include national ID field in request if member photo is not
        //  being sent in order to limit the size of the request
        if (requestPartMap.get(API_NAME_MEMBER_PHOTO) == null) {
            if (getLocalNationalIdPhoto() != null && !getLocalNationalIdPhoto().getSynced()) {
                byte[] image =  getLocalNationalIdPhoto().bytes(context);
                if (image != null) {
                    requestPartMap.put(
                            API_NAME_NATIONAL_ID_PHOTO,
                            RequestBody.create(MediaType.parse("image/jpg"), image)
                    );
                }
            }
        }

        if (dirty(FIELD_NAME_FINGERPRINTS_GUID)) {
            if (getFingerprintsGuid() != null) {
                requestPartMap.put(
                        FIELD_NAME_FINGERPRINTS_GUID,
                        RequestBody.create(MultipartBody.FORM, getFingerprintsGuid().toString())
                );
            }
        }

        if (dirty(FIELD_NAME_PHONE_NUMBER)) {
            if (getPhoneNumber() != null) {
                requestPartMap.put(
                        FIELD_NAME_PHONE_NUMBER,
                        RequestBody.create(MultipartBody.FORM, getPhoneNumber())
                );
            }
        }

        if (dirty(FIELD_NAME_FULL_NAME)) {
            if (getFullName() != null) {
                requestPartMap.put(
                        FIELD_NAME_FULL_NAME,
                        RequestBody.create(MultipartBody.FORM, getFullName())
                );
            }
        }

        if (dirty(FIELD_NAME_CARD_ID)) {
            if (getCardId() != null) {
                requestPartMap.put(
                        FIELD_NAME_CARD_ID,
                        RequestBody.create(MultipartBody.FORM, getCardId())
                );
            }
        }

        return requestPartMap;
    }

    public Map<String, RequestBody> formatPostRequest(Context context) {
        Map<String, RequestBody> requestBodyMap = new HashMap<>();

        requestBodyMap.put(FIELD_NAME_ID, RequestBody.create(MultipartBody.FORM, getId().toString()));

        requestBodyMap.put(
                "provider_assignment[provider_id]",
                RequestBody.create(MultipartBody.FORM, String.valueOf(BuildConfig.PROVIDER_ID))
        );

        requestBodyMap.put(
                "provider_assignment[start_reason]",
                RequestBody.create(MultipartBody.FORM, "birth")
        );

        if (getEnrolledAt() != null) {
            requestBodyMap.put(
                    FIELD_NAME_ENROLLED_AT,
                    RequestBody.create(MultipartBody.FORM, Clock.asIso(getEnrolledAt()))
            );
        } else {
            ExceptionManager.reportErrorMessage("Member.sync called on member without an enrolled date.");
        }

        if (getBirthdate() != null){
            requestBodyMap.put(
                    Member.FIELD_NAME_BIRTHDATE,
                    RequestBody.create(MultipartBody.FORM, Clock.asIso(getBirthdate()))
            );
        } else {
            ExceptionManager.reportErrorMessage("Member.sync called on member with a null birthdate.");
        }

        if (getBirthdateAccuracy() != null) {
            requestBodyMap.put(
                    Member.FIELD_NAME_BIRTHDATE_ACCURACY,
                    RequestBody.create(MultipartBody.FORM, getBirthdateAccuracy().toString())
            );
        } else {
            ExceptionManager.reportErrorMessage("Member.sync called on member with a null birthdateAccuracy.");
        }

        if (getHouseholdId() != null) {
            requestBodyMap.put(
                    Member.FIELD_NAME_HOUSEHOLD_ID,
                    RequestBody.create(MultipartBody.FORM, getHouseholdId().toString())
            );
        } else {
            ExceptionManager.reportErrorMessage("Member.sync called on member with a null household ID.");
        }

        if (getLocalMemberPhoto() != null) {
            byte[] image = getLocalMemberPhoto().bytes(context);
            if (image != null) {
                requestBodyMap.put(API_NAME_MEMBER_PHOTO, RequestBody.create(MediaType.parse("image/jpg"), image));
            }
        }

        // TODO: why do we only do null checks on some of these?
        if (getGender() != null) {
            requestBodyMap.put(
                    FIELD_NAME_GENDER,
                    RequestBody.create(MultipartBody.FORM, getGender().toString())
            );
        } else {
            ExceptionManager.reportErrorMessage("Member.sync called on member with a null gender.");
        }

        if (getFullName() != null) {
            requestBodyMap.put(
                    FIELD_NAME_FULL_NAME,
                    RequestBody.create(MultipartBody.FORM, getFullName())
            );
        } else {
            ExceptionManager.reportErrorMessage("Member.sync called on member without a full name.");
        }

        if (getCardId() != null) {
            requestBodyMap.put(
                    FIELD_NAME_CARD_ID,
                    RequestBody.create(MultipartBody.FORM, getCardId())
            );
        } else {
            ExceptionManager.reportErrorMessage("Member.sync called on member without a valid card ID.");
        }

        clearDirtyFields();
        return requestBodyMap;
    }

    public static boolean validCardId(String cardId) {
        if (cardId == null || cardId.isEmpty()) {
            return false;
        } else {
            return cardId.matches("[A-Z]{3}[0-9]{6}");
        }
    }

    public static boolean validPhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            return false;
        } else {
            return phoneNumber.matches("0?[1-9]\\d{8}");
        }
    }

    public String getFormattedPhoneNumber() {
        if (getPhoneNumber() == null) {
            return null;
        } else if (getPhoneNumber().length() == 10) {
            return "(0) " + getPhoneNumber().substring(1,4) + " " +
                    getPhoneNumber().substring(4,7) + " " + getPhoneNumber().substring(7);
        } else if (getPhoneNumber().length() == 9) {
            return "(0) " + getPhoneNumber().substring(0,3) + " " + getPhoneNumber().substring(3,6) + " " +
                    getPhoneNumber().substring(6,9);
        } else {
            return null;
        }
    }

    public IdentificationEvent currentCheckIn() throws SQLException {
        return IdentificationEventDao.openCheckIn(getId());
    }

    public Member createNewborn() {
        Member newborn = new Member();
        newborn.setHouseholdId(getHouseholdId());
        newborn.setBirthdateAccuracy(BirthdateAccuracyEnum.D);
        newborn.setEnrolledAt(Clock.getCurrentTime());
        return newborn;
    }
}
