package org.com.models.messages;

public enum MessageStatus {
    ERROR, PROCESS_SDP_ANSWER,

    RECEIVE_VIDEO_FROM,
    LEAVE_ROOM,

    MESSAGE, ESTABLISHING, PARTICIPANTS, ADD_ICE_CANDIDATE, SDP_OFFER
}
