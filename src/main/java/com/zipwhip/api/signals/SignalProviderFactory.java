package com.zipwhip.api.signals;

import com.zipwhip.api.signals.dto.DeliveredMessage;
import com.zipwhip.executors.CommonExecutorFactory;
import com.zipwhip.executors.CommonExecutorTypes;
import com.zipwhip.executors.SimpleCommonExecutorFactory;
import com.zipwhip.important.ImportantTaskExecutor;
import com.zipwhip.lifecycle.DestroyableBase;
import com.zipwhip.util.Factory;

import java.util.concurrent.ExecutorService;

/**
 * Date: 9/25/13
 * Time: 1:39 PM
 *
 * @author Michael
 * @version 1
 */
public class SignalProviderFactory implements Factory<SignalProvider> {

    private ImportantTaskExecutor importantTaskExecutor;
    private Factory<BufferedOrderedQueue<DeliveredMessage>> bufferedOrderedQueueFactory;
    private Factory<SignalConnection> signalConnectionFactory;
    private SignalsSubscribeActor signalsSubscribeActor;
    private CommonExecutorFactory eventExecutorFactory = SimpleCommonExecutorFactory.getInstance();

    @Override
    public SignalProvider create() throws Exception {
        final ExecutorService eventExecutor = eventExecutorFactory.create(CommonExecutorTypes.EVENTS, "Events");

        SignalProviderImpl signalProvider = new SignalProviderImpl(eventExecutor);

        signalProvider.link(new DestroyableBase() {
            @Override
            protected void onDestroy() {
                eventExecutor.shutdown();
            }
        });

        signalProvider.setImportantTaskExecutor(importantTaskExecutor);
        signalProvider.setBufferedOrderedQueue(bufferedOrderedQueueFactory.create());
        signalProvider.setSignalConnection(signalConnectionFactory.create());
        signalProvider.setSignalsSubscribeActor(signalsSubscribeActor);

        return signalProvider;
    }

    public ImportantTaskExecutor getImportantTaskExecutor() {
        return importantTaskExecutor;
    }

    public void setImportantTaskExecutor(ImportantTaskExecutor importantTaskExecutor) {
        this.importantTaskExecutor = importantTaskExecutor;
    }

    public Factory<BufferedOrderedQueue<DeliveredMessage>> getBufferedOrderedQueueFactory() {
        return bufferedOrderedQueueFactory;
    }

    public void setBufferedOrderedQueueFactory(Factory<BufferedOrderedQueue<DeliveredMessage>> bufferedOrderedQueueFactory) {
        this.bufferedOrderedQueueFactory = bufferedOrderedQueueFactory;
    }

    public Factory<SignalConnection> getSignalConnectionFactory() {
        return signalConnectionFactory;
    }

    public void setSignalConnectionFactory(Factory<SignalConnection> signalConnectionFactory) {
        this.signalConnectionFactory = signalConnectionFactory;
    }

    public SignalsSubscribeActor getSignalsSubscribeActor() {
        return signalsSubscribeActor;
    }

    public void setSignalsSubscribeActor(SignalsSubscribeActor signalsSubscribeActor) {
        this.signalsSubscribeActor = signalsSubscribeActor;
    }

    public CommonExecutorFactory getEventExecutorFactory() {
        return eventExecutorFactory;
    }

    public void setEventExecutorFactory(CommonExecutorFactory eventExecutorFactory) {
        this.eventExecutorFactory = eventExecutorFactory;
    }
}
