package com.zipwhip.important;

import com.zipwhip.concurrent.*;
import com.zipwhip.events.Observer;
import com.zipwhip.executors.SimpleExecutor;
import com.zipwhip.important.schedulers.TimerScheduler;
import com.zipwhip.lifecycle.CascadingDestroyableBase;
import com.zipwhip.util.FutureDateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by IntelliJ IDEA.
 * User: Russ
 * Date: 8/29/12
 * Time: 11:53 AM
 */
public class ImportantTaskExecutor extends CascadingDestroyableBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImportantTaskExecutor.class);

    private final Map<String, ScheduledRequest> queuedRequests = Collections.synchronizedMap(new HashMap<String, ScheduledRequest>());
//    private final Set<String> executingRequests = Collections.synchronizedSet(new HashSet<String>());

    private Scheduler scheduler;

    private long timeout = 0;
    private TimeUnit units = TimeUnit.SECONDS;

    public ImportantTaskExecutor() {
        this(null);
    }

    public ImportantTaskExecutor(Scheduler scheduler) {
        if (scheduler == null){
            TimerScheduler scheduler1 = new TimerScheduler((String)null);
            this.setScheduler(scheduler1);
            this.link(scheduler1);
        } else {
            this.setScheduler(scheduler);
        }
    }

    public <T> ObservableFuture<T> enqueue(Executor executor, final Callable<ObservableFuture<T>> request) {
        return enqueue(executor, request, null);
    }

    public <T> ObservableFuture<T> enqueue(Executor executor, final Callable<ObservableFuture<T>> request, long timeoutInSeconds) {
        return enqueue(executor, request, FutureDateUtil.inFuture(timeoutInSeconds, TimeUnit.SECONDS));
    }

    public <T> ObservableFuture<T> enqueue(Executor executor, final Callable<ObservableFuture<T>> request, final Date expirationDate) {
        if (executor == null){
            executor = SimpleExecutor.getInstance();
        }

        final String requestId = UUID.randomUUID().toString();

        // we're returning this value.
        final MutableObservableFuture<T> parentFuture = createObservableFuture(executor, request);

        /**
         * Schedule a timeout in the future if it has an expirationDate.
         */
        final ScheduledRequest<T> scheduledRequest = createScheduledRequestIfExpiring(parentFuture, requestId, expirationDate, request);

        try {
            executor.execute(new Runnable() {

                @Override
                public void run() {
                    synchronized (request) {
                        try {
                            // immediately check to see if the parentFuture was cancelled.
                            // the scheduler could have timed us out before we got here.
                            if (parentFuture.isDone()) {
                                LOGGER.warn("The parentFuture is already done, so we're just going to quit. Did we time out?");
                                // someone cancelled our shit or we timed out.
                                return;
                            }

                            final ObservableFuture<T> requestFuture = request.call();

                            // sync over the requestFuture to the parentFuture.
                            // ie: if the requestFuture is already done then cascade that over.
                            if (requestFuture instanceof MutableObservableFuture) {
                                NestedObservableFuture.syncState(parentFuture, (MutableObservableFuture)requestFuture);
                            }

                            NestedObservableFuture.syncState(requestFuture, parentFuture);

                            // NOTE: We are doing it twice on purpose. If someone uses an ObservableFuture
                            // that is asynchronous, then we will exit this method with the wrong state.
                            // we have to do it both ways.
                            if (!parentFuture.isDone() || !requestFuture.isDone()) {
                                requestFuture.addObserver(new Observer<ObservableFuture<T>>() {
                                    @Override
                                    public void notify(Object sender, ObservableFuture<T> item) {
                                        // sync over the requestFuture to the parentFuture.
                                        // ie: if the requestFuture is already done then cascade that over.
                                        NestedObservableFuture.syncState(requestFuture, parentFuture);
                                    }
                                });

                                parentFuture.addObserver(new Observer<ObservableFuture<T>>() {
                                    @Override
                                    public void notify(Object sender, ObservableFuture<T> item) {
                                        // sync over the requestFuture to the parentFuture.
                                        // ie: if the requestFuture is already done then cascade that over.
                                        if (requestFuture instanceof MutableObservableFuture) {
                                            NestedObservableFuture.syncState(parentFuture, (MutableObservableFuture)requestFuture);
                                        }

                                        if (expirationDate != null) {
                                            scheduler.cancel(requestId);
                                        }
                                    }
                                });
                            }

                            if (parentFuture.isDone() || requestFuture.isDone()) {
                                LOGGER.debug("Since either the parentFuture and requestFuture were done, we are not going to attach the future to the scheduledRequest for cancellation/timeout.");
                                return;
                            }

                            // if we have something scheduled, we need set it up.
                            if (scheduledRequest != null) {
                                scheduledRequest.setRequestFuture(requestFuture);
                            }
                        } catch (Exception e) {
                            LOGGER.error("ImportantTask error!", e);
                            parentFuture.setFailure(e);
                        } finally {
                            LOGGER.debug(String.format("Finished synchronous execution portion for task %s, future: %s", request, parentFuture));
                        }
                    }
                }

                @Override
                public String toString() {
                    return String.format("[ImportantTask: %s]", request.toString());
                }
            });

        } catch (Exception e) {
            return new FakeFailingObservableFuture<T>(this, e);
        } finally {
            LOGGER.debug(String.format("Queued up task %s. Expiration: %s", request, expirationDate));
        }

        return parentFuture;
    }

    private <T> ScheduledRequest<T> createScheduledRequestIfExpiring(MutableObservableFuture<T> parentFuture, String requestId, Date expirationDate, Callable<ObservableFuture<T>> request) {
        ScheduledRequest<T> scheduledRequest = null;
        expirationDate = getExpirationDate(expirationDate);
        if (expirationDate != null) {
            scheduledRequest = new ScheduledRequest<T>(requestId, request, parentFuture, expirationDate);

            // in case it times out
            queuedRequests.put(requestId, scheduledRequest);

            scheduler.schedule(requestId, expirationDate);
        }

        return scheduledRequest;
    }

    private Date getExpirationDate(Date expirationDate) {
        if (expirationDate == null) {
            if (timeout != 0) {
                expirationDate = FutureDateUtil.inFuture(timeout, units);
            }
        }
        return expirationDate;
    }

    private final Observer<String> onTimerScheduleComplete = new Observer<String>() {

        @Override
        public void notify(Object sender, String requestId) {
            if (!queuedRequests.containsKey(requestId)) {
                return; // wasn't our timer
            }

            synchronized (ImportantTaskExecutor.this) {
                if (isDestroyed()) {
                    LOGGER.error("We were destroyed, but the onTimerScheduleComplete hit. Did you forget to shut down the executor?");
                    return;
                }
            }

            ScheduledRequest scheduledRequest = queuedRequests.get(requestId);
            if (scheduledRequest == null) {
                // must be a reboot of the service.
                LOGGER.warn("The timeout hit on request, though it was not found in the store: " + requestId);
                return;
            }

//            Callable<ObservableFuture<?>> request = scheduledRequest.getRequest();
            Date expirationDate = scheduledRequest.getExpirationDate();
            if (expirationDate != null && nowIsAfterThisDate(expirationDate)) {
                queuedRequests.remove(scheduledRequest.getRequestId());

                // was the initial future even finished yet?
                ObservableFuture f = scheduledRequest.getRequestFuture();
                if (f == null) {
                    // it took too long to even make the request. Wow.
                    scheduledRequest.getParentFuture().setFailure(new TimeoutException("Too late? " + expirationDate));
                } else {
                    // it just simply timed out normally
                    if (scheduledRequest.getRequestFuture() instanceof MutableObservableFuture) {
                        ((MutableObservableFuture<?>)scheduledRequest.getRequestFuture()).setFailure(new TimeoutException("Too late? " + expirationDate));
                    }
                }
            } else {
                LOGGER.warn(String.format("The expiration was too early?!?!! expires: %s", expirationDate));
            }
        }
    };

    private boolean nowIsAfterThisDate(Date date) {
        if (date == null) {
            throw new IllegalArgumentException("The date can't be null!");
        }

        long diff =  System.currentTimeMillis() - date.getTime();

        return diff > -1000;
    }

    private <TResponse> DefaultObservableFuture<TResponse> createObservableFuture(Executor executor, final Callable request) {
        return new DefaultObservableFuture<TResponse>(request, executor) {
            @Override
            public String toString() {
                return String.format("[ImportantTaskExecutorFuture: %s]", request);
            }
        };
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public void setScheduler(Scheduler scheduler) {
        if (this.scheduler != null) {
            this.scheduler.removeOnScheduleComplete(onTimerScheduleComplete);
        }

        this.scheduler = new ScopedScheduler(scheduler) {
            @Override
            public String toString() {
                return String.format("[ImportantTaskScheduler: %s]", ImportantTaskExecutor.this.toString());
            }
        };

        if (this.scheduler != null) {
            this.scheduler.onScheduleComplete(onTimerScheduleComplete);
        }
    }

    @Override
    protected void onDestroy() {
        queuedRequests.clear();
//        executingRequests.clear();
    }

    /**
     * Created by IntelliJ IDEA.
     * User: Russ
     * Date: 8/29/12
     * Time: 12:09 PM
     */
    public static class ScheduledRequest<T> {

        private final Callable<ObservableFuture<T>> request;
        private final MutableObservableFuture<T> parentFuture;
        private final String requestId;
        private final Date expirationDate;

        private ObservableFuture<T> requestFuture;

        public ScheduledRequest(String requestId, Callable<ObservableFuture<T>> request, MutableObservableFuture<T> parentFuture, Date expirationDate) {
            this.requestId = requestId;
            this.parentFuture = parentFuture;
            this.request = request;
            this.expirationDate = expirationDate;
        }

        public MutableObservableFuture<T> getParentFuture() {
            return parentFuture;
        }

        public Callable<ObservableFuture<T>> getRequest() {
            return request;
        }

        public void setRequestFuture(ObservableFuture<T> requestFuture) {
            this.requestFuture = requestFuture;
        }

        public ObservableFuture<T> getRequestFuture() {
            return requestFuture;
        }

        public Date getExpirationDate() {
            return expirationDate;
        }

        public String getRequestId() {
            return requestId;
        }
    }

    @Override
    public String toString() {
        return String.format("[ImportantTaskExecutor: %s", hashCode());
    }

    public static boolean isFailedDueToTimeout(ObservableFuture<?> future) {
        if (future == null){
            throw new NullPointerException("future");
        } else if (!future.isFailed()) {
            return false;
        }

        return future.getCause() instanceof TimeoutException;
    }

}
