package org.bgjug.jprime.core.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.apache.commons.lang3.StringUtils;
import org.bgjug.jprime.core.model.Branch;
import org.bgjug.jprime.core.model.Registrant;
import org.bgjug.jprime.core.model.Speaker;
import org.bgjug.jprime.core.model.Submission;
import org.bgjug.jprime.core.model.Visitor;
import org.bgjug.jprime.core.model.VisitorJPro;
import org.bgjug.jprime.core.model.VisitorStatus;
import org.bgjug.jprime.core.model.VisitorType;
import org.bgjug.jprime.core.repository.RegistrantRepository;
import org.bgjug.jprime.core.repository.VisitorJProRepository;
import org.bgjug.jprime.core.repository.VisitorRepository;
import org.supercsv.exception.SuperCsvException;
import org.supercsv.io.CsvListWriter;
import org.supercsv.io.CsvMapReader;
import org.supercsv.prefs.CsvPreference;

/** CSV exports (submissions, visitors) and the visitors import. */
@ApplicationScoped
public class CsvService {

    private static final String[] SUBMISSION_HEADER = {"Title", "Abstract", "Session level", "Session type",
        "Speaker Name", "Speaker Bio", "Co-Speaker Name", "Co-Speaker Bio"};
    private static final String[] VISITOR_HEADER = {"id", "name", "email", "company", "status"};

    // jPrime import columns
    static final String NAMES = "Names";
    static final String EMAIL = "Email";
    static final String COMPANY = "Company";
    static final String IS_COMPANY = "Is Company";
    static final String REGISTRANT = "Registrant";
    static final String ADDRESS = "Address";
    static final String VAT_NUMBER = "VAT Number";
    static final String MOL = "Material Responsible Person";
    static final String COMPANY_EMAIL = "Company E-Mail";
    private static final String[] JPRIME_FIELDS = {NAMES, EMAIL, COMPANY, IS_COMPANY, REGISTRANT, ADDRESS, VAT_NUMBER,
        MOL, COMPANY_EMAIL};

    // Eventbrite export columns (jProfessionals)
    static final String FIRST_NAME = "First Name";
    static final String LAST_NAME = "Last Name";
    static final String EMAIL_ADDRESS = "Email Address";
    private static final String[] JPRO_FIELDS = {"Event Name", "Order ID", "Date", "Venue Name", "Venue ID",
        "Gross Revenue (USD)", "Ticket Revenue (USD)", "Eventbrite Fees (USD)", "Eventbrite Payment Processing (USD)",
        "Tickets", "Type", "Status", "Distribution Partner", "Delivery Method", "Transaction ID", FIRST_NAME,
        LAST_NAME, EMAIL_ADDRESS};

    @Inject
    RegistrantRepository registrantRepository;

    @Inject
    VisitorRepository visitorRepository;

    @Inject
    VisitorJProRepository visitorJProRepository;

    @Inject
    BranchService branchService;

