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

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.lightstreamer.interfaces.data.IndexedItemEvent;

/**
 * Class ErrorProducer.
 *
 * @author              ...
 * last author:         $Author: Sfabiano $
 * @version             $Revision: 24498 $
 * last modified:       $Modtime: 20/03/06 18:13 $
 * last check-in:       $Date: 20/03/06 18:15 $
 */
class ErrorProducer extends Thread {

    private static Logger logger =
        Logger.getLogger("LS_demos_Logger.NewsAggregator.feed");
    private final RSSReaderProvider provider;
    //riferimento al produttore di notizie (ne viene sfruttato il semaforo)
    private RssProducer myRif;
    private boolean snap;
    private Object itemKey;
    private boolean push;
    private boolean rifSnap = true;

    //Constructor: get a RssProducer to be able to use the semaphore,
    // an itemKey for the recognition of the item, a waitingtime in milliseconds 
    // indicating the # of seconds to wait at every polling cycle on the semaphore.

    /**
     * Constructor ErrorProducer.
     *
     * @param  provider  ...
     * @param  itemKey  ...
     * @param  mainTh  ...
     */
    public ErrorProducer(RSSReaderProvider provider, Object itemKey,
                         String mainTh, boolean push) {
        super("Control Thread for RSS @ " + mainTh);

        this.provider = provider;
        this.itemKey = itemKey;
        this.push = push;
        setSnap(true);

    }

    /**
     * Method run.
     */
    public void run() {

        /*if (myRif == null) {
            return;
        }*/

        if (myRif.isReading()) {
            if (this.snap && this.push) {
                String title = myRif.getTitle();
                sendToServer("Loading...", title);
                setSnap(false);
            }
            boolean rifSnapSent = myRif.produceEvent(this.rifSnap);
            if (rifSnapSent) {
                myRif.confirmSnapshot();
                this.rifSnap = false;
            }
            
        }
        produceEvent();

    }

    // Perform the test on the semaphore and if it gets OK, sends the message
    // as event for the item
    private void produceEvent() {
        String mex = myRif.testIfReadyError();
        String title = myRif.getTitle();
        if (this.push) {
            sendToServer(mex, title);
        }
        logger.log(Level.DEBUG, "INCOMING DATA FROM --> " + myRif.getURL());

    }

    private void sendToServer(String mex, String title) {
        final String[] event = new String[2];
        if (!(mex.equals("")) && (mex != null)) {
            event[1] = mex;
        } else {
            event[1] = "";
        }
        if (!(title.equals("")) && (title != null)) {
            event[0] = title;

            this.provider.getListener().smartUpdate(itemKey,
                                                    new IndexedItemEvent() {
                public int getMaximumIndex() {
                    return 1;
                }

                public int getIndex(String name) {
                    if (name.equals("controllo")) {
                        return 1;
                    } else {
                        return 0;
                    }
                }

                public String getName(int index) {
                    if (index == 1) {
                        return "controllo";
                    } else {
                        return "titolo";
                    }
                }

                public Object getValue(int index) {
                    return event[index];
                }

            }, false);
        }
    }

    /**
     * Method close.
     */
    public void close() {
        //interrupt();
        myRif = null;
    }

    /**
     * @return Returns the myRif.
     */
    public RssProducer getMyRif() {
        return myRif;
    }

    /**
     * @param myRif The myRif to set.
     */
    public void setMyRif(RssProducer myRif) {
        this.myRif = myRif;
    }

    /**
     * @param snap The snap to set.
     */
    public synchronized void setSnap(boolean snap) {
        this.snap = snap;
    }

    public void setPush(boolean push) {
        this.push = push;
    }
    /**
     * @param itemKey The itemKey to set.
     */
    public void setItemKey(Object itemKey) {
        this.itemKey = itemKey;
    }
    /**
     * @return Returns the push.
     */
    public boolean isPush() {
        return push;
    }
}


/*--- Formatted in Lightstreamer Java Convention Style on 2004-12-10 ---*/
