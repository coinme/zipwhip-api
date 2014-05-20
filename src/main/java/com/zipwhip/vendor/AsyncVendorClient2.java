package com.zipwhip.vendor;

import com.zipwhip.api.dto.MessageToken;
import com.zipwhip.concurrent.ObservableFuture;

import java.util.List;

/**
 * @author Michael
 * @date 5/20/2014
 *
 * The older AsyncVendorClient was deprecated. We've moving away from the older servers and migrating to a new system.
 * The new system is less functional than the older one. Those API's will stop working in the near future.
 * Use this one instead. This one requires different API keys than the older one, so make sure you contact Zipwhip for
 * access.
 */
public interface AsyncVendorClient2 {

    /**
     * Send a text.
     *
     * @param subscriberPhoneNumber The subscriber phone number. You need to have "vendor access" to this account.
     *                              It must already be an active Zipwhip account. Accounts are not created on demand via
     *                              this method.
     * @param friendPhoneNumber The contact phone number (The system will create contacts on demand. No need to save
     *                          a contact first).
     * @param body The textual body. Since it's an SMS, don't go over the character limit as specified by the alphabet.
     *             The current size limit is 160 characters. Transmission will fail via signal (even though it succeeded
     *             in the future)
     * @param advertisement Pass in null to leave it defaulted to Zipwhip's standard advertisement detection system.
     *                      This is typically called the "Signature line" at the bottom.
     *                      If you want to override the detection system and force no advertisement/signature, just pass
     *                      in the word "none"
     * @return
     * @throws Exception
     */
    ObservableFuture<List<MessageToken>> send(String subscriberPhoneNumber, String friendPhoneNumber, String body, String advertisement) throws Exception;

}
