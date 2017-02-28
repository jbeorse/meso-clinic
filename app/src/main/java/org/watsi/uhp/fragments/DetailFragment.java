package org.watsi.uhp.fragments;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
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
import org.watsi.uhp.database.IdentificationEventDao;
import org.watsi.uhp.database.MemberDao;
import org.watsi.uhp.managers.Clock;
import org.watsi.uhp.managers.ConfigManager;
import org.watsi.uhp.managers.NavigationManager;
import org.watsi.uhp.models.IdentificationEvent;
import org.watsi.uhp.models.Member;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class DetailFragment extends Fragment {

    private Member mMember;
    private IdentificationEvent.SearchMethodEnum mIdMethod;
    private Member mThroughMember;
    private TextView mMemberName;
    private TextView mMemberAge;
    private TextView mMemberGender;
    private TextView mMemberCardId;
    private ImageView mMemberPhoto;
    private TextView mMemberNotification;
    private Button mConfirmButton;
    private Button mRejectButton;
    private TextView mHouseholdListLabel;
    private ListView mHouseholdListView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getActivity().setTitle(R.string.detail_fragment_label);
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);

        View view = inflater.inflate(R.layout.fragment_detail, container, false);

        mIdMethod = IdentificationEvent.SearchMethodEnum.valueOf(getArguments().getString("idMethod"));
        String memberId = getArguments().getString("memberId");
        String throughMemberId = getArguments().getString("throughMemberId");

        try {
            mMember = MemberDao.findById(UUID.fromString(memberId));
            if (throughMemberId != null) {
                mThroughMember = MemberDao.findById(UUID.fromString(throughMemberId));
            } else {
                mThroughMember = null;
            }
        } catch (SQLException e) {
            Rollbar.reportException(e);
        }

        mMemberName = (TextView) view.findViewById(R.id.member_name);
        mMemberAge = (TextView) view.findViewById(R.id.member_age);
        mMemberGender = (TextView) view.findViewById(R.id.member_gender);
        mMemberCardId = (TextView) view.findViewById(R.id.member_card_id);
        mMemberPhoto = (ImageView) view.findViewById(R.id.member_photo);
        mMemberNotification = (TextView) view.findViewById(R.id.member_notification);
        mConfirmButton = (Button) view.findViewById(R.id.approve_identity);
        mRejectButton = (Button) view.findViewById(R.id.reject_identity);
        mHouseholdListLabel = (TextView) view.findViewById(R.id.household_members_label);
        mHouseholdListView = (ListView) view.findViewById(R.id.household_members);

        setPatientCard();
        setConfirmButton();
        setRejectButton();
        setHouseholdList();
        return view;
    }

    private void setPatientCard() {
        mMemberName.setText(mMember.getFullName());
        mMemberAge.setText("Age - " + String.valueOf(mMember.getAge()));
        mMemberGender.setText(String.valueOf(mMember.getGender()));
        mMemberCardId.setText(String.valueOf(mMember.getFormattedCardId()));
        Bitmap photoBitmap = mMember.getPhotoBitmap();
        if (photoBitmap != null) {
            mMemberPhoto.setImageBitmap(photoBitmap);
        } else {
            mMemberPhoto.setImageResource(R.drawable.portrait_placeholder);
        }
        if (mMember.getAbsentee()) {
            mMemberNotification.setVisibility(View.VISIBLE);
            mMemberNotification.setText(R.string.absentee_notification);
        } else if (mMember.getCardId() == null) {
            mMemberNotification.setVisibility(View.VISIBLE);
            mMemberNotification.setText(R.string.replace_card_notification);
        }
    }

    private void setConfirmButton() {
        mConfirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openClinicNumberDialog();
            }
        });
    }

    private void openClinicNumberDialog() {
        DialogFragment clinicNumberDialog = new ClinicNumberDialogFragment();
        clinicNumberDialog.show(getActivity().getSupportFragmentManager(),
                "ClinicNumberDialogFragment");
        clinicNumberDialog.setTargetFragment(this, 0);
    }

    private void setRejectButton() {
        mRejectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                completeIdentification(false, null, null);
            }
        });
    }

    private void setHouseholdList() {
        try {
            List<Member> householdMembers = MemberDao.getRemainingHouseholdMembers(
                    mMember.getHouseholdId(), mMember.getId());
            ListAdapter adapter = new MemberAdapter(getContext(), householdMembers, false);
            int householdSize = householdMembers.size() + 1;

            mHouseholdListLabel.setText(String.valueOf(householdSize) + " " +
                    getActivity().getString(R.string.household_label));
            mHouseholdListView.setAdapter(adapter);
            mHouseholdListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Member member = (Member) parent.getItemAtPosition(position);
                    MainActivity activity = (MainActivity) getActivity();

                    new NavigationManager(activity).setDetailFragment(
                            member.getId().toString(),
                            IdentificationEvent.SearchMethodEnum.THROUGH_HOUSEHOLD,
                            String.valueOf(mMember.getId())
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

        // TODO: this should be in a transaction
        IdentificationEvent idEvent = new IdentificationEvent();
        idEvent.setMember(mMember);
        idEvent.setSearchMethod(mIdMethod);
        idEvent.setThroughMember(mThroughMember);
        idEvent.setClinicNumberType(clinicNumberType);
        idEvent.setClinicNumber(clinicNumber);
        idEvent.setAccepted(accepted);
        idEvent.setOccurredAt(Clock.getCurrentTime());
        idEvent.setToken(ConfigManager.getLoggedInUserToken(getContext()));
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
}
