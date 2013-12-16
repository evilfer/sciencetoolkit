package org.greengin.sciencetoolkit;



import org.greengin.sciencetoolkit.logic.datalogging.DeprecatedDataLogger;
import org.greengin.sciencetoolkit.logic.sensors.SensorWrapperManager;
import org.greengin.sciencetoolkit.model.ProfileManager;
import org.greengin.sciencetoolkit.model.SettingsManager;

import android.app.Application;
import android.content.Context;

public class ScienceToolkitApplication extends Application {

    public void onCreate(){
        super.onCreate();
        Context context = this.getApplicationContext();
        
        SettingsManager.init(context);
        SensorWrapperManager.init(context);
        ProfileManager.init(context);
        DeprecatedDataLogger.init(context);
        
        VersionManager.check(context);
    }
    
}
