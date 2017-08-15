/*
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
 * Dambach-Werke GmbH
 * Elektronische Leitsysteme
 * Fritz-Minhardt-Str. 1
 * 76456 Kuppenheim
 * Phone: +49-7222-402-0
 * Fax: +49-7222-402-200
 * mailto: info@els.dambach.de
 */

package de.bsvrz.sys.startstopp.prozessvew;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.jdom.Element;

import de.bsvrz.dav.daf.main.ApplicationCloseActionHandler;
import de.bsvrz.sys.funclib.debug.Debug;
import de.bsvrz.sys.startstopp.skriptvew.GlobaleDaten;
import de.bsvrz.sys.startstopp.skriptvew.Kernsystem;
import de.bsvrz.sys.startstopp.skriptvew.ProtokollDatei;
import de.bsvrz.sys.startstopp.skriptvew.SkriptVewEnums;
import de.bsvrz.sys.startstopp.skriptvew.StartStoppApp;
import de.bsvrz.sys.startstopp.skriptvew.StartStoppBlock;
import de.bsvrz.sys.startstopp.skriptvew.StartStoppVerwaltung;
import de.bsvrz.sys.startstopp.skriptvew.SkriptVewEnums.E_START_FEHLER_VERHALTEN;
import de.bsvrz.sys.startstopp.skriptvew.SkriptVewEnums.E_STOPP_FEHLER_VERHALTEN;


/**
 * Die Klasse verwaltet die einzelnen Prozesse die durch StartStopp gestartet
 * werden sollen. Sie verf�gt �ber Methoden zum Starten, Stoppen und Neustarten
 * einzelner oder aller Prozesse. Die Klasse wird als Singleton ausgef�hrt. 
 * @author Dambach Werke GmbH
 */
public class ProzessVerwaltung implements Serializable
{
  private static final long serialVersionUID = 1L;
  
  /**
   * Debug Einstellungen
   */
  private static final Debug logger = Debug.getLogger();
  
  /**
   * Debug Einstellungen
   */
  private final boolean _debug = false;
  
  /**
   * Prozessdaten der Prozesse des lokalen Rechners
   */
  private ProzessDatenVerwaltung m_prozessDatenVerwaltung;
  
  /**
   * Prozessdaten der Prozesse der remote Rechner
   */
  private ProzessDatenVerwaltung m_prozessDatenVerwaltungRemote;
  
  /**
   * Z�hler wie oft welche Datei verwendet wird. Realisierung durch Hashmap. Als
   * Key dient der Dateiname.
   */  
  private HashMap<String, Integer> m_dateiVerwaltung = new HashMap<String, Integer>();
  
  /**
   * Verweis auf Singelton Instanz der Klasse GlobaleDaten
   */  
  private GlobaleDaten m_global = null;

  /**
   * Verweis auf Singelton Instanz der Klasse DaVKommunikation
   */  
  private DaVKommunikation m_daVKommunikation = null;

  /**
   * Merker ob Kernsystem gestartet wurde
   */
  private boolean m_kernSystemGestartet = false;

  /**
   * Thread der die StartStopp Applikation beendet
   */
  private ThreadBeendeStartStopp m_threadBeendeStartStopp;

  /**
   * Merker, ob in dem Inkarnationsanteil der Datei startStopp.xml ein sematischer 
   * Fehler vorliegt (z.B. Falsche Schl�sselworte etc.). 
   */
  private boolean m_fehlerInkarantion = false;
  
  /**
   * Liste mit den Klassen dich sich als Listener auf die Klasse angemeldet haben. Diese Klassen
   * werden informiert wenn sich �nderungen im Attribut "m_zustand" ereignet haben. 
   */
  private List<IKernsystemListener> m_angemeldeteListener = new LinkedList<IKernsystemListener>();

  private Object m_neuDavVerbindungAufbauen = new Object();

  /**
   * Methode zum Lesen der einzigen Instanz der Klasse
   * @return einzige Instanz der Klasse
   */
  public static ProzessVerwaltung getInstanz()
  {
    return Inner.INSTANCE;
  }

  /**
   * Innere Klasse zum Sicherstellen, dass wirklich nur eine Instanz der Klasse
   * gebildet wird
   * @author Dambach Werke GmbH
   */
  public static class Inner
  {
    private static ProzessVerwaltung INSTANCE = new ProzessVerwaltung();
  }

  /**
   * Parameterloser Konstruktor der Klasse
   */
  private ProzessVerwaltung()
  {
    // Hashmap anlegen f�r Inkarnationen die auf dem lokalen Rechner gestartet werden
    
    m_prozessDatenVerwaltung = new ProzessDatenVerwaltung ();

    // Hashmap anlegen f�r Inkarnationen die auf Remoterechnern gestartet werden

    m_prozessDatenVerwaltungRemote = new ProzessDatenVerwaltung ();
  }

  /**
   * Methode zum Resetten der Datenstrukturen der Prozessverwaltung
   */
  public void reset ()
  {
    // Hashmap anlegen f�r Inkarnationen die auf dem lokalen Rechner gestartet werden
    
    m_prozessDatenVerwaltung = new ProzessDatenVerwaltung ();

    // Hashmap anlegen f�r Inkarnationen die auf Remoterechnern gestartet werden

    m_prozessDatenVerwaltungRemote = new ProzessDatenVerwaltung ();
    
    // Dateiverwaltung
    
    m_dateiVerwaltung = new HashMap<String, Integer> ();
  }
  
  /**
   * Methode zum Starten eines Prozesses
   * @param prID Id des zu startenden Prozesses
   */
  public void starteProzess (String prID)
  {
    starteProzess (prID, 0l, 0l);
  }

  /**
   * Methode zum Starten eines Prozesses
   * @param prID  ProzessID
   * @param absender Absender der Start ausl�st 
   */
  public void starteProzess (String prID, long absender, long simulationsvariante)
  {
    ProzessDaten pd = getProzessDaten( prID );
    
    if (pd == null)
    {
      logger.error ("*** Keine Prozessdaten gefunden: " + prID);
      return;
    }
    
    // Werte �bernehmen
    
    pd.setSimulationsVariante( simulationsvariante );
    
    pd.setAbsender( absender );
    
    // Inkarnation noch nicht angelegt
    
    Inkarnation ik = pd.getInkarnation();
    
    if (ik == null)
    {
      ik = new Inkarnation ( prID );
      pd.setInkarnation( ik );
    }
    
    ik.starteInkarnation( absender, 0l );
  }

  /**
   * Methode zum zyklsichen Starten eines Prozesses
   * @param prID  ProzessID
   * @param absender Absender der den zyklischen Start ausl�st
   */
  public void starteProzessZyklisch (String prID, long absender, long simulationsvariante)
  {
    ProzessDaten pd = getProzessDaten( prID );
    
    if (pd == null)
    {
      logger.error("*** Keine Prozessdaten gefunden: " + prID);
      return;
    }
    
    // Daten �bernehmen
    
    pd.setAbsender( absender );
    pd.setSimulationsVariante( simulationsvariante );
    
    // Inkarnation noch nicht angelegt
    
    Inkarnation ik = pd.getInkarnation();
    
    if (ik == null)
    {
      ik = new Inkarnation ( prID );
      pd.setInkarnation( ik );
    }
    
    ik.starteInkarnationZyklisch( absender );
  }

  /**
   * Methode zum manuellen Starten eines Prozesses
   * @param prID  ProzessID
   * @param absender Absender der den manuellen Start ausl�st
   */
  public void starteProzessManuell (String prID, long absender, long simulationsvariante)
  {
    ProzessDaten pd = getProzessDaten( prID );
    
    if (pd == null)
    {
      logger.error("*** Keine Prozessdaten gefunden: " + prID);
      return;
    }
    
    // Daten �bernehmen
    
    pd.setAbsender( absender );
    pd.setSimulationsVariante( simulationsvariante );
    
    // Inkarnation noch nicht angelegt
    
    Inkarnation ik = pd.getInkarnation();
    
    if (ik == null)
    {
      ik = new Inkarnation ( prID );
      pd.setInkarnation( ik );
    }
  }

