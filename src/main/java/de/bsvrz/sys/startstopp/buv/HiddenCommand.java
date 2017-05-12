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

/**
 * Kommando das nicht im Men� angezeigt wird.
 * Darf nur in der obersten Men�ebene hinter allen "sichtbaren" Eintr�gen verwendet werden.
 * @author beck et al. projects GmbH
 * @author Martin Hilgers
 * @version $Revision: 1.5 $ / $Date: 2008/01/29 13:16:26 $ / ($Author: ObertM $)
 */
public abstract class HiddenCommand extends Command
{
	private int index = 0;

    /**
     * Kommando f�r {@link CmdInterpreter}
     * @param desc die Beschreibung
     * @param index Index des Kommandos. Muss eindeutig sein. Es sollte eine Zahl > 100 gew�hlt werden,
     * 				damit es zu keinen �berschneidungen mit den �brigen Kommandos kommt.
     */
	public HiddenCommand(String desc, int index)
	{
		super(desc, "");
		this.index = index;
	}
	
	/**
	 * @see de.bsvrz.sys.startstopp.buv.CmdInterpreter
	 */
	public int getIndex()
	{
		return index;
	}

}
