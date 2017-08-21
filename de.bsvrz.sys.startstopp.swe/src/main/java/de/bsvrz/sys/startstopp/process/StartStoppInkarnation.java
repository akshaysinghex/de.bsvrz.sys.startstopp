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

import de.bsvrz.sys.startstopp.api.jsonschema.Inkarnation;
import de.bsvrz.sys.startstopp.api.jsonschema.KernSystem;
import de.bsvrz.sys.startstopp.api.jsonschema.StartArt;
import de.bsvrz.sys.startstopp.config.StartStoppException;
import de.bsvrz.sys.startstopp.config.StartStoppKonfiguration;

public class StartStoppInkarnation extends Inkarnation {

	private KernSystem kernSystem = null;

	public KernSystem getKernSystem() {
		return kernSystem;
	}

	public StartStoppInkarnation(StartStoppKonfiguration skript, Inkarnation inkarnation) throws StartStoppException {

		for (KernSystem ks : skript.getSkript().getGlobal().getKernsysteme()) {
			if (ks.getInkarnationsName().equals(inkarnation.getInkarnationsName())) {
				this.kernSystem = ks;
			}
		}

		setApplikation(skript.makroResolvedString(inkarnation.getApplikation()));
		for (String aufrufParameter : inkarnation.getAufrufParameter()) {
			getAufrufParameter().add(skript.makroResolvedString(aufrufParameter));
		}
		setInkarnationsName(inkarnation.getInkarnationsName());
		setInkarnationsTyp(inkarnation.getInkarnationsTyp());
		StartArt startArt = inkarnation.getStartArt();
		if (startArt == null) {
			setStartArt(new StartArt().withOption(StartArt.Option.AUTOMATISCH));
		} else {
			setStartArt(startArt);
		}
		setInitialize(inkarnation.getInitialize());
		setMitInkarnationsName(inkarnation.getMitInkarnationsName());
		setStartBedingung(skript.getResolvedStartBedingung(inkarnation.getStartBedingung()));
		setStartFehlerVerhalten(skript.getResolvedStartFehlerVerhalten(inkarnation.getStartFehlerVerhalten()));
		setStoppBedingung(skript.getResolvedStoppBedingung(inkarnation.getStoppBedingung()));
		setStoppFehlerVerhalten(skript.getResolvedStoppFehlerVerhalten(inkarnation.getStoppFehlerVerhalten()));
	}

	public boolean isKernSystem() {
		return kernSystem != null;
	}

	public boolean isTransmitter() {
		for (String parameter : getAufrufParameter()) {
			if (parameter.contains("de.bsvrz.dav.dav.main.Transmitter")) {
				return true;
			}

			if (parameter.contains("de.bsvrz.dav.dav-runtime.jar")) {
				return true;
			}
		}

		return false;
	}

	public boolean isConfiguration() {
		for (String parameter : getAufrufParameter()) {
			if (parameter.contains("de.bsvrz.puk.config.main.ConfigurationApp")) {
				return true;
			}

			if (parameter.contains("de.bsvrz.puk.config-runtime.jar")) {
				return true;
			}
		}

		return false;
	}
}
