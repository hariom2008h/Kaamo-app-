package com.kaamo.app.model

/** Placeholder data for demo mode (no Supabase keys). Names are made up. */
val SamplePartners: List<Partner> = listOf(
    Partner("1", "Ramesh Yadav", ServiceCategory.Plumber, listOf("Leak repair", "Bathroom fitting"), "Arera Colony", "Bhopal", 2, maskIndianMobile("9000000001"), true, null),
    Partner("2", "Suresh Patel", ServiceCategory.Jcb, listOf("Excavation", "Loading"), "Nandgaon", "Bhopal", 7, maskIndianMobile("9000000002"), true, null),
    Partner("3", "Imran Khan", ServiceCategory.Borewell, listOf("Borewell drilling"), "Kolar", "Bhopal", 9, maskIndianMobile("9000000003"), false, null),
    Partner("4", "Dinesh Kumar", ServiceCategory.Construction, listOf("Mason (mistri)", "Tiles"), "Salempur", "Sehore", 15, maskIndianMobile("9000000004"), true, "₹900 / day"),
    Partner("5", "Anil Verma", ServiceCategory.Electrician, listOf("House wiring"), "Bairagarh", "Bhopal", 12, maskIndianMobile("9000000005"), true, null),
    Partner("6", "Kishan Lal", ServiceCategory.Tractor, listOf("Cultivator", "Thresher"), "Chandpur", "Raisen", 26, maskIndianMobile("9000000006"), false, "₹800 / hour"),
)
