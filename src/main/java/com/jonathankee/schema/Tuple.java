package com.jonathankee.schema;

public class Tuple {
    String fileName;
    String url;

    public Tuple(String fileName, String url) {
        this.fileName = fileName;
        this.url = url;
    }

    public Tuple() {
        this.fileName = "";
        this.url = "";
    }

    public String getFileName() {
        return fileName;
    }

    public String getUrl() {
        return url;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return "Tuple{" +
                "fileName='" + fileName + '\'' +
                ", url='" + url + '\'' +
                '}';
    }
}
