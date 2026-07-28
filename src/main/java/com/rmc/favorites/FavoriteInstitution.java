package com.rmc.favorites;

/**
 * Учреждение, добавленное в "Избранное" — храним только ID (как на сайте,
 * /org/{id}/) и название, показанное в фильтре на момент добавления.
 */
public class FavoriteInstitution {
    
    private final String id;
    private final String name;
    
    public FavoriteInstitution(String id, String name) {
        this.id = id;
        this.name = name;
    }
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
}
