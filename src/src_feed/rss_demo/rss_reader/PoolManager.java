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

import java.util.TimerTask;

/**
 * Class PoolManager.
 *
 * @author              ...
 * last author:         $Author: Sfabiano $
 * @version             $Revision: 23395 $
 * last modified:       $Modtime: 20/03/06 18:11 $
 * last check-in:       $Date: 20/03/06 18:15 $
 */
public class PoolManager extends TimerTask {
    private Runnable inPool;
    private ThreadPool pool;
    private String name;

    /**
     * Constructor PoolManager.
     *
     * @param  inPool  ...
     * @param  pool  ...
     * @param  name  ...
     */
    public PoolManager(Runnable inPool, ThreadPool pool, String name) {
        this.inPool = inPool;
        this.pool = pool;
        this.name = name;
    }

    /**
     * Method run.
     */
    public void run() {
        if (inPool != null) {
            pool.execute(inPool);
        }
    }
}


/*--- Formatted in Lightstreamer Java Convention Style on 2004-12-10 ---*/
