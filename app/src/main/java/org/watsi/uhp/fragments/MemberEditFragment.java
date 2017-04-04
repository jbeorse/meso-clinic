package org.watsi.uhp.fragments;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import org.watsi.uhp.R;
import org.watsi.uhp.database.MemberDao;
import org.watsi.uhp.managers.ConfigManager;
import org.watsi.uhp.managers.ExceptionManager;
import org.watsi.uhp.managers.NavigationManager;
import org.watsi.uhp.models.AbstractModel;
import org.watsi.uhp.models.IdentificationEvent;
import org.watsi.uhp.models.Member;

import java.sql.SQLException;

public class MemberEditFragment extends Fragment {

    private Member mMember;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getActivity().setTitle(R.string.member_edit_label);

        View view = inflater.inflate(R.layout.fragment_member_edit, container, false);

        mMember = (Member) getArguments().getSerializable(NavigationManager.MEMBER_BUNDLE_FIELD);

        final EditText nameView = (EditText) view.findViewById(R.id.member_name);
        nameView.getText().append(mMember.getFullName());
        final EditText cardIdView = (EditText) view.findViewById(R.id.card_id);

        String mScannedCardId = getArguments().getString(
                NavigationManager.SCANNED_CARD_ID_BUNDLE_FIELD);
        if (mScannedCardId != null) {
            cardIdView.getText().append(mScannedCardId);
        } else if (mMember.getCardId() != null) {
            cardIdView.getText().append(mMember.getCardId());
        }
        final EditText phoneNumView = (EditText) view.findViewById(R.id.phone_number);
        if (mMember.getPhoneNumber() != null) {
            phoneNumView.getText().append(mMember.getPhoneNumber());
        }

        view.findViewById(R.id.scan_card).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle bundle = new Bundle();
                String idMethodString =
                        getArguments().getString(NavigationManager.ID_METHOD_BUNDLE_FIELD);
                bundle.putString(NavigationManager.ID_METHOD_BUNDLE_FIELD, idMethodString);
                new NavigationManager(getActivity())
                        .setBarcodeFragment(
                                BarcodeFragment.ScanPurposeEnum.MEMBER_EDIT, mMember, bundle);
            }
        });

        view.findViewById(R.id.save_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveChanges(nameView, cardIdView, phoneNumView);
            }
        });

        return view;
    }

    public void saveChanges(EditText nameView, EditText cardIdView, EditText phoneNumView) {
        if (valid(nameView, cardIdView, phoneNumView)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setMessage(R.string.member_edit_confirmation);
            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String toastMessage = mMember.getFullName() + "'s information has been updated.";
                    try {
                        mMember.setUnsynced(ConfigManager.getLoggedInUserToken(getContext()));
                        MemberDao.update(mMember);
                    } catch (SQLException e) {
                        ExceptionManager.handleException(e);
                        toastMessage = "Failed to update the member information.";
                    }

                    String idMethodString =
                            getArguments().getString(NavigationManager.ID_METHOD_BUNDLE_FIELD);
                    IdentificationEvent.SearchMethodEnum idMethod = null;
                    if (idMethodString != null) {
                        idMethod =  IdentificationEvent.SearchMethodEnum.valueOf(idMethodString);
                    }
                    new NavigationManager(getActivity()).setDetailFragment(mMember, idMethod, null);
                    Toast.makeText(
                            getActivity().getApplicationContext(),
                            toastMessage,
                            Toast.LENGTH_LONG
                    ).show();
                }
            });
            builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.show();
        }
    }

    private boolean valid(EditText nameView, EditText cardIdView, EditText phoneNumView) {
        boolean valid = true;

        try {
            mMember.setFullName(nameView.getText().toString());
        } catch (AbstractModel.ValidationException e) {
            nameView.setError(getString(R.string.name_validation_error));
            valid = false;
        }

        try {
            mMember.setCardId(cardIdView.getText().toString());
        } catch (AbstractModel.ValidationException e) {
            cardIdView.setError(getString(R.string.card_id_validation_error));
            valid = false;
        }

        try {
            String updatedPhoneNumber = phoneNumView.getText().toString();
            if (updatedPhoneNumber.isEmpty()) updatedPhoneNumber = null;
            mMember.setPhoneNumber(updatedPhoneNumber);
        } catch (AbstractModel.ValidationException e) {
            phoneNumView.setError(getString(R.string.phone_number_validation_error));
            valid = false;
        }

        return valid;
    }
}
