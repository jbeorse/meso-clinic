package org.watsi.uhp.presenters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.VectorDrawable;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.simprints.libsimprints.Constants;
import com.simprints.libsimprints.SimHelper;
import com.simprints.libsimprints.Verification;

import org.watsi.uhp.BuildConfig;
import org.watsi.uhp.R;
import org.watsi.uhp.activities.ClinicActivity;
import org.watsi.uhp.fragments.CheckInMemberDetailFragment;
import org.watsi.uhp.fragments.ClinicNumberDialogFragment;
import org.watsi.uhp.managers.Clock;
import org.watsi.uhp.managers.ExceptionManager;
import org.watsi.uhp.managers.NavigationManager;
import org.watsi.uhp.managers.SessionManager;
import org.watsi.uhp.models.IdentificationEvent;
import org.watsi.uhp.models.Member;

import java.sql.SQLException;
import java.util.Vector;

/**
 * Created by michaelliang on 6/13/17.
 */

public class CheckInMemberDetailPresenter extends MemberDetailPresenter {
    static final int SIMPRINTS_VERIFICATION_INTENT = 1;
    static final int ANDROID_MATERIAL_DESIGN_RED = Color.rgb(244,67,54);
    static final int ANDROID_MATERIAL_DESIGN_GREEN = Color.rgb(76,175,80);
    static final int ANDROID_MATERIAL_DESIGN_BLUE = Color.rgb(33,150,243);
    static final int ANDROID_MATERIAL_DESIGN_GRAY = Color.rgb(158,158,158);
    static final int DEFAULT_BORDER_WIDTH = 1;

    private final SessionManager mSessionManager;
    private IdentificationEvent mIdEvent;
    private final CheckInMemberDetailFragment mCheckInMemberDetailFragment;

    private final Button mMemberSecondaryButton;
    private final TextView mMemberIndicator;

    public CheckInMemberDetailPresenter(NavigationManager navigationManager, SessionManager sessionManager, CheckInMemberDetailFragment checkInMemberDetailFragment, View view, Context context, Member member, IdentificationEvent idEvent) {
        super(view, context, member, navigationManager);
        mCheckInMemberDetailFragment = checkInMemberDetailFragment;
        mSessionManager = sessionManager;
        mIdEvent = idEvent;

        mMemberSecondaryButton = getMemberSecondaryButton();
        mMemberIndicator = getMemberIndicator();
        setFingerprintScanResult();
    }

