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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.bsvrz.sys.funclib.debug.Debug;
import de.bsvrz.sys.startstopp.api.client.StartStoppClient;
import de.bsvrz.sys.startstopp.api.jsonschema.Applikation;
import de.bsvrz.sys.startstopp.api.jsonschema.Applikation.Status;
import de.bsvrz.sys.startstopp.api.jsonschema.KernSystem;
import de.bsvrz.sys.startstopp.api.jsonschema.Rechner;
import de.bsvrz.sys.startstopp.api.jsonschema.StartBedingung;
import de.bsvrz.sys.startstopp.api.jsonschema.StoppBedingung;
import de.bsvrz.sys.startstopp.config.SkriptManagerListener;
import de.bsvrz.sys.startstopp.config.StartStoppException;
import de.bsvrz.sys.startstopp.config.StartStoppKonfiguration;
import de.bsvrz.sys.startstopp.startstopp.StartStopp;

public class ProcessManager extends Thread implements SkriptManagerListener, ManagedApplikationListener {

	private static final Debug LOGGER = Debug.getLogger();
	private boolean stopped;
	private Object lock = new Object();
	private ManagerStatus managerStatus = new ManagerStatus();

	private Map<String, StartStoppApplikation> applikationen = new LinkedHashMap<>();
	private final StartStopp startStopp;
	private SkriptStopper stopper;
	private StartStoppKonfiguration currentSkript;
	private ArrayList<String> kernSystem;
	private DavConnector davConnector;

	public ProcessManager() {
		this(StartStopp.getInstance());
	}

	public ProcessManager(StartStopp startStopp) {
		super("ProcessManager");
		this.startStopp = startStopp;
	}

