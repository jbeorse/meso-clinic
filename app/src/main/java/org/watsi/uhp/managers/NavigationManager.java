package org.watsi.uhp.managers;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

import org.watsi.uhp.R;
import org.watsi.uhp.fragments.AddNewBillableFragment;
import org.watsi.uhp.fragments.PresentingConditionsFragment;
import org.watsi.uhp.fragments.BarcodeFragment;
import org.watsi.uhp.fragments.BaseFragment;
import org.watsi.uhp.fragments.CheckInMemberDetailFragment;
import org.watsi.uhp.fragments.CurrentMemberDetailFragment;
import org.watsi.uhp.fragments.CurrentPatientsFragment;
import org.watsi.uhp.fragments.DiagnosisFragment;
import org.watsi.uhp.fragments.EncounterFormFragment;
import org.watsi.uhp.fragments.EncounterFragment;
import org.watsi.uhp.fragments.EnrollNewbornInfoFragment;
import org.watsi.uhp.fragments.EnrollNewbornPhotoFragment;
import org.watsi.uhp.fragments.EnrollmentContactInfoFragment;
import org.watsi.uhp.fragments.EnrollmentFingerprintFragment;
import org.watsi.uhp.fragments.EnrollmentIdPhotoFragment;
import org.watsi.uhp.fragments.EnrollmentMemberPhotoFragment;
import org.watsi.uhp.fragments.MemberEditFragment;
import org.watsi.uhp.fragments.ReceiptFragment;
import org.watsi.uhp.fragments.SearchMemberFragment;
import org.watsi.uhp.fragments.VersionAndSyncFragment;
import org.watsi.uhp.models.Encounter;
import org.watsi.uhp.models.IdentificationEvent;
import org.watsi.uhp.models.Member;

import java.sql.SQLException;

/**
 * Helper class for managing navigation between fragments
 */
public class NavigationManager {
    public static String IDENTIFICATION_EVENT_BUNDLE_FIELD = "identificationEvent";
    public static String SCAN_PURPOSE_BUNDLE_FIELD = "scanPurpose";
    public static String MEMBER_BUNDLE_FIELD = "member";
    public static String SYNCABLE_MODEL_BUNDLE_FIELD = "syncableModel";
    public static String FRAGMENT_TRANSITION_BACKPRESS = "backPress";


    private FragmentActivity mActivity;
    private FragmentProvider mFragmentProvider;
    private String mLastFragmentTransition;
    private Encounter diagnosisFragment;


    public NavigationManager(FragmentActivity activity) {
        this.mActivity = activity;
        this.mFragmentProvider = new FragmentProvider();
        this.mLastFragmentTransition = "";
    }

    protected void setFragment(BaseFragment fragment) {
        setFragment(fragment, null);
    }

