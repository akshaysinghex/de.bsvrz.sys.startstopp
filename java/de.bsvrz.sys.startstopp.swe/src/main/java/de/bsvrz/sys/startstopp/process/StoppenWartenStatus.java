/*
 * Segment 10 System (Sys), SWE 10.1 StartStopp
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

package de.bsvrz.sys.startstopp.process;

import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.CompletableFuture;

import de.bsvrz.sys.startstopp.api.StartStoppException;
import de.bsvrz.sys.startstopp.api.jsonschema.Applikation;
import de.bsvrz.sys.startstopp.api.jsonschema.StartStoppStatus;
import de.bsvrz.sys.startstopp.api.jsonschema.StartStoppStatus.Status;
import de.bsvrz.sys.startstopp.api.util.Util;
import de.bsvrz.sys.startstopp.api.jsonschema.StoppBedingung;
import de.bsvrz.sys.startstopp.api.jsonschema.StoppFehlerVerhalten;
import de.bsvrz.sys.startstopp.process.OnlineApplikation.TaskType;

public class StoppenWartenStatus extends OnlineApplikationStatus {

	StoppenWartenStatus(OnlineApplikation applikation) {
		super(Applikation.Status.STOPPENWARTEN, applikation);
	}

	@Override
	public boolean wechsleStatus(TaskType task, StartStoppStatus.Status startStoppStatus) {

		if (startStoppStatus == Status.STOPPING_CANCELED) {
			return restoreRunningState();
		}

		if (task == TaskType.STOPPFEHLER) {
			
			if( startStoppStatus == Status.SHUTDOWN) {
				applikation.stoppeApplikation();
				return applikation.updateStatus(Applikation.Status.GESTOPPT, "Stoppfehler ignoriert");
			}

			if( applikation.getProzessManager().isRekonfigurationAktiv()) {
				applikation.stoppeApplikation();
				return applikation.updateStatus(Applikation.Status.GESTOPPT, "Rekonfiguration: Stoppfehler ignoriert");
			}
			
			StoppFehlerVerhalten fehlerVerhalten = applikation.getApplikation().getInkarnation()
					.getStoppFehlerVerhalten();
			switch (fehlerVerhalten.getOption()) {
			case ABBRUCH:
				applikation.stoppeApplikation();
				return false;
			case IGNORIEREN:
				return applikation.updateStatus(Applikation.Status.GESTOPPT, "Stoppfehler ignoriert");
			case STOPP:
				CompletableFuture.runAsync(
						() -> applikation.getStartStopp().setStatus(StartStoppStatus.Status.STOPPING_CANCELED));
				return false;
			default:
				break;
			}
		} else {
			if (applikation.getOnlineApplikationTimer().isStoppFehlerTaskAktiv()) {
				return false;
			}
		}

		String message = applikation.kernSystemKannGestopptWerden();
		if (message != null) {
			applikation.getOnlineApplikationTimer().clear();
			return applikation.updateStatus(Applikation.Status.STOPPENWARTEN, message);
		}

		StoppBedingung stoppBedingung = applikation.getStoppBedingung();
		if (stoppBedingung != null) {
			StoppBedingungStatus pruefStatus = applikation.getStoppBedingungStatus();
			if (!pruefStatus.isErfuellt()) {
				applikation.getOnlineApplikationTimer().clear();
				return applikation.updateStatus(Applikation.Status.STOPPENWARTEN, pruefStatus.getMessage());
			}
			if (task != TaskType.WARTETIMER) {
				if (applikation.getOnlineApplikationTimer().isWarteTaskAktiv()) {
					return applikation.updateStatus(Applikation.Status.STOPPENWARTEN,
							applikation.getApplikation().getStartMeldung());
				}
				long warteZeitInMsec;
				try {
					warteZeitInMsec = Util.convertToWarteZeitInMsec(stoppBedingung.getWartezeit());
				} catch (StartStoppException e) {
					throw new IllegalStateException(
							"Sollte hier nicht passieren, weil nur geprüfte Skripte ausgeführt werden!", e);
				}
				if (warteZeitInMsec > 0) {
					applikation.getOnlineApplikationTimer().initWarteTask(warteZeitInMsec);
					return applikation.updateStatus(Applikation.Status.STOPPENWARTEN, "Wartezeit bis " + DateFormat
							.getDateTimeInstance().format(new Date(System.currentTimeMillis() + warteZeitInMsec)));
				}
			} else {
				applikation.getOnlineApplikationTimer().clear();
			}
		}

		if (applikation.getOnlineApplikationTimer().isWarteTaskAktiv()) {
			return false;
		}

		if (applikation.getOnlineApplikationTimer().isStoppFehlerTaskAktiv()) {
			return false;
		}

		if (!applikation.isManuellGestartetOderGestoppt()) {
			applikation.getOnlineApplikationTimer().initStoppFehlerTask(applikation);
		}

		applikation.updateStatus(Applikation.Status.STOPPENWARTEN, "Warte auf Prozessende");
		applikation.stoppeApplikation();
		return true;
	}

	private boolean restoreRunningState() {
		if (applikation.isStopInProgress()) {
			return false;
		}

		applikation.getOnlineApplikationTimer().clear();
		if (applikation.getProzessManager().getDavConnector().getAppStatus(applikation) || applikation.getApplikation().getInkarnation().getInitialize()) {
			return applikation.updateStatus(Applikation.Status.INITIALISIERT, "Stoppen abgebrochen");
		}
		return applikation.updateStatus(Applikation.Status.GESTARTET, "Stoppen abgebrochen");
	}

}
