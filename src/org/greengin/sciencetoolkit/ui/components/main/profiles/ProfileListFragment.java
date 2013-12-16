package org.greengin.sciencetoolkit.ui.components.main.profiles;

import java.util.List;
import java.util.Vector;

import org.greengin.sciencetoolkit.R;
import org.greengin.sciencetoolkit.logic.datalogging.DeprecatedDataLogger;
import org.greengin.sciencetoolkit.logic.datalogging.DataLoggerStatusListener;
import org.greengin.sciencetoolkit.model.ProfileManager;
import org.greengin.sciencetoolkit.model.SettingsManager;
import org.greengin.sciencetoolkit.model.notifications.ModelNotificationListener;
import org.greengin.sciencetoolkit.ui.Arguments;
import org.greengin.sciencetoolkit.ui.CreateProfileDialogFragment;
import org.greengin.sciencetoolkit.ui.ParentListFragment;
import org.greengin.sciencetoolkit.ui.components.main.profiles.files.FileManagementActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;

public class ProfileListFragment extends ParentListFragment implements DataLoggerStatusListener, ModelNotificationListener {

	public static final String REQUEST_SELECTED_PROFILE = "REQUEST_SELECTED_PROFILE";

	LinearLayout buttonBar;
	Button switchProfile;

	BroadcastReceiver requestReceiver;
	Vector<ProfileFragment> profileFragments;
	String selectedProfile;

	public ProfileListFragment() {
		super(R.id.profile_list);
		profileFragments = new Vector<ProfileFragment>();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);

		requestReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				setSelectedProfile(intent.getStringExtra(Arguments.ARG_PROFILE), true);
			}
		};
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

		View rootView = inflater.inflate(R.layout.fragment_profile_list, container, false);

		buttonBar = (LinearLayout) rootView.findViewById(R.id.switch_profile_button_bar);
		
		switchProfile = (Button) rootView.findViewById(R.id.switch_profile);
		switchProfile.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!DeprecatedDataLogger.i().isRunning()) {
					ProfileManager.i().switchActiveProfile(selectedProfile);
				}
			}
		});

		updateChildrenList();

		return rootView;
	}

	@Override
	public void onResume() {
		super.onResume();
		updateView();

		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(requestReceiver, new IntentFilter(REQUEST_SELECTED_PROFILE));
		SettingsManager.i().registerDirectListener("profiles", this);
		ProfileManager.i().registerDirectListener(this);
		DeprecatedDataLogger.i().registerStatusListener(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		DeprecatedDataLogger.i().unregisterStatusListener(this);
		ProfileManager.i().unregisterDirectListener(this);
		SettingsManager.i().unregisterDirectListener("profiles", this);
		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(requestReceiver);
	}

	@Override
	public void modelNotificationReceived(String msg) {
		if ("list".equals(msg)) {
			updateView();
		} else if ("profiles".equals(msg)) {
			updateSwitchButton();
		}
	}

	private void updateView() {
		setSelectedProfile(ProfileManager.i().getActiveProfileId(), false);
		updateChildrenList();
		updateSwitchButton();
	}

	private void updateSelectedChild() {
		for (ProfileFragment f : profileFragments) {
			f.updateRadioChecked(selectedProfile);
		}
	}

	@Override
	protected List<Fragment> getUpdatedFragmentChildren() {
		profileFragments.clear();

		Vector<Fragment> fragments = new Vector<Fragment>();

		for (String profileId : ProfileManager.i().getProfileIds()) {
			ProfileFragment fragment = new ProfileFragment();
			Bundle args = new Bundle();
			args.putString(Arguments.ARG_PROFILE, profileId);
			fragment.setArguments(args);
			fragments.add(fragment);
			profileFragments.add(fragment);
		}

		return fragments;
	}

	@Override
	protected boolean removeChildFragmentOnUpdate(Fragment child) {
		return child instanceof ProfileFragment;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.profiles, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case R.id.action_data_logging_new:
			CreateProfileDialogFragment.showCreateProfileDialog(getChildFragmentManager(), false, false);
			return true;

		case R.id.action_data_file_management: {
			Intent intent = new Intent(getActivity(), FileManagementActivity.class);
			startActivity(intent);
			return true;
		}

		}

		return super.onOptionsItemSelected(item);
	}

	private void setSelectedProfile(String profileId, boolean updateChildren) {
		if (!profileId.equals(selectedProfile)) {
			this.selectedProfile = profileId;
			
			updateSwitchButton();
			
			if (updateChildren) {
				updateSelectedChild();
			}
		}
	}
	
	private void updateSwitchButton() {
		boolean showButtonBar = !DeprecatedDataLogger.i().isRunning() && this.selectedProfile != null && !this.selectedProfile.equals(ProfileManager.i().getActiveProfileId());
		 
		LayoutParams lp = buttonBar.getLayoutParams();
		lp.height = showButtonBar ? LayoutParams.WRAP_CONTENT : 0;
		if (showButtonBar) {
			lp.height = LayoutParams.WRAP_CONTENT;
			switchProfile.setText(ProfileManager.i().profileIdIsDefault(this.selectedProfile) ? R.string.switch_profile_to_default : R.string.switch_profile);
		} else {
			lp.height = 0;
		}
		buttonBar.setLayoutParams(lp);		
	}

	@Override
	public void dataLoggerStatusModified() {
		updateSwitchButton();
	}
}