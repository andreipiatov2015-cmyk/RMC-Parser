package com.rmc.parser.model;

import java.util.Objects;
import java.util.Optional;

/**
 * Организация (школа, центр и т.д.).
 */
public class Organization {
    
    private final String id;
    private final String name;
    private final String url;
    private final String address;
    private final String phone;
    private final String website;
    
    private Organization(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.url = builder.url;
        this.address = builder.address;
        this.phone = builder.phone;
        this.website = builder.website;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * @return ID учреждения (из ссылки /org/{id}/), может быть null,
     * если организация построена не из ссылки на её страницу.
     */
    public Optional<String> getId() {
        return Optional.ofNullable(id);
    }
    
    public String getName() {
        return name;
    }
    
    /**
     * @return относительная или абсолютная ссылка на страницу учреждения (/org/{id}/)
     */
    public Optional<String> getUrl() {
        return Optional.ofNullable(url);
    }
    
    public Optional<String> getAddress() {
        return Optional.ofNullable(address);
    }
    
    public Optional<String> getPhone() {
        return Optional.ofNullable(phone);
    }
    
    public Optional<String> getWebsite() {
        return Optional.ofNullable(website);
    }
    
    @Override
    public String toString() {
        return name != null ? name : "";
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Organization that = (Organization) o;
        if (id != null && that.id != null) {
            return id.equals(that.id);
        }
        return Objects.equals(name, that.name);
    }
    
    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : Objects.hashCode(name);
    }
    
    public static class Builder {
        
        private String id;
        private String name;
        private String url;
        private String address;
        private String phone;
        private String website;
        
        public Builder id(String id) {
            this.id = id;
            return this;
        }
        
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        public Builder url(String url) {
            this.url = url;
            return this;
        }
        
        public Builder address(String address) {
            this.address = address;
            return this;
        }
        
        public Builder phone(String phone) {
            this.phone = phone;
            return this;
        }
        
        public Builder website(String website) {
            this.website = website;
            return this;
        }
        
        public Organization build() {
            return new Organization(this);
        }
    }
}
