package sd2526.trab.impl.utils.zoho.records;

// Pedido de envio de email
public record SendMailRequest(
    String fromAddress,
    String toAddress,
    String subject,
    String content
) {}