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

//import it.soltec.mfi.bdr.msf.msf_adapter.MsfAdapter;
//import it.soltec.mfi.log.LoggerInterface;

import java.util.GregorianCalendar;
import java.util.LinkedList;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Class ThreadPool.
 *
 * @author              Dario Crivelli
 * last author:         $Author: Dcrivel $
 * @version             $Revision: 27770 $
 * last modified:       $Modtime: 5/06/07 11.23 $
 * last check-in:       $Date: 5/06/07 11.26 $
 */
public class ThreadPool {

    /**
     * Class PoolException.
     *
     * @author          ...
     * last author:     $Author: Dcrivel $
     * @version         $Revision: 27770 $
     * last modified:   $Modtime: 5/06/07 11.23 $
     * last check-in:   $Date: 5/06/07 11.26 $
     */
    public static class PoolException extends Exception {

        /**
		 * 
		 */
		private static final long serialVersionUID = 4228116588283050453L;

		/**
         * @param message
         */
        public PoolException(String message) {
            super(message);
        }

    }

    private static Logger logger =
        Logger.getLogger("LS_demos_Logger.NewsAggregator.ThreadPool");
    private String tNames;
    private int waiting_threads;

    private int total_threads;

    /*  */
    private int active_threads;

    /*  */
    private int notifications;

    /*  */
    private int queued;
    private Object statusMutex = new Object();
    private Object startTimeMutex = new Object();
    
    private LinkedList queue;
    private boolean closed;
    private int count = 0;
    private int maxThreads = 0;
    private int minThreads = 0;
    private int timeout;
    private final LinkedList pooledThreadsReference = new LinkedList();
    private static int uniqueID = 0;

    /**
     * Constructor ThreadPool.
     *
     * @param  poolMaxSize  ...
     * @param  poolMinSize  ...
     * @param  tName  ...
     * @param  secondsTimeout  ...
     * @throws ThreadPool.PoolException  
     */
    public ThreadPool(
            int poolMaxSize, int poolMinSize, String tName, int secondsTimeout)
                throws ThreadPool.PoolException {
        if ((poolMaxSize <= 0) || (poolMinSize <= 0)) {
            throw new ThreadPool.PoolException(
                "poolMaxSize and poolMinSize MUST be greater than 0");
        }
        queue = new LinkedList();
        this.tNames = tName;
        closed = false;
        maxThreads = poolMaxSize;
        minThreads = poolMinSize;
        timeout = secondsTimeout;
        count = 0;
        waiting_threads = 0;
        total_threads = 0;

        /*  */
        active_threads = 0;

        /*  */
        notifications = 0;

        /*  */
        queued = 0;

        if (minThreads > maxThreads) {
            minThreads = maxThreads;
            logger.log(
                Level.WARN,
                "poolMaxSize cannot be lesser then poolMinSize. poolMinSize is setted as poolMaxSize");
        }

        // new PoolMonitor(1000).start();
        for (int i = 0; i < minThreads; i++) {
            addThread();
        }
    }

    /**
     * Method register.
     *
     * @param  theThread  ...
     */
    public synchronized void register(PooledThread theThread) {
        pooledThreadsReference.add(theThread);
    }

    /**
     * Method unregister.
     *
     * @param  theThread  ...
     */
    public synchronized void unregister(PooledThread theThread) {
        int i = pooledThreadsReference.size();
        for (int x = 0; x < i; x++) {
            PooledThread myThread =
                (PooledThread) pooledThreadsReference.get(x);
            if (myThread.personalID == theThread.personalID) {
                pooledThreadsReference.remove(x);
                break;
            }
        }
    }

    /**
     * Method killToKill.
     */
    public synchronized void killToKill() {
        int i = 0;
        i = pooledThreadsReference.size();

        for (int x = 0; x < i; x++) {
            PooledThread myThread =
                (PooledThread) pooledThreadsReference.get(x);
            //GregorianCalendar myTime=myThread.startTime;
            GregorianCalendar myTime = null;
            synchronized (startTimeMutex) {
	            if (myThread.startTime != null) {
	            	myTime = (GregorianCalendar) myThread.startTime.clone();
	            }
            }
               
            if (myTime != null) {
                myTime.add(GregorianCalendar.SECOND, timeout);
                GregorianCalendar now = new GregorianCalendar();
                if (myTime.before(now)) {
                    try {
                        //myThread.releaseSocket();
                        logger.log(Level.WARN,
                                   "Thread timeout reached. The thread is hung down on reading from remote file");
                        myThread.interrupt();
                        synchronized (startTimeMutex) {
                        	myThread.startTime = null;
                        }
                    } catch (SecurityException cant) {

                    }

                }
            }
        }
    }

    /**
     * Method execute.
     *
     * @param  action  ...
     */
    public void execute(Runnable action) {
        killToKill();
        synchronized (queue) {
            if (closed) {
                return;
            }
            queue.addLast(action);
            queued++;

            if (waiting_threads > 0) {
                queue.notify();

                /*  */
                notifications++;
                waiting_threads--;
                return;
            }
            if (notifications > 0) {
                return;
            }
            synchronized (statusMutex) {
                if (active_threads != total_threads) {
                    return;
                }
            }
        }
        addThread();
    }

    /**
     * Method addThread.
     */
    private void addThread() {
        if ((maxThreads > 0) && (total_threads >= maxThreads)) {
            logger.warn(
                "Richiesta di elaborazione con il pool pieno: richiesta accodata");
        } else {
            count++;
            waiting_threads++;
            PooledThread thread = new PooledThread(count);

            thread.start();
            logger.info("Thread " + thread.getName() + " added to pool");
        }
    }

