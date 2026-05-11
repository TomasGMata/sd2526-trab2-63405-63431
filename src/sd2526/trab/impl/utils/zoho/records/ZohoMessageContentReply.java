package sd2526.trab.impl.utils.zoho.records;

import sd2526.trab.impl.utils.zoho.msgs.ZohoStatus;

public record ZohoMessageContentReply(ZohoStatus status, ZohoMessageContent data) {}