package com.pyson

object NativeEngine {
    init {
        System.loadLibrary("pyson_engine")
    }

    external fun analyzeCodeFast(code: String): String
}
