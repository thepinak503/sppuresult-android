package pinak.sppunotify.util

object DepartmentClassifier {
    
    val departments = listOf(
        "All", "FE", "SE", "TE", "BE",
        "MBA", "MCA", "M.Sc", "M.A./M.Com",
        "B.Sc", "B.Com", "BBA/BCA", "B.A.",
        "B.Pharm", "Other UG", "Other PG",
        "Law", "Diploma",
    )

    fun classify(title: String): String {
        val n = title.replace(YEAR_PREFIX_REGEX, "").replace(FY_REGEX, "")
        val nu = n.uppercase()
        
        return when {
            FE_REGEX.containsMatchIn(title) || FE_LONG_REGEX.containsMatchIn(title) -> "FE"
            SE_REGEX.containsMatchIn(title) -> "SE"
            TE_REGEX.containsMatchIn(title) -> "TE"
            BE_REGEX.containsMatchIn(title) -> "BE"
            MBA_REGEX.containsMatchIn(n) || nu.contains("MBA") || MBA_LONG_REGEX.containsMatchIn(nu) -> "MBA"
            MCA_REGEX.containsMatchIn(n) || MCA_LONG_REGEX.containsMatchIn(nu) -> "MCA"
            MSC_REGEX.containsMatchIn(n) -> "M.Sc"
            MA_MCOM_REGEX.containsMatchIn(n) || MA_MCOM_LONG_REGEX.containsMatchIn(nu) -> "M.A./M.Com"
            LAW_REGEX.containsMatchIn(title) || LAW_REGEX.containsMatchIn(n) -> "Law"
            DIPLOMA_REGEX.containsMatchIn(title) || PG_DIPLOMA_REGEX.containsMatchIn(title) -> "Diploma"
            OTHER_PG_REGEX.containsMatchIn(n) || OTHER_PG_LONG_REGEX.containsMatchIn(nu) || MASTER_OF_REGEX.containsMatchIn(n) -> "Other PG"
            
            YEAR_PREFIX_REGEX.containsMatchIn(title) || FY_REGEX.containsMatchIn(title) -> {
                when {
                    BSC_LONG_REGEX.containsMatchIn(nu) || BSC_REGEX.containsMatchIn(n) -> "B.Sc"
                    BCOM_LONG_REGEX.containsMatchIn(nu) || BCOM_REGEX.containsMatchIn(n) -> "B.Com"
                    BA_LONG_REGEX.containsMatchIn(nu) || BA_REGEX.containsMatchIn(n) -> "B.A."
                    BBA_BCA_LONG_REGEX.containsMatchIn(nu) || BBA_BCA_REGEX.containsMatchIn(n) -> "BBA/BCA"
                    else -> "Other UG"
                }
            }
            
            BSC_REGEX.containsMatchIn(n) -> "B.Sc"
            BCOM_REGEX.containsMatchIn(n) -> "B.Com"
            BBA_BCA_DIRECT_REGEX.containsMatchIn(n) -> "BBA/BCA"
            BA_REGEX.containsMatchIn(n) -> "B.A."
            BPHARM_REGEX.containsMatchIn(n) -> "B.Pharm"
            OTHER_UG_CODES_REGEX.containsMatchIn(n) -> "Other UG"
            nu.startsWith("BACHELOR OF") || nu.startsWith("BACHELOR IN") -> {
                when {
                    nu.contains("SCIENCE") -> "B.Sc"
                    nu.contains("COMMERCE") -> "B.Com"
                    nu.contains("ARTS") -> "B.A."
                    nu.contains("BUSINESS ADMINISTRATION") || nu.contains("COMPUTER APPLICATION") -> "BBA/BCA"
                    else -> "Other UG"
                }
            }
            else -> "Other UG"
        }
    }

    private val YEAR_PREFIX_REGEX = Regex("^(FIRST|SECOND|THIRD|FOURTH|FINAL)\\s+YEAR\\s+", RegexOption.IGNORE_CASE)
    private val FY_REGEX = Regex("^FirstYear\\s+", RegexOption.IGNORE_CASE)
    
