/*
 *
 * Copyright (c) Lightstreamer Srl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package rss_demo.rss_reader;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashMap;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.lightstreamer.interfaces.data.IndexedItemEvent;

import de.nava.informa.core.ChannelIF;
import de.nava.informa.core.ItemIF;
import de.nava.informa.core.ParseException;
import de.nava.informa.impl.basic.ChannelBuilder;
import de.nava.informa.parsers.FeedParser;

/**
 * Class RssProducer.
 *
 * @author              ...
 * last author:         $Author: Sfabiano $
 * @version             $Revision: 24498 $
 * last modified:       $Modtime: 20/03/06 18:13 $
 * last check-in:       $Date: 20/03/06 18:15 $
 */
class RssProducer extends Thread {

    /** Field $objectName$. */
    final static String emptyUrl = "Can't accept an empty URL";

    /** Field $objectName$. */
    final static String parseError = "Error while parsing the RSS file: ";

    /** Field $objectName$. */
    final static String readError = "Error reading file: ";

    /** Field $objectName$. */
    final static String notAvailable = "Not available";

    /** Field $objectName$. */
    final static String readChanErr = "Error while reading the RSS channel: ";

    /** Field $objectName$. */
    final static String tooBig =
        "The file you've selected is too big to be read";

    /** Field $objectName$. */
    final static String dontReadMex =
        "Sorry, maximum number of concurrent feeds exceeded";

    /** Field $objectName$. */
    final static String timeOutMex = "Sorry, timeout reached";

    /** Field $objectName$. */
    final static String malformedURL = "Error parsing URL: ";

    private static final String[] names = new String[]{"titolo", "testo",
                                                       "link", "creator",
                                                       "data", "feed", "titoLink"};

    private static final HashMap codes = new HashMap();

    static {
        for (int code = 0; code < names.length; code++) {
            codes.put(names[code], new Integer(code));
        }
    }

    private final RSSReaderProvider provider;

    private String URL;
    private String lastNews;
    private int snapLength;
    private String title;
    private boolean isReadyError;
    private String theError;
    private int maxBytes;
    private boolean dontRead;
    private static Logger logger =
        Logger.getLogger("LS_demos_Logger.NewsAggregator.feed");
    private boolean isActive;
    private boolean push = false;

    /** Field $objectName$. */
    private Sincro synchronizer;
    private int substituteSnapLength;

    /**
     * Constructor RssProducer.
     *
     * @param  provider  ...
     * @param  itemName  ...
     * @param  snapLength  ...
     * @param  maxBytes  ...
     * @param  dontRead  ...
     * @param  substituteSnapLength  ...
     */
    public RssProducer(RSSReaderProvider provider, String itemName,
                       int snapLength, int maxBytes, boolean dontRead,
                       int substituteSnapLength, boolean push) {

        synchronizer = new Sincro();
        this.push = push;
        this.dontRead = dontRead;
        this.provider = provider;
        this.maxBytes = maxBytes;
        this.URL = itemName;
        this.substituteSnapLength = substituteSnapLength;

        String falseTitle = itemName;

        URL myIA = null;
        try {
            myIA = new URL(falseTitle);
        } catch (MalformedURLException e) {
            setErrorMessage(malformedURL + e.getMessage());
        }
        if (myIA != null) {
            falseTitle = myIA.getHost();
        }
        setTitle(falseTitle);
        if (dontRead) {
            setActivity(false);
            setErrorMessage(dontReadMex);
            logger.log(Level.INFO,
                       "Feed queued for max feeds number reached: " + getURL());
        } else {
            setActivity(true);
            //setSemaforo("File "+itemName);
            logger.log(Level.INFO, "Feed launched: " + getURL());
        }

        if (URL.length() <= 0) {
            setErrorMessage(emptyUrl);
        }

        this.snapLength = snapLength;

        setLastNews("");

    }

    // Set last news value.
    private synchronized void setLastNews(String news) {
        this.lastNews = news;
    }

    // Returns the last news value.
    private synchronized String getLastNews() {
        return this.lastNews;
    }

    // Returns the URL value.
    /**
     * Method getURL.
     * @return ...
     */
    public String getURL() {
        return this.URL;
    }

    //Set isActive attribute.

    /**
     * Method setAttivita.
     *
     * @param  value  ...
     */
    public synchronized void setActivity(boolean value) {
        this.isActive = value;
    }

    // Returns the number of news.
    private int getIdAlreadyHave(HashMap[] ridden) {
        boolean exit = false;
        if (getLastNews().equals("")) {
            return ridden.length;
        } else {
            int j = 0;
            while ((exit == false) && (j < ridden.length)) {
                String confronto = ridden[j].get("titolo").toString();
                if (confronto.equals(getLastNews())) {
                    exit = true;
                } else {
                    j++;
                }
            }
            return j;
        }
    }