  /**
   * Methode zum Stoppen eines Prozesses
   * @param prID Id des zu stoppenden Prozesses
   */
  public void stoppeProzess (String prID)
  {
    stoppeProzess (prID, 0l, false);
  }

  /**
   * Methode zum Stoppen eines Prozesses
   * @param prID  ProzessID
   * @param absender Absender der das Stoppen ausl�st
   * @param stoppeSofort sofort Stoppen ohne warten auf Stoppbedingung bzw. Delay
   */
  public void stoppeProzess (String prID, long absender, boolean stoppeSofort)
  {
    ProzessDaten pd = getProzessDaten( prID );
    
    if (pd != null)
    {
      // Inkarnation noch nicht angelegt
      
      Inkarnation ik = pd.getInkarnation();
      
      if (ik == null)
        return;
  
      ik.stoppeInkarnation( absender, stoppeSofort );
    }
  }
    
  /**
   * Methode zum Neustarten eines Prozesses
   * @param prID Id des neu zu startenden Prozesses
   */
  public void neuStartProzess (String prID)
  {
    neuStartProzess (prID, 0l, 0l);
  }

  /**
   * Methode zum Neustarten eines Prozesses
   * @param prID Id des neu zu startenden Prozesses
   * @param absender Absender der Start ausl�st
   */
  public void neuStartProzess (String prID, long absender, long simulationsVariante)
  {
    ProzessDaten pd = getProzessDaten( prID );
    
    // Inkarnation noch nicht angelegt
    
    Inkarnation ik = pd.getInkarnation();
    
    if (ik == null)
      return;

    ik.neuStartInkarnation( absender,  simulationsVariante);
  }

  /**
   * Methode zum L�schen eines Prozesses. Das L�schen wird als eigener Thread ausgef�hrt, da auf das Beenden des Prozesses
   * gewartet werden muss
   * @param prID Id des zu l�schenden Prozesses
   * @param absender Absender der das L�schen ausl�st
   */
  public void loescheProzess (String prID, long absender)
  {
    List<String> ids = new ArrayList<String> ();
    
    ids.add( prID );
    
    loescheProzesse( ids, absender );
  }

  /**
   * Methode zum L�schen mehrerer Prozesse. Das L�schen wird als eigener Thread ausgef�hrt, da auf das Beenden der Prozesse
   * gewartet werden muss
   * @param prIds Liste mit den Ids der zu l�schenden Prozesse
   * @param absender Absender der das L�schen ausl�st
   */
  public void loescheProzesse (List<String> prIds, long absender)
  {
    ThreadLoescheProzess thread = new ThreadLoescheProzess (prIds, absender);
    thread.start();
    thread.setName( "LoescheProzesse" );
  }

  /**
   * Interne Klasse zum L�schen eines Prozesses
   */

  private class ThreadLoescheProzess extends Thread
  {
    private List<String> m_prozessIds = null;
    private long         m_absender  = -1;
    
    /**
     * Konstruktor der Klasse
     */
    public ThreadLoescheProzess (List<String> prozessIds, long absender)
    {
      this.m_prozessIds = prozessIds;
      this.m_absender   = absender;
    }
    
    /**
     * Run Methode des Threads
     */
    public void run ()
    {
      // Prozesse beenden
      
      for (int i=0; i<m_prozessIds.size(); ++i)
      {
        ProzessDaten pd = m_prozessDatenVerwaltung.getProzessDaten( m_prozessIds.get( i ) );
    
        stoppeProzess( pd.getProzessId(), m_absender, true );
      }

      // Warten auf Ende der Prozesse
       
      int counter;
      
      do
      {
        counter = 0;

        for (int i=0; i<m_prozessIds.size(); ++i)
        {
          ProzessDaten pd = m_prozessDatenVerwaltung.getProzessDaten( m_prozessIds.get( i ) );
    
          if (pd.isAktiv())
          {
            logger.info( "Warten auf Ende Inkarnation " + pd.getName());
            counter++;
          }
        }
    
        try
        {
          sleep (1000);
        }
        catch (InterruptedException e)
        {
          e.printStackTrace();
        }
    
      } while (counter > 0);

      // Verweis zum StartStopp Block l�schen

      for (int i=0; i<m_prozessIds.size(); ++i)
      {
        String id = m_prozessIds.get( i );
        
        ProzessDaten pd = m_prozessDatenVerwaltung.getProzessDaten( id );
        
        if (pd != null)
        {
          String startStoppBlockId = pd.getStartStoppBlockId();
        
          StartStoppBlock ssb = StartStoppVerwaltung.getInstanz().getStartStoppBlock( startStoppBlockId );
        
          if (ssb != null)
            ssb.loescheProzess( id );
        }

        // Prozessdaten l�schen
        
        m_prozessDatenVerwaltung.removeProzessDaten( id );

        // Betriebsmeldung absetzen
      
        String buffer = " Inkarnation \"" + pd.getName() + "\" Status: " + "gel�scht" + " Absender: ";
        if (m_absender == -1)
          buffer += " Nutzerschnittstelle";
        else
          buffer += "Pid: " + m_absender;  
      
        // Betriebsmeldung absetzen
      
        DaVKommunikation.getInstanz().sendeBetriebsmeldung( buffer );
        
      } // for (int i=0; i<m_prozessIds.size(); ++i)

      // Information �ber gel�schte Inkarnation an DaV senden
      
      DaVKommunikation.getInstanz().sendeProzessInfo();

      DaVKommunikation.getInstanz().sendeStartStoppInfo();
      
      // Nutzerschnittstelle aktualisieren
      
      if (StartStoppApp.getNutzerSchnittstelle() != null)
        StartStoppApp.getNutzerSchnittstelle().aktualisiereProzessEintraege();
    }
  }

  /**
   * Methode zum Setzen der Prozessdaten eines Prozesses
   * @param id Id des Prozesses
   * @param prozessDaten Prozessdaten des Prozesses
   */
  public void setProzessDaten( String id, ProzessDaten prozessDaten )
  {
    m_prozessDatenVerwaltung.addProzessDaten( id, prozessDaten );
  }

  /**
   * Methode liefert die Anzahl der Prozesses 
   * @return Anzahl der Prozesses
   */
  public int getAnzahlProzesse ()
  {
    return m_prozessDatenVerwaltung.getAlleProzessDaten().size();
  }
  
  /**
   * Methode liefert die Prozessdaten eines lokalen Prozesses zur�ck, der �ber seine
   * ID angesprochen wird
   * @return Prozessdaten eines Prozesses
   */
  public ProzessDaten getProzessDaten(String id)
  {
    return m_prozessDatenVerwaltung.getProzessDaten( id );
   }
  
  /**
   * Methode liefert die Prozessdaten eines remote Prozesses (Prozess der auf anderem Rechner
   * l�uft) zur�ck, der �ber seine ID angesprochen wird
   * @return Prozessdaten eines remote Prozesses
   */
  public ProzessDaten getProzessDatenRemote(String id)
  {
    return m_prozessDatenVerwaltungRemote.getProzessDaten( id );
  }

