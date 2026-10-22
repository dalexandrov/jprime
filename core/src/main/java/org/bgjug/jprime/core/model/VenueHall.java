package org.bgjug.jprime.core.model;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "VenueHall")
public class VenueHall extends AbstractEntity {

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "map", columnDefinition = "LONGBLOB")
    private byte[] map;

    public VenueHall() {
    }

    public VenueHall(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public byte[] getMap() {
        return map;
    }

    public void setMap(byte[] map) {
        this.map = map;
    }

    @Override
    public String toString() {
        return name;
    }
}
