package com.rmc.parser.model;

import java.util.Optional;

/**
 * Преподаватель.
 */
public class Teacher {
    
    private final String name;
    private final String qualification;
    private final String experience;
    
    private Teacher(Builder builder) {
        this.name = builder.name;
        this.qualification = builder.qualification;
        this.experience = builder.experience;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public String getName() {
        return name;
    }
    
    public Optional<String> getQualification() {
        return Optional.ofNullable(qualification);
    }
    
    public Optional<String> getExperience() {
        return Optional.ofNullable(experience);
    }
    
    @Override
    public String toString() {
        return name != null ? name : "";
    }
    
    public static class Builder {
        
        private String name;
        private String qualification;
        private String experience;
        
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        public Builder qualification(String qualification) {
            this.qualification = qualification;
            return this;
        }
        
        public Builder experience(String experience) {
            this.experience = experience;
            return this;
        }
        
        public Teacher build() {
            return new Teacher(this);
        }
    }
}