  /**
   * Methode zum Auslesen der Inkarnationsdaten aus einer XML-Struktur.
   * @param root Verweis auf Root Element der XML-Struktur
   * @param absender Absender
   * @param startStoppBlockId ID des StartStopp Blocks
   * @param simulationsVariante Simulationsvariante des StartStopp Blocks
   */
   public void initialisiereInkarnationen (Element root, long absender, String startStoppBlockId, long simulationsVariante) 
   {
     List<ProzessDaten> procListe = new ArrayList<ProzessDaten>();
     
     m_global = GlobaleDaten.getInstanz();
     
     Element startStoppElement = root.getChild("startStopp");
     
     //Holen von global
     
     Element globalElement = startStoppElement.getChild( "applikationen" );
     
     ListIterator<Element> listeKinderGlobal = globalElement.getChildren().listIterator();

     // StartStopp Block laden
     
     StartStoppBlock ssb = StartStoppVerwaltung.getInstanz().getStartStoppBlock( startStoppBlockId );
     
     //----------------------------------------------------------------------
     // While Loop Inkarnationen
     //----------------------------------------------------------------------

     m_fehlerInkarantion = false;
     
     List<String> neueInkarnationen = new ArrayList<String>();
     
     while (listeKinderGlobal.hasNext() && !m_fehlerInkarantion)
     {
       Element inkarnation = listeKinderGlobal.next();
     
       if (inkarnation.getName() == "inkarnation")
       {
         ProzessDaten proc = new ProzessDaten ();  
         
         proc.setStartStoppBlockId( startStoppBlockId );
       
         String nameOriginal = inkarnation.getAttribute( "name" ).getValue();
         
         String name = Tools.bestimmeInkarnationsName( nameOriginal, simulationsVariante );

         // Pr�fen ob Name definiert wurde, sonst Fehlermeldung
         
         if ((name == null) || name.equals( "" ))
         {
           String buffer = "Inkarnation mit Name \"" + name + "\" nicht zul�ssig !";
           
           logger.error(buffer);
           
           m_fehlerInkarantion = true;
         }
         else
         {
           // Pr�fen ob es bereits einen Prozess mit diesem Namen gibt

           String buffer = "Inkarnationsname \"" + nameOriginal + "\" unter Simulationsvariante " + simulationsVariante + " "; 

           if (!isEindeutigerName( name ))
           {
             buffer += "wird bereits verwendet !";
             
             logger.error(buffer);
             
             m_fehlerInkarantion = true;
           }
           else
           {
             buffer += "ist eindeutig !";
//             System.out.println(buffer);
             
             proc.setName( nameOriginal );
             proc.setSimulationsVariante( simulationsVariante );
             
             neueInkarnationen.add( name );
             
             if (m_global.isKernsystem( name ))
               proc.setKernsystem( true );
           }
         }

         //----------------------------------------------------------------------
         // While Loop Attribute der Inkarnation
         //----------------------------------------------------------------------

         ListIterator<Element> listeAttribute = inkarnation.getChildren().listIterator();

         while (listeAttribute.hasNext() && !m_fehlerInkarantion)
         {
           Element child = listeAttribute.next();

           if (child.getName() == "applikation")
           {
             String value = child.getAttribute( "name" ).getValue();
             
             proc.setAusfuehrbareDatei( value );
           }
           
           if (child.getName() == "aufrufparameter")
           {
             String value = child.getAttribute( "wert" ).getValue();
             
             proc.addAufrufParameter( value );
           }

           if (child.getName() == "startart")
           {
             String option        = child.getAttribute( "option" ).getValue();
             String neustart      = child.getAttribute( "neustart" ).getValue();
             String intervall     = child.getAttribute( "intervall" ).getValue();
             
             proc.setStartArt( option, neustart, intervall );
           }

           if (child.getName() == "startbedingung")
           {
             String vorgaenger = child.getAttribute( "vorgaenger" ).getValue();
             String warteart   = child.getAttribute( "warteart" ).getValue();

             String rechner    = "";
             String wartezeit  = "0";

             try
             {
               rechner = child.getAttribute("rechner").getValue();
             }
             catch (Exception e) 
             {
               rechner = "";
             }
             
             try
             {
               wartezeit = child.getAttribute("wartezeit").getValue();
             }
             catch (Exception e) 
             {
               wartezeit = "0";
             }
             
             proc.addStartBedingung(vorgaenger, warteart, rechner, myLong(wartezeit));
           }

           if (child.getName() == "stoppbedingung")
           {
             String nachfolger = child.getAttribute( "nachfolger" ).getValue();
             
             String rechner    = "";
             String wartezeit  = "0";

             try
             {
               rechner = child.getAttribute("rechner").getValue();
             }
             catch (Exception e) 
             {
               rechner = "";
             }

             try
             {
               wartezeit = child.getAttribute("wartezeit").getValue();
             }
             catch (Exception e) 
             {
               wartezeit = "0";
             }
             
             proc.addStoppBedingung(nachfolger, rechner, myLong(wartezeit));
           }

           if (child.getName() == "standardAusgabe")
           {
             String option = child.getAttribute( "option" ).getValue();
             String alias  = child.getAttribute( "dateiname" ).getValue();

             proc.setStandardAusgabe (startStoppBlockId, SkriptVewEnums.E_AUSGABE.getEnum( option ), alias); 
             
             ProtokollDatei pd = GlobaleDaten.getInstanz().getProtokollDateiByAlias( startStoppBlockId, alias );
             if (pd != null)
               verwalteDatei (pd.getNameDatei()); 
           }

           if (child.getName() == "standardFehlerAusgabe")
           {
             String option = child.getAttribute( "option" ).getValue();
             String alias  = child.getAttribute( "dateiname" ).getValue();

             proc.setStandardFehlerAusgabe( startStoppBlockId, SkriptVewEnums.E_AUSGABE.getEnum( option ), alias); 
                                      
             ProtokollDatei pd = GlobaleDaten.getInstanz().getProtokollDateiByAlias( startStoppBlockId, alias );
             if (pd != null)
               verwalteDatei (pd.getNameDatei()); 
           }

           if (child.getName() == "startFehlerverhalten")
           {
             String option = child.getAttribute( "option" ).getValue();
             String wiederholungen = child.getAttribute( "wiederholungen" ).getValue();

             proc.setStartVerhaltenFehler( E_START_FEHLER_VERHALTEN.getEnum ( option ), 
                                           myLong ( wiederholungen ) );
           }

           if (child.getName() == "stoppFehlerverhalten")
           {
             String option = child.getAttribute( "option" ).getValue();
             String wiederholungen = child.getAttribute( "wiederholungen" ).getValue();

             proc.setStoppVerhaltenFehler( E_STOPP_FEHLER_VERHALTEN.getEnum ( option ), 
                                           myLong ( wiederholungen ) );
           }

         } // while
         
         procListe.add( proc );

       } // if (inkarnation.getName() == "inkarnation")
       
     } // while (listeKinderGlobal.hasNext())

     // Pr�fen ob Prozessdaten semantisch plausibel sind

     if (!m_fehlerInkarantion)
     {
       for (int ii=0; ii<procListe.size() && !m_fehlerInkarantion; ++ii)
       {
         ProzessDaten proc = procListe.get( ii );
         
         proc.setStartStoppBlockId( startStoppBlockId );
  
         if (!proc.isPlausibel(neueInkarnationen))
           m_fehlerInkarantion = true;
       }
     }

     // Pr�fen ob innerhalb der Applikationen ein Zirkelschluss entsteht (Prozesse haben
     // sich gegenseitig als Vorg�nger in den Startbedingungen bzw. den Stoppbedingungen eingetragen)
     
     if (!m_fehlerInkarantion)
     {
       boolean gefunden = false;
       
       for (int ii=0; ii<procListe.size() && !gefunden; ++ii)
       {
         ProzessDaten proc = procListe.get( ii );
         
         List<String> zuPruefendeNamen = new ArrayList<String> ();
         zuPruefendeNamen.add( proc.getName() );
         
         gefunden = isZirkelSchlussInStartbedingung (proc, zuPruefendeNamen, procListe);

         if (gefunden)
         {
           logger.error("Fehler Zirkelschluss bei den Startbedingungen von Inkarnation " + proc.getName());
           m_fehlerInkarantion = true;
         }
       }
     }

     if (!m_fehlerInkarantion)
     {
       boolean gefunden = false;
       
       for (int ii=0; ii<procListe.size() && !gefunden; ++ii)
       {
         ProzessDaten proc = procListe.get( ii );

         List<String> zuPruefendeNamen = new ArrayList<String> ();
         zuPruefendeNamen.add( proc.getName() );

         gefunden = isZirkelSchlussInStoppbedingung (proc, zuPruefendeNamen, procListe);

         if (gefunden)
         {
           logger.error("Fehler Zirkelschluss bei den Stoppbedingungen von Inkarnation " + proc.getName());
           m_fehlerInkarantion = true;
         }
       }
     }

     // Nur wenn keine Fehler in den Inkarantionen vorliegen werden die Inkarnationen �bernommen
     
     if (!m_fehlerInkarantion)
     {
       for (int ii=0; ii<procListe.size(); ++ii)
       {
         ProzessDaten proc = procListe.get( ii );

         // Kontrollausgabe
         
         if (_debug)
         {
           System.out.println("Inkarnation:");
           
           System.out.println("- Name: " + proc.getName());
           System.out.println("- Applikation: " + proc.getAusfuehrbareDatei());
           System.out.println("- StartStoppBlockId: " + proc.getStartStoppBlockId());
           System.out.println("- Aufrufparameter: ");
           
           for (int i=0; i<proc.getAufrufParameter().size(); ++i)
             System.out.println("  - " + proc.getAufrufParameter().get( i ));

           System.out.println("- Startart: ");

           System.out.println("  - Option: " + proc.getStartArt().getOption());
           System.out.println("  - Neustart: " + proc.getStartArt().getNeuStart());
           System.out.println("  - Zeit: " + proc.getStartArt().getIntervallZeit());

           System.out.println("- Startbedingung: ");
           for (int i=0; i<proc.getStartBedingung().size(); ++i)
           {
             System.out.println("  - Vorg�nger: " + proc.getStartBedingung().get( i ).getProzessName());
             System.out.println("  - Warteart: " + proc.getStartBedingung().get( i ).getWarteArt());
             System.out.println("  - Rechner: " + proc.getStartBedingung().get( i ).getRechnerAlias());
             System.out.println("  - Wartezeit: " + proc.getStartBedingung().get( i ).getWarteZeit());
           }

           System.out.println("- Stoppbedingung: ");
           for (int i=0; i<proc.getStoppBedingung().size(); ++i)
           {
             System.out.println("  - Nachfolger: " + proc.getStoppBedingung().get( i ).getProzessName());
             System.out.println("  - Rechner: " + proc.getStoppBedingung().get( i ).getRechnerAlias());
             System.out.println("  - Wartezeit: " + proc.getStoppBedingung().get( i ).getWarteZeit());
           }

           System.out.println("- Standardausgabe: ");
           System.out.println("  - Option: " + proc.getStandardAusgabe().getOption());
           System.out.println("  - Dateiname: " + proc.getStandardAusgabe().getDateiAlias());

           System.out.println("- Standardfehlerausgabe: ");
           System.out.println("  - Option: " + proc.getStandardFehlerAusgabe().getOption());
           System.out.println("  - Dateiname: " + proc.getStandardFehlerAusgabe().getDateiAlias());

           System.out.println("- Startfehlerverhalten: ");
           System.out.println("  - Option: " + proc.getStartVerhaltenFehler().getOption());
           System.out.println("  - Wdh.: " + proc.getStartVerhaltenFehler().getWiederholungen());

           System.out.println("- Stoppfehlerverhalten: ");
           System.out.println("  - Option: " + proc.getStoppVerhaltenFehler().getOption());
           System.out.println("  - Wdh.: " + proc.getStoppVerhaltenFehler().getWiederholungen());

         } // if (_debug)

         // Inkarnation �bernehmen
         
         // ID des Prozesses bestimmen
         
         String key = bestimmeProzessPid( startStoppBlockId, proc.getName());
         
         // Verweise zum StartStopp Block setzen
         
         if (ssb != null)
         {
           ssb.addProzess ( key );
           
           proc.setStartStoppBlockId ( startStoppBlockId );
         }

         // Pr�fen ob Prozess bereits vorhanden ist
         
         boolean neueInkarantion = false;
         
         neueInkarantion = !m_prozessDatenVerwaltung.isProzessBekannt( key );
           
         proc.setProzessId( key );

         if (neueInkarantion)
         {
           m_prozessDatenVerwaltung.addProzessDaten( key, proc ); // Eintragen muss hier bereits erfolgen, damit Inkarnation
                                                        // auf diese Daten zugreifen kann
           
           Inkarnation ik = new Inkarnation ( key );
           
           proc.setInkarnation( ik );
         }
         else
         {
           ProzessDaten procAlt = m_prozessDatenVerwaltung.getProzessDaten( key );
           
           proc.setInkarnation( procAlt.getInkarnation() );
         }
         
       } // for (int ii=0; ii<procListe.size(); ++ii)
     
     } // if (!m_fehlerInkarnation)
         
     // Kontrollausgabe f�r die Verwendung der Dateien
     
//     Iterator it = m_dateiVerwaltung.entrySet().iterator();
//     while (it.hasNext())
//     {
//       Map.Entry me = (Map.Entry) it.next();
//       
//       String datei = (String)me.getKey();
//       int counter  = (Integer)me.getValue();
//       
//       System.out.println("Datei " + datei + " --> " + counter);
//     }
   }
   
