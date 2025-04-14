package com.newssearch.model;

import java.util.Date;

public class Article {
    private int index;
    private String author;
    private Date datePublished;
    private String category;
    private String section;
    private String url;
    private String headline;
    private String description;
    private String keywords;
    private String secondHeadline;
    private String articleText;

    public Article() {
    }
   
    public Article(int index, String author, Date datePublished, String category, String section, String url,
                   String headline, String description, String keywords, String secondHeadline, String articleText) {
        this.index = index;
        this.author = author;
        this.datePublished = datePublished;
        this.category = category;
        this.section = section;
        this.url = url;
        this.headline = headline;
        this.description = description;
        this.keywords = keywords;
        this.secondHeadline = secondHeadline;
        this.articleText = articleText;
    }

    // Getters and Setters for each field
    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public Date getDatePublished() {
        return datePublished;
    }

    public void setDatePublished(Date datePublished) {
        this.datePublished = datePublished;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getHeadline() {
        return headline;
    }

    public void setHeadline(String headline) {
        this.headline = headline;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public String getSecondHeadline() {
        return secondHeadline;
    }

    public void setSecondHeadline(String secondHeadline) {
        this.secondHeadline = secondHeadline;
    }

    public String getArticleText() {
        return articleText;
    }

    public void setArticleText(String articleText) {
        this.articleText = articleText;
    }
}
