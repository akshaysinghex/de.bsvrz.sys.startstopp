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

package de.bsvrz.sys.startstopp.console.ui.editor;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.Button.Listener;
import com.googlecode.lanterna.gui2.CheckBox;
import com.googlecode.lanterna.gui2.ComboBox;
import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.Interactable;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.TextBox;

import de.bsvrz.sys.startstopp.api.jsonschema.Inkarnation;
import de.bsvrz.sys.startstopp.api.jsonschema.Inkarnation.InkarnationsTyp;
import de.bsvrz.sys.startstopp.api.util.Util;
import de.bsvrz.sys.startstopp.api.jsonschema.StartBedingung;
import de.bsvrz.sys.startstopp.api.jsonschema.StartStoppSkript;
import de.bsvrz.sys.startstopp.api.jsonschema.StoppBedingung;
import de.bsvrz.sys.startstopp.console.ui.StartStoppButton;

class InkarnationEditor extends StartStoppElementEditor<Inkarnation> {

	private Inkarnation inkarnation;
	private StartStoppSkript skript;

	InkarnationEditor(StartStoppSkript skript, Inkarnation inkarnation) {
		super(skript, "Inkarnation: " + inkarnation.getInkarnationsName());
		this.skript = skript;
		this.inkarnation = (Inkarnation) Util.cloneObject(inkarnation);
	}

	@Override
	protected void initComponents(Panel mainPanel) {
		mainPanel.setLayoutManager(new GridLayout(2).setLeftMarginSize(1).setRightMarginSize(1).setTopMarginSize(1));

		initInkarnationsName(mainPanel);
		initApplikation(mainPanel);
		initAufrufParameter(mainPanel);
		initInkarnationsTyp(mainPanel);
		initStartArt(mainPanel);
		initInitialisieren(mainPanel);
		initUseInkarnationsName(mainPanel);
		initStartBedingung(mainPanel);
		initStartFehlerverhalten(mainPanel);
		initStoppBedingung(mainPanel);
		initStoppFehlerverhalten(mainPanel);
	}

	private void initInkarnationsName(Panel mainPanel) {
		mainPanel.addComponent(new Label("Inkarnationsname:"));
		TextBox nameField = new TextBox() {
			@Override
			protected void afterLeaveFocus(FocusChangeDirection direction, Interactable nextInFocus) {
				inkarnation.setInkarnationsName(getText());
				super.afterLeaveFocus(direction, nextInFocus);
			}
		};
		nameField.setText(inkarnation.getInkarnationsName());
		mainPanel.addComponent(nameField, GridLayout.createHorizontallyFilledLayoutData(1));
	}

	private void initApplikation(Panel mainPanel) {
		mainPanel.addComponent(new Label("Applikation:"));
		TextBox applikationField = new TextBox("") {
			@Override
			protected void afterLeaveFocus(FocusChangeDirection direction, Interactable nextInFocus) {
				inkarnation.setApplikation(getText());
				super.afterLeaveFocus(direction, nextInFocus);
			}
		};
		applikationField.setText(inkarnation.getApplikation());
		mainPanel.addComponent(applikationField, GridLayout.createHorizontallyFilledLayoutData(1));
	}

	private void initAufrufParameter(Panel mainPanel) {
		Label parameterLabel = new Label("");
		parameterLabel.setPreferredSize(new TerminalSize(20, 1));
		Button parameterButton = new StartStoppButton("&Parameter:");
		parameterButton.addListener(new Listener() {
			@Override
			public void onTriggered(Button button) {
				AufrufParameterEditor editor = new AufrufParameterEditor(getSkript(), inkarnation);
				if (editor.showDialog(getTextGUI())) {
					inkarnation.getAufrufParameter().clear();
					inkarnation.getAufrufParameter().addAll(editor.getElement());
					String parameterStr = String.join(",", inkarnation.getAufrufParameter());
					parameterLabel.setText(Util.shorterString(parameterStr, 20));
				}
			}
		});
		mainPanel.addComponent(parameterButton, GridLayout.createHorizontallyFilledLayoutData(1));
		String parameterStr = String.join(",", inkarnation.getAufrufParameter());
		parameterLabel.setText(Util.shorterString(parameterStr, 20));
		mainPanel.addComponent(parameterLabel);
	}