   /**
    * Methode pr�ft, ob der Name einer Applikation in seinen Startbedingungen direkt, bzw. indirekt
    * vorkommt. Hierzu werden alle Vorg�ngerprozesse der Startbedinungen �berpr�ft. Die Methode ruft
    * sich selbst rekursiv auf bis alle Vorg�ngerprozesse bzw. die Vorg�ngerprozesse der Vorg�ngerprozesse
    * �berpr�ft worden sind.
    * @param proc Prozessdaten des aktuelle zu pr�fenden Prozesses
    * @param zuPruefendeNamen Liste mit den Namen die gesucht werden (= Pfad der Startbedingungen)
    * @param procListe Liste mit den zu pr�fenden Prozessdaten
    * @return true: Zirkelschluss gefunden (Applikation taucht selbst aus Vorg�ngerprozess auf), sonst false
    */
   private boolean isZirkelSchlussInStartbedingung( ProzessDaten proc, List<String> zuPruefendeNamen, List<ProzessDaten> procListe )
   {
     boolean gefunden = false; 

     String id = proc.getStartStoppBlockId();

     List<StartBedingung> lsb = proc.getStartBedingung();
     Iterator<StartBedingung> it = lsb.listIterator();
     
     while (it.hasNext() && !gefunden)
     {
       StartBedingung sb = it.next();
 
       String ip = GlobaleDaten.getInstanz().getIpAdresse (id, sb.getRechnerAlias());
       
       // Nur wenn Vorg�ngerprozess auf eigenem Rechner l�uft
       
       if (ip.equals( StartStoppApp.getRechnerAdresse()))
       {
         if (zuPruefendeNamen.contains( sb.getProzessName() ))
           gefunden = true;
         else
         {
            for (int i=0; i<procListe.size() && !gefunden; ++i)
            {
              ProzessDaten p = procListe.get( i );
              
              // Prozessdaten gefunden
              
              if (p.getName().equals( sb.getProzessName()))
              {
                List<String> neueNamen = new ArrayList<String> ();
                
                Iterator<String> itNamen = zuPruefendeNamen.iterator();
                while (itNamen.hasNext())
                  neueNamen.add( itNamen.next() );
                neueNamen.add( p.getName() );
                
                gefunden = isZirkelSchlussInStartbedingung( p, neueNamen, procListe );
                break;
              }
            }
         }
       }
     } // while
     
     return gefunden;
   }

