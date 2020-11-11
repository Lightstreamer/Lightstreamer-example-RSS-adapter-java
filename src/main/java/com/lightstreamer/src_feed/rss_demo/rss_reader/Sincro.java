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

import java.io.IOException;
import java.io.InputStream;
import java.util.GregorianCalendar;

/**
 * Class Sincro.
 *
 * @author              ...
 * last author:         $Author: Sfabiano $
 * @version             $Revision: 23395 $
 * last modified:       $Modtime: 20/03/06 18:11 $
 * last check-in:       $Date: 20/03/06 18:15 $
 */
public class Sincro {

    // If true then a thread for this feed is running.
    private boolean flag;
    private GregorianCalendar settingDate;
    private boolean runningIsSnap;
    private boolean snapHasBeenSent;

    /** Field $objectName$. */
    private InputStream stream4All;

    /**
     *
     */
    public Sincro() {
        reset();
    }

    public void reset() {
        setFlag(false);
        setSettingDate(null);
        setRunningIsSnap(false);
        setSnapHasBeenSent(false);
    }
    
    /**
     * Method set.
     *
     * @param  snapShot  ...
     * @param  set  ...
     * @return ...
     */
    public synchronized boolean set(boolean snapShot, GregorianCalendar set) {
        boolean oldIsSnap = isRunningIsSnap();
        if (isFlag()) {
            setSettingDate(set);

            boolean retValue = false;
            if (oldIsSnap) {
                retValue = true;
            }
            setRunningIsSnap(retValue);
            return retValue;
        } else {
            if (oldIsSnap) {
                setSnapHasBeenSent(true);
            }
            setFlag(true);
            setSettingDate(set);
            setRunningIsSnap(snapShot && !isSnapHasBeenSent());
            return false;
        }
    }

    /**
     * Method get.
     *
     * @param  setted  ...
     * @return ...
     */
    public synchronized boolean get(GregorianCalendar setted) {
        if (imAlive(setted)) {
            setFlag(false);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Method imAlive.
     *
     * @param  setted  ...
     * @return ...
     */
    public synchronized boolean imAlive(GregorianCalendar setted) {
        GregorianCalendar lastSet = getSettingDate();
        if (lastSet.equals(setted)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Method closeStream.
     */
    public synchronized void closeStream() {
        if (stream4All == null) {
            return;
        }
        try {
            stream4All.close();
        } catch (IOException ecc) {

        }
        stream4All = null;

    }

    /**
     * Method kill.
     *
     * @param  timeout  ...
     */
    public synchronized void kill(int timeout) {

        GregorianCalendar startTime = getSettingDate();

        if (startTime != null) {
            GregorianCalendar myTime = null;

            myTime = (GregorianCalendar) startTime.clone();
            myTime.add(GregorianCalendar.SECOND, timeout);

            GregorianCalendar now = null;
            now = new GregorianCalendar();

            if (myTime.before(now)) {
                closeStream();
            }

        }

    }

    /**
     * @return Returns the flag.
     */
    private boolean isFlag() {
        return flag;
    }

    /**
     * @param flag The flag to set.
     */
    private void setFlag(boolean flag) {
        this.flag = flag;
    }

    /**
     * @return Returns the runningIsSnap.
     */
    private boolean isRunningIsSnap() {
        return runningIsSnap;
    }

    /**
     * @param runningIsSnap The runningIsSnap to set.
     */
    private void setRunningIsSnap(boolean runningIsSnap) {
        this.runningIsSnap = runningIsSnap;
    }

    /**
     * @return Returns the settingDate.
     */
    public GregorianCalendar getSettingDate() {
        return settingDate;
    }

    /**
     * @param settingDate The settingDate to set.
     */
    private void setSettingDate(GregorianCalendar settingDate) {
        this.settingDate = settingDate;
    }

    /**
     * @return Returns the snapHasBeenSent.
     */
    public boolean isSnapHasBeenSent() {
        return snapHasBeenSent;
    }

    /**
     * @param snapHasBeenSent The snapHasBeenSent to set.
     */
    private void setSnapHasBeenSent(boolean snapHasBeenSent) {
        this.snapHasBeenSent = snapHasBeenSent;
    }

    /**
     * @param stream4All The stream4All to set.
     */
    public void setStream4All(InputStream stream4All) {
        this.stream4All = stream4All;
    }

    /**
     * @return Returns the stream4All.
     */
    public InputStream getStream4All() {
        return stream4All;
    }
}


/*--- Formatted in Lightstreamer Java Convention Style on 2004-12-10 ---*/