    // Reads only unread elements. 
    private HashMap[] readNewNews(boolean isSnapshot) {
        HashMap[] ridden = new HashMap[0];
        GregorianCalendar myDate = new GregorianCalendar();
        boolean snap = synchronizer.set(isSnapshot, myDate);
        try {
            ridden = read(myDate);
        } catch (ProducerException error) {
            setErrorMessage(error.getMessage());
            ridden = null;
        }

        HashMap[] nuove = new HashMap[0];

        boolean timeOk = synchronizer.get(myDate);
        if (!timeOk) {
            return null;
        }

        int x = 0;

        if (ridden == null) {
            return null;
        }

        x = getIdAlreadyHave(ridden);

        if (snap) {
            x = substituteSnapLength;
        }

        if (snapLength < x) {
            x = snapLength;
        }

        /*if ((snapLength<x)&&(isSnapshot))
                nuove=new HashMap[snapLength];
        else*/
        nuove = new HashMap[x];

        for (int i = 0; i < nuove.length; i++) {
            nuove[i] = ridden[i];
        }

        if (x > 0 && this.push) {
            setLastNews(ridden[0].get("titolo").toString());
        }
        return nuove;
    }

    // Reads the RSS file remotely.
    private HashMap[] read(GregorianCalendar startTime)
            throws ProducerException {

        ChannelIF myChan = null;
        ChannelBuilder chBuild = new ChannelBuilder();
        Collection news = null;

        try {

            URL myURL = new URL(this.URL);

            if (synchronizer.getStream4All() != null) {
                synchronizer.closeStream();
            }

            try {
                synchronizer.setStream4All(myURL.openStream());
            } catch (IOException error) {
                setErrorMessage(readError + error.getMessage());
                return null;
            }
            BufferedInputStream buffStream =
                new BufferedInputStream(synchronizer.getStream4All());
            buffStream.mark(maxBytes);

            int readNumber = 0;
            int control = 1;
            while ((readNumber < maxBytes) && (control != -1)) {
                if (Thread.interrupted()) {
                    setErrorMessage(timeOutMex);
                    return null;
                }
                if (!synchronizer.imAlive(startTime)) {
                    return null;
                }

                int av = buffStream.available();
                if (av == 0) {
                    av = 1024;
                }
                byte[] bArray = new byte[av];
                try {
                    control = buffStream.read(bArray, 0, av);
                } catch (IOException ecc) {
                    logger.log(Level.INFO, "File reading interrupted");
                    setErrorMessage(timeOutMex);
                    return null;
                } catch (Throwable ecc) {
                    logger.log(Level.INFO, "File reading interrupted");
                    setErrorMessage(timeOutMex);
                    return null;
                }
                if (control != -1) {
                    readNumber += control;
                }

                if (readNumber >= maxBytes) {
                    throw new ProducerException(tooBig);
                }

                if (synchronizer.getStream4All() == null) {
                    setErrorMessage(timeOutMex);
                    return null;
                }

            }

            if (readNumber >= maxBytes) {
                throw new ProducerException(tooBig);
            }
            //if (buffStream.read()!=-1)

            buffStream.reset();

            myChan = FeedParser.parse(chBuild, buffStream);
            news = myChan.getItems();
        } catch (ParseException perror) {
            throw new ProducerException(parseError + " " + perror.getMessage());
        } catch (Exception error) {
            throw new ProducerException(readError + " " + error.getMessage());
        }

        String myTitle;
        try {
            myTitle = myChan.getTitle();
        } catch (Exception readingTitle) {
            myTitle = notAvailable;
        }

        if (myTitle.length() > 0) {
            setTitle(myTitle);
        }

        HashMap[] readNews = new HashMap[0];
        Object[] chan = news.toArray();
        readNews = new HashMap[chan.length];
        try {
            for (int j = 0; j < chan.length; j++) {
                readNews[j] = new HashMap();
                ItemIF tmp = (ItemIF) chan[j];
                readOneNews(readNews[j], tmp, myTitle);
            }
        } catch (Exception error) {
            throw new ProducerException(readChanErr + " " + error.getMessage());
        }

        return readNews;
    }

