/* AboutActivity.java
 * 
 * PROGRAMMER:    Jeffrey T. Darlington
 * DATE:          February 24, 2011
 * PROJECT:       Perfect Paper Passwords for Android
 * ANDROID V.:	  1.1
 * 
 * [Description}
 * 
 * This program is Copyright 2011, Jeffrey T. Darlington.
 * E-mail:  android_apps@gpf-comics.com
 * Web:     http://www.gpf-comics.com/
 * 
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this
 * program.  If not, see http://www.gnu.org/licenses/.
*/
package com.gpfcomics.android.ppp;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

/**
 * Display a brief bit of information about the application as a whole
 * @author Jeffrey T. Darlington
 * @version 1.0
 * @since 1.0
 */
public class AboutActivity extends Activity {
	
	/** The TextView that contains the version number string */
	private TextView lblVersion = null;

    public void onCreate(Bundle savedInstanceState) {
		// Do the normal start-up stuff, like defining layout:
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about_layout);
        // Get a copy of the version label:
        lblVersion = (TextView)findViewById(R.id.about_version_label);
        // This is a bit funky, but get the version string from the application and
        // insert it into the version display string in the resources, then apply
        // this to the version label.  If that fails, the version label will end
        // up being blank.
        if (PPPApplication.getVersionName() != null) {
        	String versionString = getResources().getString(R.string.password_version_label);
	        versionString = versionString.replace(getResources().getString(R.string.meta_replace_token),
	        		PPPApplication.getVersionName());
	        lblVersion.setText(versionString);
        } else {
        	lblVersion.setText("");
        }
    }

}
