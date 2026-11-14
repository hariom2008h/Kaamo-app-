package com.kaamo.app.legal

import com.kaamo.app.model.ServiceCategory
import com.kaamo.app.model.NoticeLanguage
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle

/** Replace every TODO value before release and have Indian counsel approve all wording. */
object LegalConfig {
    const val TERMS_VERSION = "v1.0_2026"
    const val NOTICE_VERSION = "v1.0_2026"
    const val DECLARATION_VERSION = "v1.0_2026"

    const val TERMS_URL = "https://kaamo-web.vercel.app/terms.html"
    const val PRIVACY_URL = "https://kaamo-web.vercel.app/privacy.html"
    const val GRIEVANCE_EMAIL = "grievance@kaamo.com"      // TODO confirm
    const val GRIEVANCE_OFFICER = "[Name of Resident Grievance Officer]" // TODO appoint (IT Rules 2021, Rule 3(2))
    const val GRIEVANCE_ADDRESS = "[Registered office address]"          // TODO
}

object LegalCopy {

    const val DIRECTORY_ONLY_SHORT =
        "Kaamo is a directory only. We don't own, verify, insure or supervise any service provider, machine, operator or work."

    const val DIRECTORY_ONLY_FULL =
        "Kaamo is a neutral online directory that solely enables users to discover contact information of " +
            "other users who list local services, workers and machinery. Kaamo does not own, operate, maintain, insure " +
            "or control any machinery; does not employ, train or supervise any operator or driver; does not " +
            "verify the fitness, roadworthiness, insurance, licensing or suitability of any machine or operator; " +
            "does not select, recommend, rank or match users; does not take part in any hire or service contract; and " +
            "does not handle any payment. All commercial terms, safety checks and payments are solely between " +
            "the users."

    const val GRIEVANCE_SLA =
        "We acknowledge every complaint within 24 hours and aim to resolve it within 15 days."

    // ---- Pre-call acknowledgement (shown before every call) ----
    const val PRECALL_EN =
        "You are speaking directly with the service provider. Kaamo does not verify the person, machine, " +
            "licence, insurance or skill. Please check these yourself and agree the price before any work begins."
    const val PRECALL_HI =
        "आप सीधे सेवा देने वाले से बात कर रहे हैं। Kaamo किसी व्यक्ति, मशीन, लाइसेंस, बीमा या हुनर की जाँच नहीं करता। " +
            "काम शुरू करने से पहले इन्हें खुद जाँच लें और दाम पहले तय कर लें।"
    const val PRECALL_CHECK_EN = "I understand and will check the provider, documents and site safety myself."
    const val PRECALL_CHECK_HI = "मैं समझता/समझती हूँ और सेवा देने वाले, दस्तावेज़ व साइट की सुरक्षा खुद जाँचूँगा/जाँचूँगी।"

    fun phoneNotice(language: NoticeLanguage): String = when (language) {
        NoticeLanguage.English ->
            "We collect your name, mobile number, village or area and service category to create and display your " +
                "service-provider listing. If you turn on \"Show my number\", your mobile number will be visible " +
                "to Kaamo customers, who may contact you directly by phone. Kaamo does not arrange or " +
                "guarantee any resulting hire. You can turn off phone visibility or remove your listing in " +
                "your account at any time. This stops new Kaamo disclosures but cannot retrieve information " +
                "already copied by third parties."
        NoticeLanguage.Hindi ->
            "हम आपकी सेवा देने वाले की लिस्टिंग बनाने और दिखाने के लिए आपका नाम, मोबाइल नंबर, गाँव/इलाका और सेवा की श्रेणी लेते हैं। " +
                "यदि आप \"मेरा नंबर दिखाएँ\" चालू करते हैं, तो आपका मोबाइल नंबर Kaamo के ग्राहकों को दिखेगा और वे आपको " +
                "सीधे फ़ोन कर सकते हैं। Kaamo किसी भी किराये की व्यवस्था नहीं करता और न ही उसकी गारंटी देता है। आप कभी भी " +
                "अपने अकाउंट में फ़ोन नंबर दिखाना बंद कर सकते हैं या अपनी लिस्टिंग हटा सकते हैं। इससे Kaamo द्वारा आगे का " +
                "प्रदर्शन रुक जाएगा, लेकिन जो जानकारी तीसरे पक्ष पहले ही कॉपी कर चुके हैं उसे वापस नहीं लिया जा सकता।"
    }

