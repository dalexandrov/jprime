package org.bgjug.jprime.core.model;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "Article")
public class Article extends AbstractEntity {

    @NotBlank
    @Column(name = "title", nullable = false)
    private String title = "";

    @Column(name = "description")
    private String description;

    @Column(name = "published", nullable = false)
    private boolean published;

    /** HTML content. */
    @NotNull
    @Lob
    @Column(name = "text", length = 10000, nullable = false, columnDefinition = "TEXT")
    private String text;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author", foreignKey = @ForeignKey(name = "fk_article_author"))
    private User author;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "tags_articles",
        joinColumns = @JoinColumn(name = "article_pk", foreignKey = @ForeignKey(name = "fk_tag_article")),
        inverseJoinColumns = @JoinColumn(name = "tag_pk", foreignKey = @ForeignKey(name = "fk_article_tag")),
        indexes = @Index(columnList = "article_pk"))
    private Set<Tag> tags = new HashSet<>();

    public Article() {
    }

    public Article(String title, String text) {
        this.title = title;
        this.text = text;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    public Set<Tag> getTags() {
        return tags;
    }

    public void setTags(Set<Tag> tags) {
        this.tags = tags;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Article article)) {
            return false;
        }
        return Objects.equals(getId(), article.getId()) && Objects.equals(title, article.title);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(title);
    }
}
