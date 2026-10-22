package org.bgjug.jprime.core.model;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "Tag")
public class Tag extends AbstractEntity {

    @NotBlank
    @Column(name = "name")
    private String name;

    @ManyToMany(mappedBy = "tags")
    private Set<Article> articles = new HashSet<>();

    public Tag() {
    }

    public Tag(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<Article> getArticles() {
        return articles;
    }

    /** Tags named after a year ("2019") are archive tags and are not shown in the main menu. */
    public boolean isYearTag() {
        return name != null && name.startsWith("20");
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Tag tag && Objects.equals(name, tag.name));
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }

    @Override
    public String toString() {
        return name;
    }
}
