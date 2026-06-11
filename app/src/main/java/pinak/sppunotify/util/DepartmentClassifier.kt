package pinak.sppunotify.util

object DepartmentClassifier {
    
    val departments = listOf(
        "All", "FE", "SE", "TE", "BE",
        "MBA", "MCA", "M.Sc", "M.A./M.Com",
        "B.Sc", "B.Com", "BBA/BCA", "B.A.",
        "B.Pharm", "M.Pharm", "B.Ed", "M.Ed",
        "B.Arch", "M.Arch", "LL.B", "LL.M",
        "HMCT", "B.Voc", "Other UG", "Other PG",
        "Law", "Diploma",
    )

    /**
     * Comprehensive suggested keywords for the watchlist — extracted from actual SPPU result portal data.
     * Organized by category for clarity. Every keyword here appears in real SPPU result titles.
     */
    val suggestedKeywords = listOf(
        // ═══ YEAR LEVELS ═══
        "First Year", "Second Year", "Third Year", "Fourth Year", "Final Year",
        "F.E.", "S.E.", "T.E.", "B.E.", "M.E.",
        "FE", "SE", "TE", "BE", "ME",
        // ═══ ENGINEERING SPECIALIZATIONS ═══
        "Computer Engineering", "Mechanical", "Civil", "Electrical", "E&TC",
        "Information Technology", "Electronics", "Instrumentation",
        "Artificial Intelligence", "Machine Learning", "Data Science", "Robotics",
        "Automation", "Cyber Security", "AI and ML",
        // ═══ MBA PROGRAMS ═══
        "MBA", "Master of Business Administration",
        "MBA (Digital Marketing)", "MBA (Fintech)", "MBA (Project Management)",
        "MBA (Service Management)", "MBA (Information Technology)",
        "MBA (Human Resource Development)", "MBA (HRD)",
        // ═══ MCA PROGRAMS ═══
        "MCA", "Master of Computer Application", "MCA Integrated", "MCA (Engineering)",
        // ═══ BBA / BCA PROGRAMS ═══
        "BBA", "Bachelor of Business Administration",
        "BBA(CA)", "BBA (Computer Application)",
        "BBA(IB)", "BBA (International Business)",
        "BCA", "Bachelor of Computer Applications",
        "BCA (Science)", "BCA (Commerce and Management)",
        // ═══ B.Com PROGRAMS ═══
        "B.Com", "Bachelor of Commerce",
        "B.Com (Business Management)", "B.Com (Computer Application)",
        "B.Com (International Business)", "B.Com (CBCS)",
        // ═══ B.Sc PROGRAMS ═══
        "B.Sc", "Bachelor of Science",
        "B.Sc (Computer Science)", "B.Sc (Computer Application)",
        "B.Sc (Biotechnology)", "B.Sc (Cyber and Digital Science)",
        "B.Sc (Hospitality Studies)", "B.Sc (Animation)",
        "B.Sc (Data Science)", "B.Sc (Information Technology)",
        "B.Sc (Home Science)", "B.Sc (Fashion Design)",
        "B.Sc (Artificial Intelligence and Machine Learning)",
        "B.Sc (Wine Brewing and Alcohol Technology)",
        // ═══ M.Sc PROGRAMS ═══
        "M.Sc", "Master of Science",
        "M.Sc (Computer Science)", "M.Sc (Computer Applications)",
        "M.Sc (Mathematics)", "M.Sc (Physics)", "M.Sc (Chemistry)",
        "M.Sc (Organic Chemistry)", "M.Sc (Inorganic Chemistry)",
        "M.Sc (Physical Chemistry)", "M.Sc (Analytical Chemistry)",
        "M.Sc (Botany)", "M.Sc (Zoology)", "M.Sc (Microbiology)",
        "M.Sc (Biotechnology)", "M.Sc (Electronics)",
        "M.Sc (Environmental Science)", "M.Sc (Geography)",
        "M.Sc (Statistics)", "M.Sc (Drug Chemistry)",
        "M.Sc (Bio-Chemistry)", "M.Sc (IMCA)",
        // ═══ MA / M.Com PROGRAMS ═══
        "MA", "M.A.", "Master of Arts",
        "M.Com", "Master of Commerce",
        "MA (Journalism and Mass Communication)",
        "MA/M.Sc Geography", "MA/M.Sc Statistics",
        // ═══ BA / B.A. PROGRAMS ═══
        "B.A.", "Bachelor of Arts",
        // ═══ LAW PROGRAMS ═══
        "LLB", "LL.B", "LLM", "LL.M",
        "B.A.LL.B", "BA LLB",
        "Diploma in Taxation Law", "Diploma in Intellectual Property Rights",
        "Diploma in Cyber Laws", "Diploma in Labour Laws",
        "Course in Forensic and Medical Jurisprudence",
        // ═══ PHARMACY ═══
        "B.Pharm", "Bachelor of Pharmacy",
        "M.Pharm", "Master of Pharmacy",
        "Pharm.D", "Doctor of Pharmacy",
        // ═══ ARCHITECTURE ═══
        "B.Arch", "Bachelor of Architecture",
        "M.Arch", "Master of Architecture",
        "M.Arch (Architectural Conservation)",
        "M.Arch (Construction Management)",
        "M.Arch (Digital Architecture)",
        "M.Arch (Design and Project Management)",
        "M.Arch (Environmental Architecture)",
        "M.Arch (Landscape Architecture)",
        "M.Arch (Urban Design)",
        // ═══ EDUCATION ═══
        "B.Ed", "Bachelor of Education",
        "M.Ed", "Master of Education",
        "B.P.Ed", "Bachelor of Physical Education",
        "M.P.Ed", "Master of Physical Education",
        "B.Ed Special Education",
        // ═══ HOTEL MANAGEMENT ═══
        "BHMCT", "Bachelor of Hotel Management",
        "MHMCT", "Masters in Hotel Management",
        "Hotel Management and Catering Technology",
        // ═══ LIBRARY SCIENCE ═══
        "Library and Information Science", "Library Science",
        "Bachelor of Library", "Master of Library",
        // ═══ DESIGN & OTHER UG ═══
        "Bachelor of Design", "B.Des",
        "B.Voc", "Vocational",
        // ═══ DIPLOMAS ═══
        "PG Diploma", "Post Graduate Diploma",
        "PGDCM", "Diploma in Banking",
        "Diploma in Hospital Management",
        "Diploma in International Business",
        // ═══ EXAM PATTERNS ═══
        "2015 Pattern", "2017 Pattern", "2018 Pattern",
        "2019 Pattern", "2020 Pattern", "2021 Pattern",
        "2023 Pattern", "2024 Pattern", "2025 Pattern",
        "Credit Pattern", "CBCS", "NEP 2020",
        "REV.2015", "REV.2016", "REV.2017", "REV.2019", "REV.2020",
        // ═══ SESSIONS ═══
        "Summer Session", "Winter Session",
        // ═══ SPECIAL DESIGNATIONS ═══
        "Backlog", "Fresh", "External", "Program 125",
        // Science common
        "Physics", "Chemistry", "Mathematics", "Biology", "Zoology",
        "Botany", "Biotechnology", "Microbiology", "Statistics",
        "Geography", "Geology", "Environmental Science",
        "Biochemistry", "Drug Chemistry", "Analytical Chemistry",
        "Organic Chemistry", "Inorganic Chemistry", "Physical Chemistry",
        // Management
        "Finance", "Marketing", "Human Resource", "International Business",
        "Digital Marketing", "Project Management", "Service Management",
        // Arts & Humanities
        "Economics", "History", "Political Science", "Psychology",
        "Sociology", "Philosophy", "English", "Marathi", "Hindi",
        "Sanskrit", "Journalism", "Mass Communication",
        // Other
        "Engineering", "Technology", "Semester", "Annual",
        "Hospitality", "Tourism", "Animation", "Fashion",
        "Home Science", "Wine", "Brewing",
    )

