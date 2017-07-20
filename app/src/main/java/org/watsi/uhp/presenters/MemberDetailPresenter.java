package org.watsi.uhp.presenters;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.watsi.uhp.R;
import org.watsi.uhp.adapters.MemberAdapter;
import org.watsi.uhp.custom_components.NotificationBar;
import org.watsi.uhp.database.MemberDao;
import org.watsi.uhp.managers.ExceptionManager;
import org.watsi.uhp.managers.NavigationManager;
import org.watsi.uhp.models.IdentificationEvent;
import org.watsi.uhp.models.Member;

import java.sql.SQLException;
import java.util.List;

public abstract class MemberDetailPresenter {
    private final NavigationManager mNavigationManager;
    private final View mView;
    private final Context mContext;
    private final Member mMember;

    MemberDetailPresenter(View view, Context context, Member member, NavigationManager navigationManager) {
        mView = view;
        mContext = context;
        mMember = member;
        mNavigationManager = navigationManager;
    }

    public void setUp() {
        setMemberNotifications();
        setPatientCardTextFields();
        setPatientCardPhoto();
        setMemberActionButton();
        setBottomListView();
    }

    protected abstract void setMemberActionButton();

    protected abstract void navigateToCompleteEnrollmentFragment();

    public abstract void navigateToMemberEditFragment();

    void setMemberNotifications() {
        if (mMember.isAbsentee()) {
            setAbsenteeNotification();
        }
        if (mMember.getCardId() == null) {
            setReplaceCardNotification();
        }
    }

    void setBottomListView() {
        List<Member> householdMembers = getMembersForBottomListView();
        if (householdMembers != null) {
            setBottomListWithMembers(householdMembers);
        }
    }

    void setPatientCardPhoto() {
        Bitmap photoBitmap = mMember.getPhotoBitmap(mContext.getContentResolver());
        if (photoBitmap != null) {
            setPatientCardPhotoBitmap(photoBitmap);
        } else {
            setPatientCardPhotoAsDefault();
        }
    }

    void setPatientCardTextFields() {
        getMemberNameDetailTextView().setText(mMember.getFullName());
        getMemberAgeAndGenderTextView().setText(mMember.getFormattedAgeAndGender());
        getMemberCardIdDetailTextView().setText(mMember.getFormattedCardId());
        getMemberPhoneNumberTextView().setText(mMember.getFormattedPhoneNumber());
    }

    void setBottomListWithMembers(List<Member> householdMembers) {
        TextView householdListLabel = getHouseholdMembersLabelTextView();
        ListView householdListView = getHouseholdMembersListView();

        int householdSize = householdMembers.size() + 1;

        householdListLabel.setText(formatQuantityStringFromHouseholdSize(householdSize));
        householdListView.setAdapter(new MemberAdapter(getContext(), householdMembers, false));
        householdListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Member member = (Member) parent.getItemAtPosition(position);
                getNavigationManager().setMemberDetailFragment(
                        member,
                        IdentificationEvent.SearchMethodEnum.THROUGH_HOUSEHOLD,
                        getMember()
                );
            }
        });
    }

    List<Member> getMembersForBottomListView() {
        try {
            return MemberDao.getRemainingHouseholdMembers(
                    getMember().getHouseholdId(), getMember().getId());
        } catch (SQLException e) {
            ExceptionManager.reportException(e);
            return null;
        }
    }

    TextView getHouseholdMembersLabelTextView() {
        return (TextView) getView().findViewById(R.id.household_members_label);
    }

    ListView getHouseholdMembersListView() {
        return (ListView) getView().findViewById(R.id.household_members);
    }

    String formatQuantityStringFromHouseholdSize(int householdSize) {
        return getContext().getResources().getQuantityString(
                R.plurals.household_label, householdSize, householdSize);
    }

    TextView getMemberNameDetailTextView() {
        return ((TextView) mView.findViewById(R.id.member_name_detail_fragment));
    }

    TextView getMemberAgeAndGenderTextView() {
        return ((TextView) mView.findViewById(R.id.member_age_and_gender));
    }

    TextView getMemberCardIdDetailTextView() {
        return ((TextView) mView.findViewById(R.id.member_card_id_detail_fragment));
    }

    TextView getMemberPhoneNumberTextView() {
        return ((TextView) mView.findViewById(R.id.member_phone_number));
    }

    Button getMemberActionButton() {
        return ((Button) mView.findViewById(R.id.member_action_button));
    }

    void setPatientCardPhotoBitmap(Bitmap photoBitMap) {
        ImageView memberPhoto = (ImageView) mView.findViewById(R.id.member_photo);
        memberPhoto.setImageBitmap(photoBitMap);
    }

    void setPatientCardPhotoAsDefault() {
        ImageView memberPhoto = (ImageView) mView.findViewById(R.id.member_photo);
        memberPhoto.setImageResource(R.drawable.portrait_placeholder);
    }

    private void setAbsenteeNotification() {
        NotificationBar memberNotification =
                (NotificationBar) mView.findViewById(R.id.absentee_notification);
        memberNotification.setVisibility(View.VISIBLE);
        memberNotification.setOnActionClickListener(
                new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        navigateToCompleteEnrollmentFragment();
                    }
                }
        );
    }

    private void setReplaceCardNotification() {
        NotificationBar memberNotification =
                (NotificationBar) mView.findViewById(R.id.replace_card_notification);
        memberNotification.setVisibility(View.VISIBLE);
        memberNotification.setOnActionClickListener(
                new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        navigateToMemberEditFragment();
                    }
                }
        );
    }

    public Member getMember() {
        return mMember;
    }

    public Context getContext() {
        return mContext;
    }

    public View getView() {
        return mView;
    }

    public NavigationManager getNavigationManager() {
        return mNavigationManager;
    }
}
