package com.martinfricke;

public class Article {

    private final String description;
    private final double price;

    public Article(String description, double price) {
        this.description = description;
        this.price = price;
    }

    public String getDescription() {
        return description;
    }

    public double getPrice() {
        return price;
    }
}
