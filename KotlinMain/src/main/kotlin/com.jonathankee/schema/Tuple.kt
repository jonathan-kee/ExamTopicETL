package com.jonathankee.schema

class Tuple {
    var fileName: String
    var url: String

    constructor(fileName: String, url: String) {
        this.fileName = fileName
        this.url = url
    }

    constructor() {
        this.fileName = ""
        this.url = ""
    }

    override fun toString(): String {
        return "Tuple{" +
                "fileName='" + fileName + '\'' +
                ", url='" + url + '\'' +
                '}'
    }
}