   /**
    * Methode pr�ft, ob der Name einer Applikation in seinen Stoppbedingungen direkt, bzw. indirekt
    * vorkommt. Hierzu werden alle Nachfolgerprozesse der Stoppbedinungen �berpr�ft. Die Methode ruft
    * sich selbst rekursiv auf bis alle Nachfolgerprozesse bzw. die Nachfolgerprozesse der Nachfolgerprozesse
    * �berpr�ft worden sind.
    * @param proc Prozessdaten des aktuelle zu pr�fenden Prozesses
    * @param zuPruefendeNamen Liste mit den Namen die gesucht werden (= Pfad der Stoppbedingungen)
    * @param procListe Liste mit den zu pr�fenden Prozessdaten
    * @return true: Zirkelschluss gefunden (Applikation taucht selbst aus Vorg�ngerprozess auf), sonst false
    */
   private boolean isZirkelSchlussInStoppbedingung( ProzessDaten proc, List<String> zuPruefendeNamen, List<ProzessDaten> procListe )
   {
     boolean gefunden = false; 
    
     String id = proc.getStartStoppBlockId();
     
     List<StoppBedingung> lsb = proc.getStoppBedingung();
     Iterator<StoppBedingung> it = lsb.listIterator();
     
     while (it.hasNext() && !gefunden)
     {
       StoppBedingung sb = it.next();
 
       String ip = GlobaleDaten.getInstanz().getIpAdresse (id, sb.getRechnerAlias());
       
       // Nur wenn Nachfolgerprozess auf eigenem Rechner l�uft
       
       if (ip.equals( StartStoppApp.getRechnerAdresse()))
       {
         if (sb.getProzessName().equals( zuPruefendeNamen ))
           gefunden = true;
         else
         {
            for (int i=0; i<procListe.size() && !gefunden; ++i)
            {
              ProzessDaten p = procListe.get( i );
              
              // Prozessdaten gefunden
              
              if (p.getName().equals( sb.getProzessName()))
              {
                List<String> neueNamen = new ArrayList<String> ();
                
                Iterator<String> itNamen = zuPruefendeNamen.iterator();
                while (itNamen.hasNext())
                  neueNamen.add( itNamen.next() );
                neueNamen.add( p.getName() );

                gefunden = isZirkelSchlussInStoppbedingung( p, zuPruefendeNamen, procListe );
                break;
              }
            }
         }
       }
     } // while
     
     return gefunden;
   }

  /**
    * Methode dient dazu festzustellen, ob eine Datei mehrfach verwendet wird.
    * Hierzu wird f�r jede Datei ein Z�hler angelegt, der bei jeder Referenzierung
    * der Datei inkrementiert wird.
    * @param dateiName Name der Datei (aus startstopp.xml)
    */
   private void verwalteDatei( String dateiName )
   {
     int counter;
     
     if (m_dateiVerwaltung.containsKey( dateiName ))
       counter = m_dateiVerwaltung.get( dateiName );
     else
       counter = 0;
     
     counter++;
     
     m_dateiVerwaltung.put( dateiName, counter );
   }

   /**
    * Methode liefert die Information ob eine Datei mehrfach verwendet wird
    * (mehrere Inkarnationen nutzen die selbe Datei f�r ihre Ausgaben)
    * @param datei Name der Datei
    * @return true Datei wird mehrfach genutzt, sonst false.
    */
   public boolean wirdDateiMehrfachVerwendet (String datei)
   {
     int counter;
     
     if (m_dateiVerwaltung.containsKey( datei ))
       counter = m_dateiVerwaltung.get( datei );
     else
       counter = 0;
     
     return (counter > 1);
   }
   
  /**
    * Methode pr�ft ob der �bergebene Name bereits als Prozessname verwendet
    * wird. Wenn ja, dann wird der Namen erweitert um eine Z�hler der Form
    * xxx_1.
    * @param name Name der gepr�ft werden soll
    * @return orginal Name wenn eindeutig, sonst modifizierter Name
    */
   public String bildeEindeutigenNamen( String name )
   {
     String nameNeu = name;

     List<String> pNamen = new ArrayList<String> ();
     
     HashMap<String, ProzessDaten> copy = m_prozessDatenVerwaltung.getAlleProzessDaten();
     
     Iterator<ProzessDaten> prozessDaten = copy.values().iterator();
     
     while (prozessDaten.hasNext())
     {
       ProzessDaten zw = prozessDaten.next();
       
       pNamen.add( zw.getName() );
     }
     
     if (pNamen.contains( name ))
     {
       int counter = 1;
       do
       {
         nameNeu = name + "_" + counter;
         counter++;
       }
       while (pNamen.contains( nameNeu ));
     }

     return nameNeu;
   }

   /**
    * Methode pr�ft ob der �bergebene Name als Prozessname eindeutig ist. 
    * @param name Name der gepr�ft werden soll
    * @return true: Name ist eindeutig, sonst false
    */
   public boolean isEindeutigerName( String name  )
   {
     List<String> pNamen = new ArrayList<String> ();

     HashMap<String, ProzessDaten> copy = m_prozessDatenVerwaltung.getAlleProzessDaten();

     Iterator<ProzessDaten> prozessDaten = copy.values().iterator();
     
     while (prozessDaten.hasNext())
     {
       ProzessDaten pd = prozessDaten.next();
       
       pNamen.add(pd.getName());
     }
     
     return (!pNamen.contains(name));
   }

  /**
    * Methode zum Bestimmen der Prozessid. Die Prozessid setzt sich zusammen aus der
    * ID des StartStopp Blocks zu dem der Prozess geh�rt plus dem Inkarantionsnamen,
    * @param startStoppBlockId Id des zu diesem Prozess geh�renden StartStoppBlocks
    * @param name Name der Inkarnation
    * @return eindeutiger Key der Inkarnation
    */
   public String bestimmeProzessPid (String startStoppBlockId, String name)
   {
     String key = startStoppBlockId + "_" + name;
     return key;     
   }

  /**
   * Methode zum Starten des Kernsystems. Das Starten selbst wird durch einen
   * Thread ausgef�hrt. �ber die Methode "isKernsystemGestartet" kann gepr�ft 
   * werden ob das Kernsystem l�uft oder nicht.
   */   
   
  public void starteKernsystem ()
  {
    if (!m_kernSystemGestartet)
    {
      ThreadStarteKernsystem tsk = new ThreadStarteKernsystem ();
      tsk.start();
      tsk.setName( "starteKernsystem" );
    }
  }
  
  /**
   * Intern Klasse die zum Starten des Kernsystems verwendet wird.
   * Das Starten des Kernsystems wird als Thread ausgef�hrt, da zwischen
   * dem einzelnen Prozessesn des Kernsystems Pausen eingelegt werden.
   * @author Dambach Werke GmbH
   *
   */
  private class ThreadStarteKernsystem extends Thread 
  {
    final String FEHLERMELDUNG_VERBINDUNGDAV_GESTARTET = "Verbindung mit selbst gestartetem Kernsystem konnte nicht aufgebaut werden !";
    final String FEHLERMELDUNG_VERBINDUNGDAV_VORHANDEN = "Verbindung mit bestehendem Kernsystem konnte nicht aufgebaut werden !";
    final String FEHLERMELDUNG_VERBINDUNGDAV_ABGEBROCHEN = "Verbindung mit Datenverteiler wurde beendet !";

    public ThreadStarteKernsystem ()
    {
        
    }
    