	@Override
	public void run() {

		while (!stopped) {

			if (currentSkript == null) {
				try {
					currentSkript = startStopp.getSkriptManager().getCurrentSkript();
					davConnector = new DavConnector(currentSkript.getResolvedZugangDav());
					davConnector.start();
					kernSystem = new ArrayList<>();
					for (KernSystem ks : currentSkript.getSkript().getGlobal().getKernsysteme()) {
						kernSystem.add(ks.getInkarnationsName());
					}
					for (StartStoppInkarnation inkarnation : currentSkript.getInkarnationen()) {
						StartStoppApplikation applikation = new StartStoppApplikation(this, inkarnation);
						applikationen.put(applikation.getInkarnationsName(), applikation);
						applikation.addManagedApplikationListener(this);
					}
				} catch (StartStoppException e) {
					currentSkript = null;
				}
			}
			for (StartStoppApplikation applikation : applikationen.values()) {
				applikation.updateStatus();
			}

			try {
				synchronized (lock) {
					lock.wait(30000);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		this.startStopp.getSkriptManager().removeSkriptManagerListener(this);
	}

	public List<Applikation> getApplikationen() {

		List<Applikation> result = new ArrayList<>();
		for (StartStoppApplikation applikation : applikationen.values()) {
			result.add(applikation.getApplikation());
		}

		return result;
	}

	public Collection<StartStoppApplikation> getManagedApplikationen() {
		return Collections.unmodifiableCollection(applikationen.values());
	}

	public Applikation getApplikation(String inkarnationsName) throws StartStoppException {
		StartStoppApplikation managedApplikation = applikationen.get(inkarnationsName);
		if (managedApplikation != null) {
			return managedApplikation.getApplikation();
		}

		throw new StartStoppException(
				"Eine Applikation mit dem Inkarnationsname \"" + inkarnationsName + "\" konnte nicht gefunden werden");
	}

	public Applikation starteApplikation(String inkarnationsName) throws StartStoppException {
		StartStoppApplikation applikation = applikationen.get(inkarnationsName);
		if (applikation == null) {
			throw new StartStoppException("Eine Applikation mit dem Inkarnationsname \"" + inkarnationsName
					+ "\" konnte nicht gefunden werden");
		}

		applikation.startSystemProcess();

		return applikation.getApplikation();
	}

	/**
	 * Die Funktion startet die mit dem Inkarnationsname beschriebene Applikation
	 * neu.
	 * 
	 * Beim Neustart einer Applikation werden die Start-Stopp-Regeln nicht
	 * angewendet!
	 * 
	 * @param inkarnationsName
	 *            der Inkarnationsname der Applikation
	 * @return die Informationen zur Applikation
	 * @throws StartStoppException
	 *             der Neustart ist fehlgeschlagen
	 */
	public Applikation restarteApplikation(String inkarnationsName) throws StartStoppException {
		StartStoppApplikation applikation = applikationen.get(inkarnationsName);
		if (applikation == null) {
			throw new StartStoppException("Eine Applikation mit dem Inkarnationsname \"" + inkarnationsName
					+ "\" konnte nicht gefunden werden");
		}

		applikation.stoppSystemProcess();
		applikation.startSystemProcess();

		return applikation.getApplikation();
	}

	public Applikation stoppeApplikationOhnePruefung(String inkarnationsName) throws StartStoppException {
		StartStoppApplikation applikation = applikationen.get(inkarnationsName);
		if (applikation != null) {
			applikation.stoppSystemProcess();
			return applikation.getApplikation();
		}

		throw new StartStoppException(
				"Eine Applikation mit dem Inkarnationsname \"" + inkarnationsName + "\" konnte nicht gefunden werden");
	}

	public void stopp() {
		stopped = true;
		synchronized (lock) {
			lock.notify();
		}
	}

	public boolean isSkriptRunning() {
		return managerStatus.getState() == ManagerStatus.State.RUNNING;
	}

	public boolean isSkriptStopped() {
		return managerStatus.getState() == ManagerStatus.State.STOPPED;
	}

	public Thread stoppeSkript(boolean restart) {

		if (managerStatus.getState() == ManagerStatus.State.STOPPING) {
			return null;
		}
		managerStatus.setState(ManagerStatus.State.STOPPING);

		stopper = new SkriptStopper(this);
		stopper.start();
		return stopper;
	}

	@Override
	public void skriptAktualisiert(StartStoppKonfiguration oldValue, StartStoppKonfiguration newValue) {
		if (currentSkript == null) {
			synchronized (lock) {
				lock.notifyAll();
			}
		}

		// TODO Änderungen berechnen und Applikationen aktualisieren
	}

	public Applikation waitForStartBedingung(StartStoppApplikation managedApplikation) {

		StartBedingung bedingung = managedApplikation.getStartBedingung();
		if (bedingung == null) {
			return null;
		}

		String rechnerName = bedingung.getRechner();
		if ((rechnerName != null) && !rechnerName.trim().isEmpty()) {
			return waitForRemoteStartBedingung(managedApplikation, rechnerName, bedingung);
		}

		StartStoppApplikation applikation = applikationen.get(bedingung.getVorgaenger());
		if (applikation == null) {
			LOGGER.warning("In der Startbedingung referenzierte Inkarnation \"" + bedingung.getVorgaenger()
					+ "\" existiert nicht!");
			return null;
		}

		if (canBeStartet(applikation.getApplikation(), bedingung)) {
			return null;
		}

		LOGGER.info(managedApplikation.getInkarnationsName() + " muss auf " + applikation.getInkarnationsName()
				+ " warten!");
		return applikation.getApplikation();
	}

	private boolean canBeStartet(Applikation applikation, StartBedingung bedingung) {
		switch (bedingung.getWarteart()) {
		case BEGINN:
			if ((applikation.getStatus() != Status.GESTARTET) && (applikation.getStatus() != Status.INITIALISIERT)) {
				return false;
			}
			break;
		case ENDE:
			if (applikation.getStatus() != Status.INITIALISIERT) {
				return false;
			}
			break;
		}

		return true;
	}

	public Applikation waitForStoppBedingung(StartStoppApplikation managedApplikation) {

		StoppBedingung bedingung = managedApplikation.getStoppBedingung();
		if (bedingung == null) {
			return null;
		}

		String rechnerName = bedingung.getRechner();
		if ((rechnerName != null) && !rechnerName.trim().isEmpty()) {
			return waitForRemoteStoppBedingung(managedApplikation, rechnerName, bedingung);
		}

		StartStoppApplikation applikation = applikationen.get(bedingung.getNachfolger());
		if (applikation == null) {
			LOGGER.warning("In der Stoppbedingung referenzierte Inkarnation \"" + bedingung.getNachfolger()
					+ "\" existiert nicht!");
			return null;
		}

		if (canBeStopped(applikation.getApplikation())) {
			return null;
		}

		return applikation.getApplikation();

	}

	private boolean canBeStopped(Applikation applikation) {
		switch (applikation.getStatus()) {
		case GESTOPPT:
		case INSTALLIERT:
		case STARTENWARTEN:
			break;
		case GESTARTET:
		case INITIALISIERT:
		case STOPPENWARTEN:
			return false;
		default:
			break;
		}

		return true;
	}

	private Applikation waitForRemoteStoppBedingung(StartStoppApplikation managedApplikation, String rechnerName, StoppBedingung bedingung) {
		try {
			Rechner rechner = currentSkript.getResolvedRechner(rechnerName);
			StartStoppClient client = new StartStoppClient(rechner.getTcpAdresse(),
					Integer.parseInt(rechner.getPort()));
			Applikation applikation = client.getApplikation(bedingung.getNachfolger());

			if (canBeStopped(applikation)) {
				return null;
			}
			
			LOGGER.info(managedApplikation.getInkarnationsName() + " muss auf " + applikation.getInkarnationsName()
			+ " auf Rechner \"" + rechnerName + "\" warten!");
			return applikation;

		} catch (NumberFormatException | StartStoppException e) {
			LOGGER.warning(e.getLocalizedMessage());
		}

		return null;
	}

	private Applikation waitForRemoteStartBedingung(StartStoppApplikation managedApplikation, String rechnerName, StartBedingung bedingung) {
		try {
			Rechner rechner = currentSkript.getResolvedRechner(rechnerName);
			StartStoppClient client = new StartStoppClient(rechner.getTcpAdresse(),
					Integer.parseInt(rechner.getPort()));
			Applikation applikation = client.getApplikation(bedingung.getVorgaenger());

			if (canBeStartet(applikation, bedingung)) {
				return null;
			}

			LOGGER.info(managedApplikation.getInkarnationsName() + " muss auf " + applikation.getInkarnationsName()
					+ " auf Rechner \"" + rechnerName + "\" warten!");
			return applikation;
		} catch (NumberFormatException | StartStoppException e) {
			LOGGER.warning(e.getLocalizedMessage());
		}

		return null;
	}

	public Applikation waitForKernsystemStart(StartStoppApplikation managedApplikation) {
		for (String name : kernSystem) {
			if (name.equals(managedApplikation.getInkarnationsName())) {
				return null;
			}
			StartStoppApplikation app = applikationen.get(name);
			switch (app.getStatus()) {
			case GESTARTET:
			case INITIALISIERT:
				break;
			default:
				return managedApplikation.getApplikation();
			}
		}

		return null;
	}

	public Applikation waitForKernsystemStopp(StartStoppApplikation startStoppApplikation) {
		boolean foundKernsoftwareApplikation = false;
		for (String name : kernSystem) {
			if (!foundKernsoftwareApplikation) {
				if (name.equals(startStoppApplikation.getInkarnationsName())) {
					foundKernsoftwareApplikation = true;
				}
			} else {
				StartStoppApplikation app = applikationen.get(name);
				switch (app.getStatus()) {
				case GESTARTET:
				case INITIALISIERT:
				case STOPPENWARTEN:
					return app.getApplikation();
				case GESTOPPT:
				case INSTALLIERT:
				case STARTENWARTEN:
					break;
				default:
					break;
				}
			}
		}

		return null;
	}

	public char[] isStopable(StartStoppApplikation managedApplikation) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void applicationStatusChanged(StartStoppApplikation managedApplikation, Status oldValue, Status newValue) {
		synchronized (lock) {
			lock.notifyAll();
		}
	}

	public String getDavConnectionMsg() {
		return davConnector.getConnectionMsg();
	}
}
