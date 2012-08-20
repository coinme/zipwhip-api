package com.zipwhip.util;

import com.zipwhip.util.CollectionUtil;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: Michael
 * Date: 8/15/12
 * Time: 10:06 AM
 *
 * Handy utility for working with internet addresses.
 */
public class AddressUtil {

    private static final int DEFAULT_PORT = 80;

    public static Collection<InetSocketAddress> get(String address, Integer... ports) {
        return get(address, Arrays.asList(ports));
    }

    public static Collection<InetSocketAddress> get(String address, Collection<Integer> ports) {
        if (CollectionUtil.isNullOrEmpty(ports)) {
            return Arrays.asList(new InetSocketAddress(address, DEFAULT_PORT));
        }

        Collection<InetSocketAddress> result = new ArrayList<InetSocketAddress>(ports.size());

        for (int port : ports) {
            result.add(new InetSocketAddress(address, port));
        }

        return result;
    }
}
