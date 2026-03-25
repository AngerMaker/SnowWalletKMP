package com.zanini.snowwallet

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform