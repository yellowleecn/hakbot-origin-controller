/*
 * This file is part of RESTjob Controller.
 *
 * RESTjob Controller is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * RESTjob Controller is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * RESTjob Controller. If not, see http://www.gnu.org/licenses/.
 */
package com.restjob.controller.workers;

import com.restjob.controller.Config;
import com.restjob.controller.ConfigItem;
import com.restjob.controller.listener.LocalEntityManagerFactory;
import com.restjob.controller.logging.Logger;
import com.restjob.controller.model.Job;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * The JobManager is used to store the current queued jobs waiting
 * to be executed and the jobs which are currently being executed.
 * It also implements a TimerTask which will check for new work to
 * do and another TimerTask which will cleanup references to
 * completed jobs. Both tasks intervals are configurable in
 * application.properties. If new work is found, the scheduler will
 * take one job off the queue every time the scheduled task runs.
 */
public class JobManager {

    // Setup logging
    private static final Logger logger = Logger.getLogger(JobManager.class);

    // Holds all work that has been requested to be performed and is currently in the queue.
    // Job objects will be removed from queue once the job has started
    private LinkedList<Job> workQueue;

    // Hold all of the JobExecutors currently in process
    private ArrayBlockingQueue<JobExecutor> executors;

    // Defines the value for the maximum number of concurrent jobs that can take place at a time
    private int maxJobSize;

    // Defines the value for the maximum number of items in the queue to prevent DoS attacks
    private int maxQueueSize;

    // Holds an instance of JobManager
    private static final JobManager instance = new JobManager();

    // JPA Entity Manager
    private EntityManager em;

    /**
     * Construct a new JobManager instance and setups up queues and scheduling
     */
    private JobManager() {
        logger.info("Initializing JobManager");

        maxJobSize = Config.getInstance().getPropertyAsInt(ConfigItem.MAX_JOB_SIZE);
        maxQueueSize = Config.getInstance().getPropertyAsInt(ConfigItem.MAX_QUEUE_SIZE);

        int queueCheckInterval = Config.getInstance().getPropertyAsInt(ConfigItem.QUEUE_CHECK_INTERVAL) * 1000;
        int jobCleanupInterval = Config.getInstance().getPropertyAsInt(ConfigItem.JOB_CLEANUP_INTERVAL) * 1000;

        if (logger.isDebugEnabled()) {
            logger.debug("Max Job Size: " + maxJobSize);
            logger.debug("Max Queue Size: " + maxQueueSize);
            logger.debug("Queue Check Interval: " + queueCheckInterval);
            logger.debug("Job Cleanup Interval: " + jobCleanupInterval);
        }

        executors = new ArrayBlockingQueue<>(maxJobSize);
        workQueue = new LinkedList<>();

        em = LocalEntityManagerFactory.createEntityManager();

        // Creates a new JobSchedulerTask every 15 seconds
        Timer schTimer = new Timer();
        schTimer.schedule(new JobSchedulerTask(), 0, queueCheckInterval);

        // Creates a new JobCleanupTask every 5 seconds
        Timer cleanupTimer = new Timer();
        cleanupTimer.schedule(new JobCleanupTask(), 0, jobCleanupInterval);
    }

    /**
     * Return an instance of the JobManager instance
     * @return a JobManager instance
     */
    public static JobManager getInstance() {
        return instance;
    }

    /**
     * Adds a new Job to the queue
     * @param job the Job object to add to queue
     */
    private synchronized void add(Job job) {
        if (logger.isDebugEnabled()) {
            logger.debug("Adding job to queue: " + job.getUuid());
        }
        if (workQueue.size() < maxQueueSize) {
            em.getTransaction().begin();
            job.setState(State.IN_QUEUE);
            em.getTransaction().commit();
            workQueue.add(job);
        }
    }

