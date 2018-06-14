package org.apache.catalina.startup;

public class ClassLoaderFactoryRepository {
    private String location;
    private ClassLoaderFactoryRepositoryType type;
    
    public ClassLoaderFactoryRepository(String location, ClassLoaderFactoryRepositoryType type) {
        this.location = location;
        this.type = type;
    }
    
    public String getLocation() {
        return location;
    }
    
    public ClassLoaderFactoryRepositoryType getType() {
        return type;
    }
}