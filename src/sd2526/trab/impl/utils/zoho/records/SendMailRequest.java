package sd2526.trab.impl.utils.zoho.records;

public record SendMailRequest(
    String fromAddress,
    String toAddress,
    String subject,
    String content
) {}