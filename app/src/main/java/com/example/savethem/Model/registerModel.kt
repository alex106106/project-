package com.example.savethem.Model

data class registerModel (
    var email: String? = "",
    var pass: String? = "",
    var name: String? = "",
    var UUID: String? = "",
    var token: String? = "",
    var profilePic: String? = ""
){
    constructor() : this("", "", "", "", "", "")
}