    private val FE_REGEX = Regex("^F\\.?\\s*E\\.?\\s*(\\(|\\d)", RegexOption.IGNORE_CASE)
    private val FE_LONG_REGEX = Regex("^FIRST\\s+YEAR\\s+ENGINEERING", RegexOption.IGNORE_CASE)
    private val SE_REGEX = Regex("^S\\.?\\s*E\\.?\\s*(\\(|\\d)", RegexOption.IGNORE_CASE)
    private val TE_REGEX = Regex("^T\\.?\\s*E\\.?\\s*(\\(|\\d)", RegexOption.IGNORE_CASE)
    private val BE_REGEX = Regex("^B\\.?\\s*E\\.?\\s*(\\(|\\d)", RegexOption.IGNORE_CASE)
    
    private val MBA_REGEX = Regex("^M\\.?\\s*B\\.?\\s*A", RegexOption.IGNORE_CASE)
    private val MBA_LONG_REGEX = Regex("MASTER\\s+OF\\s+BUSINESS\\s+ADMINISTRATION", RegexOption.IGNORE_CASE)
    private val MCA_REGEX = Regex("^M\\.?(CA|CS)\\b", RegexOption.IGNORE_CASE)
    private val MCA_LONG_REGEX = Regex("MASTER\\s+OF\\s+COMPUTER\\s+(?:APPLICATION|APPLICATIONS)", RegexOption.IGNORE_CASE)
    private val MSC_REGEX = Regex("^M\\.?\\s*SC\\b", RegexOption.IGNORE_CASE)
    private val MA_MCOM_REGEX = Regex("^M\\.?\\s*(?:A|COM)\\b", RegexOption.IGNORE_CASE)
    private val MA_MCOM_LONG_REGEX = Regex("^MASTER\\s+OF\\s+(?:COMMERCE|ARTS)", RegexOption.IGNORE_CASE)
    
    private val OTHER_PG_REGEX = Regex("^M\\.?\\s*(?:PHARM|ARCH|ED|E\\.?)\\b", RegexOption.IGNORE_CASE)
    private val OTHER_PG_LONG_REGEX = Regex("^MASTER\\s+OF\\s+(?:LIBRARY|EDUCATION|HOSPITAL|PHARMACY|ARCHITECTURE|ENGINEERING)", RegexOption.IGNORE_CASE)
    private val MASTER_OF_REGEX = Regex("^MASTERS?\\s+(?:OF|IN)\\b", RegexOption.IGNORE_CASE)
    
    private val LAW_REGEX = Regex("(^LL[BMD]\\b|^B\\.?\\s*A\\.?\\s*LL)", RegexOption.IGNORE_CASE)
    private val DIPLOMA_REGEX = Regex("^(DIPLOMA|POST\\s+GRADUATE\\s+DIPLOMA)", RegexOption.IGNORE_CASE)
    private val PG_DIPLOMA_REGEX = Regex("^POST\\s+GRADUATE\\s+", RegexOption.IGNORE_CASE)
    
    private val BSC_REGEX = Regex("B\\.?\\s*SC\\b", RegexOption.IGNORE_CASE)
    private val BSC_LONG_REGEX = Regex("BACHELOR\\s+OF\\s+SCIENCE", RegexOption.IGNORE_CASE)
    private val BCOM_REGEX = Regex("B\\.?\\s*COM\\b", RegexOption.IGNORE_CASE)
    private val BCOM_LONG_REGEX = Regex("BACHELOR\\s+OF\\s+COMMERCE", RegexOption.IGNORE_CASE)
    private val BA_REGEX = Regex("B\\.?\\s*A\\b(?!\\.?\\s*LL)", RegexOption.IGNORE_CASE)
    private val BA_LONG_REGEX = Regex("BACHELOR\\s+OF\\s+ARTS", RegexOption.IGNORE_CASE)
    private val BBA_BCA_REGEX = Regex("(B\\.?\\s*B\\.?\\s*A|B\\.?\\s*C\\.?\\s*A)", RegexOption.IGNORE_CASE)
    private val BBA_BCA_LONG_REGEX = Regex("BACHELOR\\s+OF\\s+(BUSINESS\\s+ADMINISTRATION|COMPUTER\\s+APPLICATION)", RegexOption.IGNORE_CASE)
    private val BBA_BCA_DIRECT_REGEX = Regex("^B\\.?(?:BA|B\\.?\\s*A|CA)\\b", RegexOption.IGNORE_CASE)
    private val BPHARM_REGEX = Regex("B\\.?PHR?ARM", RegexOption.IGNORE_CASE)
    private val OTHER_UG_CODES_REGEX = Regex("^B\\.?(?:ARCH|ED|HMCT)\\b", RegexOption.IGNORE_CASE)
}
