package com.zipwhip.api.signals.dto;

import com.zipwhip.api.dto.TransmissionState;

import java.util.Date;

/**
 * Date: 9/4/13
 * Time: 4:21 PM
 *
 * We send a smaller message payload via signal compared to /message/list or /message/get
 *
 *
 *
 * @author Michael
 * @version 1
 */
public class MessageSignal {

    private long id;
    private String address;
    private String body;
    private TransmissionState transmissionState;
    private String advertisement;
    private String fromName;
    private String fingerprint;
    private long deviceId;
    private long contactId;
    private long contactDeviceId;
    private String messageType;
    private Date dateCreated;
    private Date dateRead;
    private Date dateDeleted;
    private boolean attachments;
    private String carrier;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public TransmissionState getTransmissionState() {
        return transmissionState;
    }

    public void setTransmissionState(TransmissionState transmissionState) {
        this.transmissionState = transmissionState;
    }

    public String getAdvertisement() {
        return advertisement;
    }

    public void setAdvertisement(String advertisement) {
        this.advertisement = advertisement;
    }

    public String getFromName() {
        return fromName;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    public long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(long deviceId) {
        this.deviceId = deviceId;
    }

    public long getContactId() {
        return contactId;
    }

    public void setContactId(long contactId) {
        this.contactId = contactId;
    }

    public long getContactDeviceId() {
        return contactDeviceId;
    }

    public void setContactDeviceId(long contactDeviceId) {
        this.contactDeviceId = contactDeviceId;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public Date getDateRead() {
        return dateRead;
    }

    public void setDateRead(Date dateRead) {
        this.dateRead = dateRead;
    }

    public Date getDateDeleted() {
        return dateDeleted;
    }

    public void setDateDeleted(Date dateDeleted) {
        this.dateDeleted = dateDeleted;
    }

//    public boolean isAttachments() {
//        return attachments;
//    }

    public boolean hasAttachments() {
        return attachments;
    }

    public void setAttachments(boolean attachments) {
        this.attachments = attachments;
    }

    public String getCarrier() {
        return carrier;
    }

    public void setCarrier(String carrier) {
        this.carrier = carrier;
    }
}
