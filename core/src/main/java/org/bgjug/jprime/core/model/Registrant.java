package org.bgjug.jprime.core.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/**
 * Who pays (and receives the invoice) for one or more {@link Visitor}s.
 */
@Entity
@Table(name = "Registrant")
public class Registrant extends AbstractEntity {

    @OneToMany(mappedBy = "registrant", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @OrderBy("id")
    private List<Visitor> visitors = new ArrayList<>();

    @Column(name = "isCompany", nullable = false)
    private boolean company = true;

    @Column(name = "isStudent", nullable = false)
    private boolean student;

    @Column(name = "name")
    private String name = "";

    @Column(name = "address")
    private String address;

    @Column(name = "vatNumber")
    private String vatNumber;

    /** Accountable person (MOL). */
    @Column(name = "mol")
    private String mol;

    @Column(name = "email")
    private String email = "";

    @Column(name = "eik")
    private String eik;

    @Column(name = "epayInvoiceNumber")
    private Long epayInvoiceNumber;

    /** The real invoice number - generated only after payment. */
    @Column(name = "realInvoiceNumber")
    private Long realInvoiceNumber;

    @Column(name = "proformaInvoiceNumber")
    private Long proformaInvoiceNumber;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "paymentType")
    private PaymentType paymentType;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "branch", referencedColumnName = "label", foreignKey = @ForeignKey(name = "fk_registrant_branch"))
    private Branch branch;

    @Embedded
    private EpayResponse epayResponse;

    public Registrant() {
    }

    /** A private person registrant. */
    public Registrant(String name, String email, Branch branch) {
        this.company = false;
        this.name = name;
        this.email = email;
        this.branch = branch;
        this.paymentType = PaymentType.BANK_TRANSFER;
    }

    /** A company registrant. */
    public Registrant(String name, String address, String vatNumber, String mol, String email, Branch branch) {
        this.company = true;
        this.name = name;
        this.address = address;
        this.vatNumber = vatNumber;
        this.eik = vatNumber;
        this.mol = mol;
        this.email = email;
        this.branch = branch;
        this.paymentType = PaymentType.BANK_TRANSFER;
    }

    public void addVisitor(Visitor visitor) {
        visitor.setRegistrant(this);
        visitors.add(visitor);
    }

    public List<Visitor> getVisitors() {
        return visitors;
    }

    public void setVisitors(List<Visitor> visitors) {
        this.visitors = visitors;
    }

    public boolean isCompany() {
        return company;
    }

    public void setCompany(boolean company) {
        this.company = company;
    }

    public boolean isStudent() {
        return student;
    }

    public void setStudent(boolean student) {
        this.student = student;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    /** The VAT number always starts with "BG" (as before). */
    public String getVatNumber() {
        if (vatNumber != null && !vatNumber.isEmpty() && !vatNumber.startsWith("BG")) {
            return "BG" + vatNumber;
        }
        return vatNumber;
    }

    public String getRawVatNumber() {
        return vatNumber;
    }

    public void setVatNumber(String vatNumber) {
        this.vatNumber = vatNumber;
    }

    public String getMol() {
        return mol;
    }

    public void setMol(String mol) {
        this.mol = mol;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEik() {
        return eik;
    }

    public void setEik(String eik) {
        this.eik = eik;
    }

    public long getEpayInvoiceNumber() {
        return epayInvoiceNumber == null ? 0L : epayInvoiceNumber;
    }

    public void setEpayInvoiceNumber(Long epayInvoiceNumber) {
        this.epayInvoiceNumber = epayInvoiceNumber;
    }

    public long getRealInvoiceNumber() {
        return realInvoiceNumber == null ? 0L : realInvoiceNumber;
    }

    public void setRealInvoiceNumber(Long realInvoiceNumber) {
        this.realInvoiceNumber = realInvoiceNumber;
    }

    public long getProformaInvoiceNumber() {
        return proformaInvoiceNumber == null ? 0L : proformaInvoiceNumber;
    }

    public void setProformaInvoiceNumber(Long proformaInvoiceNumber) {
        this.proformaInvoiceNumber = proformaInvoiceNumber;
    }

    public PaymentType getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(PaymentType paymentType) {
        this.paymentType = paymentType;
    }

    public Branch getBranch() {
        return branch;
    }

    public void setBranch(Branch branch) {
        this.branch = branch;
    }

    public EpayResponse getEpayResponse() {
        return epayResponse;
    }

    public void setEpayResponse(EpayResponse epayResponse) {
        this.epayResponse = epayResponse;
    }

    @Override
    public String toString() {
        return "Registrant{name='" + name + "', email='" + email + "', company=" + company + ", visitors="
            + visitors.size() + '}';
    }
}