    protected void setMemberActionButton() {
        Button memberActionButton = getMemberActionButton();
        memberActionButton.setText(R.string.check_in);
        memberActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openClinicNumberDialog();
            }
        });
    }

    protected void setFingerprintScanResult() {
        if (getMember().isAbsentee()) {
            mMemberSecondaryButton.setVisibility(View.VISIBLE);
            mMemberSecondaryButton.setText("Complete Enrollment");
            getMemberSecondaryButton().setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            getNavigationManager().setEnrollmentMemberPhotoFragment(getMember(), mIdEvent);
                        }
                    }
            );
        } else if (getMember().getFingerprintsGuid() == null) {
            // show nothing.
        } else if (mIdEvent.getFingerprintsVerificationTier() == null) {
            mMemberSecondaryButton.setVisibility(View.VISIBLE);
            mMemberSecondaryButton.setText("Scan");
            Drawable fingerprintIcon = getContext().getResources().getDrawable(R.drawable.fingerprints, null);
            fingerprintIcon.setTint(ANDROID_MATERIAL_DESIGN_GRAY);
            mMemberSecondaryButton.setCompoundDrawablesWithIntrinsicBounds(fingerprintIcon, null, null, null);
            getMemberSecondaryButton().setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            SimHelper simHelper = new SimHelper(BuildConfig.SIMPRINTS_API_KEY, mSessionManager.getCurrentLoggedInUsername());
                            Intent fingerprintIdentificationIntent = simHelper.verify(BuildConfig.PROVIDER_ID.toString(), getMember().getFingerprintsGuid().toString());
                            mCheckInMemberDetailFragment.startActivityForResult(
                                    fingerprintIdentificationIntent,
                                    SIMPRINTS_VERIFICATION_INTENT
                            );
                        }
                    }
            );
        } else if (mIdEvent.getFingerprintsVerificationTier() == "TIER_5") {
            mMemberIndicator.setVisibility(View.VISIBLE);
            mMemberIndicator.setText("No Match");
            mMemberIndicator.setTextColor(ANDROID_MATERIAL_DESIGN_RED);
            GradientDrawable border = (GradientDrawable) mMemberIndicator.getBackground();
            border.setStroke(DEFAULT_BORDER_WIDTH, ANDROID_MATERIAL_DESIGN_RED);
            VectorDrawable fingerprintIcon = (VectorDrawable) mMemberIndicator.getCompoundDrawables()[0];
            fingerprintIcon.setTint(ANDROID_MATERIAL_DESIGN_RED);
        } else {
            mMemberIndicator.setVisibility(View.VISIBLE);
            mMemberIndicator.setText("Good match");
            mMemberIndicator.setTextColor(ANDROID_MATERIAL_DESIGN_GREEN);
            GradientDrawable border = (GradientDrawable) mMemberIndicator.getBackground();
            border.setStroke(DEFAULT_BORDER_WIDTH, ANDROID_MATERIAL_DESIGN_GREEN);
            VectorDrawable fingerprintIcon = (VectorDrawable) mMemberIndicator.getCompoundDrawables()[0];
            fingerprintIcon.setTint(ANDROID_MATERIAL_DESIGN_GREEN);
        }
    }

    //// Tested above
    //// Below TBD because of fingerprints

    public void handleOnActivityResult(int requestCode, int resultCode, Intent data) {
        mIdEvent.setFingerprintsVerificationResultCode(resultCode);

        // Report any errors if necessary.
        if (requestCode != SIMPRINTS_VERIFICATION_INTENT) {
            ExceptionManager.reportException(new IllegalStateException("Request code in simprints call was from a different intent: " + requestCode));
        }

        if (resultCode != Constants.SIMPRINTS_OK) {
            ExceptionManager.reportException(new IllegalStateException("ResultCode in simprints call is not OK. resultCode: " + resultCode));
        }

        if (resultCode == Constants.SIMPRINTS_CANCELLED) {
            showScanFailedToast();
        } else if (resultCode == Constants.SIMPRINTS_OK) {
            Verification verification = data.getParcelableExtra(Constants.SIMPRINTS_VERIFICATION);
            String fingerprintTier = verification.getTier().toString();
            float fingerprintConfidence = verification.getConfidence();

            mIdEvent.setFingerprintsVerificationConfidence(fingerprintConfidence);
            mIdEvent.setFingerprintsVerificationTier(fingerprintTier);

            showScanSuccessfulToast();
            navigateToCheckInMemberDetailFragment();
        } else {
            navigateToCheckInMemberDetailFragment();
        }
    }

    public IdentificationEvent getIdEvent() {
        return mIdEvent;
    }

    protected void navigateToCheckInMemberDetailFragment() {
        getNavigationManager().setCheckInMemberDetailFragment(getMember(), mIdEvent);
    }

    protected void showScanFailedToast() {
        Toast.makeText(
                getContext(),
                "Fingerprint Scan Failed",
                Toast.LENGTH_LONG).show();
    }

    protected void showScanSuccessfulToast() {
        Toast.makeText(getContext(), "Fingerprint Scan Successful!", Toast.LENGTH_LONG).show();
    }

    private void openClinicNumberDialog() {
        ClinicNumberDialogFragment clinicNumberDialog = new ClinicNumberDialogFragment();
        clinicNumberDialog.show(((ClinicActivity) getContext()).getSupportFragmentManager(),
                "ClinicNumberDialogFragment");
        clinicNumberDialog.setTargetFragment(mCheckInMemberDetailFragment, 0);
    }

    public void saveIdentificationEventAndCheckIn(IdentificationEvent.ClinicNumberTypeEnum clinicNumberType, int clinicNumber) throws SQLException {
        mIdEvent.setClinicNumber(clinicNumber);
        mIdEvent.setClinicNumberType(clinicNumberType);
        mIdEvent.setAccepted(true);
        mIdEvent.setOccurredAt(Clock.getCurrentTime());
        mIdEvent.saveChanges(((ClinicActivity) getContext()).getAuthenticationToken());
        displayIdentificationSuccessfulToast();
        navigateToCurrentPatientsFragment();
    }

    protected void navigateToCurrentPatientsFragment() {
        getNavigationManager().setCurrentPatientsFragment();
    }

    protected void displayIdentificationSuccessfulToast() {
        Toast.makeText(getContext(),
                mIdEvent.getMember().getFullName() + " " + getContext().getString(R.string.identification_approved),
                Toast.LENGTH_LONG).
                show();
    }

    public void reportMember() {
        mIdEvent.setAccepted(false);
        if (mIdEvent.getOccurredAt() == null) {
            mIdEvent.setOccurredAt(Clock.getCurrentTime());
        }
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.reject_identity_alert)
                .setNegativeButton(android.R.string.no, null)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface arg0, int arg1) {
                        try {
                            mIdEvent.saveChanges(((ClinicActivity) getContext()).getAuthenticationToken());
                            getNavigationManager().setCurrentPatientsFragment();
                            Toast.makeText(getContext(),
                                    getMember().getFullName() + " " + getContext().getString(R.string.identification_rejected),
                                    Toast.LENGTH_LONG).
                                    show();
                        } catch (SQLException e) {
                            ExceptionManager.reportException(e);
                        }
                    }
                }).create().show();
    }
}
