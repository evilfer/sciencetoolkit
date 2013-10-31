package org.greengin.sciencetoolkit.model;

import java.util.Map.Entry;

import org.greengin.sciencetoolkit.model.notifications.NotificationListener;
import org.greengin.sciencetoolkit.model.notifications.NotificationListenerAggregator;

import android.content.Context;
import android.util.Log;

public class ProfileManager extends AbstractModelManager implements NotificationListener {

	private static ProfileManager instance;
	
	public static void init(Context applicationContext) {
		instance = new ProfileManager(applicationContext);
	}

	public static ProfileManager getInstance() {
		return instance;
	}

	NotificationListenerAggregator listeners;
	
	Model settings;
	String activeProfile;
	
	private ProfileManager(Context applicationContext) {
		super(applicationContext, "profiles.xml", 500);
		settings = SettingsManager.getInstance().get("profiles");
		activeProfile = settings.getString("current_profile", null);
		listeners = new NotificationListenerAggregator(applicationContext, "profiles:notifications");
		SettingsManager.getInstance().registerDirectListener("profiles", this);
		initDefaultProfile();
		checkDataConsistency();
	}
	
	private void initDefaultProfile() {
		if (settings.getBool("create_default", true) && items.size() == 0) {
			Model defaultProfile = createEmptyProfile();
			defaultProfile.setString("title", "Default");
			
			settings.setBool("create_default", false);
			settings.setString("current_profile", defaultProfile.getString("id"));
		}
	}
	
	private void checkDataConsistency() {
		for(Entry<String, Model> entry : items.entrySet()) {
			String id = entry.getKey();
			Model model = entry.getValue();
			
			if (id.length() == 0) {
				Log.d("stk profiles", "empty id");
			} else {
				if (!id.equals(model.getString("id"))) {
					Log.d("stk profiles", "conflicting ids: " + id + " " + model.getString("id"));
				}
			}
		}
	}
	
	private Model createEmptyProfile() {
		String id = getNewId();
		Model profile = new Model(this);
		items.put(id, profile);
		
		profile.setString("id", id);
		profile.getModel("sensors", true, true);
		modelModified(profile);
		return profile;
	}
	
	public void addSensor(Model profile, String sensorId) {
		Model profileSensors = profile.getModel("sensors", true, true);
		int weight = profileSensors.getModels().size();
		
		Model profileSensor = profileSensors.getModel(sensorId, true, true);
		profileSensor.setString("id", sensorId, true);
		profileSensor.setInt("weight", weight, true);
		Model sensorSettings = SettingsManager.getInstance().get("sensor:" + sensorId);
		Model profileSensorSettings = profileSensor.getModel("sensor_settings", true, true);
		profileSensorSettings.copyPrimitives(sensorSettings, true);
		this.modelModified(profile);
	}
	
	public void removeSensor(Model profile, String sensorId) {
		Model profileSensors = profile.getModel("sensors", true, true);
		profileSensors.clear(sensorId);
	}

	public Model get(String key) {
		return get(key, false);
	}
	
	public Model getActiveProfile() {
		Model profile = get(activeProfile);
		return profile;
	}
	
	
	private String getNewId() {
		for (int i = 1;; i++) {
			String test = Integer.toString(i);
			if (!items.containsKey(test)) {
				return test;
			}
		}
	}
	
	@Override
	public void modelModified(Model model) {
		super.modelModified(model);
		
		Model profile = model.getRootParent();
		String profileId = profile.getString("id", null);
		listeners.fireEvent(profileId);
	}

	@Override
	public void notificationReveiced(String msg) {
		String profile = settings.getString("current_profile", null);
		boolean changed = profile == null ? activeProfile != null : !profile.equals(activeProfile);
		if (changed) {
			this.activeProfile = profile;
			listeners.fireEvent("switch");
		}
	}
	
	public void registerUIListener(NotificationListener listener) {
		listeners.addUIListener(listener);
	}

	public void unregisterUIListener(NotificationListener listener) {
		listeners.removeUIListener(listener);
	}

	public void registerDirectListener(NotificationListener listener) {
		listeners.addDirectListener(listener);
	}

	public void unregisterDirectListener(NotificationListener listener) {
		listeners.removeDirectListener(listener);
	}
}
