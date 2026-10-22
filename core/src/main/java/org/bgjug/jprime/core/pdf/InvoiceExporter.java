package org.bgjug.jprime.core.pdf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import net.sf.jasperreports.engine.JasperReport;

/** Renders invoices (proforma and original) with the JasperReports templates. */
@ApplicationScoped
public class InvoiceExporter {

    public byte[] exportInvoice(InvoiceData data, boolean isCompany, InvoiceLanguage language, int conferenceYear) {
        String suffix = language == InvoiceLanguage.BG ? "_bg" : "";
        String resource = isCompany ? "invoice/invoice_company_template" + suffix + ".jasper" :
                          "invoice/invoice_individual_template" + suffix + ".jasper";
        JasperReport report = JasperSupport.load(resource);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("jprime.year", Integer.toString(conferenceYear));
        JasperSupport.copyBeanProperties(report, data, parameters);
        return JasperSupport.exportPdf(report, parameters, new ArrayList<>(data.getInvoiceDetails()));
    }
}