    public void run ()
    {
      m_global = GlobaleDaten.getInstanz();
  
      m_daVKommunikation = DaVKommunikation.getInstanz();
  
      // Unterscheidung ob ThreadStarteKernsystem von Start/Stopp aus gestartet werden muss,
      // oder ob sich die Applikation mit einem Kernsystem eines anderen Rechners
      // verbindet.
      
      if (m_global.getKernSystem().size() > 0)
      {
        logger.info("Starte Kernsystem");
        
        // Vor dem Start des Kernsystems wird gepr�ft, ob das Kernsystem
        // bereits l�uft. Dies wird dadurch gepr�ft, indem versucht wird 
        // sich mit dem DaV zu verbinden.
  
        if (m_daVKommunikation.baueVerbindungDaVAuf())
        {
          logger.info("Kernsystem soll von StartStopp gestartet werden und l�uft bereits !");
          
          StartStoppApp.beendeStartStoppWegenFehler("Kernsystem soll von StartStopp gestartet werden und l�uft bereits !");

          return;
        }
        else
        {
          logger.info("Kernsystem muss von StartStopp gestartet werden");

          long wartezeitErsteAnmeldung = StartStoppApp.getWartezeitErsteAnmeldung();
          
          // Prozesse starten die zum Kernsystem geh�ren (Abstand 5 Sekunden)
          
          for (int i=0; i<m_global.getKernSystem().size(); ++i)
          {
            Kernsystem ks = m_global.getKernSystem().get( i );
            String applikation = ks.getApplikation();
            
            long wartezeit = 5;  // Defaultzeit
              
            if (ks.isWarteZeitVersorgt())
              wartezeit = ks.getWartezeit();

            logger.info ("- Starte Kernsystem: " + applikation + " in " + wartezeit + " Sekunden");
            mySleep( wartezeit * 1000 );
            
            starteProzess (getPidByName( applikation ));
          }
          
          if (wartezeitErsteAnmeldung < 5) // Wartezeit mindestens 5 Sekunden
          {
            wartezeitErsteAnmeldung = 5;
          }

          if (wartezeitErsteAnmeldung != 5) // Ausgabe nur bei vorgegebenem Wert
          {
            logger.info("Beginn Wartezeit vor der ersten Anmeldung am DAV " + wartezeitErsteAnmeldung + " Sekunden");
          }
          
          // hier die Wartezeit vor der ersten Anmeldung am DAV wg. Timeout (10 Minuten) bei System mit Rechteverwaltung abwarten
          
          mySleep(wartezeitErsteAnmeldung * 1000);

          if (wartezeitErsteAnmeldung != 5) // Ausgabe nur bei vorgegebenem Wert
          {
            logger.info("Ende Wartezeit vor der ersten Anmeldung am DAV");
          }
        }
      }
      else
      {
        logger.info("mit bestehendem Kernsystem verbinden");
      }
      
      // Verbinden mit vorhandenem Datenverteiler (warten, bis Datenverteiler vorhanden) 

      String fehlerMeldungDav = null;
      String pidTransmitter = null;

      boolean kernSystemWirdVonStartStoppGestartet = false;

      if (m_global.getKernSystem().size() > 0) // StartStopp startet Kernsystem selbst
      {
      	kernSystemWirdVonStartStoppGestartet = true;
      	
       // Bestimmen der Pid des Transmitters
        
        Kernsystem datenverteiler = m_global.getKernSystem().get( 0 );
        String applikation = datenverteiler.getApplikation();
        pidTransmitter = getPidByName( applikation );
      }
      
      fehlerMeldungDav = kernSystemWirdVonStartStoppGestartet ? FEHLERMELDUNG_VERBINDUNGDAV_GESTARTET : FEHLERMELDUNG_VERBINDUNGDAV_VORHANDEN;
      
    	boolean verbindungAufgebaut = false;

    	while (true)
    	{
        long wartezeitZwischenAnmeldungen = StartStoppApp.getWartezeitZwischenAnmeldungen();

	    	while (!verbindungAufgebaut)
	    	{
	    	  // Merker ob versucht werden kann sich mit dem DaV zu verbinden, bei fremdem DaV sofort m�glich,
	    	  // bei selbst gestartetem DaV nur m�glich wenn der von StartStopp gestartete Transmitter aktiv
	    	  // ist (k�nnte ja �ber StartStopp manuell beendet worden sein)
	    	  
	    	  boolean versuchenVerbindungAuszubauen = true;
	    	  
	    	  if (kernSystemWirdVonStartStoppGestartet)
	    	  {
	    	    // Pr�fen ob Transmitter l�uft
	    	    
            ProzessDaten pd = getProzessDaten( pidTransmitter );
            if (!pd.isAktiv())
              versuchenVerbindungAuszubauen = false;
	    	  }

	    	  if (versuchenVerbindungAuszubauen)
	    	  {
  	    		if (m_daVKommunikation.baueVerbindungDaVAuf())
  	    		{
  	    			verbindungAufgebaut = true;
  	    		}
  	    	  else
              logger.info( fehlerMeldungDav );
	    	  }
	    	  
	    	  if (!verbindungAufgebaut) // nach einer Wartezeit erneuten Verbindungsaufbau starten
	    		{
	    			try
						{
							Thread.sleep(wartezeitZwischenAnmeldungen * 1000);
						} 
	    			catch (InterruptedException e)
						{
							e.printStackTrace();
						}
	    		}
	    	}
	      
	      m_daVKommunikation.getConnection().setCloseHandler(new ApplicationCloseActionHandler()
	      {
	      	public void close(String error)
	      	{
	      		synchronized (m_neuDavVerbindungAufbauen)
						{
	      			m_neuDavVerbindungAufbauen.notifyAll();
						}

	      		logger.info(FEHLERMELDUNG_VERBINDUNGDAV_ABGEBROCHEN);
	      	}
	      });

	      // Anmelden am DaV zum Senden bzw. Empfangen der Daten
	      
	      m_daVKommunikation.anmeldeDaV();
	
	      m_kernSystemGestartet = true;
	      
	      benachrichtigeListener();
	
	      // auf Datenverteiler-Verbindungsabbruch warten
	      
	  		synchronized (m_neuDavVerbindungAufbauen)
				{
	  			try
					{
						m_neuDavVerbindungAufbauen.wait();
						
						verbindungAufgebaut = false;
						
						m_kernSystemGestartet = false;
						
						loescheAlleProzessDatenRemote();
						
						m_daVKommunikation.allesAbmelden();

			      benachrichtigeListener();
						
						Thread.sleep(10000);
					} 
	  			catch (InterruptedException e)
					{
						e.printStackTrace();
					}
				}
    	}
    }
  }

  /**
   * Methode liefert die Information, ob das Kernsystem gestartet
   * ist und Informationen an des Kernsystem �bertragen werden k�nnen.
   */
  public boolean isKernSystemGestartet()
  {
    if (StartStoppApp.isStartStoppWirdBeendet())
      return false;
    
    return m_kernSystemGestartet;
  }
  
  /**
   * Methode setzt den Merker der signalisiert, dass das Kernsystem gestartet
   * wurde.
   * @param kernSystemGestartet Kernsystem gestartet
   */
  public void setKernSystemGestartet (boolean kernSystemGestartet)
  {
    m_kernSystemGestartet = kernSystemGestartet;   
  }
  
  /**
   * Methode bestimmt zu einem Prozess des lokalen Rechners die dazugeh�rende Pid
   * @param name Name des Prozess
   * @return Pid des Prozess
   */
  public String getPidByName (String name)
  {
    HashMap<String, ProzessDaten> copy = m_prozessDatenVerwaltung.getAlleProzessDaten();

    Iterator it = copy.entrySet().iterator();
    while (it.hasNext())
    {
      Map.Entry me = (Map.Entry)it.next();
      
      String id       = (String)       me.getKey();
      ProzessDaten pd = (ProzessDaten) me.getValue();

      String n = pd.getName();
      
//      System.out.println("id = " + id + " Name = " + n);
      
      if (name.equals( n ))
        return id;
    }

    return null;
  }

  /**
   * Methode bestimmt zu einem Prozess eines Remoterechners die dazugeh�rende Pid
   * @param name Name des Prozess
   * @param ipAdresse IP-Adresse des Rechners auf dem der Prozess l�uft
   * @return Pid des Prozess
   */
  public String getPidByNameRemote (String name, String ipAdresse)
  {
    HashMap<String, ProzessDaten> copy = m_prozessDatenVerwaltungRemote.getAlleProzessDaten();

    Iterator it = copy.entrySet().iterator();
    while (it.hasNext())
    {
      Map.Entry me = (Map.Entry)it.next();
      
      String id       = (String)       me.getKey();
      ProzessDaten pd = (ProzessDaten) me.getValue();
      
      String n   = pd.getName();
      String ip  = pd.getIpAdresse();
      
      if (name.equals( n ) && ipAdresse.equals( ip ))
        return id;
    }
    
//    System.err.println("getPidByNameRemote: Prozess \"" + name + "\" auf Rechner " + ipAdresse + " nicht gefunden !" );
    return null;
  }

  /**
   * Hilfmethode zum Umwandeln eines Longwertes in einen String
   * @param s String der umgewandelt werden soll
   * @return String als Longwert
   */
  public static long myLong (String s)
  {
    long l = 0l;
    
    try
    {
      l = Long.parseLong( s );
    }
    catch ( Exception e )
    {
    }                
    
    return l;
  }
  
