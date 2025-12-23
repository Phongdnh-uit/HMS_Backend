package com.hms.billing_service.repositories;

import com.hms.billing_service.entities.InvoiceItem;
import com.hms.common.repositories.SimpleRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceItemRepository extends SimpleRepository<InvoiceItem, String> {

    List<InvoiceItem> findByInvoiceId(String invoiceId);
}