	private void initInkarnationsTyp(Panel mainPanel) {
		mainPanel.addComponent(new Label("Typ:"));
		ComboBox<InkarnationsTyp> typSelektor = new ComboBox<>(InkarnationsTyp.values());
		for (int idx = 0; idx < typSelektor.getItemCount(); idx++) {
			if (typSelektor.getItem(idx) == inkarnation.getInkarnationsTyp()) {
				typSelektor.setSelectedIndex(idx);
			}
		}
		typSelektor.addListener(new ComboBox.Listener() {
			@Override
			public void onSelectionChanged(int selectedIndex, int previousSelection) {
				inkarnation.setInkarnationsTyp(typSelektor.getItem(selectedIndex));
			}
		});
		mainPanel.addComponent(typSelektor, GridLayout.createHorizontallyFilledLayoutData(1));
	}

	private void initStartArt(Panel mainPanel) {
		Label startArtLabel = new Label("");
		Button startArtButton = new StartStoppButton("&Startart:");
		startArtButton.addListener(new Listener() {
			@Override
			public void onTriggered(Button button) {
				StartArtEditor editor = new StartArtEditor(getSkript(), inkarnation.getStartArt());
				if (editor.showDialog(getTextGUI())) {
					inkarnation.setStartArt(editor.getElement());
					startArtLabel.setText(inkarnation.getStartArt().getOption().toString());
				}
			}
		});
		
		startArtLabel.setText(inkarnation.getStartArt().getOption().toString());
		mainPanel.addComponent(startArtButton, GridLayout.createHorizontallyFilledLayoutData(1));
		mainPanel.addComponent(startArtLabel);
	}

	private void initInitialisieren(Panel mainPanel) {
		CheckBox initCheckbox = new CheckBox("Initialisieren");
		initCheckbox.setChecked(inkarnation.getInitialize());
		initCheckbox.addListener(new CheckBox.Listener() {
			@Override
			public void onStatusChanged(boolean checked) {
				inkarnation.setInitialize(checked);
			}
		});
		mainPanel.addComponent(initCheckbox, GridLayout.createHorizontallyFilledLayoutData(1));
	}

	private void initUseInkarnationsName(Panel mainPanel) {
		CheckBox setInkarnationsNameCheckbox = new CheckBox("Setze Inkarnationsname");
		setInkarnationsNameCheckbox.setChecked(inkarnation.getMitInkarnationsName());
		setInkarnationsNameCheckbox.addListener(new CheckBox.Listener() {
			@Override
			public void onStatusChanged(boolean checked) {
				inkarnation.setMitInkarnationsName(checked);
			}
		});
		mainPanel.addComponent(setInkarnationsNameCheckbox, GridLayout.createHorizontallyFilledLayoutData(1));
	}

	private void initStartBedingung(Panel mainPanel) {
		Label bedingungLabel = new Label("");
		Button startBedingungButton = new StartStoppButton("S&tartbedingung:");
		startBedingungButton.addListener(new Listener() {
			@Override
			public void onTriggered(Button button) {
				StartBedingungEditor editor = new StartBedingungEditor(skript, inkarnation);
				if (editor.showDialog(getTextGUI())) {
					inkarnation.setStartBedingung(editor.getElement());
					StartBedingung startBedingung = inkarnation.getStartBedingung();
					if ((startBedingung == null) || startBedingung.getVorgaenger().isEmpty()) {
						bedingungLabel.setText("Keine");
					} else {
						bedingungLabel.setText(startBedingung.getVorgaenger().get(0));
					}
				}
			}
		});
		mainPanel.addComponent(startBedingungButton, GridLayout.createHorizontallyFilledLayoutData(1));
		StartBedingung startBedingung = inkarnation.getStartBedingung();
		if ((startBedingung == null) || startBedingung.getVorgaenger().isEmpty()) {
			bedingungLabel.setText("Keine");
		} else {
			bedingungLabel.setText(startBedingung.getVorgaenger().get(0));
		}
		mainPanel.addComponent(bedingungLabel);
	}

