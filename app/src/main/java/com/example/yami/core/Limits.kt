package com.example.yami.core

data class Limits(
    val dailyLimit: Int,
    val appLimitEnabled: Boolean,
    val appLimit: Int,
    val breakAfter: Int
)