  /**
   * Methode liefet die Prozessdaten aller Prozesse
   * @return Prozessdaten aller Prozesse
   */
  public List<ProzessDaten> getAlleProzessDaten ()
  {
    List<ProzessDaten> l = new ArrayList<ProzessDaten>();

    HashMap<String, ProzessDaten> copy = m_prozessDatenVerwaltung.getAlleProzessDaten();

    Iterator it = copy.entrySet().iterator();
    while (it.hasNext())
    {
      Map.Entry me = (Map.Entry)it.next();
      
      ProzessDaten pd = (ProzessDaten) me.getValue();
      
      l.add( pd );
    }
      
    return l;
  }

  /**
   * Methode liefet die Prozessdaten aller Remoteprozesse als Liste
   * @return Prozessdaten aller Remote Rrozesse
   */
  public List<ProzessDaten> getAlleProzessDatenRemote ()
  {
    List<ProzessDaten> l = new ArrayList<ProzessDaten>();
    
    HashMap<String, ProzessDaten> copy = m_prozessDatenVerwaltungRemote.getAlleProzessDaten();

    Iterator it = copy.entrySet().iterator();
    while (it.hasNext())
    {
      Map.Entry me = (Map.Entry)it.next();
      
      ProzessDaten pd = (ProzessDaten) me.getValue();
      
      l.add( pd );
    }
      
    return l;
  }

  /**
   * Methode zum Beenden des Startvorgangs. Alle noch nicht gestarteten Inkarnationen
   * werden nicht gestartet. �ber das Flag "beendeLaufendeInkarnationen" kann gesteuert
   * werden, ob bereits gestartete Inkarnationen beendet werden oder nicht
   * @param beendeLaufendeInkarnationen true: bereits gestartete Inkarnationen werden beendet,
   * false: bereits gestartet Inakrantionen laufen weiter.
   */

  public void beendeStartVorgang( boolean beendeLaufendeInkarnationen )
  {
    HashMap<String, ProzessDaten> copy = m_prozessDatenVerwaltung.getAlleProzessDaten();

    Iterator it = copy.entrySet().iterator();
    while (it.hasNext())
    {
      Map.Entry me = (Map.Entry)it.next();
      
      ProzessDaten pd = (ProzessDaten) me.getValue();

      if (pd.isKernsystem())
        continue;

      // Alle noch nicht gestarteten Prozesse anhalten
      
      pd.getInkarnation().beendeStart();

      // Gestartete Prozesse beenden
      
      if (beendeLaufendeInkarnationen)
      {
        stoppeAlleProzesse( true, false );
      }
    }
    
  }

  /**
   * Methode zum Beenden des Stoppvorgangs. Alle noch nicht gestoppten Inkarnationen
   * werden nicht gestoppt. 
   */

  public void beendeStoppVorgang (  )
  {
    HashMap<String, ProzessDaten> copy = m_prozessDatenVerwaltung.getAlleProzessDaten();
    
    Iterator it = copy.entrySet().iterator();
    while (it.hasNext())
    {
      Map.Entry me = (Map.Entry)it.next();
      
      ProzessDaten pd = (ProzessDaten) me.getValue();

      if (pd.isKernsystem())
        continue;

      // Alle noch nicht gestoppten Prozesse anhalten
      
      pd.getInkarnation().beendeStopp ();
    }
  }

  //---------------------------------------------------------------------------------------
  //---------------------------------------------------------------------------------------
  //
  // Methoden/Klassen zum gezielten Beenden aller Prozesse und Beenden der 
  // StartStopp Applikation
  //
  //---------------------------------------------------------------------------------------
  //---------------------------------------------------------------------------------------

  /**
   * Methode zum Stoppen aller Prozesse. Das Kernsystem wird als letztes in umgekehrter 
   * Reihenfolge beendet wie beim Start
   * @param beendeKernsystem soll Kensystem beendet werden oder nicht
   * @param stoppeSofort soll System sofort (d.h. ohne Warten auf Erf�llung der Stoppbedingung)
   * beendet werden.
   */

  public void stoppeAlleProzesse ( boolean beendeKernsystem, boolean stoppeSofort )
  {
    if (m_threadBeendeStartStopp != null)
    {
      // Thread l�uft schon, kein Eingreifen notwendig
    }
    else
    {    
      m_threadBeendeStartStopp = new ThreadBeendeStartStopp (0l, beendeKernsystem, false, stoppeSofort, false);
      m_threadBeendeStartStopp.start();
      m_threadBeendeStartStopp.setName( "stoppeAlleProzesse" );
    }
  }
  
  /**
   * gezieltes Beenden aller gestarteten Prozesse und Beenden der StartStopp Applikation
   * @param absender Absender Ausl�ser der Aktivit�t
   * @param stoppeSofort soll System sofort (d.h. ohne Warten auf Erf�llung der Stoppbedingung)
   * beendet werden.
   */
  public void beendeStartStoppApplikation (long absender, boolean stoppeSofort)
  {
    if (m_threadBeendeStartStopp != null)
    {
      m_threadBeendeStartStopp.setBeendeApplikation ( true );
      m_threadBeendeStartStopp.setBeendeKernsystem  ( true );
    }
    else
    {    
      m_threadBeendeStartStopp = new ThreadBeendeStartStopp (absender, true, true, stoppeSofort, true);
      m_threadBeendeStartStopp.start();
      m_threadBeendeStartStopp.setName( "beendeStartStopp Applikation" );
      
    }
  }

  /**
   * Interne Klasse zum Beenden der StartStopp Applikation
   */

  private class ThreadBeendeStartStopp extends Thread
  {
    private long m_absender = 0l;

    private boolean m_beendeKernsystem  = false; 
    private boolean m_beendeApplikation = false;
    private boolean m_beendeSofort      = false;
    private boolean m_abmeldeDaV        = false;
    

    /**
     * Konstruktor der Klasse
     * @param absender Absender
     * @param beendeKs Soll Kernsystem beendet werden ?
     * @param beendeApplikation Soll StartStopp Applikation beendet werden ?
     * @param abmeldeDaV Soll Verbindung zum DaV abgemedet werden ?
     */
    public ThreadBeendeStartStopp (long absender, boolean beendeKs, boolean beendeApplikation, boolean beendeSofort, boolean abmeldeDaV)
    {
      m_absender = absender;
      
      m_beendeKernsystem  = beendeKs;
      m_beendeApplikation = beendeApplikation;
      m_beendeSofort      = beendeSofort;
      m_abmeldeDaV        = abmeldeDaV;
    }
    
    /**
     * Run Methode des Threads
     */
    public void run ()
    {
      m_global = GlobaleDaten.getInstanz();
      
      if (m_absender == -1)
        logger.info( "StartStopp Applikation durch Benutzer beendet !");
      else
        logger.info( "StartStopp Applikation beendet !");

      beendeApplikationen( m_absender, m_beendeSofort);

      // Abmelden am DaV
      
      if (m_abmeldeDaV)
        DaVKommunikation.getInstanz().trenneDaV();

      if (m_beendeKernsystem)
      {
        // Wenn Kernsystem gestartet wurde, dann muss auch hier eine Abmeldung erfolgen,
        // unabh�ngig von der Variable m_abmeldeDaV
        
        if (m_global.getKernSystem().size() > 0)
        {
          DaVKommunikation.getInstanz().trenneDaV();
          
          // Kernsystem beenden
    
          beendeKernsystem();
        }
      }
  
      // StartStopp Applikation beenden
      
      if (m_beendeApplikation)
      {
        logger.info("StartStopp Applikation beendet");
        System.exit( 0 );
      }
      
      logger.info("Prozesse beendet");
      
      m_threadBeendeStartStopp = null;
    }

    /**
     * @param m_beendeApplikation setzt die Variable beendeApplikation
     */
    public void setBeendeApplikation( boolean m_beendeApplikation )
    {
      this.m_beendeApplikation = m_beendeApplikation;
    }

    /**
     * @param m_beendeKernsystem setzt die Variable beendeKernsystem
     */
    public void setBeendeKernsystem( boolean m_beendeKernsystem )
    {
      this.m_beendeKernsystem = m_beendeKernsystem;
    }
    
