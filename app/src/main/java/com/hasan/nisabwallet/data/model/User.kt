package com.hasan.nisabwallet.data.model

data class User(
    val uid: String       = "",
    val email: String     = "",
    val displayName: String = "",
    val createdAt: Long   = 0L,
    val currency: String  = "BDT",
    val language: String  = "en",
    val theme: String     = "system"
)