	private void initStartFehlerverhalten(Panel mainPanel) {
		Label fehlerLabel = new Label("");
		Button startFehlerVerhaltenButton = new StartStoppButton("Sta&rtfehlerverhalten:");
		startFehlerVerhaltenButton.addListener(new Listener() {
			@Override
			public void onTriggered(Button button) {
				StartFehlerVerhaltenEditor editor = new StartFehlerVerhaltenEditor(getSkript(), inkarnation);
				if (editor.showDialog(getTextGUI())) {
					inkarnation.setStartFehlerVerhalten(editor.getElement());
					fehlerLabel.setText(inkarnation.getStartFehlerVerhalten().getOption().toString());
				}
			}
		});
		fehlerLabel.setText(inkarnation.getStartFehlerVerhalten().getOption().toString());
		mainPanel.addComponent(startFehlerVerhaltenButton, GridLayout.createHorizontallyFilledLayoutData(1));
		mainPanel.addComponent(fehlerLabel);
	}

	private void initStoppBedingung(Panel mainPanel) {
		Label bedingungLabel = new Label("");
		Button stoppBedingungButton = new StartStoppButton("Stopp&bedingung:");
		stoppBedingungButton.addListener(new Listener() {
			@Override
			public void onTriggered(Button button) {
				StoppBedingungEditor editor = new StoppBedingungEditor(skript, inkarnation);
				if (editor.showDialog(getTextGUI())) {
					inkarnation.setStoppBedingung(editor.getElement());
					StoppBedingung stoppBedingung = inkarnation.getStoppBedingung();
					if ((stoppBedingung == null) || stoppBedingung.getNachfolger().isEmpty()) {
						bedingungLabel.setText("Keine");
					} else {
						bedingungLabel.setText(stoppBedingung.getNachfolger().get(0));
					}
				}
			}
		});
		mainPanel.addComponent(stoppBedingungButton, GridLayout.createHorizontallyFilledLayoutData(1));
		StoppBedingung stoppBedingung = inkarnation.getStoppBedingung();
		if ((stoppBedingung == null) || stoppBedingung.getNachfolger().isEmpty()) {
			bedingungLabel.setText("Keine");
		} else {
			bedingungLabel.setText(stoppBedingung.getNachfolger().get(0));
		}
		mainPanel.addComponent(bedingungLabel);
	}

	private void initStoppFehlerverhalten(Panel mainPanel) {
		Label fehlerLabel = new Label("");
		Button stoppFehlerVerhaltenButton = new StartStoppButton("Stoppfehler&verhalten:");
		stoppFehlerVerhaltenButton.addListener(new Listener() {
			@Override
			public void onTriggered(Button button) {
				StoppFehlerVerhaltenEditor editor = new StoppFehlerVerhaltenEditor(getSkript(), inkarnation);
				if (editor.showDialog(getTextGUI())) {
					inkarnation.setStoppFehlerVerhalten(editor.getElement());
					fehlerLabel.setText(inkarnation.getStoppFehlerVerhalten().getOption().toString());
				}
			}
		});
		fehlerLabel.setText(inkarnation.getStoppFehlerVerhalten().getOption().toString());
		mainPanel.addComponent(stoppFehlerVerhaltenButton, GridLayout.createHorizontallyFilledLayoutData(1));
		mainPanel.addComponent(fehlerLabel);
	}

	@Override
	public Inkarnation getElement() {
		return inkarnation;
	}
}
