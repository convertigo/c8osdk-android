/*
 * Copyright (c) 2001-2014 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 * $URL: svn://devus.twinsoft.fr/convertigo/C8oSDK/Android/trunk/C8oSDKAndroid/src/com/convertigo/clientsdk/listener/C8oExceptionListener.java $
 * $Author: julesg $
 * $Revision: 40144 $
 * $Date: 2015-07-09 16:43:47 +0200 (jeu., 09 juil. 2015) $* 
 * 
 */

package com.convertigo.clientsdk.listener;

import java.util.List;

import org.apache.http.NameValuePair;

/**
 * Listens thrown exceptions
 */
public interface C8oExceptionListener {
	/**
	 * Called on thrown exceptions.
	 * 
	 * @param exception - The thrown exception
	 */
	public void onException(List<NameValuePair> requestParameters, Exception exception);
}
