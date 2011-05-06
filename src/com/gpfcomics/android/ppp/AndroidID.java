/* AndroidID.java
 * 
 * PROGRAMMER:    Jeffrey T. Darlington
 * DATE:          December 9, 2009
 * PROJECT:       Perfect Paper Passwords for Android
 * ANDROID V.:	  1.1
 * REQUIRES:      
 * REQUIRED BY:   
 * 
 * This class provides a basic wrapper around the ANDROID_ID property, the
 * unique identifier assigned by the Android Market to a given device.  We
 * use this identifier to generate our database encryption key to provide
 * a unique encryption per device.
 * 
 * Unfortunately, we run afoul of progress in this case.  In Android 1.5,
 * Google (wisely) moved this value from the read-and-write location in 
 * android.provider.Settings.System.ANDROID_ID to the more secure read-only
 * android.provider.Settings.Secure.ANDROID_ID.  This means that
 * Settings.System.ANDROID_ID has been deprecated and will eventually break.
 * 
 * To remain both forward and backward compatible, this class creates a
 * wrapper around these properties to make sure the right one is returned for
 * the platform we're running on.  This is based heavily on a blog post from
 * the official Android developer's blog, found here:
 * http://android-developers.blogspot.com/2010/07/how-to-have-your-cupcake-and-eat-it-too.html
 * 
 * This program is Copyright 2011, Jeffrey T. Darlington.
 * E-mail:  android_apps@gpf-comics.com
 * Web:     http://www.gpf-comics.com/
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See theGNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
 * Boston, MA  02110-1301, USA.
*/
package com.gpfcomics.android.ppp;

import android.app.Application;
import android.os.Build;
import android.provider.Settings;

public abstract class AndroidID {

	
	/** A reference to our calling application.  We need this to get the
	 *  ContentResolver needed to get the system properties. */
	protected static Application theApp;
	
	/**
	 * Return the ANDROID_ID value for the current platform
	 * @return The ANDROID_ID value for the current platform
	 */
	public abstract String getAndroidID(); 

	/**
	 * Get the appropriate instance of this class for this platform
	 * @param app A reference to the calling application
	 * @return A subclass of AndroidID for the current platform
	 */
	public static AndroidID newInstance(Application app)
	{
		theApp = app;
		final int sdkVersion = Integer.parseInt(Build.VERSION.SDK);
		if (sdkVersion < Build.VERSION_CODES.CUPCAKE)
			return new SystemAndroidID();
		else return new SecureAndroidID();
	}

	/**
	 * The AndroidID subclass for pre-Cupcake (< Android 1.5) APIs 
	 * @author Jeffrey T. Darlington
	 * @version 1.1
	 * @since 1.1
	 */
	private static class SystemAndroidID extends AndroidID {

		public SystemAndroidID() {}
		
		@Override
		public String getAndroidID() {
			return Settings.System.getString(theApp.getContentResolver(),
        			android.provider.Settings.System.ANDROID_ID);
		}
		
	}

	/**
	 * The AndroidID subclass for post-Cupcake (>= Android 1.5) APIs 
	 * @author Jeffrey T. Darlington
	 * @version 1.1
	 * @since 1.1
	 */
	private static class SecureAndroidID extends AndroidID {

		public SecureAndroidID() {}
		
		@Override
		public String getAndroidID() {
			return Settings.Secure.getString(theApp.getContentResolver(),
					android.provider.Settings.Secure.ANDROID_ID); 
		}
		
	}
}