    /**
     * Removes a Job from the queue if found. If job is currently being executed,
     * a call to quit(Job) is made to gracefully quit the process. The state of
     * the specified job is marked as canceled.
     * @param job the Job object to remove from the queue or from current running processes
     */
    public synchronized void cancel(Job job) {
        if (logger.isDebugEnabled()) {
            logger.debug("Canceling job: " + job.getUuid());
        }
        // First, check the queue, if job exists in queue, remove it
        for (Job checkJob: workQueue) {
            if (checkJob.getUuid().equals(job.getUuid())) {
                workQueue.remove(checkJob);
            }
        }

        // Next, check the list of current jobs in progress, if found, quit the process
        for (JobExecutor executor: executors) {
            if (executor.getJob().getUuid().equals(job.getUuid())) {
                executor.cancel();
                sleep(2000); // sleep for 2 seconds
                executor.waitFor();
            }
        }
        em.getTransaction().begin();
        job.setState(State.CANCELED);
        em.getTransaction().commit();
    }

    /**
     * Checks to see if the specified job is currently in progress.
     * Compares the UUID against the UUID of the jobs in progress
     * @param uuid the uuid of the job
     * @return true if job is in progress, false if not in progress
     */
    public boolean inProgress(UUID uuid) {
        return getJobInProgress(uuid) != null;
    }

    /**
     * Returns a Job which is in progress from the specified UUID
     * Returns null if the specified uuid is not found in the list
     * of jobs in progress.
     * @param uuid The uuid of job
     * @return a Job object matching the specified uuid
     */
    public Job getJobInProgress(UUID uuid) {
        for (JobExecutor executor: executors) {
            if (executor.getJob().getUuid().equals(uuid))
                return executor.getJob();
        }
        return null;
    }

    /**
     * Returns a list of all jobs in progress
     * @return a list of Job objects
     */
    public List<Job> getJobsInProgress() {
        List<Job> jobs = new ArrayList<Job>();
        for (JobExecutor executor: executors) {
            jobs.add(executor.getJob());
        }
        return jobs;
    }

    /**
     * Returns a list of all jobs in the queue
     * @return a list of Job object
     */
    public List<Job> getJobsInQueue() {
        return new ArrayList<Job>(workQueue);
    }

    /**
     * Checks to see if the specified job is currently in the queue.
     * Compares the UUID against the UUID of the jobs in the queue
     * @param uuid the uuid of the job
     * @return true if job is in the queue, false if not in the queue
     */
    public boolean inQueue(UUID uuid) {
        return getJobInQueue(uuid) != null;
    }

    /**
     * Returns a Job which is in the queue from the specified UUID
     * Returns null if the specified uuid is not found in the list
     * of jobs in the queue.
     * @param uuid The uuid of job
     * @return a Job object matching the specified uuid
     */
    public Job getJobInQueue(UUID uuid) {
        for (Job job: workQueue) {
            if (job.getUuid().equals(uuid)) {
                return job;
            }
        }
        return null;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (Exception e) {
            // eat it
        }
    }

    /**
     * This class will look for slots available and remove new items
     * from the queue and add them to the list of jobs being performed
     * and execute the job.
     */
    class JobSchedulerTask extends TimerTask {
        public synchronized void run() {
            if (logger.isDebugEnabled()) {
                logger.debug("Polling for new jobs");
            }

            for (Job job: getWaitingJobs()) {
                add(job);
            }

            if (executors.size() < maxJobSize && workQueue.size() > 0) {
                // There is an opening for new work to be performed. Get the job from the queue.
                Job job = workQueue.remove();

                // Create and start new JobExecutor
                JobExecutor executor = new JobExecutor(em, job);
                Thread thread = new Thread(executor);
                thread.start();

                // Add job to the list of jobs being executed
                executors.add(executor);
            }
        }

        private List<Job> getWaitingJobs() {
            List<Job> jobs = new ArrayList<>();

            TypedQuery<Job> query = em.createNamedQuery("Job.getJobsByState", Job.class);
            query.setParameter("state", State.UNAVAILABLE.getValue());
            jobs.addAll(query.getResultList());

            query = em.createNamedQuery("Job.getJobsByState", Job.class);
            query.setParameter("state", State.CREATED.getValue());
            jobs.addAll(query.getResultList());

            return jobs;
        }
    }

    /**
     * This class will look for Jobs which are no loner being executed and cleanup references
     */
    class JobCleanupTask extends TimerTask {
        public synchronized void run() {
            for (JobExecutor executor: executors) {
                // Check to see if the executor job is still executing
                if (!executor.isExecuting()) {
                    // Execution has completed. Remove reference
                    executors.remove(executor);
                }
            }
        }
    }

}


