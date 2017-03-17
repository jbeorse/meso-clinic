package org.watsi.uhp.fragments;

import android.app.ProgressDialog;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.rollbar.android.Rollbar;

import org.watsi.uhp.R;
import org.watsi.uhp.database.EncounterDao;
import org.watsi.uhp.database.IdentificationEventDao;
import org.watsi.uhp.database.MemberDao;
import org.watsi.uhp.managers.ConfigManager;
import org.watsi.uhp.models.Member;

import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class VersionAndSyncFragment extends Fragment {

    private SimpleDateFormat mLastModifiedFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
    private SimpleDateFormat mDisplayDateFormat = new SimpleDateFormat("hh:mm:ss a  yyyy/M/d");

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
        getActivity().setTitle(R.string.version_and_sync_label);

        View view = inflater.inflate(R.layout.fragment_version_and_sync, container, false);

        try {
            PackageInfo pInfo = getActivity().getPackageManager()
                    .getPackageInfo(getActivity().getPackageName(), 0);
            ((TextView) view.findViewById(R.id.version_number)).setText(pInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            Rollbar.reportException(e);
        }

        final View finalView = view;
        view.findViewById(R.id.refresh_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshValues(finalView);
            }
        });

        Log.d("UHP", "before refresh method");
        refreshValues(view);
        Log.d("UHP", "after refresh method");

        return view;
    }

    private void updateTimestamps(View view) {
        ((TextView) view.findViewById(R.id.fetch_members_timestamp))
                .setText(formatTimestamp(ConfigManager.getMemberLastModified(getContext())));

        ((TextView) view.findViewById(R.id.fetch_billables_timestamp))
                .setText(formatTimestamp(ConfigManager.getBillablesLastModified(getContext())));
    }

    private void refreshValues(View view) {
        updateTimestamps(view);

        final ProgressDialog spinner = new ProgressDialog(getContext(), ProgressDialog.STYLE_SPINNER);
        spinner.setCancelable(false);
        spinner.setMessage("Loading...");
        spinner.show();

        new AsyncTask<String, Void, int[]>() {
            @Override
            protected int[] doInBackground(String... params) {
                int[] counts = new int[5];
                try {
                    counts[0] = MemberDao.membersWithPhotosToFetch().size();
                    List<Member> unsyncedMembers = MemberDao.unsynced();
                    int newMembersCount = 0;
                    int editedMembersCount = 0;
                    for (Member member : unsyncedMembers) {
                        if (member.isNew()) {
                            newMembersCount++;
                        } else {
                            editedMembersCount++;
                        }
                    }
                    counts[1] = newMembersCount;
                    counts[2] = editedMembersCount;
                    counts[3] = IdentificationEventDao.unsynced().size();
                    counts[4] = EncounterDao.unsynced().size();
                } catch (SQLException | IllegalStateException e) {
                    Rollbar.reportException(e);
                }
                return counts;
            }

            @Override
            protected void onPostExecute(int[] result) {
                View view = getView();

                ((TextView) view.findViewById(R.id.fetch_member_pictures_quantity))
                        .setText(formattedQuantity(result[0]));
                ((TextView) view.findViewById(R.id.sync_edited_members_quantity))
                        .setText(formattedQuantity(result[1]));
                ((TextView) view.findViewById(R.id.sync_new_members_quantity))
                        .setText(formattedQuantity(result[2]));
                ((TextView) view.findViewById(R.id.sync_id_events_quantity))
                        .setText(formattedQuantity(result[3]));
                ((TextView) view.findViewById(R.id.sync_encounters_quantity))
                        .setText(formattedQuantity(result[4]));

                spinner.dismiss();
            }
        }.execute();
    }

    private String formattedQuantity(int count) {
        if (count == 0) {
            return getString(R.string.all_synced);
        } else {
            return count + " pending";
        }
    }

    private String formatTimestamp(String lastModifiedTimestamp) {
        try {
            Date lastModified = mLastModifiedFormat.parse(lastModifiedTimestamp);
            return mDisplayDateFormat.format(lastModified);
        } catch (ParseException e) {
            return "Could not parse timestamp";
        }
    }
}