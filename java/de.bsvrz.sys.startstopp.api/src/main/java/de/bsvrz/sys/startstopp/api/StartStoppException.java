/*
 * Segment 10 System (Sys), SWE 10.1 StartStopp API
 * Copyright (C) 2007-2017 BitCtrl Systems GmbH
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * Contact Information:<br>
 * BitCtrl Systems GmbH<br>
 * Weißenfelser Straße 67<br>
 * 04229 Leipzig<br>
 * Phone: +49 341-490670<br>
 * mailto: info@bitctrl.de
 */

package de.bsvrz.sys.startstopp.api;

import java.util.ArrayList;
import java.util.List;

public class StartStoppException extends Exception {

	private final List<String> messages = new ArrayList<>();

	public StartStoppException(String string) {
		super(string);
	}

	public StartStoppException(Exception e) {
		super(e);
		if( e instanceof StartStoppException) {
			addMessages(((StartStoppException) e).getMessages());
		}
	}

	public StartStoppException(String string, Exception e) {
		super(string, e);
		if( e instanceof StartStoppException) {
			addMessages(((StartStoppException) e).getMessages());
		}
	}

	protected void addMessages(List<String> newMessages) {
		this.messages.addAll(newMessages);
	}
	
	public List<String> getMessages() {
		return messages;
	}
	
	public String getFullString() {
		StringBuilder builder = new StringBuilder(getLocalizedMessage());
		for( String message : messages) {
			builder.append('\n');
			builder.append(message);
		}
		return builder.toString();
	}
}
