//package com.zipwhip.vendor;
//
//import com.zipwhip.api.NingApiConnectionFactory;
//import com.zipwhip.api.NingHttpConnection;
//import com.zipwhip.api.dto.MessageToken;
//import com.zipwhip.api.response.MessageSendResult;
//import com.zipwhip.concurrent.ExecutorFactory;
//import com.zipwhip.concurrent.ObservableFuture;
//import com.zipwhip.lifecycle.DestroyableBase;
//import com.zipwhip.util.Factory;
//import org.slf4j.LoggerFactory;
//
//import java.util.List;
//import java.util.concurrent.ExecutorService;
//
///**
// * @author Michael
// * @date 5/20/2014
// */
//public class AsyncVendorClient2Factory implements Factory<AsyncVendorClient2> {
//
//    private final String vendorKey;
//
//    public AsyncVendorClient2Factory(String vendorKey) {
//        this.vendorKey = vendorKey;
//    }
//
//    public static AsyncVendorClient2 create(String vendorKey) {
//        final DefaultAsyncVendorClient2 client = new DefaultAsyncVendorClient2();
//
//        {
//            final ExecutorService workerExecutor = ExecutorFactory.newInstance("AsyncVendorClient-worker");
//
//            client.link(new DestroyableBase() {
//                @Override
//                protected void onDestroy() {
//                    workerExecutor.shutdown();
//                }
//            });
//
//            {
//                NingHttpConnection connection = new NingHttpConnection(workerExecutor);
//
//                connection.setHost("https://vendor.zipwhip.com");
//
//                client.setConnection(connection);
//                client.link(connection);
//            }
//
//            client.setEventExecutor(workerExecutor);
//        }
//
//        client.setVendorKey(vendorKey);
//
//        return client;
//    }
//
//    @Override
//    public AsyncVendorClient2 create() throws Exception {
//        return AsyncVendorClient2Factory.create(vendorKey);
//    }
//}
