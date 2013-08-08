/*
 *
 * Copyright 2013 Weswit s.r.l.
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

/**
 * Class TimeoutterTask.
 *
 * @author              ...
 * last author:         $Author: Sfabiano $
 * @version             $Revision: 23395 $
 * last modified:       $Modtime: 20/03/06 18:11 $
 * last check-in:       $Date: 20/03/06 18:15 $
 */
public class TimeoutterTask extends Thread {

    /** Field $objectName$. */
    private int timeout;

    /** Field $objectName$. */
    private Sincro synchronizer;

    /**
     * Constructor TimeoutterTask.
     *
     * @param  syn  ...
     * @param  threadTimeout  ...
     */
    public TimeoutterTask(Sincro syn, int threadTimeout) {
        timeout = threadTimeout;
        synchronizer = syn;
    }

    /**
     * Method run.
     */
    public void run() {
        synchronizer.kill(timeout);

    }

}


/*--- Formatted in Lightstreamer Java Convention Style on 2004-12-10 ---*/
