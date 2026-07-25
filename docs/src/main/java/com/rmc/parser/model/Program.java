package com.rmc.parser.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Программа (кружок, секция и т.д.).
 */
public class Program {
    
    private final String id;
    private final String title;
    private final String description;
    private final String url;
    private final Organization organization;
    private final List<Teacher> teachers;
    private final String direction;
    private final String activity;
    private final String age;
    private final String hours;
    private final String price;
    private final String schedule;
    private final String imageUrl;
    
    private Program(Builder builder) {
        this.id = builder.id;
        this.title = builder.title;
        this.description = builder.description;
        this.url = builder.url;
        this.organization = builder.organization;
        this.teachers = List.copyOf(builder.teachers);
        this.direction = builder.direction;
        this.activity = builder.activity;
        this.age = builder.age;
        this.hours = builder.hours;
        this.price = builder.price;
        this.schedule = builder.schedule;
        this.imageUrl = builder.imageUrl;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public String getId() {
        return id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }
    
    public Optional<String> getUrl() {
        return Optional.ofNullable(url);
    }
    
    public Optional<Organization> getOrganization() {
        return Optional.ofNullable(organization);
    }
    
    public List<Teacher> getTeachers() {
        return teachers;
    }
    
    public Optional<String> getDirection() {
        return Optional.ofNullable(direction);
    }
    
    public Optional<String> getActivity() {
        return Optional.ofNullable(activity);
    }
    
    public Optional<String> getAge() {
        return Optional.ofNullable(age);
    }
    
    public Optional<String> getHours() {
        return Optional.ofNullable(hours);
    }
    
    public Optional<String> getPrice() {
        return Optional.ofNullable(price);
    }
    
    public Optional<String> getSchedule() {
        return Optional.ofNullable(schedule);
    }
    
    public Optional<String> getImageUrl() {
        return Optional.ofNullable(imageUrl);
    }
    
    @Override
    public String toString() {
        return "Program{" +
                "title='" + title + '\'' +
                ", organization=" + organization +
                '}';
    }
    
    public static class Builder {
        
        private String id;
        private String title;
        private String description;
        private String url;
        private Organization organization;
        private List<Teacher> teachers = new ArrayList<>();
        private String direction;
        private String activity;
        private String age;
        private String hours;
        private String price;
        private String schedule;
        private String imageUrl;
        
        public Builder id(String id) {
            this.id = id;
            return this;
        }
        
        public Builder title(String title) {
            this.title = title;
            return this;
        }
        
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        public Builder url(String url) {
            this.url = url;
            return this;
        }
        
        public Builder organization(Organization organization) {
            this.organization = organization;
            return this;
        }
        
        public Builder addTeacher(Teacher teacher) {
            this.teachers.add(teacher);
            return this;
        }
        
        public Builder teachers(List<Teacher> teachers) {
            this.teachers = new ArrayList<>(teachers);
            return this;
        }
        
        public Builder direction(String direction) {
            this.direction = direction;
            return this;
        }
        
        public Builder activity(String activity) {
            this.activity = activity;
            return this;
        }
        
        public Builder age(String age) {
            this.age = age;
            return this;
        }
        
        public Builder hours(String hours) {
            this.hours = hours;
            return this;
        }
        
        public Builder price(String price) {
            this.price = price;
            return this;
        }
        
        public Builder schedule(String schedule) {
            this.schedule = schedule;
            return this;
        }
        
        public Builder imageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
            return this;
        }
        
        public Program build() {
            return new Program(this);
        }
    }
}
