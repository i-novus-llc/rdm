package ru.inovus.ms.rdm.model;

public class FileModel {
    private String path;
    private String name;

    public FileModel() {
    }

    public FileModel(String path, String name) {
        this.path = path;
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
