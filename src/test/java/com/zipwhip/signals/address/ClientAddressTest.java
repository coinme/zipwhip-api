package com.zipwhip.signals.address;

import com.zipwhip.signals2.address.ClientAddress;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 8/29/11
 * Time: 12:29 PM
 */
public class ClientAddressTest {

	@Test
	public void testEquals()
	{
		ClientAddress ca1 = new ClientAddress("123456");
		ClientAddress ca2 = new ClientAddress("654321");

		assertTrue(ca1.equals(ca1));
		assertFalse(ca1.equals(null));
		assertFalse(ca1.equals(ca2));
		assertFalse(ca2.equals(ca1));

		ca2.setClientId(ca1.getClientId());
		assertTrue(ca1.equals(ca2));
	}

}