    /*
       @fragment: The new fragment to transition to.
       @overrideFragmentNameToPop: The name of the fragment to pop, which means any fragment in the backstack
                                   that matches this parameter and above it would be popped.
     */
    protected void setFragment(BaseFragment fragment, String overrideFragmentNameToPop) {
        String nextFragmentName = fragment.getName();

        FragmentManager fm = mActivity.getSupportFragmentManager();
        BaseFragment currentFragment = (BaseFragment) fm.findFragmentById(R.id.fragment_container);

        String addTobackStackTag = "add" + nextFragmentName;

        // No need for a removeFragment transaction if there is no current fragment, i.e. when you first open app.
        if (currentFragment != null) {
            // This ensures that we never have call fragmentA -> fragmentB in the stack twice.
            // Sometimes setFragment can be accidentally called twice from the same source fragment to the same
            // destination fragment. In order to prevent this from messing up the stack, since the call to
            // setFragment is synchronous, we can make sure only one of those transactions is committed.
            String nextFragmentTransition = formatUniqueFragmentTransition(currentFragment, nextFragmentName);
            if (!FRAGMENT_TRANSITION_BACKPRESS.equals(mLastFragmentTransition) && !nextFragmentName.equals(currentFragment.getName()) && nextFragmentTransition.equals(this.mLastFragmentTransition)) {
                return;
            } else {
                this.mLastFragmentTransition = nextFragmentTransition;
            }

            fm.beginTransaction()
                    .remove(currentFragment)
                    .addToBackStack("remove" + currentFragment.getName())
                    .commit();

            if (overrideFragmentNameToPop == null && fm.findFragmentByTag(nextFragmentName) != null) {
                fm.popBackStack(addTobackStackTag, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            } else if (fm.findFragmentByTag(overrideFragmentNameToPop) != null) {
                fm.popBackStack("add" + overrideFragmentNameToPop, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            }
        }

        fm.beginTransaction()
                .add(R.id.fragment_container, fragment, nextFragmentName)
                .addToBackStack(addTobackStackTag)
                .commit();
    }

    public void setCurrentPatientsFragment() {
        setFragment(mFragmentProvider.createFragment(CurrentPatientsFragment.class));
    }

    public void setMemberDetailFragment(Member member) {
        setMemberDetailFragment(member, null);
    }

    public void setMemberDetailFragment(Member member, IdentificationEvent idEvent) {
        // TODO: make sure checking for null idEvent is all we need to do here
        if (idEvent != null) {
            setCheckInMemberDetailFragment(member, idEvent, null);
        } else {
            setCurrentMemberDetailFragment(member);
        }
    }

    public void setCheckInMemberDetailFragmentAfterEnrollNewborn(Member member, IdentificationEvent idEvent) {
        // This is to make after enrolling a newborn jump to the newborn's member detail fragment.
        // Clicking back would return to the parent's member detail fragment.
        setCheckInMemberDetailFragment(member, idEvent, "EnrollNewbornInfoFragment");
    }

    protected void setCheckInMemberDetailFragment(Member member, IdentificationEvent idEvent, String customFragmentNameToPop) {
        Bundle bundle = new Bundle();
        bundle.putSerializable(MEMBER_BUNDLE_FIELD, member);
        bundle.putSerializable(IDENTIFICATION_EVENT_BUNDLE_FIELD, idEvent);
        setFragment(mFragmentProvider.createFragment(CheckInMemberDetailFragment.class, bundle), customFragmentNameToPop);
    }

    private void setCurrentMemberDetailFragment(Member member) {
        Bundle bundle = new Bundle();
        bundle.putSerializable(MEMBER_BUNDLE_FIELD, member);
        setFragment(mFragmentProvider.createFragment(CurrentMemberDetailFragment.class, bundle));
    }

    public void setBarcodeFragment(BarcodeFragment.ScanPurposeEnum scanPurpose,
                                   Member member,
                                   IdentificationEvent idEvent) {
        Bundle bundle = new Bundle();
        bundle.putString(SCAN_PURPOSE_BUNDLE_FIELD, scanPurpose.toString());
        if (member != null) bundle.putSerializable(MEMBER_BUNDLE_FIELD, member);
        if (idEvent != null) bundle.putSerializable(IDENTIFICATION_EVENT_BUNDLE_FIELD, idEvent);
        setFragment(mFragmentProvider.createFragment(BarcodeFragment.class, bundle));
    }

    public void setSearchMemberFragment() {
        setFragment(mFragmentProvider.createFragment(SearchMemberFragment.class));
    }

    public void setEncounterFragment(Encounter encounter) {
        Bundle bundle = new Bundle();
        bundle.putSerializable(SYNCABLE_MODEL_BUNDLE_FIELD, encounter);
        setFragment(mFragmentProvider.createFragment(EncounterFragment.class, bundle));
    }

    public void setDiagnosisFragment(Encounter encounter) {
        Bundle bundle = new Bundle();
        bundle.putSerializable(SYNCABLE_MODEL_BUNDLE_FIELD, encounter);
        setFragment(mFragmentProvider.createFragment(DiagnosisFragment.class, bundle));
    }

    public void setEncounterFormFragment(Encounter encounter) {
        Bundle bundle = new Bundle();
        bundle.putSerializable(SYNCABLE_MODEL_BUNDLE_FIELD, encounter);
        setFragment(mFragmentProvider.createFragment(EncounterFormFragment.class, bundle));
    }

    public void setAdditionalInformationFragment(Encounter encounter) {
        Bundle bundle = new Bundle();
        bundle.putSerializable(SYNCABLE_MODEL_BUNDLE_FIELD, encounter);
        setFragment(mFragmentProvider.createFragment(PresentingConditionsFragment.class, bundle));
    }

    public void setReceiptFragment(Encounter encounter) {
        Bundle bundle = new Bundle();
        bundle.putSerializable(SYNCABLE_MODEL_BUNDLE_FIELD, encounter);
        setFragment(mFragmentProvider.createFragment(ReceiptFragment.class, bundle));
    }

    public void setAddNewBillableFragment(Encounter encounter) {
        Bundle bundle = new Bundle();
        bundle.putSerializable(SYNCABLE_MODEL_BUNDLE_FIELD, encounter);
        setFragment(mFragmentProvider.createFragment(AddNewBillableFragment.class, bundle));
    }

    public void setEnrollmentContactInfoFragment(Member member, IdentificationEvent idEvent) {
        Bundle bundle = new Bundle();
        bundle.putSerializable(SYNCABLE_MODEL_BUNDLE_FIELD, member);
        bundle.putSerializable(IDENTIFICATION_EVENT_BUNDLE_FIELD, idEvent);
        setFragment(mFragmentProvider.createFragment(EnrollmentContactInfoFragment.class, bundle));
    }

    public void startCompleteEnrollmentFlow(Member member, IdentificationEvent idEvent) {
        if (member.getCroppedPhotoBytes() == null &&
                member.getRemoteMemberPhotoUrl() == null &&
                member.getLocalMemberPhoto() == null) {
            setEnrollmentMemberPhotoFragment(member, idEvent);
        } else if (member.shouldCaptureNationalIdPhoto()) {
            setEnrollmentIdPhotoFragment(member, idEvent);
        } else if (member.getPhoneNumber() == null) {
            setEnrollmentContactInfoFragment(member, idEvent);
        } else if (member.shouldCaptureFingerprint()) {
            setEnrollmentFingerprintFragment(member, idEvent);
        } else {
            ExceptionManager.reportException(new IllegalStateException("Clinic user clicked complete enrollment for member with photo and fingerprint."));
        }
    }

    private void setEnrollmentMemberPhotoFragment(Member member, IdentificationEvent idEvent) {
        Bundle bundle = new Bundle();
        bundle.putSerializable(SYNCABLE_MODEL_BUNDLE_FIELD, member);
        bundle.putSerializable(IDENTIFICATION_EVENT_BUNDLE_FIELD, idEvent);
        setFragment(mFragmentProvider.createFragment(EnrollmentMemberPhotoFragment.class, bundle));
    }

    public void setEnrollmentIdPhotoFragment(Member member, IdentificationEvent idEvent) {
        Bundle bundle = new Bundle();
        bundle.putSerializable(SYNCABLE_MODEL_BUNDLE_FIELD, member);
        bundle.putSerializable(IDENTIFICATION_EVENT_BUNDLE_FIELD, idEvent);
        setFragment(mFragmentProvider.createFragment(EnrollmentIdPhotoFragment.class, bundle));
    }

    public void setEnrollmentFingerprintFragment(Member member, IdentificationEvent idEvent) {
        Bundle bundle = new Bundle();
        bundle.putSerializable(SYNCABLE_MODEL_BUNDLE_FIELD, member);
        bundle.putSerializable(IDENTIFICATION_EVENT_BUNDLE_FIELD, idEvent);
        setFragment(mFragmentProvider.createFragment(EnrollmentFingerprintFragment.class, bundle));
    }

    public void setMemberEditFragment(Member member, IdentificationEvent idEvent) {
        Bundle bundle = new Bundle();
        bundle.putSerializable(SYNCABLE_MODEL_BUNDLE_FIELD, member);
        bundle.putSerializable(IDENTIFICATION_EVENT_BUNDLE_FIELD, idEvent);
        setFragment(mFragmentProvider.createFragment(MemberEditFragment.class, bundle));
    }

    public void setEnrollNewbornInfoFragment(Member newborn, IdentificationEvent idEvent) {
        Bundle bundle = new Bundle();
        bundle.putSerializable(SYNCABLE_MODEL_BUNDLE_FIELD, newborn);
        bundle.putSerializable(IDENTIFICATION_EVENT_BUNDLE_FIELD, idEvent);
        setFragment(mFragmentProvider.createFragment(EnrollNewbornInfoFragment.class, bundle));
    }

    public void setEnrollNewbornPhotoFragment(Member newborn, IdentificationEvent idEvent) {
        Bundle bundle = new Bundle();
        bundle.putSerializable(SYNCABLE_MODEL_BUNDLE_FIELD, newborn);
        bundle.putSerializable(IDENTIFICATION_EVENT_BUNDLE_FIELD, idEvent);
        setFragment(mFragmentProvider.createFragment(EnrollNewbornPhotoFragment.class, bundle));
    }

    public void setVersionFragment() {
        setFragment(mFragmentProvider.createFragment(VersionAndSyncFragment.class));
    }

    public void setLastFragmentTransitionAsBackPress() {
        this.mLastFragmentTransition = FRAGMENT_TRANSITION_BACKPRESS;
    }

    @NonNull
    static String formatUniqueFragmentTransition(BaseFragment currentFragment, String nextFragmentName) {
        if (currentFragment == null) {
            return "->" + nextFragmentName;
        } else {
            return currentFragment.getName() + "->" + nextFragmentName;
        }
    }

    static class FragmentProvider {
        BaseFragment createFragment(Class<? extends BaseFragment> clazz) {
            return createFragment(clazz, null);
        }

        BaseFragment createFragment(Class<? extends BaseFragment> clazz, Bundle bundle) {
            try {
                BaseFragment fragment = clazz.newInstance();
                if (bundle != null) {
                    fragment.setArguments(bundle);
                }
                return fragment;
                // TODO should not catch here
            } catch (InstantiationException | IllegalAccessException e) {
                ExceptionManager.reportException(e);
                return null;
            }
        }
    }
}
