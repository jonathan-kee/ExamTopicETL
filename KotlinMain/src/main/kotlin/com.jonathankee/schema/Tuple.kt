package com.jonathankee.schema

class Tuple(
    var fileName: String?,
    var url: String
) {
    // Secondary no-argument constructor
    constructor() : this(null, "")

    override fun toString(): String {
        return "Tuple{fileName='$fileName', url='$url'}"
    }
}