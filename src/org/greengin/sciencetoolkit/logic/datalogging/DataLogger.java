package org.greengin.sciencetoolkit.logic.datalogging;

import java.io.File;
import java.util.HashMap;
import java.util.Vector;
import java.util.concurrent.locks.ReentrantLock;

import org.greengin.sciencetoolkit.logic.sensors.SensorWrapper;
import org.greengin.sciencetoolkit.logic.sensors.SensorWrapperManager;
import org.greengin.sciencetoolkit.logic.streams.DataPipe;
import org.greengin.sciencetoolkit.logic.streams.filters.FixedRateDataFilter;
import org.greengin.sciencetoolkit.model.Model;
import org.greengin.sciencetoolkit.model.ModelDefaults;
import org.greengin.sciencetoolkit.model.ModelOperations;
import org.greengin.sciencetoolkit.model.ProfileManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

public class DataLogger implements DataLoggerDataListener {
	private static final String DATA_LOGGING_NEW_DATA = "DATA_LOGGING_NEW_DATA";
	private static final String DATA_LOGGING_NEW_STATUS = "DATA_LOGGING_NEW_STATUS";

	private static DataLogger instance;

	public static void init(Context applicationContext) {
		instance = new DataLogger(applicationContext);
	}

	public static DataLogger i() {
		return instance;
	}

	ReentrantLock runningLock;
	ReentrantLock listenersLock;

	Context applicationContext;

	String profileId;
	Model profile;
	int series;
	boolean running;
	Vector<DataPipe> pipes;
	Vector<DataLoggerDataListener> dataListeners;
	BroadcastReceiver dataReceiver;
	Vector<DataLoggerStatusListener> statusListeners;
	BroadcastReceiver statusReceiver;
	
	DataLoggerFileManager fileManager;
	DataLoggerSerializer serializer;

	public DataLogger(Context applicationContext) {
		this.applicationContext = applicationContext;

		runningLock = new ReentrantLock();
		listenersLock = new ReentrantLock();
		pipes = new Vector<DataPipe>();
		fileManager = new DataLoggerFileManager(applicationContext);
		series = 0;
		serializer = new DataLoggerSerializer();

		dataListeners = new Vector<DataLoggerDataListener>();
		statusListeners = new Vector<DataLoggerStatusListener>();

		dataReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String msg = intent.getExtras().getString("msg");
				for (DataLoggerDataListener listener : dataListeners) {
					listener.dataLoggerDataModified(msg);
				}
			}
		};
		statusReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				for (DataLoggerStatusListener listener : statusListeners) {
					listener.dataLoggerStatusModified();
				}
			}
		};
	}
	
	

	public void registerDataListener(DataLoggerDataListener listener) {
		listenersLock.lock();
		if (!dataListeners.contains(listener)) {
			dataListeners.add(listener);
			if (dataListeners.size() == 1) {
				LocalBroadcastManager.getInstance(applicationContext).registerReceiver(dataReceiver, new IntentFilter(DataLogger.DATA_LOGGING_NEW_DATA));
			}
		}

		listenersLock.unlock();
	}

	public void unregisterDataListener(DataLoggerDataListener listener) {
		listenersLock.lock();

		if (dataListeners.remove(listener) && dataListeners.size() == 0) {
			LocalBroadcastManager.getInstance(applicationContext).unregisterReceiver(dataReceiver);
		}
	}

	public void registerStatusListener(DataLoggerStatusListener listener) {
		listenersLock.lock();
		if (!statusListeners.contains(listener)) {
			statusListeners.add(listener);
			if (dataListeners.size() == 1) {
				LocalBroadcastManager.getInstance(applicationContext).registerReceiver(statusReceiver, new IntentFilter(DataLogger.DATA_LOGGING_NEW_STATUS));
			}
		}

		listenersLock.unlock();
	}

	public void unregisterStatusListener(DataLoggerStatusListener listener) {
		listenersLock.lock();
		if (statusListeners.remove(listener) && statusListeners.size() == 0) {
			LocalBroadcastManager.getInstance(applicationContext).unregisterReceiver(statusReceiver);
		}
	}

	public boolean isRunning() {
		return running;
	}

	public void startNewSeries() {
		runningLock.lock();
		if (!running) {
			profile = ProfileManager.i().getActiveProfile();
			profileId = profile.getString("id");
			pipes.clear();
			
			Vector<Model> sensors = profile.getModel("sensors", true).getModels();
			if (sensors.size() > 0) {
				running = true;
				
				series = fileManager.startNewSeries(profileId);
				File file = fileManager.getCurrentSeriesFile(profileId);
				serializer.open(file);
				
				for (Model profileSensor : sensors) {
					String sensorId = profileSensor.getString("id");
					SensorWrapper sensor = SensorWrapperManager.getInstance().getSensor(sensorId);
					int period = ModelOperations.rate2period(profileSensor, "sample_rate", ModelDefaults.DATA_LOGGING_RATE, null, ModelDefaults.DATA_LOGGING_RATE_MAX);

					if (sensor != null) {
						DataPipe pipe = new DataPipe(sensor);
						pipe.addFilter(new FixedRateDataFilter(period));
						pipe.setEnd(new DataLoggingInput(profileId, sensorId, serializer));
						pipes.add(pipe);
					}
				}

				for (DataPipe pipe : pipes) {
					pipe.attach();
				}

				statusModified();
			}
		}
		runningLock.unlock();
	}

	public void stopSeries() {
		runningLock.lock();

		if (running) {
			for (DataPipe pipe : pipes) {
				pipe.detach();
			}
			pipes.clear();
			running = false;
			
			serializer.close();

			statusModified();
		}

		runningLock.unlock();
	}

	public int getCurrentSeries() {
		return series;
	}
	
	public int getSeriesCount(String profileId) {
		return this.fileManager.seriesCount(profileId);
	}

	public HashMap<String, Integer> getCurrentSeriesSampleCount() {
		return this.serializer.getCount();
	}

	public void deleteAllData() {
		//this.helper.emptyData(null);
	}

	public void deleteData(String profileId) {
		//this.helper.emptyData(profileId);
	}
	
	

	private void statusModified() {
		if (statusListeners.size() > 0) {
			Intent intent = new Intent(DataLogger.DATA_LOGGING_NEW_STATUS);
			LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent);
		}
	}

	@Override
	public void dataLoggerDataModified(String msg) {
		if (dataListeners.size() > 0) {
			Intent intent = new Intent(DataLogger.DATA_LOGGING_NEW_DATA);
			intent.putExtra("msg", msg);
			LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent);
		}
	}

	
}