package com.rmc.parser.model;

import java.util.Optional;

/**
 * Организация (школа, центр и т.д.).
 */
public class Organization {
    
    private final String name;
    private final String address;
    private final String phone;
    private final String website;
    
    private Organization(Builder builder) {
        this.name = builder.name;
        this.address = builder.address;
        this.phone = builder.phone;
        this.website = builder.website;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public String getName() {
        return name;
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
    
    public static class Builder {
        
        private String name;
        private String address;
        private String phone;
        private String website;
        
        public Builder name(String name) {
            this.name = name;
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
