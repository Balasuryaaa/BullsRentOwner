package com.example.bullsrentowner

data class ListingCustomer(
    var productName: String? = null,
    var rentType: String? = null,
    var rentPrice: String? = null,
    var description: String? = null,
    var location: String? = null,
    var ownerName: String? = null,
    var ownerPhone: String? = null,
    var imageUrls: List<String> = emptyList(),
    var imageBase64: List<String> = emptyList()
)
