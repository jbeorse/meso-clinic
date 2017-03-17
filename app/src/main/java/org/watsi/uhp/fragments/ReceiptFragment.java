package org.watsi.uhp.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.rollbar.android.Rollbar;

import org.watsi.uhp.R;
import org.watsi.uhp.adapters.ReceiptItemAdapter;
import org.watsi.uhp.database.EncounterDao;
import org.watsi.uhp.managers.ConfigManager;
import org.watsi.uhp.managers.NavigationManager;
import org.watsi.uhp.models.Encounter;
import org.watsi.uhp.models.EncounterItem;

import java.sql.SQLException;
import java.util.List;

public class ReceiptFragment extends Fragment {

    private Button mCreateEncounterButton;
    private Encounter mEncounter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
        getActivity().setTitle(R.string.receipt_fragment_label);
        mEncounter = (Encounter) getArguments().getSerializable(NavigationManager.ENCOUNTER_BUNDLE_FIELD);

        View view = inflater.inflate(R.layout.fragment_receipt, container, false);
        List<EncounterItem> encounterItems = (List<EncounterItem>) mEncounter.getEncounterItems();
        mCreateEncounterButton = (Button) view.findViewById(R.id.create_encounter);

        ListView listView = (ListView) view.findViewById(R.id.receipt_items);
        Adapter mAdapter = new ReceiptItemAdapter(getActivity(), encounterItems);
        listView.setAdapter((ListAdapter) mAdapter);

        TextView priceTextView = (TextView) view.findViewById(R.id.total_price);

        String formattedPrice = Encounter.PRICE_FORMAT.format(mEncounter.price());
        priceTextView.setText(getString(R.string.price_with_currency, formattedPrice));

        ((TextView) view.findViewById(R.id.forms_attached)).setText(
                getString(R.string.receipt_forms_attached, mEncounter.getEncounterForms().size()));

        setCreateEncounterButton();
        return view;
    }

    private void setCreateEncounterButton() {
        mCreateEncounterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    mEncounter.setToken(ConfigManager.getLoggedInUserToken(getContext()));
                    EncounterDao.create(mEncounter);
                } catch (SQLException e) {
                    Rollbar.reportException(e);
                }

                new NavigationManager(getActivity()).setCurrentPatientsFragment();

                String toastMessage = mEncounter.getMember()
                        .getFullName() + getString(R.string.encounter_submitted);

                Toast.makeText(getContext(), toastMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
}
