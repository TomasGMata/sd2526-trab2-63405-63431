package sd2526.trab.impl.utils.zoho.records;

import java.util.List;

import sd2526.trab.impl.utils.zoho.msgs.ZohoStatus;

public record ZohoMessageListReply(ZohoStatus status, List<ZohoMessage> data) {}