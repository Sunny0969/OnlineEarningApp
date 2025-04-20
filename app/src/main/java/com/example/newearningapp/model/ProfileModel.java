package com.example.newearningapp.model;

public class ProfileModel {
    private String name, email, image;
private int coins, spins;

    public ProfileModel() {
    }

    public ProfileModel(String name, String email, int coins, String image, int spins) {
        this.name = name;
        this.email = email;
        this.coins = coins;
        this.image = image;
        this.spins = spins;

    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getCoins() {
        return coins;
    }

    public void setCoins(int coins) {
        this.coins = coins;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    public String getImage() {
        return image;
    }
    public void setImage(String image) {
        this.image = image;
    }
    public int getSpins() {
        return spins;
    }
    public void setSpins(int spins) {
        this.spins = spins;
    }
}