    // Copies a news from the channel to the Hashmap.
    private void readOneNews(HashMap dest, ItemIF data, String feed) {
        //x=dati.getComments().toString();
        //x=dati.getEnclosure().toString();
        //x=dati.getFound().toString();
        //x=dati.getSource().toString();
        //dati.getAttributeValue("path",stringa)
        String titolo = "";
        dest.put("feed", feed);
        try {
            //dest.put("titolo", leaveTags(data.getTitle()));
            titolo = data.getTitle();
            dest.put("titolo", titolo);
        } catch (Exception empty) {
            dest.put("titolo", notAvailable);
        }
        try {
            //dest.put("testo", leaveTags(data.getDescription()));
            dest.put("testo", data.getDescription());
        } catch (Exception empty) {
            dest.put("testo", notAvailable);
        }
        try {
            //dest.put("link", leaveTags((data.getLink()).toString()));
            String linkTag = "<a href='" + (data.getLink()).toString() + "' target='_blank'>" + (data.getLink()).toString() + "</a>";
            dest.put("link", linkTag);
        } catch (Exception empty) {
            dest.put("link", notAvailable);
        }
        try {
            if (titolo == "") {
                titolo = notAvailable;
            }
            String linkTag = "<a href='" + (data.getLink()).toString() + "' target='_blank'>" + titolo + "</a>";
            dest.put("titoLink", linkTag);
        } catch (Exception empty) {
            dest.put("titoLink", titolo);
        }
        try {
            dest.put("creator", (data.getCreator()));
        } catch (Exception empty) {
            dest.put("creator", notAvailable);
        }
        try {
            dest.put("data", (data.getDate()).toString());
        } catch (Exception empty) {
            dest.put("data", notAvailable);
        }
    }

    // Produces the items to send.

    /**
     * Method produceEvent.
     *
     * @param  isSnapshot  ...
     * @return ...
     */
    public boolean produceEvent(boolean isSnapshot) {
        HashMap[] newNews;

        newNews = readNewNews(isSnapshot);
        if (!isSnapshot) {
            if (!synchronizer.isSnapHasBeenSent()) {
                isSnapshot = true;
            }
        }

        if (newNews != null) {
            setErrorMessage("");
            if (this.push) {
	            for (int j = newNews.length - 1; j >= 0; j--) {
	
	                final HashMap event = copyValues(newNews[j]);
	
	                this.provider.getListener().update(getURL(),
	                                                   new IndexedItemEvent() {
	                    public int getMaximumIndex() {
	                        return names.length - 1;
	                    }
	
	                    public int getIndex(String name) {
	                        Object keyObject = codes.get(name);
	                        if (keyObject == null) {
	                            return -1;
	                        } else {
	                            return ((Integer) keyObject).intValue();
	                        }
	                    }
	
	                    public String getName(int index) {
	                        if (event.get(names[index]) == null) {
	                            return null;
	                        } else {
	                            return names[index];
	                        }
	                    }
	
	                    public Object getValue(int index) {
	                        return event.get(names[index]);
	                    }
	
	                }, isSnapshot);
	
	                logger.log(Level.DEBUG,
	                           "INCOMING DATA --> RSS = " + getURL() + " event = "
	                           + event);
	            }
	            
	            return isSnapshot;
            }
        }

        return false;
    }

    /**
     * Method confirmSnapshot.
     *
     */
    public void confirmSnapshot() {
        this.provider.getListener().endOfSnapshot(getURL());
    }

    private HashMap copyValues(HashMap valori) {
        final HashMap newz = new HashMap();
        copyValue(newz, "feed", valori);
        copyValue(newz, "titolo", valori);
        copyValue(newz, "testo", valori);
        copyValue(newz, "link", valori);
        copyValue(newz, "creator", valori);
        copyValue(newz, "data", valori);
        copyValue(newz, "titoLink", valori);
        return newz;
    }

    private void copyValue(HashMap map, String field, HashMap orig) {
        try {
            map.put(field, orig.get(field).toString());
        } catch (Exception e) {
            map.put(field, notAvailable);
        }

    }

    private synchronized void setTitle(String title) {
        this.title = title;
    }

    /**
     * Method getTitle.
     * @return ...
     */
    public synchronized String getTitle() {
        return title;
    }

    private synchronized void setErrorMessage(String mex) {
        if (mex == null) {
            return;
        }
        isReadyError = true;
        theError = mex;
    }

    /**
     * Method testSemaforo.
     * @return ...
     */
    public synchronized String testIfReadyError() {
        if (dontRead) {
            return theError;
        }
        if (isReadyError == true) {
            isReadyError = false;
            return theError;
        } else {
            return "";
        }
    }

    /**
     * Method enableReading.
     */
    public synchronized void enableReading() {
        this.dontRead = false;
        setActivity(true);
    }

    //

    /**
     * Method isReading.
     * @return ...
     */
    public synchronized boolean isReading() {
        return (!dontRead);
    }

    /**
     * Method attivo.
     * @return ...
     */
    public boolean active() {
        return isActive;
    }

    //chiude il thread

    /**
     * Method close.
     */
    public void close() {

        setActivity(false);
    }

    /**
     * @return Returns the synchronizer.
     */
    public Sincro getSynchronizer() {
        return synchronizer;
    }
    /**
     * @param push The push to set.
     */
    public void setPush(boolean push) {
        this.push = push;
        this.setLastNews("");
        this.synchronizer.reset();
    }
    /**
     * @return Returns the push.
     */
    public boolean isPush() {
        return push;
    }
}


/*--- Formatted in Lightstreamer Java Convention Style on 2004-12-10 ---*/