    /** Owner's self-declaration. Accepted via an UNTICKED checkbox; stored with version + timestamp. */
    fun declaration(machine: ServiceCategory, language: NoticeLanguage): List<String> {
        val hi = language == NoticeLanguage.Hindi
        val intro = if (hi) "मैं पुष्टि करता/करती हूँ कि मेरे द्वारा दिए गए सभी विवरण और दस्तावेज़ सही और वैध हैं।"
        else "I confirm that the details and documents I submitted are accurate and current."

        val specific = when (machine) {
            ServiceCategory.Tractor -> if (hi)
                "मैं अपने ट्रैक्टर का उपयोग और लिस्टिंग केवल खेती के कामों (जुताई, बुआई, कटाई, थ्रेशिंग) के लिए करूँगा/करूँगी। " +
                    "Kaamo के माध्यम से किराये पर रेत, बजरी, ईंट, मिट्टी या अन्य निर्माण सामग्री नहीं ढोऊँगा/ढोऊँगी।"
            else "I will use and list my tractor only for lawful farm work (tillage, sowing, harvesting, threshing). " +
                "I will not carry sand, bricks, earth or other construction material for hire through Kaamo."
            ServiceCategory.Jcb -> if (hi)
                "मेरी मशीन का RTO फिटनेस सर्टिफिकेट (जहाँ आवश्यक हो) वैध है और हर ऑपरेटर के पास वैध लाइसेंस है।"
            else "My machine has a valid RTO fitness certificate where required, and every operator holds a valid licence."
            ServiceCategory.Transport -> if (hi)
                "मेरा वाहन माल ढुलाई के लिए पंजीकृत, परमिट-युक्त और बीमा-युक्त है, और हर ड्राइवर के पास वैध लाइसेंस है।"
            else "My vehicle is registered, permitted and insured for goods carriage, and every driver holds a valid licence."
            ServiceCategory.Borewell -> if (hi)
                "मेरी बोरवेल मशीन और ऑपरेटर पर लागू नियम लागू हैं और जहाँ अनुमति ज़रूरी है वहाँ मेरे पास वह अनुमति है।"
            else "My borewell machine and operators follow the applicable rules, and I hold any permission required where I work."
            ServiceCategory.Plumber, ServiceCategory.Electrician, ServiceCategory.Construction -> if (hi)
                "मैं स्वतंत्र रूप से काम करता/करती हूँ और अपने काम के लिए स्वयं ज़िम्मेदार हूँ।"
            else "I work independently and I am responsible for my own work."
        }

        val directory = if (hi)
            "मैं समझता/समझती हूँ कि Kaamo केवल एक डिजिटल डायरेक्टरी है। वह किसी मशीन की फिटनेस या काम की गुणवत्ता की गारंटी नहीं देता।"
        else "I understand Kaamo is only a digital directory. It does not guarantee any machine's fitness or the quality of any work."

        val responsibility = if (hi)
            "किसी भी हादसे, जुर्माने या चालान के लिए मैं स्वयं ज़िम्मेदार रहूँगा/रहूँगी और Kaamo को संबंधित दावों से मुक्त रखूँगा/रखूँगी।"
        else "I am personally responsible for any accident, fine or challan, and I will keep Kaamo harmless from related claims."

        val validity = if (hi)
            "मैं अपने दस्तावेज़ वैध रखूँगा/रखूँगी। समाप्त हो चुके दस्तावेज़ों पर मेरी लिस्टिंग अपने-आप छिप जाएगी, और जानकारी गलत पाए " +
                "जाने या प्रतिबंधित उपयोग मिलने पर Kaamo लिस्टिंग निलंबित या हटा सकता है।"
        else "I will keep my documents valid. My listing is hidden automatically when a document expires, and Kaamo may " +
            "suspend or remove it if information is false or a prohibited use is detected."

        return listOf(intro, specific, directory, responsibility, validity)
    }
}

/** DD/MM/YYYY parsing for document expiry fields. */
object DateInput {
    private val format = DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT)

    fun parse(text: String): LocalDate? = try {
        LocalDate.parse(text.trim(), format)
    } catch (e: DateTimeParseException) {
        null
    }

    fun isFutureDate(text: String): Boolean = parse(text)?.isAfter(LocalDate.now()) == true
}

/**
 * Client-side keyword guard for construction-material haulage (MVA s.66 / s.192A risk).
 * This is only a UX hint. The same blacklist MUST be enforced and reviewed server-side.
 */
object ProhibitedTerms {
    private val terms = setOf(
        "sand", "bajri", "bajari", "mitti", "eent", "int", "brick", "bricks", "cement", "mining",
        "रेत", "बजरी", "मिट्टी", "ईंट",
    )
    private val splitter = Regex("[^\\p{L}\\p{M}]+")

    fun isFlagged(text: String): Boolean =
        text.lowercase().split(splitter).any { it in terms }
}