    public byte[] exportSubmissions(List<Submission> submissions) {
        StringWriter out = new StringWriter();
        try (CsvListWriter writer = new CsvListWriter(out, CsvPreference.STANDARD_PREFERENCE)) {
            writer.writeHeader(SUBMISSION_HEADER);
            for (Submission s : submissions) {
                Speaker speaker = s.getSpeaker();
                Speaker co = s.getCoSpeaker();
                writer.write(s.getTitle(), s.getDescription(), Objects.toString(s.getLevel(), ""),
                    Objects.toString(s.getType(), ""), speaker.getName(), speaker.getBio(),
                    co != null ? co.getName() : null, co != null ? co.getBio() : null);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] exportVisitors(List<Visitor> visitors) {
        StringWriter out = new StringWriter();
        try (CsvListWriter writer = new CsvListWriter(out, CsvPreference.STANDARD_PREFERENCE)) {
            writer.writeHeader(VISITOR_HEADER);
            for (Visitor v : visitors) {
                writer.write(v.getId(), v.getName(), v.getEmail(), v.getCompany(), Objects.toString(v.getStatus(), ""));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }

    public record ImportResult(int imported, List<String> errors) {
    }

    /**
     * Imports visitors from a CSV file. The first line may contain the column names in any order.
     *
     * @param emptyBefore for jProfessionals only: delete all jPro visitors first
     */
    @Transactional
    public ImportResult importVisitors(InputStream csv, VisitorType type, VisitorStatus status, boolean emptyBefore) {
        String[] fields = type == VisitorType.JPRIME ? JPRIME_FIELDS : JPRO_FIELDS;
        Branch branch = branchService.currentBranchReference();
        Map<String, Registrant> registrants = new HashMap<>();
        if (type == VisitorType.JPRIME) {
            registrants.putAll(registrantRepository.listByBranch(branch).stream()
                .filter(r -> registrantKey(r) != null)
                .collect(Collectors.toMap(CsvService::registrantKey, Function.identity(), (a, b) -> a)));
        } else if (emptyBefore) {
            visitorJProRepository.deleteAll();
        }
        int imported = 0;
        try (CsvMapReader reader = new CsvMapReader(
            new BufferedReader(new InputStreamReader(csv, StandardCharsets.UTF_8)), CsvPreference.STANDARD_PREFERENCE)) {
            // the first line is the header - columns may come in any order
            String[] header = reader.getHeader(true);
            if (header == null) {
                return new ImportResult(0, List.of());
            }
            String[] columns = Arrays.stream(header).map(h -> h == null ? null : h.trim()).toArray(String[]::new);
            if (Arrays.stream(fields).noneMatch(f -> Arrays.asList(columns).contains(f))) {
                return new ImportResult(0, List.of("The CSV header must contain the columns: " + String.join(", ",
                    fields)));
            }
            Map<String, String> line = reader.read(columns);
            Registrant last = null;
            while (line != null) {
                if (type == VisitorType.JPRIME) {
                    last = importJPrimeVisitor(line, registrants, status, last, branch);
                } else {
                    VisitorJPro visitor = new VisitorJPro(
                        StringUtils.defaultString(line.get(FIRST_NAME)) + " " + StringUtils.defaultString(
                            line.get(LAST_NAME)), line.get(EMAIL_ADDRESS), "");
                    visitorJProRepository.persist(visitor);
                }
                imported++;
                line = reader.read(columns);
            }
        } catch (IOException | SuperCsvException e) {
            return new ImportResult(imported, List.of(e.getMessage() != null ? e.getMessage() : e.toString()));
        }
        return new ImportResult(imported, List.of());
    }

    private static String registrantKey(Registrant r) {
        return StringUtils.isEmpty(r.getRawVatNumber()) ? r.getName() : r.getRawVatNumber();
    }

    private Registrant importJPrimeVisitor(Map<String, String> line, Map<String, Registrant> registrants,
        VisitorStatus status, Registrant last, Branch branch) {
        String name = line.get(NAMES);
        String email = line.get(EMAIL);
        Registrant registrant;
        if (StringUtils.isNotBlank(line.get(IS_COMPANY)) || StringUtils.isNotBlank(line.get(REGISTRANT))) {
            String registrantName = line.get(REGISTRANT);
            String vat = line.get(VAT_NUMBER);
            String key = StringUtils.isEmpty(vat) ? registrantName : vat;
            if (last != null && (StringUtils.isEmpty(key) || key.equals(registrantKey(last)))) {
                registrant = last;
            } else {
                registrant = registrants.computeIfAbsent(key,
                    k -> new Registrant(registrantName, line.get(ADDRESS), vat, line.get(MOL), line.get(COMPANY_EMAIL),
                        branch));
            }
        } else {
            registrant = new Registrant(name, email, branch);
        }
        if (registrant.getId() == null) {
            registrantRepository.persist(registrant);
        }
        Visitor visitor = new Visitor(name, email, line.get(COMPANY));
        visitor.setStatus(status);
        registrant.addVisitor(visitor);
        visitorRepository.persist(visitor);
        return registrant;
    }
}
