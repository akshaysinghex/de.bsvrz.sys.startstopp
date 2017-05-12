/**
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 * 
 * Contact Information:
 * beck et al. projects GmbH
 * Theresienhoehe 13
 * 80336 Munich
 * Phone: +49-89-5442530
 * mailto: info@bea.de
 */
package de.bsvrz.sys.startstopp.buv;

import java.util.Vector;

/**
 * Men� f�r den {@link de.bsvrz.sys.startstopp.buv.CmdInterpreter}
 * 
 * @author beck et al. projects GmbH
 * @author Thomas M�ller
 * @version $Revision: 1.7 $ / $Date: 2011/11/04 09:02:21 $ / ($Author: Drapp $)
 */
public class CmdMenu {

    private CmdMenu parentMenu;
    private Vector<CmdMenu> subMenues;
    private Vector<Command> commands;
    private int index;
    private String help;
    private String description;
    private String inkarnationsName;
    /**
     * Anzahl zus�tzlicher Kommandos.
     */
    private int numHiddenCommands = 0;
    
    /** Men� mit Beschreibung und Hilfetext
     * 
     * @param desc Beschreibung
     * @param help Hilfetext
     */
    public CmdMenu(String desc, String help) {
        subMenues = new Vector<CmdMenu>();
        commands = new Vector<Command>();
        this.description = (desc.equals("")) ? "Keine Beschreibung verfuegbar." : desc;
        this.help = (help.equals("")) ? "Keine Hilfe verfuegbar." : help;
        this.inkarnationsName = this.description;
    }

    /** Men� mit Beschreibung, Hilfetext und Ausgabetext
     * 
     * @param desc Beschreibung
     * @param help Hilfetext
     * @param inkarnationsName Name der Inkarnation
     */
    public CmdMenu(String desc, String help, String inkarnationsName) 
    {
        subMenues = new Vector<CmdMenu>();
        commands = new Vector<Command>();
        this.description = (desc.equals("")) ? "Keine Beschreibung verfuegbar." : desc;
        this.help = (help.equals("")) ? "Keine Hilfe verfuegbar." : help;
        this.inkarnationsName = (inkarnationsName.equals("")) ? this.description : inkarnationsName;
    }

    /** Eltern-Men� dieses Men�s setzen
     * 
     * @param parent das Eltern-Men�
     */
    public void setParent(CmdMenu parent) {
        this.parentMenu = parent;
        setIndex();
    }
    
    /**
     * Eltern-Men� dieses Men�s lesen
     * @return Eltern-Men�
     */
    public CmdMenu getParent() {
        return this.parentMenu;
    }
    
    /**
     * Index des Men�s setzen (abh�ngig vom Eltern-Men� und Geschwister-Eintr�gen), ist eindeutig
     *
     */   
    public void setIndex() {
        if (this.parentMenu==null) {
            this.index = 0;
        }
        else {
            this.index = this.parentMenu.getSubMenues().size()+this.parentMenu.getCommands().size()+1;
        }
    }
    /**
     * Index des Men�s auslesen
     * @return Index (immer eindeutig auf einer Men�hierarchie-Ebene)
     */
    public int getIndex() {
        return this.index;
    }
    
    /** Unter-Men� hinzuf�gen
     * 
     * @param childMenu men�
     */
    public void addNode(CmdMenu childMenu) {
        childMenu.setParent(this);
        this.subMenues.add(childMenu);
    }

    /** Unter-Men� l�schen
     * 
     * @param childMenu men�
     */
    public void removeNode(CmdMenu childMenu) {
        this.subMenues.remove(childMenu);
    }
    
    public void clearNode() {
      this.subMenues.clear();
  }

    /** Alle Untermen�s auslesen
     * 
     * @return Vektor mit Untermen�s
     */
    public Vector<CmdMenu> getSubMenues() {
        return this.subMenues;
    }
    
    /** Ein bestimmtes Untermen� auslesen
     * 
     * @param i Index des Untermen�s
     * @return das Untermen�
     */
    public CmdMenu getChildNode(int i) {
        return this.subMenues.get(i);
    }
    
    /** Ein Kommando hinzuf�gen.
     * F�gt das Kommando vor etwaigen versteckten Kommandos ein.
     * 
     * @param cmd das Kommando
     * @see HiddenCommand
     */
    public void addCmd(Command cmd) {
    	if ( cmd instanceof HiddenCommand )
    	{
    		commands.add(cmd);
    		numHiddenCommands++;
    	}
    	else	//"Normales" Kommando
    	{
			//Es handelt sich um einen Command -> Hinter letzten Command und ersten HiddenCommand einf�gen
        	int i = commands.size()-1;
	        while (i >= 0 && commands.get(i) instanceof HiddenCommand)		
	        	i--;
        	commands.add(i+1, cmd);	//Hinter den letzten Command der Liste einf�gen
    	}
        cmd.setParent(this);
    }
    
    /** Alle Kommandos auslesen
     * 
     * @return die Kommandos als Vektor
     */
    public Vector<Command> getCommands() {
        return this.commands;
    }
    
    /**
     * Ein bestimmtes Kommando 
     * @param i der Index des Kommandos
     * @return das Kommando
     */
    public Command getLeaf(int i) {
        return this.commands.get(i);
    }
   
    /** Einen Hilfetext f�r das Men� setzen
     * 
     * @param help der Hilfetext
     */
    public void setHelp(String help) {
        this.help = help;
    }
    
    /**
     * Hilfe f�r das Men� ermitteln
     * @return der Hilfetext
     */
    public String getHelp() {
        return this.help;
    }
    
    /** Eine Beschreibung setzen
     * 
     * @param desc die Beschreibung
     */
    public void setDesc(String desc) {
        this.description = desc;
    }
    
    /** Die Beschreibung auslesen
     * 
     * @return Beschreibung
     */
    public String getDesc() {
        return this.description;
    }

    /** Den Inkarnationsnamen auslesen
     * 
     * @return Inkarnationsname
     */
    public String getInkarnationsName() {
        return this.inkarnationsName;
    }

	/**
	 * Liefert die Anzahl der zus�tzlichen Kommandos.
	 * @return	Liefert die Anzahl der zus�tzlichen Kommandos.
	 */
	public int getNumHiddenCommands()
	{
		return numHiddenCommands;
	}
}