    /**
     * Priority-focused keywords — the most impactful ones users would want
     * high-importance notifications for. A subset of suggestedKeywords for the
     * Priority Watchlist suggestions UI.
     */
    val suggestedPriorityKeywords = listOf(
        // Year levels (most common use case)
        "BE", "B.E.", "TE", "T.E.", "SE", "S.E.", "FE", "F.E.",
        "Final Year", "Fourth Year",
        // High-demand programs
        "Computer", "IT", "Information Technology",
        "Artificial Intelligence", "Machine Learning", "Data Science",
        "Cyber Security", "AI and ML",
        "Mechanical", "Civil", "Electrical", "E&TC", "Electronics",
        // Popular PG programs
        "MBA", "Master of Business Administration",
        "MCA", "Master of Computer Application",
        "M.Sc (Computer Science)", "M.Sc (Computer Applications)",
        // Popular UG programs
        "BBA", "BCA", "B.Com", "B.Sc (Computer Science)",
        // Law
        "LLB", "LL.B", "LLM", "B.A.LL.B",
        // Pharmacy
        "B.Pharm", "M.Pharm", "Pharm.D",
        // Architecture
        "B.Arch", "M.Arch",
        // Education
        "B.Ed", "M.Ed",
        // Hotel Management
        "BHMCT", "Hotel Management",
        // Exam patterns
        "2019 Pattern", "2024 Pattern", "2025 Pattern", "NEP 2020",
        // Session
        "Summer Session", "Winter Session",
        // Special
        "Backlog",
    )

    fun classify(title: String): String {
        val n = title.replace(YEAR_PREFIX_REGEX, "").replace(FY_REGEX, "")
        val nu = n.uppercase()
        
        return when {
            FE_REGEX.containsMatchIn(title) || FE_LONG_REGEX.containsMatchIn(title) -> "FE"
            SE_REGEX.containsMatchIn(title) -> "SE"
            TE_REGEX.containsMatchIn(title) -> "TE"
            BE_REGEX.containsMatchIn(title) -> "BE"
            ME_REGEX.containsMatchIn(title) || ME_LONG_REGEX.containsMatchIn(title) -> "ME"
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
    
    private val FE_REGEX = Regex("^F\\.?\\s*E\\.?\\s*(\\(|\\d|\\s)", RegexOption.IGNORE_CASE)
    private val FE_LONG_REGEX = Regex("^FIRST\\s+YEAR\\s+ENGINEERING", RegexOption.IGNORE_CASE)
    private val SE_REGEX = Regex("^S\\.?\\s*E\\.?\\s*(\\(|\\d|\\s)", RegexOption.IGNORE_CASE)
    private val TE_REGEX = Regex("^T\\.?\\s*E\\.?\\s*(\\(|\\d|\\s)", RegexOption.IGNORE_CASE)
    private val BE_REGEX = Regex("^B\\.?\\s*E\\.?\\s*(\\(|\\d|\\s)", RegexOption.IGNORE_CASE)
    private val ME_REGEX = Regex("^M\\.?\\s*E\\.?\\s*(\\(|\\d|\\s)", RegexOption.IGNORE_CASE)
    private val ME_LONG_REGEX = Regex("^MASTER\\s+OF\\s+ENGINEERING", RegexOption.IGNORE_CASE)
    
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
