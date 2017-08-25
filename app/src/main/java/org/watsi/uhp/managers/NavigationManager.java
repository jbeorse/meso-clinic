package org.watsi.uhp.managers;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

import org.watsi.uhp.R;
import org.watsi.uhp.fragments.AddNewBillableFragment;
import org.watsi.uhp.fragments.BarcodeFragment;
import org.watsi.uhp.fragments.BaseFragment;
import org.watsi.uhp.fragments.CheckInMemberDetailFragment;
import org.watsi.uhp.fragments.CurrentMemberDetailFragment;
import org.watsi.uhp.fragments.CurrentPatientsFragment;
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

/**
 * Helper class for managing navigation between fragments
 */
public class NavigationManager {
    public static String IDENTIFICATION_EVENT_BUNDLE_FIELD = "identificationEvent";
    public static String SCAN_PURPOSE_BUNDLE_FIELD = "scanPurpose";
    public static String MEMBER_BUNDLE_FIELD = "member";
    public static String SYNCABLE_MODEL_BUNDLE_FIELD = "syncableModel";

    private FragmentActivity mActivity;
    private FragmentProvider mFragmentProvider;

    public NavigationManager(FragmentActivity activity, FragmentProvider fragmentProvider) {
        this.mActivity = activity;
        this.mFragmentProvider = fragmentProvider;
    }

    public NavigationManager(FragmentActivity activity) {
        this(activity, new FragmentProvider());
    }

    protected void setFragment(BaseFragment fragment) {
        setFragment(fragment, fragment.getName());
    }

    protected void setFragment(BaseFragment fragment, String nextFragmentName) {
        FragmentManager fm = mActivity.getSupportFragmentManager();
        BaseFragment currentFragment = (BaseFragment) fm.findFragmentById(R.id.fragment_container);

        // No need for a removeFragment transaction if there is no current fragment. (i.e. open app)
        if (currentFragment != null) {
            fm.beginTransaction()
                    .remove(currentFragment)
                    .addToBackStack("remove" + currentFragment.getName())
                    .commit();
            // If the fragment exists in the backstack, remove backstack entries above and including that point.
            if (fm.findFragmentByTag(nextFragmentName) != null) {
                fm.popBackStack("add" + nextFragmentName, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            }
        }

        fm.beginTransaction()
                .add(R.id.fragment_container, fragment, nextFragmentName)
                .addToBackStack("add" + nextFragmentName)
                .commit();
    }

    public void setCurrentPatientsFragment() {
        setFragment(mFragmentProvider.createFragment(CurrentPatientsFragment.class));
    }

    public void setMemberDetailFragment(Member member) {
        setMemberDetailFragment(member, null);
    }

    public void setMemberDetailFragment(Member member, IdentificationEvent idEvent) {
        if (member.currentCheckIn() == null) {
            setCheckInMemberDetailFragment(member, idEvent, null);
        } else {
            setCurrentMemberDetailFragment(member);
        }
    }

    // This method should only be used after enrolling a newborn.
    public void setMemberDetailFragmentAfterEnrollNewborn(Member member, IdentificationEvent idEvent) {
        if (member.currentCheckIn() == null && idEvent.getThroughMember() != null) {
            setCheckInMemberDetailFragment(member, idEvent, "MemberDetailFragment-" + idEvent.getThroughMember().getId());
        } else {
            throw new IllegalStateException("setMemberDetailFragmentAfterEnrollNewborn should only be called on complete enrollment.");
        }
    }

    protected void setCheckInMemberDetailFragment(Member member, IdentificationEvent idEvent, String nextFragmentName) {
        Bundle bundle = new Bundle();
        bundle.putSerializable(MEMBER_BUNDLE_FIELD, member);
        bundle.putSerializable(IDENTIFICATION_EVENT_BUNDLE_FIELD, idEvent);
        if (nextFragmentName != null) {
            setFragment(mFragmentProvider.createFragment(CheckInMemberDetailFragment.class, bundle), nextFragmentName);
        } else {
            setFragment(mFragmentProvider.createFragment(CheckInMemberDetailFragment.class, bundle));
        }
    }

    protected void setCurrentMemberDetailFragment(Member member) {
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

    public void setEncounterFormFragment(Encounter encounter) {
        Bundle bundle = new Bundle();
        bundle.putSerializable(SYNCABLE_MODEL_BUNDLE_FIELD, encounter);
        setFragment(mFragmentProvider.createFragment(EncounterFormFragment.class, bundle));
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

    public void setEnrollmentMemberPhotoFragment(Member member, IdentificationEvent idEvent) {
        Bundle bundle = new Bundle();
        bundle.putSerializable(SYNCABLE_MODEL_BUNDLE_FIELD, member);
        bundle.putSerializable(IDENTIFICATION_EVENT_BUNDLE_FIELD, idEvent);
        if (member.getPhotoUrl() == null && member.getPhoto() == null) {
            setFragment(mFragmentProvider.createFragment(EnrollmentMemberPhotoFragment.class, bundle));
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

    public static class FragmentProvider {
        public BaseFragment createFragment(Class<? extends BaseFragment> clazz) {
            return createFragment(clazz, null);
        }

        public BaseFragment createFragment(Class<? extends BaseFragment> clazz, Bundle bundle) {
            try {
                BaseFragment fragment = clazz.newInstance();
                if (bundle != null) {
                    fragment.setArguments(bundle);
                }
                return fragment;
            } catch (InstantiationException | IllegalAccessException e) {
                ExceptionManager.reportException(e);
                return null;
            }
        }
    }
}
