package org.watsi.uhp.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.rollbar.android.Rollbar;

import org.watsi.uhp.R;
import org.watsi.uhp.activities.MainActivity;
import org.watsi.uhp.adapters.MemberAdapter;
import org.watsi.uhp.database.EncounterItemDao;
import org.watsi.uhp.database.IdentificationEventDao;
import org.watsi.uhp.database.MemberDao;
import org.watsi.uhp.managers.Clock;
import org.watsi.uhp.managers.ConfigManager;
import org.watsi.uhp.managers.NavigationManager;
import org.watsi.uhp.models.Encounter;
import org.watsi.uhp.models.IdentificationEvent;
import org.watsi.uhp.models.Member;

import java.sql.SQLException;
import java.util.List;

public class DetailFragment extends Fragment {

    private Member mMember;
    private IdentificationEvent.SearchMethodEnum mIdMethod = null;
    private Member mThroughMember = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getActivity().setTitle(R.string.detail_fragment_label);
        setHasOptionsMenu(true);
        getActivity().invalidateOptionsMenu();
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);

        View view = inflater.inflate(R.layout.fragment_detail, container, false);

        String searchMethodString = getArguments().getString(NavigationManager.ID_METHOD_BUNDLE_FIELD);
        if (searchMethodString != null) {
            mIdMethod = IdentificationEvent.SearchMethodEnum.valueOf(searchMethodString);
        }
        mMember = (Member) getArguments().getSerializable(NavigationManager.MEMBER_BUNDLE_FIELD);

        mThroughMember = (Member) getArguments()
                .getSerializable(NavigationManager.THROUGH_MEMBER_BUNDLE_FIELD);

        setPatientCard(view);
        setButton(view);
        setHouseholdList(view);
        if (mMember.currentCheckIn() == null) {
            setRejectIdentityLink(view);
        } else {
            setDismissPatientLink(view);
        }

        return view;
    }

    private void setRejectIdentityLink(View view) {
        view.findViewById(R.id.reject_identity).setVisibility(View.VISIBLE);
        view.findViewById(R.id.reject_identity).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(getContext())
                        .setTitle(R.string.reject_identity_alert)
                        .setNegativeButton(android.R.string.no, null)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface arg0, int arg1) {
                                completeIdentification(false, null, null);
                            }
                        }).create().show();
            }
        });
    }

    private void setDismissPatientLink(View view) {
        view.findViewById(R.id.dismiss_patient).setVisibility(View.VISIBLE);
        view.findViewById(R.id.dismiss_patient).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(getContext())
                        .setTitle(R.string.dismiss_patient_alert)
                        .setItems(IdentificationEvent.getFormattedDismissedReasons(), new
                                DialogInterface
                                .OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dismissIdentification(IdentificationEvent
                                        .DismissedReasonEnum.values()[which]);
                            }
                        }).create().show();
            }
        });
    }

    private void setPatientCard(View detailView) {
        ((TextView) detailView.findViewById(R.id.member_name)).setText(mMember.getFullName());
        ((TextView) detailView.findViewById(R.id.member_gender_and_age))
                .setText(mMember.getFormattedGender() + " - " + mMember.getFormattedAge());
        ((TextView) detailView.findViewById(R.id.member_card_id)).setText(mMember.getFormattedCardId());
        ((TextView) detailView.findViewById(R.id.member_phone_number)).setText(mMember.getFormattedPhoneNumber());

        Bitmap photoBitmap = mMember.getPhotoBitmap(getContext().getContentResolver());
        ImageView memberPhoto = (ImageView) detailView.findViewById(R.id.member_photo);
        if (photoBitmap != null) {
            memberPhoto.setImageBitmap(photoBitmap);
        } else {
            memberPhoto.setImageResource(R.drawable.portrait_placeholder);
        }

        TextView memberNotification = (TextView) detailView.findViewById(R.id.member_notification);
        if (mMember.getAbsentee()) {
            memberNotification.setVisibility(View.VISIBLE);
            memberNotification.setText(R.string.absentee_notification);
        } else if (mMember.getCardId() == null) {
            memberNotification.setVisibility(View.VISIBLE);
            memberNotification.setText(R.string.replace_card_notification);
        }
    }

    private void setButton(View view) {
        Button confirmButton = (Button) view.findViewById(R.id.approve_identity);
        if (mMember.currentCheckIn() == null) {
            confirmButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openClinicNumberDialog();
                }
            });
        } else {
            confirmButton.setText(R.string.detail_create_encounter);
            confirmButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Encounter encounter = new Encounter();
                    IdentificationEvent checkIn = mMember.currentCheckIn();
                    encounter.setOccurredAt(Clock.getCurrentTime());
                    encounter.setMember(mMember);
                    encounter.setIdentificationEvent(checkIn);
                    try {
                        encounter.setEncounterItems(
                                EncounterItemDao.getDefaultEncounterItems(checkIn.getClinicNumberType()));
                    } catch (SQLException e) {
                        Rollbar.reportException(e);
                    }
                    MainActivity activity = (MainActivity) getActivity();
                    new NavigationManager(activity).setEncounterFragment(encounter);
                }
            });
        }
    }

    private void openClinicNumberDialog() {
        DialogFragment clinicNumberDialog = new ClinicNumberDialogFragment();
        clinicNumberDialog.show(getActivity().getSupportFragmentManager(),
                "ClinicNumberDialogFragment");
        clinicNumberDialog.setTargetFragment(this, 0);
    }

    private void setHouseholdList(View detailView) {
        TextView householdListLabel = (TextView) detailView.findViewById(R.id.household_members_label);
        ListView householdListView = (ListView) detailView.findViewById(R.id.household_members);

        try {
            List<Member> householdMembers = MemberDao.getRemainingHouseholdMembers(
                    mMember.getHouseholdId(), mMember.getId());
            ListAdapter adapter = new MemberAdapter(getContext(), householdMembers, false);
            int householdSize = householdMembers.size() + 1;

            householdListLabel.setText(String.valueOf(householdSize) + " " +
                    getActivity().getString(R.string.household_label));
            householdListView.setAdapter(adapter);
            householdListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Member member = (Member) parent.getItemAtPosition(position);
                    MainActivity activity = (MainActivity) getActivity();

                    new NavigationManager(activity).setDetailFragment(
                            member,
                            IdentificationEvent.SearchMethodEnum.THROUGH_HOUSEHOLD,
                            mMember
                    );
                }
            });
        } catch (SQLException e) {
            Rollbar.reportException(e);
        }
    }

    public void completeIdentification(boolean accepted,
                                       IdentificationEvent.ClinicNumberTypeEnum clinicNumberType,
                                       Integer clinicNumber) {

        IdentificationEvent idEvent =
                new IdentificationEvent(ConfigManager.getLoggedInUserToken(getContext()));
        idEvent.setMember(mMember);
        idEvent.setSearchMethod(mIdMethod);
        idEvent.setThroughMember(mThroughMember);
        idEvent.setClinicNumberType(clinicNumberType);
        idEvent.setClinicNumber(clinicNumber);
        idEvent.setAccepted(accepted);
        idEvent.setOccurredAt(Clock.getCurrentTime());
        if (mMember.getPhoto() == null) {
            idEvent.setPhotoVerified(false);
        }
        try {
            IdentificationEventDao.create(idEvent);
        } catch (SQLException e) {
            Rollbar.reportException(e);
        }

        new NavigationManager(getActivity()).setCurrentPatientsFragment();
        int messageStringId = accepted ? R.string.identification_approved : R.string.identification_rejected;
        Toast.makeText(getActivity().getApplicationContext(),
                mMember.getFullName() + " " + getActivity().getString(messageStringId),
                Toast.LENGTH_LONG).
                show();
    }

    public void dismissIdentification(IdentificationEvent.DismissedReasonEnum dismissReason) {
        IdentificationEvent checkIn = mMember.currentCheckIn();

        checkIn.setDismissed(true);
        checkIn.setDismissedReason(dismissReason);

        try {
            IdentificationEventDao.update(checkIn);
            new NavigationManager(getActivity()).setCurrentPatientsFragment();
            Toast.makeText(getActivity().getApplicationContext(),
                    mMember.getFullName() + " " + getActivity().getString(R.string.identification_dismissed),
                    Toast.LENGTH_LONG).
                    show();
        } catch (SQLException e) {
            Rollbar.reportException(e);
            Toast.makeText(getActivity().getApplicationContext(),
                    getActivity().getString(R.string.identification_dismissed_failure),
                    Toast.LENGTH_LONG).
                    show();
        }
    }

    public IdentificationEvent.SearchMethodEnum getIdMethod() {
        return this.mIdMethod;
    }

    public Member getMember() { return this.mMember; }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.menu_member_edit).setVisible(true);
        menu.findItem(R.id.menu_enroll_newborn).setVisible(true);
        if (mMember != null && mMember.getAbsentee()) {
            menu.findItem(R.id.menu_complete_enrollment).setVisible(true);
        }
    }
}
