package com.jonathankee.schema

class Tuple (var fileName: String,
             var url: String){

    override fun toString(): String {
        return "Tuple{" +
                "fileName='" + fileName + '\'' +
                ", url='" + url + '\'' +
                '}'
    }
}