    /**
     * Method close.
     */
    public void close() {
        synchronized (queue) {
            closed = true;
            queue.notifyAll();
        }
    }

    /**
     * Class PooledThread.
     *
     * @author          ...
     * last author:     $Author: Dcrivel $
     * @version         $Revision: 27770 $
     * last modified:   $Modtime: 5/06/07 11.23 $
     * last check-in:   $Date: 5/06/07 11.26 $
     */
    private class PooledThread extends Thread {

        /** Field $objectName$. */
        public GregorianCalendar startTime = null;

        /** Field $objectName$. */
        public int personalID;

        /** Field $objectName$. */
        Runnable running;

        /**
         * Constructor PooledThread.
         *
         * @param  count  ...
         */
        public PooledThread(int count) {
            super(tNames + count);
            personalID = uniqueID;
            uniqueID++;
            register(this);
            startTime = null;
            // setPriority((int) (MIN_PRIORITY * 0.3 + MAX_PRIORITY * 0.7));
        }

        /*  public void releaseSocket()
          {
                  try
                          {
                          if (running.getMyRif().synchronizer.stream4All!=null)
                                  running.getMyRif().synchronizer.stream4All.close();
                          }
                  catch (IOException error)
                          {
                  }
                  running.getMyRif().synchronizer.stream4All=null;
          }*/

        /**
         * Method run.
         */
        public void run() {

            synchronized (statusMutex) {
                total_threads++;
            }
            try {
                boolean firstTime = true;
                while (true) {
                    Runnable action;

                    synchronized (queue) {
                        while (queue.isEmpty() && !closed) {
                            if (!firstTime) {
                                waiting_threads++;
                            }

                            firstTime = false;

                            while (true) {
                                try {
                                    queue.wait();
                                } catch (InterruptedException e) {
                                }
                                if (notifications == 0) {
                                    logger.debug("spurious wakeup from wait; recovered");
                                } else {
                                    break;
                                }
                            }

                            /*  */
                            notifications--;
                        }
                        if (closed) {
                            break;
                        }
                        action = (Runnable) queue.removeFirst();
                        queued--;
                        //waiting_threads--;
                    }

                    /*  */
                    synchronized (statusMutex) {

                        /*  */
                        active_threads++;

                        /*  */
                    }

                    try {
                        //action.setMyThread(this);
                        Thread.interrupted();
                        running = action;
                        synchronized(startTimeMutex) {
                        	startTime = new GregorianCalendar();
                        }
                        action.run();
                        synchronized(startTimeMutex) {
                        	startTime = null;
                        }
                        running = null;
                    } catch (Exception exc) {
                        logger.log(Level.INFO,
                                   "The scheduled action fails: "
                                   + exc.getMessage());
                    }

                    /*  */
                    synchronized (statusMutex) {

                        /*  */
                        active_threads--;

                        /*  */
                    }
                    if ((waiting_threads >= minThreads) && (minThreads > 0)) {
                        return;
                    }
                }
            } catch (Throwable e) {
                logger.error("POOL THREAD FAILED", e);
            } finally {
                unregister(this);
                synchronized (statusMutex) {
                    total_threads--;
                }
                if (total_threads < minThreads) {
                    addThread();
                }
            }
        }
    }

    /**
     * Class Controller.
     *
     * @author          ...
     * last author:     $Author: Dcrivel $
     * @version         $Revision: 27770 $
     * last modified:   $Modtime: 5/06/07 11.23 $
     * last check-in:   $Date: 5/06/07 11.26 $
     */
    public class Controller {

        /**
         * Method getName.
         * @return ...
         */
        public String getName() {
            return tNames;
        }

        /**
         * Method reportActiveThreads.
         * @return ...
         */
        public long reportActiveThreads() {
            return active_threads;
        }

        /**
         * Method reportWaitingThreads.
         * @return ...
         */
        public long reportWaitingThreads() {
            return waiting_threads;
        }

        /**
         * Method reportTotalThreads.
         * @return ...
         */
        public long reportTotalThreads() {
            return total_threads;
        }

        /**
         * Method reportQueue.
         * @return ...
         */
        public int reportQueue() {
            synchronized (queue) {
                return queued;
            }
        }

    }

    // a disposizione per debugging ma non usato -> l'ho commentato

    /**
     * Class PoolMonitor.
     *
     * @author          ...
     * last author:     $Author: Dcrivel $
     * @version         $Revision: 27770 $
     * last modified:   $Modtime: 5/06/07 11.23 $
     * last check-in:   $Date: 5/06/07 11.26 $
     */
    /*private class PoolMonitor extends Thread {
        private long millis;

        /**
         * Constructor PoolMonitor.
         *
         * @param  millis  ...
         */
        /*public PoolMonitor(long millis) {
            super("POOL MONITOR");
            this.millis = millis;
        }

        /**
         * Method run.
         */
        /*public void run() {
            while (true) {
                try {
                    sleep(millis);
                } catch (InterruptedException e) {
                }
                int act = active_threads;
                int tot = total_threads;
                int wai = waiting_threads;
                int not = notifications;
                int siz = queued;
                String info = "act = " + act + ", tot = " + tot + ", wai = "
                              + wai + ", siz = " + siz + ", not = " + not;

                logger.debug(info);
            }
        }
    }*/
}


/*--- Formatted in Lightstreamer Java Convention Style on 2004-12-10 ---*/