    /**
     * Methode zum Stoppen aller Applikationen die nicht zum Kensystem geh�ren
     * @param absender Absender Ausl�ser der Aktivit�t
     * @param beendeSofort true: sofort beenden, false: warte auf Erf�llung der Stoppbedingung
     */
    private void beendeApplikationen (long absender, boolean beendeSofort)
    {
      HashMap<String, ProzessDaten> copy = m_prozessDatenVerwaltung.getAlleProzessDaten();
      
      Iterator it = copy.entrySet().iterator();
      while (it.hasNext())
      {
        Map.Entry me = (Map.Entry)it.next();
        
        ProzessDaten pd = (ProzessDaten) me.getValue();
    
        if (pd.isKernsystem())
          continue;
    
        // Prozesse beenden
        
        stoppeProzess( pd.getProzessId(), absender, beendeSofort );
      }

      // Warten auf Ende aller Inkarnationen
       
      int counter;
      
      do
      {
        counter = 0;
    
        it = copy.entrySet().iterator();
        while (it.hasNext())
        {
          Map.Entry me = (Map.Entry)it.next();
        
          ProzessDaten pd = (ProzessDaten) me.getValue();
    
          if (pd.isKernsystem())
            continue;
    
          if (pd.isAktiv())
          {
            logger.info("Warten auf Ende Inkarnation " + pd.getName());
            counter++;
          }
        }
    
        try
        {
          sleep (1000);
        }
        catch (InterruptedException e)
        {
          e.printStackTrace();
        }
    
      } while (counter > 0);
    }
    
    /**
     * Methode zum Beenden des Kernsystems.
     * Es wird nur die erste eingetragene Applikation beendet. Diese muss der
     * Datenverteiler sein. Sobald dieser beendet ist, beenden sich die anderen
     * Applikationen des Kernsystems automatisch durch den Verlust der 
     * Datenverteilerverbindung.
     */
    
    private void beendeKernsystem ()
    {
//      if (!m_kernSystemGestartet)
//        return;
      
      m_global = GlobaleDaten.getInstanz();
      
      if (m_global.getKernSystem().size() > 0)
      {
        Kernsystem ks = m_global.getKernSystem().get(0);
        String applikation = ks.getApplikation();
        
        stoppeProzess(getPidByName(applikation));

        try
        {
          sleep(10000);
        }
        catch (InterruptedException e)
        {
        }      

//        for (int i = 0; i < m_global.getKernSystem().size(); i++)
//        {
//          String ks = m_global.getKernSystem().get(i);
//          
//          stoppeProzess (getPidByName(ks));
//          
//          try
//          {
//            sleep(2000);
//          }
//          catch (InterruptedException e)
//          {
//          }      
//        }
      }
    }
  }

  /**
   * Methode liefert den Plausibilit�tszustand des Inkarantionsanteils der StartStopp.xml Datei
   * @return true: Inkarnationsanteil der StartStopp.xml Datei ist sematisch korrekt, sonst false
   */
  public boolean isInkarntionsTeilPlausibel ()
  {
    return !m_fehlerInkarantion;
  }
  
  /**
   * Methode bestimmt die Prozessdaten des Prozesses der durch die StartStopp Block Id, den
   * Alias Namen eines Rechners und einem Prozessnamen beschrieben ist.
   * @param startStoppBlockId Id des Startstopp Blocks in dem der Rechner Alias definiert wurde
   * @param rechnerAlias Alias Bezeichnung des Rechners
   * @param prozessName Prozessname
   * @return Prozessdaten des Prozesses oder null falls keine Prozessdaten f�r diesen Prozess
   * existieren.
   */
  public ProzessDaten getProzessDaten (String startStoppBlockId, String rechnerAlias, String prozessName)
  {
    ProzessDaten d = null;
    
    if (prozessName.equals( "" ))  // Platzhalter
      return d;

    String ip = GlobaleDaten.getInstanz().getIpAdresse (startStoppBlockId, rechnerAlias);

    if (ip == null)
      return d;

    if (ip.equals( StartStoppApp.getRechnerAdresse()))
    {
      String pid = getPidByName (prozessName);
      
//      System.out.println("Pid = " + pid);
      
      d = getProzessDaten( pid );
    }
    else
    {
      String pid = getPidByNameRemote (prozessName, ip);
      
      d = getProzessDatenRemote ( pid );
    }

    return d;
  }

  /**
   * Methode zum Entfernen der Prozesseintr�ge, die auf einem anderen Rechner
   * gestartet wurden. Der Rechner wird �ber seine TCP/IP Adresse angesprochen.
   * @param ip IP-Adresse des Remoterechners
   */
  public void loescheProzessDatenRemote (String ip)
  {
    List<String> liste = new ArrayList<String> ();
    
    HashMap<String, ProzessDaten> copy = m_prozessDatenVerwaltungRemote.getAlleProzessDaten();
    
    // Feststellen welche Prozesse zu diesem Remoterechner geh�ren
    
    for (int i=0; i<copy.size(); ++i)
    {
      Iterator it = copy.entrySet().iterator();
      while (it.hasNext())
      {
        Map.Entry me = (Map.Entry) it.next();
      
        String key = (String) me.getKey();
        ProzessDaten pd = (ProzessDaten) me.getValue();
        
        if (pd.getIpAdresse().equals( ip ))
          liste.add( key );          
      }
    }
  
    // Prozesse l�schen
    
    for (int i=0; i<liste.size(); ++i)
      m_prozessDatenVerwaltungRemote.removeProzessDaten( liste.get( i ) );
  }

  /**
   * Methode zum Entfernen aller Prozesseintr�ge, die auf anderen Rechnern
   * gestartet wurden.
   */
  public void loescheAlleProzessDatenRemote()
  {
    List<String> liste = new ArrayList<String> ();
    
    HashMap<String, ProzessDaten> copy = m_prozessDatenVerwaltungRemote.getAlleProzessDaten();
    
    // Feststellen welche Prozesse zu diesem Remoterechner geh�ren
    
    for (int i=0; i<copy.size(); ++i)
    {
      Iterator it = copy.entrySet().iterator();
      while (it.hasNext())
      {
        Map.Entry me = (Map.Entry) it.next();
      
        String key = (String) me.getKey();
        ProzessDaten pd = (ProzessDaten) me.getValue();

        liste.add( key );          
      }
    }
  
    // Prozesse l�schen
    
    for (int i=0; i<liste.size(); ++i)
      m_prozessDatenVerwaltungRemote.removeProzessDaten( liste.get( i ) );
  }

  //----------------------------------------------------------------------------------------------
  // Listener Funktionalit�ten
  //----------------------------------------------------------------------------------------------

  /**
   * Methode zum Hinzuf�gen eines Listeners
   * @param listener Listener der hinzugef�gt werden soll
   */
  public void addListener (IKernsystemListener listener)
  {
    m_angemeldeteListener.add( listener );
  }
 
  /**
   * Methode zum Entfernen eines Listeners
   * @param listener Listener der entfernt werden soll
   */
  public void removeListener (IKernsystemListener listener)
  {
    m_angemeldeteListener.remove( listener );
  }
 
  /**
   * Methode mit der die Klasse alle bei ihr angemeldeten Listener
   * �ber die �nderung der Daten informiert.
   */
  private void benachrichtigeListener ()
  {
    // neues Ereignis erzeugen
  
    KernsystemEreignis e = new KernsystemEreignis (this);
    
    e.setGestartet( m_kernSystemGestartet );
   
    // zu �bergebende Daten eintragen
  
    Iterator<IKernsystemListener> it = m_angemeldeteListener.iterator();
    while (it.hasNext())
    {
      IKernsystemListener l = it.next();
      l.exec(e);
    }
  }

  /**
   * Methode zum Hinzuf�gen der Prozessdaten eines Remoteprozesses
   * @param pd Prozessdaten des Remoteprozesses
   */
  public void addProzessDatenRemote( ProzessDaten pd )
  {
    m_prozessDatenVerwaltungRemote.addProzessDaten( pd );
  }
  
  /**
   * Sleep Funktionalit�t
   * @param ms Zeit in Millsekunden
   */
  private void mySleep (long ms)
  {
    try
    {
      Thread.sleep (ms);
    }
    catch (InterruptedException e)
    {
    }

  }
}