package com.kaamo.app.model

/** Placeholder data for demo mode (no Supabase keys). Names are made up and the phone numbers are deliberately invalid so a test call never rings a real person. */
val SamplePartners: List<Partner> = listOf(
    Partner("1", "Ramesh Yadav", ServiceCategory.Plumber, listOf("Leak repair", "Bathroom fitting"), "Arera Colony", "Bhopal", 2, maskIndianMobile("0000000001"), true, null),
    Partner("2", "Suresh Patel", ServiceCategory.Jcb, listOf("Excavation", "Loading"), "Nandgaon", "Bhopal", 7, maskIndianMobile("0000000002"), true, null),
    Partner("3", "Imran Khan", ServiceCategory.Borewell, listOf("Borewell drilling"), "Kolar", "Bhopal", 9, maskIndianMobile("0000000003"), false, null),
    Partner("4", "Dinesh Kumar", ServiceCategory.Construction, listOf("Mason (mistri)", "Tiles"), "Salempur", "Sehore", 15, maskIndianMobile("0000000004"), true, "₹900 / day"),
    Partner("5", "Anil Verma", ServiceCategory.Electrician, listOf("House wiring"), "Bairagarh", "Bhopal", 12, maskIndianMobile("0000000005"), true, null),
    Partner("6", "Kishan Lal", ServiceCategory.Tractor, listOf("Cultivator", "Thresher"), "Chandpur", "Raisen", 26, maskIndianMobile("0000000006"), false, "₹800 / hour"),
    Partner("7", "Mohan Sahu", ServiceCategory.Plumber, listOf("Pipe fitting", "Water tank"), "Habibganj", "Bhopal", 4, maskIndianMobile("0000000007"), true, "₹400 / visit"),
    Partner("8", "Geeta Bai", ServiceCategory.Construction, listOf("Painter"), "Misrod", "Bhopal", 11, maskIndianMobile("0000000008"), true, "₹700 / day"),
    Partner("9", "Salim Ansari", ServiceCategory.Electrician, listOf("Fan / light repair", "Inverter / UPS"), "Shahpura", "Bhopal", 8, maskIndianMobile("0000000009"), true, null),
    Partner("10", "Rakesh Jain", ServiceCategory.Transport, emptyList(), "Govindpura", "Bhopal", 6, maskIndianMobile("0000000010"), true, null),
    Partner("11", "Vijay Singh", ServiceCategory.Jcb, listOf("Leveling", "Demolition"), "Ratibad", "Bhopal", 13, maskIndianMobile("0000000011"), true, null),
    Partner("12", "Pappu Lodhi", ServiceCategory.Construction, listOf("Labour", "Mason (mistri)"), "Kolar Road", "Bhopal", 8, maskIndianMobile("0000000012"), true, "₹650 / day"),
)

/** Demo coordinates (around Bhopal) so distance can be computed from the user's live GPS in demo mode. */
val SampleCoordinates: Map<String, Pair<Double, Double>> = mapOf(
    "1" to (23.22 to 77.43), "2" to (23.31 to 77.36), "3" to (23.18 to 77.42), "4" to (23.20 to 77.09),
    "5" to (23.28 to 77.34), "6" to (23.33 to 77.79), "7" to (23.23 to 77.44), "8" to (23.18 to 77.47),
    "9" to (23.19 to 77.43), "10" to (23.24 to 77.47), "11" to (23.15 to 77.36), "12" to (23.19 to 77.42),
)
