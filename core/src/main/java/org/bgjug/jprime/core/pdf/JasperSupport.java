package org.bgjug.jprime.core.pdf;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.util.JRLoader;
import org.bgjug.jprime.core.model.JPrimeException;

/** Loads the precompiled .jasper reports and fills them. */
final class JasperSupport {

    private static final Map<String, JasperReport> REPORTS = new ConcurrentHashMap<>();

    private JasperSupport() {
    }

    static JasperReport load(String resource) {
        return REPORTS.computeIfAbsent(resource, r -> {
            try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(r)) {
                if (in == null) {
                    throw new JPrimeException("Report not found on the classpath: " + r);
                }
                return (JasperReport) JRLoader.loadObject(in);
            } catch (Exception e) {
                throw new JPrimeException("Cannot load report " + r, e);
            }
        });
    }

    /** Puts each bean property with the name of a report parameter into the parameter map (legacy behaviour). */
    static void copyBeanProperties(JasperReport report, Object bean, Map<String, Object> parameters) {
        for (JRParameter parameter : report.getParameters()) {
            if (parameter.isSystemDefined() || parameters.containsKey(parameter.getName())) {
                continue;
            }
            String name = parameter.getName();
            String getter = "get" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
            try {
                Method method = bean.getClass().getMethod(getter);
                parameters.put(name, method.invoke(bean));
            } catch (NoSuchMethodException e) {
                // not a bean property - leave the report default
            } catch (ReflectiveOperationException e) {
                throw new JPrimeException(e);
            }
        }
    }

    static byte[] exportPdf(JasperReport report, Map<String, Object> parameters, Collection<?> rows) {
        try {
            JasperPrint print = JasperFillManager.fillReport(report, parameters, new JRBeanCollectionDataSource(rows));
            return JasperExportManager.exportReportToPdf(print);
        } catch (JRException e) {
            throw new JPrimeException("PDF generation failed", e);
        }
    }
}
