package com.zipwhip.api.signals;

import com.zipwhip.api.signals.dto.DeliveredMessage;
import com.zipwhip.important.ImportantTaskExecutor;
import com.zipwhip.util.Factory;

/**
 * Date: 9/25/13
 * Time: 1:39 PM
 *
 * @author Michael
 * @version 1
 */
public class SignalProviderFactory implements Factory<SignalProvider> {

    private ImportantTaskExecutor importantTaskExecutor;
    private BufferedOrderedQueue<DeliveredMessage> bufferedOrderedQueue;
    private Factory<SignalConnection> signalConnectionFactory;
    private SignalsSubscribeActor signalsSubscribeActor;

    @Override
    public SignalProvider create() {
        SignalProviderImpl signalProvider = new SignalProviderImpl();

        signalProvider.setImportantTaskExecutor(importantTaskExecutor);
        signalProvider.setBufferedOrderedQueue(bufferedOrderedQueue);
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

    public BufferedOrderedQueue<DeliveredMessage> getBufferedOrderedQueue() {
        return bufferedOrderedQueue;
    }

    public void setBufferedOrderedQueue(BufferedOrderedQueue<DeliveredMessage> bufferedOrderedQueue) {
        this.bufferedOrderedQueue = bufferedOrderedQueue;
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
}
