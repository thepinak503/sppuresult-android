package pinak.sppunotify.ui.theme

import androidx.compose.ui.graphics.Color

// ── Brand ────────────────────────────────────────────────────────────────────
val SppuBlue           = Color(0xFF1A5276)
val SppuBlueSecondary  = Color(0xFF2E86C1)
val SppuAccent         = Color(0xFFF39C12)

// ── Light Scheme ─────────────────────────────────────────────────────────────
val PrimaryLight                = Color(0xFF1A5276)
val OnPrimaryLight              = Color(0xFFFFFFFF)
val PrimaryContainerLight       = Color(0xFFD6EAF8)
val OnPrimaryContainerLight     = Color(0xFF0D2E42)

val SecondaryLight              = Color(0xFF5D6D7E)
val OnSecondaryLight            = Color(0xFFFFFFFF)
val SecondaryContainerLight     = Color(0xFFDDE3EA)
val OnSecondaryContainerLight   = Color(0xFF1B2631)

val TertiaryLight               = Color(0xFF8B5E1A)
val OnTertiaryLight             = Color(0xFFFFFFFF)
val TertiaryContainerLight      = Color(0xFFFFDDB4)
val OnTertiaryContainerLight    = Color(0xFF2D1600)

val ErrorLight                  = Color(0xFFB00020)
val OnErrorLight                = Color(0xFFFFFFFF)
val ErrorContainerLight         = Color(0xFFFFDAD6)
val OnErrorContainerLight       = Color(0xFF410002)

val SurfaceLight                = Color(0xFFFDFDFD)
val OnSurfaceLight              = Color(0xFF1C1B1F)
val SurfaceVariantLight         = Color(0xFFDDE3EA)
val OnSurfaceVariantLight       = Color(0xFF41484D)
val SurfaceContainerLowLight    = Color(0xFFF3F7FB)
val SurfaceContainerLight       = Color(0xFFEDF1F5)
val SurfaceContainerHighLight   = Color(0xFFE7EBF0)
val OutlineLight                = Color(0xFF71787E)
val OutlineVariantLight         = Color(0xFFC1C7CE)
val BackgroundLight             = Color(0xFFFDFDFD)
val OnBackgroundLight           = Color(0xFF1C1B1F)
val ScrimLight                  = Color(0xFF000000)

// ── Department badge accent colours ───────────────────────────────────────────
// Light-mode container / Dark-mode container pairs used as badge backgrounds
object DeptColors {
    val FE          = Color(0xFF4A55A2)   // indigo-blue
    val SE          = Color(0xFF0D7377)   // teal
    val TE          = Color(0xFF2E7D32)   // green-700
    val BE          = Color(0xFF1565C0)   // blue-800
    val MBA         = Color(0xFF6A1B9A)   // purple
    val MCA         = Color(0xFF00838F)   // cyan-700
    val MSc         = Color(0xFF37474F)   // blue-grey
    val BCom        = Color(0xFFF57F17)   // amber-700
    val BSc         = Color(0xFF558B2F)   // light-green
    val BA          = Color(0xFF4E342E)   // brown
    val BPharm      = Color(0xFFAD1457)   // pink-800
    val Law         = Color(0xFF37474F)   // blue-grey
    val Diploma     = Color(0xFF5D4037)   // brown
    val Default     = Color(0xFF546E7A)   // slate

    /** Returns the accent colour for a given department string */
    fun accentFor(department: String): Color = when {
        department.startsWith("FE")                          -> FE
        department.startsWith("SE")                          -> SE
        department.startsWith("TE")                          -> TE
        department.startsWith("BE")                          -> BE
        department.startsWith("MBA")                         -> MBA
        department.startsWith("MCA")                         -> MCA
        department.startsWith("M.Sc") ||
            department.startsWith("M.A") ||
            department.startsWith("M.Com")                   -> MSc
        department.startsWith("B.Com")                       -> BCom
        department.startsWith("B.Sc")                        -> BSc
        department.startsWith("B.A")                         -> BA
        department.startsWith("B.Pharm")                     -> BPharm
        department.startsWith("Law")                         -> Law
        department.startsWith("Diploma")                     -> Diploma
        else                                                 -> Default
    }
}


// ── Dark Scheme ───────────────────────────────────────────────────────────────
val PrimaryDark                 = Color(0xFF5DADE2)
val OnPrimaryDark               = Color(0xFF0D2E42)
val PrimaryContainerDark        = Color(0xFF1A5276)
val OnPrimaryContainerDark      = Color(0xFFD6EAF8)

val SecondaryDark               = Color(0xFFAEB6BF)
val OnSecondaryDark             = Color(0xFF273746)
val SecondaryContainerDark      = Color(0xFF424949)
val OnSecondaryContainerDark    = Color(0xFFF2F3F4)

val TertiaryDark                = Color(0xFFFFB86A)
val OnTertiaryDark              = Color(0xFF4A2E00)
val TertiaryContainerDark       = Color(0xFF6A4100)
val OnTertiaryContainerDark     = Color(0xFFFFDDB4)

val ErrorDark                   = Color(0xFFFFB4AB)
val OnErrorDark                 = Color(0xFF690005)
val ErrorContainerDark          = Color(0xFF93000A)
val OnErrorContainerDark        = Color(0xFFFFDAD6)

val BackgroundDark              = Color(0xFF0F1416)
val OnBackgroundDark            = Color(0xFFE1E3E5)
val SurfaceDark                 = Color(0xFF161C1F)
val OnSurfaceDark               = Color(0xFFE1E3E5)
val SurfaceVariantDark          = Color(0xFF41484D)
val OnSurfaceVariantDark        = Color(0xFFC1C7CE)
val SurfaceContainerLowDark     = Color(0xFF1E2427)
val SurfaceContainerDark        = Color(0xFF222A2D)
val SurfaceContainerHighDark    = Color(0xFF2C3438)
val OutlineDark                 = Color(0xFF8B9198)
val OutlineVariantDark          = Color(0xFF41484D)
val ScrimDark                   = Color(0xFF000000)
