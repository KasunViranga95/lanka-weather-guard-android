package com.example.data

import com.example.model.CityWeatherSnapshot
import com.example.model.AlertSeverity
import com.example.model.DailyForecastItem
import com.example.model.HourlyForecastItem
import com.example.model.RegionalWeatherReport
import com.example.model.FEET_TO_METRES
import com.example.model.GaugeFeed
import com.example.model.Reservoir
import com.example.model.ReservoirStatus
import com.example.model.RiverGaugeStation
import com.example.model.SriLankaRegion
import com.example.network.WaterLevelsStation
import com.example.network.IrrigationGaugeReading
import com.example.network.TEMPERATURE_ENSEMBLE_MODELS
import com.example.network.WeatherNetworkClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

class SriLankaWeatherRepository {

    // All 25 Districts of Sri Lanka with authentic Nearest Cities/Towns for Residing Area Notifications
    val regions: List<SriLankaRegion> = listOf(
        SriLankaRegion(
            id = "colombo",
            name = "Colombo",
            province = "Western Province",
            latitude = 6.9271,
            longitude = 79.8612,
            mapXFraction = 0.25f,
            mapYFraction = 0.72f,
            primaryRiverBasin = "Kelani Ganga (RB 01)",
            isLandslideProne = false,
            isCoastalZone = true,
            elevationMeters = 7,
            nearestCities = listOf(
                "Colombo Fort & Pettah", "Wellampitiya", "Kolonnawa", "Kaduwela",
                "Hanwella", "Avissawella", "Maharagama", "Nugegoda",
                "Dehiwala - Mount Lavinia", "Moratuwa", "Homagama", "Padukka"
            )
        ),
        SriLankaRegion(
            id = "gampaha",
            name = "Gampaha",
            province = "Western Province",
            latitude = 7.0873,
            longitude = 79.9990,
            mapXFraction = 0.29f,
            mapYFraction = 0.68f,
            primaryRiverBasin = "Attanagalu Oya (RB 103) & Kelani",
            isLandslideProne = false,
            isCoastalZone = true,
            elevationMeters = 25,
            nearestCities = listOf(
                "Gampaha Town", "Kelaniya", "Biyagama", "Wattala", "Ja-Ela",
                "Negombo", "Minuwangoda", "Veyangoda", "Nittambuwa",
                "Divulapitiya", "Mirigama", "Dompe", "Katunayake"
            )
        ),
        SriLankaRegion(
            id = "kalutara",
            name = "Kalutara",
            province = "Western Province",
            latitude = 6.5854,
            longitude = 79.9607,
            mapXFraction = 0.28f,
            mapYFraction = 0.80f,
            primaryRiverBasin = "Kalu Ganga (RB 03)",
            isLandslideProne = true,
            isCoastalZone = true,
            elevationMeters = 11,
            nearestCities = listOf(
                "Kalutara Town", "Panadura", "Horana", "Bulathsinhala",
                "Matugama", "Agalawatta", "Palindanuwara (Baduraliya)",
                "Dodangoda", "Millaniya", "Ingiriya", "Beruwala", "Aluthgama"
            )
        ),
        SriLankaRegion(
            id = "ratnapura",
            name = "Ratnapura",
            province = "Sabaragamuwa Province",
            latitude = 6.6828,
            longitude = 80.3992,
            mapXFraction = 0.44f,
            mapYFraction = 0.77f,
            primaryRiverBasin = "Kalu Ganga (RB 03) & Walawe",
            isLandslideProne = true,
            isCoastalZone = false,
            elevationMeters = 130,
            nearestCities = listOf(
                "Ratnapura Town", "Kuruwita", "Eheliyagoda", "Pelmadulla",
                "Elapatha", "Kiriella", "Ayagama", "Kalawana",
                "Nivitigala", "Kahawatta", "Balangoda", "Embilipitiya"
            )
        ),
        SriLankaRegion(
            id = "kegalle",
            name = "Kegalle",
            province = "Sabaragamuwa Province",
            latitude = 7.2513,
            longitude = 80.3464,
            mapXFraction = 0.41f,
            mapYFraction = 0.65f,
            primaryRiverBasin = "Kelani Ganga (Upper) & Gurugoda Oya",
            isLandslideProne = true,
            isCoastalZone = false,
            elevationMeters = 190,
            nearestCities = listOf(
                "Kegalle Town", "Kitulgala", "Deraniyagala", "Ruwanwella",
                "Dehiowita", "Yatiyantota", "Karawanella", "Warakapola",
                "Galigamuwa", "Mawanella", "Rambukkana", "Aranayaka"
            )
        ),
        SriLankaRegion(
            id = "galle",
            name = "Galle",
            province = "Southern Province",
            latitude = 6.0535,
            longitude = 80.2210,
            mapXFraction = 0.36f,
            mapYFraction = 0.91f,
            primaryRiverBasin = "Gin Ganga (RB 09) & Bentara",
            isLandslideProne = true,
            isCoastalZone = true,
            elevationMeters = 15,
            nearestCities = listOf(
                "Galle City", "Baddegama", "Tawalama", "Neluwa",
                "Nagoda", "Yakkalamulla", "Elpitiya", "Bentota",
                "Ambalangoda", "Hikkaduwa", "Imaduwa", "Bope-Poddala"
            )
        ),
        SriLankaRegion(
            id = "matara",
            name = "Matara",
            province = "Southern Province",
            latitude = 5.9549,
            longitude = 80.5550,
            mapXFraction = 0.48f,
            mapYFraction = 0.93f,
            primaryRiverBasin = "Nilwala Ganga (RB 12)",
            isLandslideProne = true,
            isCoastalZone = true,
            elevationMeters = 12,
            nearestCities = listOf(
                "Matara Town", "Akuressa", "Pitabeddara", "Kotapola",
                "Deniyaya", "Malimbada", "Athuraliya", "Kamburupitiya",
                "Hakmana", "Weligama", "Mirissa", "Mulatiyana"
            )
        ),
        SriLankaRegion(
            id = "hambantota",
            name = "Hambantota",
            province = "Southern Province",
            latitude = 6.1429,
            longitude = 81.1212,
            mapXFraction = 0.62f,
            mapYFraction = 0.89f,
            primaryRiverBasin = "Walawe Ganga (RB 18) & Kirindi Oya",
            isLandslideProne = false,
            isCoastalZone = true,
            elevationMeters = 16,
            nearestCities = listOf(
                "Hambantota Town", "Ambalantota", "Tissamaharama", "Tangalle",
                "Beliatta", "Walasmulla", "Katuwana", "Sooriyawewa", "Lunugamvehera"
            )
        ),
        SriLankaRegion(
            id = "kandy",
            name = "Kandy",
            province = "Central Province",
            latitude = 7.2906,
            longitude = 80.6337,
            mapXFraction = 0.51f,
            mapYFraction = 0.62f,
            primaryRiverBasin = "Mahaweli Ganga (RB 60)",
            isLandslideProne = true,
            isCoastalZone = false,
            elevationMeters = 500,
            nearestCities = listOf(
                "Kandy City", "Peradeniya", "Nawalapitiya", "Gampola",
                "Katugastota", "Kadugannawa", "Kundasale", "Digana",
                "Teldeniya", "Akurana", "Galagedara", "Udunuwara"
            )
        ),
        SriLankaRegion(
            id = "nuwara_eliya",
            name = "Nuwara Eliya",
            province = "Central Province",
            latitude = 6.9497,
            longitude = 80.7891,
            mapXFraction = 0.56f,
            mapYFraction = 0.70f,
            primaryRiverBasin = "Upper Kelani & Kotmale Oya",
            isLandslideProne = true,
            isCoastalZone = false,
            elevationMeters = 1868,
            nearestCities = listOf(
                "Nuwara Eliya Town", "Hatton", "Norwood", "Maskeliya",
                "Ginigathena", "Talawakele", "Lindula", "Kotmale",
                "Walapane", "Hanguranketha", "Ragala"
            )
        ),
        SriLankaRegion(
            id = "matale",
            name = "Matale",
            province = "Central Province",
            latitude = 7.4675,
            longitude = 80.6234,
            mapXFraction = 0.50f,
            mapYFraction = 0.56f,
            primaryRiverBasin = "Amban Ganga / Mahaweli (RB 60)",
            isLandslideProne = true,
            isCoastalZone = false,
            elevationMeters = 364,
            nearestCities = listOf(
                "Matale Town", "Dambulla", "Sigiriya", "Rattota",
                "Ukuwela", "Galewela", "Naula", "Laggala-Pallegama", "Wilgamuwa"
            )
        ),
        SriLankaRegion(
            id = "badulla",
            name = "Badulla",
            province = "Uva Province",
            latitude = 6.9934,
            longitude = 81.0550,
            mapXFraction = 0.65f,
            mapYFraction = 0.69f,
            primaryRiverBasin = "Uma Oya & Mahaweli (Weragantota)",
            isLandslideProne = true,
            isCoastalZone = false,
            elevationMeters = 680,
            nearestCities = listOf(
                "Badulla Town", "Bandarawela", "Haputale", "Ella",
                "Welimada", "Mahiyanganaya", "Passara", "Hali-Ela",
                "Namunukula", "Diyatalawa", "Girandurukotte"
            )
        ),
        SriLankaRegion(
            id = "monaragala",
            name = "Monaragala",
            province = "Uva Province",
            latitude = 6.8728,
            longitude = 81.3507,
            mapXFraction = 0.71f,
            mapYFraction = 0.74f,
            primaryRiverBasin = "Kirindi Oya, Menik Ganga & Kumbukkan Oya",
            isLandslideProne = true,
            isCoastalZone = false,
            elevationMeters = 155,
            nearestCities = listOf(
                "Monaragala Town", "Wellawaya", "Thanamalwila", "Kataragama",
                "Buttala", "Bibile", "Siyambalanduwa", "Nakkala", "Badalkumbura"
            )
        ),
        SriLankaRegion(
            id = "kurunegala",
            name = "Kurunegala",
            province = "North Western Province",
            latitude = 7.4818,
            longitude = 80.3609,
            mapXFraction = 0.39f,
            mapYFraction = 0.56f,
            primaryRiverBasin = "Deduru Oya (RB 99) & Maha Oya (RB 102)",
            isLandslideProne = true,
            isCoastalZone = false,
            elevationMeters = 116,
            nearestCities = listOf(
                "Kurunegala City", "Giriulla", "Kuliyapitiya", "Pannala",
                "Narammala", "Alawwa", "Polgahawela", "Wariyapola",
                "Nikaweratiya", "Maho", "Galgamuwa", "Ibbagamuwa"
            )
        ),
        SriLankaRegion(
            id = "puttalam",
            name = "Puttalam",
            province = "North Western Province",
            latitude = 8.0362,
            longitude = 79.8283,
            mapXFraction = 0.23f,
            mapYFraction = 0.46f,
            primaryRiverBasin = "Maha Oya, Deduru Oya & Kala Oya",
            isLandslideProne = false,
            isCoastalZone = true,
            elevationMeters = 10,
            nearestCities = listOf(
                "Puttalam Town", "Chilaw", "Wennappuwa", "Dankotuwa",
                "Marawila", "Nattandiya", "Madampe", "Anamaduwa", "Kalpitiya"
            )
        ),
        SriLankaRegion(
            id = "anuradhapura",
            name = "Anuradhapura",
            province = "North Central Province",
            latitude = 8.3114,
            longitude = 80.4037,
            mapXFraction = 0.44f,
            mapYFraction = 0.39f,
            primaryRiverBasin = "Malwathu Oya (RB 90), Kala Oya & Yan Oya",
            isLandslideProne = false,
            isCoastalZone = false,
            elevationMeters = 81,
            nearestCities = listOf(
                "Anuradhapura City", "Thanthirimale", "Horowpothana", "Kekirawa",
                "Thambuttegama", "Eppawala", "Nochchiyagama", "Medawachchiya",
                "Mihintale", "Kahatagasdigiliya", "Galenbindunuwewa"
            )
        ),
        SriLankaRegion(
            id = "polonnaruwa",
            name = "Polonnaruwa",
            province = "North Central Province",
            latitude = 7.9403,
            longitude = 81.0188,
            mapXFraction = 0.63f,
            mapYFraction = 0.49f,
            primaryRiverBasin = "Mahaweli Ganga (Manampitiya) & Maduru Oya",
            isLandslideProne = false,
            isCoastalZone = false,
            elevationMeters = 55,
            nearestCities = listOf(
                "Polonnaruwa Town", "Kaduruwela", "Manampitiya", "Somawathiya",
                "Hingurakgoda", "Medirigiriya", "Welikanda", "Dimbulagala", "Bakamuna"
            )
        ),
        SriLankaRegion(
            id = "trincomalee",
            name = "Trincomalee",
            province = "Eastern Province",
            latitude = 8.5874,
            longitude = 81.2152,
            mapXFraction = 0.70f,
            mapYFraction = 0.34f,
            primaryRiverBasin = "Mahaweli Delta (RB 60) & Yan Oya",
            isLandslideProne = false,
            isCoastalZone = true,
            elevationMeters = 8,
            nearestCities = listOf(
                "Trincomalee Town", "Kinniya", "Mutur", "Kantale",
                "Seruvila", "Kuchchaveli", "Gomarankadawala", "Pulmoddai"
            )
        ),
        SriLankaRegion(
            id = "batticaloa",
            name = "Batticaloa",
            province = "Eastern Province",
            latitude = 7.7310,
            longitude = 81.6747,
            mapXFraction = 0.77f,
            mapYFraction = 0.52f,
            primaryRiverBasin = "Mundeni Aru (RB 52) & Maduru Oya",
            isLandslideProne = false,
            isCoastalZone = true,
            elevationMeters = 6,
            nearestCities = listOf(
                "Batticaloa Town", "Kattankudy", "Eravur", "Valaichchenai",
                "Chenkalady", "Kalkudah", "Kaluwanchikudy", "Vakarai"
            )
        ),
        SriLankaRegion(
            id = "ampara",
            name = "Ampara",
            province = "Eastern Province",
            latitude = 7.2975,
            longitude = 81.6820,
            mapXFraction = 0.78f,
            mapYFraction = 0.64f,
            primaryRiverBasin = "Gal Oya (RB 44) & Heda Oya (RB 36)",
            isLandslideProne = false,
            isCoastalZone = true,
            elevationMeters = 32,
            nearestCities = listOf(
                "Ampara Town", "Kalmunai", "Akkaraipattu", "Sainthamaruthu",
                "Sammanthurai", "Pottuvil", "Padiyatalawa", "Mahaoya", "Uhana"
            )
        ),
        SriLankaRegion(
            id = "jaffna",
            name = "Jaffna",
            province = "Northern Province",
            latitude = 9.6615,
            longitude = 80.0255,
            mapXFraction = 0.31f,
            mapYFraction = 0.11f,
            primaryRiverBasin = "Thondaimanaru & Jaffna Lagoon",
            isLandslideProne = false,
            isCoastalZone = true,
            elevationMeters = 5,
            nearestCities = listOf(
                "Jaffna City", "Nallur", "Chavakachcheri", "Point Pedro",
                "Chunnakam", "Kankesanthurai", "Kopay", "Karainagar", "Kayts"
            )
        ),
        SriLankaRegion(
            id = "kilinochchi",
            name = "Kilinochchi",
            province = "Northern Province",
            latitude = 9.3803,
            longitude = 80.3770,
            mapXFraction = 0.38f,
            mapYFraction = 0.17f,
            primaryRiverBasin = "Kanakarayan Aru & Iranamadu",
            isLandslideProne = false,
            isCoastalZone = true,
            elevationMeters = 18,
            nearestCities = listOf(
                "Kilinochchi Town", "Paranthan", "Pallai", "Poonakary", "Akkarayankulam"
            )
        ),
        SriLankaRegion(
            id = "mannar",
            name = "Mannar",
            province = "Northern Province",
            latitude = 8.9810,
            longitude = 79.9044,
            mapXFraction = 0.25f,
            mapYFraction = 0.28f,
            primaryRiverBasin = "Lower Malwathu Oya (Aruvi Aru)",
            isLandslideProne = false,
            isCoastalZone = true,
            elevationMeters = 6,
            nearestCities = listOf(
                "Mannar Town", "Murunkan", "Nanattan", "Musali", "Madhu", "Pesalai"
            )
        ),
        SriLankaRegion(
            id = "vavuniya",
            name = "Vavuniya",
            province = "Northern Province",
            latitude = 8.7542,
            longitude = 80.4982,
            mapXFraction = 0.45f,
            mapYFraction = 0.31f,
            primaryRiverBasin = "Upper Malwathu & Parangi Aru",
            isLandslideProne = false,
            isCoastalZone = false,
            elevationMeters = 95,
            nearestCities = listOf(
                "Vavuniya Town", "Cheddikulam", "Nedunkerny", "Omanthai"
            )
        ),
        SriLankaRegion(
            id = "mullaitivu",
            name = "Mullaitivu",
            province = "Northern Province",
            latitude = 9.2671,
            longitude = 80.8142,
            mapXFraction = 0.52f,
            mapYFraction = 0.21f,
            primaryRiverBasin = "Ma Oya & Nayaru Lagoon",
            isLandslideProne = false,
            isCoastalZone = true,
            elevationMeters = 7,
            nearestCities = listOf(
                "Mullaitivu Town", "Puthukkudiyiruppu", "Oddusuddan", "Mankulam", "Welioya"
            )
        )
    )

    // Comprehensive RiverWatch.LK / Department of Irrigation Gauging Stations across 18 Sri Lankan River Basins
    // Each gauge's page on waterlevels.lk (history, forecast and the towns along the river), checked Nov 2026
    private val gaugeInfoPaths: Map<String, String> = mapOf(
        "kelani_nagalagam" to "kelani-ganga/nagalagam-street",
        "kelani_hanwella" to "kelani-ganga/hanwella",
        "kelani_glencorse" to "kelani-ganga/glencourse",
        "kelani_kitulgala" to "kelani-ganga/kithulgala",
        "kelani_holombuwa" to "kelani-ganga/holombuwa",
        "kelani_deraniyagala" to "kelani-ganga/deraniyagala",
        "kelani_norwood" to "kelani-ganga/norwood",
        "kalu_ratnapura" to "kalu-ganga/rathnapura",
        "kalu_ellagawa" to "kalu-ganga/ellagawa",
        "kalu_putupaula" to "kalu-ganga/putupaula",
        "kalu_millakanda" to "kalu-ganga/kalawellawa-millakanda",
        "kalu_magura" to "kalu-ganga/magura",
        "gin_baddegama" to "gin-ganga/baddegama",
        "gin_tawalama" to "gin-ganga/thawalama",
        "nilwala_panadugama" to "nilwala-ganga/panadugama",
        "nilwala_pitabeddara" to "nilwala-ganga/pitabeddara",
        "nilwala_urawa" to "nilwala-ganga/urawa",
        "walawe_moraketiya" to "walawe-ganga/moraketiya",
        "kirindi_thanamalwila" to "kirindi-oya/thanamalwila",
        "kirindi_wellawaya" to "kirindi-oya/wellawaya",
        "menik_kataragama" to "menik-ganga/katharagama",
        "kumbukkan_nakkala" to "kumbukkan-oya/nakkala",
        "heda_siyambalanduwa" to "heda-oya/siyambalanduwa",
        "mahaweli_peradeniya" to "mahaweli-ganga/peradeniya",
        "mahaweli_nawalapitiya" to "mahaweli-ganga/nawalapitiya",
        "mahaweli_weragantota" to "mahaweli-ganga/weraganthota",
        "mahaweli_manampitiya" to "mahaweli-ganga/manampitiya",
        "yan_horowpothana" to "yan-oya/horowpothana",
        "malwathu_thanthirimale" to "malwathu-oya/thanthirimale",
        "deduru_moragaswewa" to "deduru-oya/moragaswewa",
        "maha_giriulla" to "maha-oya/giriulla",
        "maha_badalgama" to "maha-oya/badalgama",
        "attanagalu_dunamale" to "aththanagalu-oya/dunamale",
        "nilwala_thalgahagoda" to "nilwala-ganga/thalgahagoda",
        "mahaweli_thaldena" to "mahaweli-ganga/thaldena",
        "kirindi_kudaoya" to "kirindi-oya/kuda-oya",
        "maduru_padiyathalawa" to "maduru-oya/padiyathalawa",
        "mee_galgamuwa" to "mee-oya/galgamuwa",
        "ma_yakawewa" to "ma-oya/yaka-wewa",
        "kelani_karawanella" to "kelani-ganga/karawanella-bridge",
        "mahaweli_mahiyangana" to "mahaweli-ganga/mahiyangana-bridge",
        "maduru_vadamunai" to "maduru-oya/vadamunai-bridge",
        "maduru_punani" to "maduru-oya/punani-anicut",
        "maduru_pulipanchakal" to "maduru-oya/pulipanchakal-bridge",
        "mundeni_galodaya" to "mundeni-aru/galodaya-bridge",
        "mundeni_lavanai" to "mundeni-aru/lavanai-aru-bridge",
        "magalawatavan_mullamunai" to "magalawatavan-aru/mullamunai-bridge",
        "andella_maalayarkattu" to "andella-oya/maalayarkattu-bridge",
        "andella_rajagalathenna" to "andella-oya/rajagalathenna-bridge",
        "kelani_mapitigama" to "kelani-ganga/mapitigama",
        "kelani_pugoda" to "kelani-ganga/pugoda",
        "kalu_kuruvita" to "kalu-ganga/kuru-ganga-kuruvita",
        "kalu_elapatha" to "kalu-ganga/niriella-ganga-elapatha",
        "kalu_ayagama" to "kalu-ganga/galathura-oya-ayagama",
        "kalu_pelmadulla" to "kalu-ganga/denawaka-ganga-pelmadulla",
        "kalu_kalawana" to "kalu-ganga/kukule-ganga-kalawana",
        "kalu_kahawatta" to "kalu-ganga/wey-ganga-kahawaththa",
        "uruwal_miriswatta" to "uruwal-oya/miriswatta",
        "wattala_kaluela" to "wattala-kalu-ela/wattala"
    )

    private val baseStations: List<RiverGaugeStation> = listOf(
        // ================= 1. KELANI GANGA BASIN (RB 01) =================
        RiverGaugeStation(
            id = "kelani_nagalagam",
            stationName = "Nagalagam Street (Colombo)",
            riverName = "Kelani Ganga",
            basinCode = "RB 01",
            district = "Colombo",
            latitude = 6.9588,
            longitude = 79.8780,
            mapXFraction = 0.26f,
            mapYFraction = 0.71f,
            currentLevelM = 1.92,
            previousLevelM = 1.68,
            alertLevelM = 1.22,
            minorFloodLevelM = 1.52,
            majorFloodLevelM = 2.13,
            catchmentRainMm24h = 148.5,
            dischargeM3s = 980.0,
            lastUpdatedText = "Live • RiverWatch.LK / Irrigation Dept",
            hourlyLevelsM = listOf(1.35, 1.48, 1.59, 1.71, 1.82, 1.92),
            downstreamTowns = listOf("Peliyagoda", "Kelaniya", "Wellampitiya", "Kolonnawa", "Mattakkuliya"),
            sourceUnit = "ft"
        ),
        RiverGaugeStation(
            id = "kelani_hanwella",
            stationName = "Hanwella",
            riverName = "Kelani Ganga",
            basinCode = "RB 01",
            district = "Colombo",
            latitude = 6.9097,
            longitude = 80.0819,
            mapXFraction = 0.33f,
            mapYFraction = 0.73f,
            currentLevelM = 8.45,
            previousLevelM = 7.90,
            alertLevelM = 6.50,
            minorFloodLevelM = 7.50,
            majorFloodLevelM = 9.50,
            catchmentRainMm24h = 164.0,
            dischargeM3s = 1140.0,
            lastUpdatedText = "Live • RiverWatch.LK / Irrigation Dept",
            hourlyLevelsM = listOf(7.10, 7.35, 7.62, 7.90, 8.18, 8.45),
            downstreamTowns = listOf("Kaduwela", "Malwana", "Biyagama", "Hanwella", "Dompe")
        ),
        RiverGaugeStation(
            id = "kelani_glencorse",
            stationName = "Glencorse (Avissawella)",
            riverName = "Kelani Ganga",
            basinCode = "RB 01",
            district = "Colombo",
            latitude = 6.9600,
            longitude = 80.2030,
            mapXFraction = 0.38f,
            mapYFraction = 0.71f,
            currentLevelM = 16.80,
            previousLevelM = 16.50,
            alertLevelM = 15.00,
            minorFloodLevelM = 15.50,
            majorFloodLevelM = 19.00,
            catchmentRainMm24h = 172.0,
            dischargeM3s = 890.0,
            lastUpdatedText = "Live • RiverWatch.LK / Irrigation Dept",
            hourlyLevelsM = listOf(15.4, 15.8, 16.1, 16.4, 16.6, 16.8),
            downstreamTowns = listOf("Avissawella", "Ruwanwella", "Pugoda", "Dehiowita")
        ),
        RiverGaugeStation(
            id = "kelani_kitulgala",
            stationName = "Kitulgala",
            riverName = "Kelani Ganga",
            basinCode = "RB 01",
            district = "Kegalle",
            latitude = 6.9892,
            longitude = 80.4178,
            mapXFraction = 0.44f,
            mapYFraction = 0.69f,
            currentLevelM = 4.35,
            previousLevelM = 4.05,
            alertLevelM = 3.00,
            minorFloodLevelM = 4.00,
            majorFloodLevelM = 6.00,
            catchmentRainMm24h = 188.0,
            dischargeM3s = 640.0,
            lastUpdatedText = "Live • RiverWatch.LK / Irrigation Dept",
            hourlyLevelsM = listOf(3.40, 3.62, 3.80, 4.05, 4.22, 4.35),
            downstreamTowns = listOf("Kitulgala", "Yatiyantota", "Karawanella")
        ),
        RiverGaugeStation(
            id = "kelani_holombuwa",
            stationName = "Holombuwa (Gurugoda Oya)",
            riverName = "Kelani Ganga",
            basinCode = "RB 01",
            district = "Kegalle",
            latitude = 7.1850,
            longitude = 80.2640,
            mapXFraction = 0.39f,
            mapYFraction = 0.66f,
            currentLevelM = 3.58,
            previousLevelM = 3.30,
            alertLevelM = 3.00,
            minorFloodLevelM = 4.00,
            majorFloodLevelM = 6.00,
            catchmentRainMm24h = 154.0,
            dischargeM3s = 410.0,
            lastUpdatedText = "Live • RiverWatch.LK / Irrigation Dept",
            hourlyLevelsM = listOf(2.85, 3.02, 3.18, 3.30, 3.45, 3.58),
            downstreamTowns = listOf("Holombuwa", "Galigamuwa", "Ruwanwella"),
            tributaryOf = "Gurugoda Oya"
        ),
        RiverGaugeStation(
            id = "kelani_deraniyagala",
            stationName = "Deraniyagala (Seethawaka Oya)",
            riverName = "Kelani Ganga",
            basinCode = "RB 01",
            district = "Kegalle",
            latitude = 6.9244,
            longitude = 80.3380,
            mapXFraction = 0.42f,
            mapYFraction = 0.72f,
            currentLevelM = 5.92,
            previousLevelM = 5.60,
            alertLevelM = 4.80,
            minorFloodLevelM = 5.80,
            majorFloodLevelM = 7.00,
            catchmentRainMm24h = 179.0,
            dischargeM3s = 490.0,
            lastUpdatedText = "Live • RiverWatch.LK / Irrigation Dept",
            hourlyLevelsM = listOf(5.10, 5.28, 5.44, 5.60, 5.76, 5.92),
            downstreamTowns = listOf("Deraniyagala", "Dehiowita", "Avissawella"),
            tributaryOf = "Seethawaka Oya"
        ),
        RiverGaugeStation(
            id = "kelani_norwood",
            stationName = "Norwood (Kehelgamu Oya)",
            riverName = "Kelani Ganga",
            basinCode = "RB 01",
            district = "Nuwara Eliya",
            latitude = 6.8356,
            longitude = 80.6147,
            mapXFraction = 0.50f,
            mapYFraction = 0.73f,
            currentLevelM = 1.85,
            previousLevelM = 1.62,
            alertLevelM = 1.50,
            minorFloodLevelM = 3.00,
            majorFloodLevelM = 4.50,
            catchmentRainMm24h = 142.0,
            dischargeM3s = 285.0,
            lastUpdatedText = "Live • RiverWatch.LK / Irrigation Dept",
            hourlyLevelsM = listOf(1.30, 1.42, 1.52, 1.62, 1.74, 1.85),
            downstreamTowns = listOf("Norwood", "Maskeliya", "Castlereagh", "Norton Bridge"),
            tributaryOf = "Kehelgamu Oya"
        ),

        // ================= 2. KALU GANGA BASIN (RB 03) =================
        RiverGaugeStation(
            id = "kalu_ratnapura",
            stationName = "Ratnapura",
            riverName = "Kalu Ganga",
            basinCode = "RB 03",
            district = "Ratnapura",
            latitude = 6.6804,
            longitude = 80.4022,
            mapXFraction = 0.44f,
            mapYFraction = 0.77f,
            currentLevelM = 9.72,
            previousLevelM = 9.18,
            alertLevelM = 5.20,
            minorFloodLevelM = 7.50,
            majorFloodLevelM = 9.50,
            catchmentRainMm24h = 212.4,
            dischargeM3s = 1420.0,
            lastUpdatedText = "Live • RiverWatch.LK / Irrigation Dept",
            hourlyLevelsM = listOf(7.90, 8.35, 8.80, 9.18, 9.48, 9.72),
            downstreamTowns = listOf("Ratnapura Town", "Kuruwita", "Eheliyagoda", "Kiriella", "Elapatha")
        ),
        RiverGaugeStation(
            id = "kalu_ellagawa",
            stationName = "Ellagawa",
            riverName = "Kalu Ganga",
            basinCode = "RB 03",
            district = "Ratnapura",
            latitude = 6.7314,
            longitude = 80.2147,
            mapXFraction = 0.39f,
            mapYFraction = 0.76f,
            currentLevelM = 11.40,
            previousLevelM = 10.85,
            alertLevelM = 10.00,
            minorFloodLevelM = 10.70,
            majorFloodLevelM = 12.20,
            catchmentRainMm24h = 198.0,
            dischargeM3s = 1360.0,
            lastUpdatedText = "Live • RiverWatch.LK / Irrigation Dept",
            hourlyLevelsM = listOf(9.90, 10.20, 10.55, 10.85, 11.15, 11.40),
            downstreamTowns = listOf("Kiriella", "Ingiriya", "Horana", "Madurawala")
        ),
        RiverGaugeStation(
            id = "kalu_putupaula",
            stationName = "Putupaula (Kalutara)",
            riverName = "Kalu Ganga",
            basinCode = "RB 03",
            district = "Kalutara",
            latitude = 6.6147,
            longitude = 80.0628,
            mapXFraction = 0.32f,
            mapYFraction = 0.79f,
            currentLevelM = 4.38,
            previousLevelM = 4.02,
            alertLevelM = 3.00,
            minorFloodLevelM = 4.00,
            majorFloodLevelM = 5.00,
            catchmentRainMm24h = 186.0,
            dischargeM3s = 1310.0,
            lastUpdatedText = "Live • RiverWatch.LK / Irrigation Dept",
            hourlyLevelsM = listOf(3.45, 3.68, 3.88, 4.02, 4.21, 4.38),
            downstreamTowns = listOf("Dodangoda", "Kalutara North", "Kalutara South", "Millaniya")
        ),
        RiverGaugeStation(
            id = "kalu_millakanda",
            stationName = "Millakanda (Kuda Ganga)",
            riverName = "Kalu Ganga",
            basinCode = "RB 03",
            district = "Kalutara",
            latitude = 6.6311,
            longitude = 80.1819,
            mapXFraction = 0.37f,
            mapYFraction = 0.78f,
            currentLevelM = 6.85,
            previousLevelM = 6.40,
            alertLevelM = 5.00,
            minorFloodLevelM = 6.50,
            majorFloodLevelM = 8.00,
            catchmentRainMm24h = 195.0,
            dischargeM3s = 760.0,
            lastUpdatedText = "Live • RiverWatch.LK / Irrigation Dept",
            hourlyLevelsM = listOf(5.80, 6.00, 6.22, 6.40, 6.62, 6.85),
            downstreamTowns = listOf("Bulathsinhala", "Paragoda", "Madurawala", "Horana"),
            tributaryOf = "Kuda Ganga"
        ),
        RiverGaugeStation(
            id = "kalu_magura",
            stationName = "Magura (Maguru Ganga)",
            riverName = "Kalu Ganga",
            basinCode = "RB 03",
            district = "Kalutara",
            latitude = 6.5180,
            longitude = 80.2190,
            mapXFraction = 0.38f,
            mapYFraction = 0.81f,
            currentLevelM = 6.32,
            previousLevelM = 5.88,
            alertLevelM = 4.00,
            minorFloodLevelM = 6.00,
            majorFloodLevelM = 7.50,
            catchmentRainMm24h = 184.0,
            dischargeM3s = 540.0,
            lastUpdatedText = "Live • RiverWatch.LK / Irrigation Dept",
            hourlyLevelsM = listOf(5.10, 5.38, 5.62, 5.88, 6.12, 6.32),
            downstreamTowns = listOf("Baduraliya", "Palindanuwara", "Agalawatta", "Matugama"),
            tributaryOf = "Maguru Ganga"
        ),

        // ================= 3. BENTARA GANGA BASIN (RB 04) =================
        RiverGaugeStation(
            id = "bentara_pitigala",
            stationName = "Pitigala (Bentara Ganga)",
            riverName = "Bentara Ganga",
            basinCode = "RB 04",
            district = "Galle",
            latitude = 6.3450,
            longitude = 80.2280,
            mapXFraction = 0.36f,
            mapYFraction = 0.85f,
            currentLevelM = 3.95,
            previousLevelM = 3.70,
            alertLevelM = 3.40,
            minorFloodLevelM = 4.20,
            majorFloodLevelM = 5.40,
            catchmentRainMm24h = 138.0,
            dischargeM3s = 390.0,
            lastUpdatedText = "Live • RiverWatch.LK / Irrigation Dept",
            hourlyLevelsM = listOf(3.20, 3.38, 3.55, 3.70, 3.84, 3.95),
            downstreamTowns = listOf("Pitigala", "Elpitiya", "Bentota", "Aluthgama")
        ),

        // ================= 4. GIN GANGA BASIN (RB 09) =================
        RiverGaugeStation(
            id = "gin_baddegama",
            stationName = "Baddegama",
            riverName = "Gin Ganga",
            basinCode = "RB 09",
            district = "Galle",
            latitude = 6.1753,
            longitude = 80.1975,
            mapXFraction = 0.35f,
            mapYFraction = 0.88f,
            currentLevelM = 4.18,
            previousLevelM = 3.92,
            alertLevelM = 3.50,
            minorFloodLevelM = 4.00,
            majorFloodLevelM = 5.00,
            catchmentRainMm24h = 142.0,
            dischargeM3s = 670.0,
            lastUpdatedText = "Live • RiverWatch.LK / Irrigation Dept",
            hourlyLevelsM = listOf(3.55, 3.68, 3.80, 3.92, 4.06, 4.18),
            downstreamTowns = listOf("Baddegama", "Bope-Poddala", "Wakwella", "Gintota")
        ),
        RiverGaugeStation(
            id = "gin_tawalama",
            stationName = "Tawalama",
            riverName = "Gin Ganga",
            basinCode = "RB 09",
            district = "Galle",
            latitude = 6.3422,
            longitude = 80.3311,
            mapXFraction = 0.41f,
            mapYFraction = 0.86f,
            currentLevelM = 6.35,
            previousLevelM = 5.90,
            alertLevelM = 4.00,
            minorFloodLevelM = 6.00,
            majorFloodLevelM = 7.50,
            catchmentRainMm24h = 168.0,
            dischargeM3s = 580.0,
            lastUpdatedText = "Live • RiverWatch.LK / Irrigation Dept",
            hourlyLevelsM = listOf(5.10, 5.40, 5.65, 5.90, 6.14, 6.35),
            downstreamTowns = listOf("Tawalama", "Neluwa", "Nagoda", "Mapalagama")
        ),

        // ================= 5. NILWALA GANGA BASIN (RB 12) =================
        RiverGaugeStation(
            id = "nilwala_panadugama",
            stationName = "Panadugama",
            riverName = "Nilwala Ganga",
            basinCode = "RB 12",
            district = "Matara",
            latitude = 6.1281,
            longitude = 80.4911,
            mapXFraction = 0.46f,
            mapYFraction = 0.89f,
            currentLevelM = 6.24,
            previousLevelM = 5.95,
            alertLevelM = 5.00,
            minorFloodLevelM = 6.00,
            majorFloodLevelM = 7.50,
            catchmentRainMm24h = 158.0,
            dischargeM3s = 590.0,
            lastUpdatedText = "Live • RiverWatch.LK / Irrigation Dept",
            hourlyLevelsM = listOf(5.30, 5.52, 5.75, 5.95, 6.10, 6.24),
            downstreamTowns = listOf("Akuressa", "Athuraliya", "Malimbada", "Matara")
        ),
        RiverGaugeStation(
            id = "nilwala_pitabeddara",
            stationName = "Pitabeddara",
            riverName = "Nilwala Ganga",
            basinCode = "RB 12",
            district = "Matara",
            latitude = 6.2125,
            longitude = 80.4730,
            mapXFraction = 0.45f,
            mapYFraction = 0.87f,
            currentLevelM = 4.45,
            previousLevelM = 4.15,
            alertLevelM = 4.00,
            minorFloodLevelM = 5.00,
            majorFloodLevelM = 6.50,
            catchmentRainMm24h = 162.0,
            dischargeM3s = 460.0,
            lastUpdatedText = "Live • RiverWatch.LK / Irrigation Dept",
            hourlyLevelsM = listOf(3.75, 3.90, 4.02, 4.15, 4.30, 4.45),
            downstreamTowns = listOf("Pitabeddara", "Kotapola", "Akuressa")
        ),
        RiverGaugeStation(
            id = "nilwala_urawa",
            stationName = "Urawa",
            riverName = "Nilwala Ganga",
            basinCode = "RB 12",
            district = "Matara",
            latitude = 6.2380,
            longitude = 80.5620,
            mapXFraction = 0.49f,
            mapYFraction = 0.86f,
            currentLevelM = 3.10,
            previousLevelM = 2.95,
            alertLevelM = 2.50,
            minorFloodLevelM = 4.00,
            majorFloodLevelM = 6.00,
            catchmentRainMm24h = 144.0,
            dischargeM3s = 320.0,
            lastUpdatedText = "Live • RiverWatch.LK / Irrigation Dept",
            hourlyLevelsM = listOf(2.55, 2.70, 2.82, 2.95, 3.04, 3.10),
            downstreamTowns = listOf("Urawa", "Morawaka", "Mulatiyana")
        ),

        // ================= 6. WALAWE GANGA BASIN (RB 18) =================
        RiverGaugeStation(
            id = "walawe_moraketiya",
            stationName = "Moraketiya (Embilipitiya)",
            riverName = "Walawe Ganga",
            basinCode = "RB 18",
            district = "Ratnapura",
            latitude = 6.3389,
            longitude = 80.8481,
            mapXFraction = 0.56f,
            mapYFraction = 0.84f,
            currentLevelM = 3.35,
            previousLevelM = 3.18,
            alertLevelM = 3.00,
            minorFloodLevelM = 5.00,
            majorFloodLevelM = 7.00,
            catchmentRainMm24h = 98.0,
            dischargeM3s = 430.0,
            lastUpdatedText = "Live • RiverWatch.LK / Irrigation Dept",
            hourlyLevelsM = listOf(2.85, 2.95, 3.08, 3.18, 3.28, 3.35),
            downstreamTowns = listOf("Embilipitiya", "Ambalantota", "Sooriyawewa")
        ),

        // ================= 7. KIRINDI OYA BASIN (RB 22) =================
        RiverGaugeStation(
            id = "kirindi_thanamalwila",
            stationName = "Thanamalwila",
            riverName = "Kirindi Oya",
            basinCode = "RB 22",
            district = "Monaragala",
            latitude = 6.4347,
            longitude = 81.1325,
            mapXFraction = 0.64f,
            mapYFraction = 0.83f,
            currentLevelM = 3.60,
            previousLevelM = 3.55,
            alertLevelM = 4.00,
            minorFloodLevelM = 5.00,
            majorFloodLevelM = 5.50,
            catchmentRainMm24h = 62.0,
            dischargeM3s = 240.0,
            lastUpdatedText = "Live • RiverWatch.LK / Irrigation Dept",
            hourlyLevelsM = listOf(3.40, 3.45, 3.50, 3.55, 3.58, 3.60),
            downstreamTowns = listOf("Thanamalwila", "Lunugamvehera", "Tissamaharama")
        ),
        RiverGaugeStation(
            id = "kirindi_wellawaya",
            stationName = "Wellawaya",
            riverName = "Kirindi Oya",
            basinCode = "RB 22",
            district = "Monaragala",
            latitude = 6.7369,
            longitude = 81.1028,
            mapXFraction = 0.65f,
            mapYFraction = 0.76f,
            currentLevelM = 3.80,
            previousLevelM = 3.72,
            alertLevelM = 4.40,
            minorFloodLevelM = 5.40,
            majorFloodLevelM = 5.90,
            catchmentRainMm24h = 74.0,
            dischargeM3s = 215.0,
            lastUpdatedText = "Live • RiverWatch.LK / Irrigation Dept",
            hourlyLevelsM = listOf(3.50, 3.58, 3.65, 3.72, 3.76, 3.80),
            downstreamTowns = listOf("Wellawaya", "Ella Gap Foothills", "Thanamalwila")
        ),

        // ================= 8. MENIK GANGA BASIN (RB 26) =================
        RiverGaugeStation(
            id = "menik_kataragama",
            stationName = "Kataragama",
            riverName = "Menik Ganga",
            basinCode = "RB 26",
            district = "Monaragala",
            latitude = 6.4139,
            longitude = 81.3325,
            mapXFraction = 0.70f,
            mapYFraction = 0.83f,
            currentLevelM = 2.95,
            previousLevelM = 2.88,
            alertLevelM = 4.00,
            minorFloodLevelM = 4.60,
            majorFloodLevelM = 6.50,
            catchmentRainMm24h = 56.0,
            dischargeM3s = 190.0,
            lastUpdatedText = "Live • RiverWatch.LK / Irrigation Dept",
            hourlyLevelsM = listOf(2.70, 2.75, 2.82, 2.88, 2.92, 2.95),
            downstreamTowns = listOf("Kataragama Sacred Town", "Sella Kataragama", "Yala Block 1")
        ),

        // ================= 9. KUMBUKKAN OYA BASIN (RB 31) =================
        RiverGaugeStation(
            id = "kumbukkan_nakkala",
            stationName = "Nakkala",
            riverName = "Kumbukkan Oya",
            basinCode = "RB 31",
            district = "Monaragala",
            latitude = 6.8920,
            longitude = 81.2980,
            mapXFraction = 0.70f,
            mapYFraction = 0.73f,
            currentLevelM = 4.20,
            previousLevelM = 4.10,
            alertLevelM = 5.00,
            minorFloodLevelM = 6.00,
            majorFloodLevelM = 7.50,
            catchmentRainMm24h = 68.0,
            dischargeM3s = 260.0,
            lastUpdatedText = "Live • RiverWatch.LK / Irrigation Dept",
            hourlyLevelsM = listOf(3.85, 3.92, 4.00, 4.10, 4.16, 4.20),
            downstreamTowns = listOf("Nakkala", "Monaragala", "Buttala", "Kumana")
        ),

        // ================= 10. HEDA OYA BASIN (RB 36) =================
        RiverGaugeStation(
            id = "heda_siyambalanduwa",
            stationName = "Siyambalanduwa",
            riverName = "Heda Oya",
            basinCode = "RB 36",
            district = "Monaragala",
            latitude = 6.9056,
            longitude = 81.5472,
            mapXFraction = 0.75f,
            mapYFraction = 0.73f,
            currentLevelM = 3.85,
            previousLevelM = 3.78,
            alertLevelM = 4.50,
            minorFloodLevelM = 6.00,
            majorFloodLevelM = 7.00,
            catchmentRainMm24h = 52.0,
            dischargeM3s = 195.0,
            lastUpdatedText = "Live • RiverWatch.LK / Irrigation Dept",
            hourlyLevelsM = listOf(3.60, 3.66, 3.72, 3.78, 3.82, 3.85),
            downstreamTowns = listOf("Siyambalanduwa", "Lahugala", "Pottuvil")
        ),

        // ================= 11. MAHAWELI GANGA BASIN (RB 60) =================
        RiverGaugeStation(
            id = "mahaweli_peradeniya",
            stationName = "Peradeniya",
            riverName = "Mahaweli Ganga",
            basinCode = "RB 60",
            district = "Kandy",
            latitude = 7.2692,
            longitude = 80.5964,
            mapXFraction = 0.50f,
            mapYFraction = 0.63f,
            currentLevelM = 5.40,
            previousLevelM = 5.25,
            alertLevelM = 5.00,
            minorFloodLevelM = 7.00,
            majorFloodLevelM = 9.00,
            catchmentRainMm24h = 112.0,
            dischargeM3s = 720.0,
            lastUpdatedText = "Live • RiverWatch.LK / Irrigation Dept",
            hourlyLevelsM = listOf(4.90, 5.02, 5.14, 5.25, 5.34, 5.40),
            downstreamTowns = listOf("Peradeniya", "Katugastota", "Polgolla", "Gampola")
        ),
        RiverGaugeStation(
            id = "mahaweli_nawalapitiya",
            stationName = "Nawalapitiya",
            riverName = "Mahaweli Ganga",
            basinCode = "RB 60",
            district = "Kandy",
            latitude = 7.0569,
            longitude = 80.5342,
            mapXFraction = 0.48f,
            mapYFraction = 0.67f,
            currentLevelM = 3.82,
            previousLevelM = 3.55,
            alertLevelM = 3.50,
            minorFloodLevelM = 5.00,
            majorFloodLevelM = 6.00,
            catchmentRainMm24h = 146.0,
            dischargeM3s = 510.0,
            lastUpdatedText = "Live • RiverWatch.LK / Irrigation Dept",
            hourlyLevelsM = listOf(3.10, 3.25, 3.40, 3.55, 3.68, 3.82),
            downstreamTowns = listOf("Nawalapitiya", "Ulapane", "Gampola")
        ),
        RiverGaugeStation(
            id = "mahaweli_weragantota",
            stationName = "Weragantota (Mahiyanganaya)",
            riverName = "Mahaweli Ganga",
            basinCode = "RB 60",
            district = "Badulla",
            latitude = 7.3211,
            longitude = 80.9869,
            mapXFraction = 0.62f,
            mapYFraction = 0.61f,
            currentLevelM = 5.15,
            previousLevelM = 4.95,
            alertLevelM = 5.00,
            minorFloodLevelM = 6.00,
            majorFloodLevelM = 8.00,
            catchmentRainMm24h = 94.0,
            dischargeM3s = 680.0,
            lastUpdatedText = "Live • RiverWatch.LK / Irrigation Dept",
            hourlyLevelsM = listOf(4.65, 4.78, 4.86, 4.95, 5.06, 5.15),
            downstreamTowns = listOf("Mahiyanganaya", "Hasalaka", "Girandurukotte")
        ),
        RiverGaugeStation(
            id = "mahaweli_manampitiya",
            stationName = "Manampitiya",
            riverName = "Mahaweli Ganga",
            basinCode = "RB 60",
            district = "Polonnaruwa",
            latitude = 7.9136,
            longitude = 81.0897,
            mapXFraction = 0.66f,
            mapYFraction = 0.49f,
            currentLevelM = 4.75,
            previousLevelM = 4.52,
            alertLevelM = 3.00,
            minorFloodLevelM = 4.30,
            majorFloodLevelM = 6.00,
            catchmentRainMm24h = 74.0,
            dischargeM3s = 690.0,
            lastUpdatedText = "Live • RiverWatch.LK / Irrigation Dept",
            hourlyLevelsM = listOf(4.20, 4.32, 4.42, 4.52, 4.64, 4.75),
            downstreamTowns = listOf("Manampitiya", "Somawathiya", "Kinniya", "Mutur")
        ),

        // ================= 12. YAN OYA BASIN (RB 67) =================
        RiverGaugeStation(
            id = "yan_horowpothana",
            stationName = "Horowpothana",
            riverName = "Yan Oya",
            basinCode = "RB 67",
            district = "Anuradhapura",
            latitude = 8.5569,
            longitude = 80.8758,
            mapXFraction = 0.56f,
            mapYFraction = 0.36f,
            currentLevelM = 5.45,
            previousLevelM = 5.38,
            alertLevelM = 6.00,
            minorFloodLevelM = 7.50,
            majorFloodLevelM = 10.50,
            catchmentRainMm24h = 48.0,
            dischargeM3s = 280.0,
            lastUpdatedText = "Live • RiverWatch.LK / Irrigation Dept",
            hourlyLevelsM = listOf(5.20, 5.26, 5.32, 5.38, 5.42, 5.45),
            downstreamTowns = listOf("Horowpothana", "Gomarankadawala", "Pulmoddai")
        ),

        // ================= 13. MALWATHU OYA BASIN (RB 90) =================
        RiverGaugeStation(
            id = "malwathu_thanthirimale",
            stationName = "Thanthirimale",
            riverName = "Malwathu Oya",
            basinCode = "RB 90",
            district = "Anuradhapura",
            latitude = 8.5675,
            longitude = 80.2511,
            mapXFraction = 0.39f,
            mapYFraction = 0.35f,
            currentLevelM = 5.65,
            previousLevelM = 5.42,
            alertLevelM = 5.00,
            minorFloodLevelM = 6.80,
            majorFloodLevelM = 7.80,
            catchmentRainMm24h = 68.0,
            dischargeM3s = 340.0,
            lastUpdatedText = "Live • RiverWatch.LK / Irrigation Dept",
            hourlyLevelsM = listOf(5.05, 5.18, 5.30, 5.42, 5.55, 5.65),
            downstreamTowns = listOf("Anuradhapura Sacred City", "Thanthirimale", "Murunkan", "Mannar")
        ),

        // ================= 14. DEDURU OYA BASIN (RB 99) =================
        RiverGaugeStation(
            id = "deduru_moragaswewa",
            stationName = "Moragaswewa (Chilaw)",
            riverName = "Deduru Oya",
            basinCode = "RB 99",
            district = "Kurunegala",
            latitude = 7.6480,
            longitude = 80.1520,
            mapXFraction = 0.34f,
            mapYFraction = 0.53f,
            currentLevelM = 4.92,
            previousLevelM = 4.65,
            alertLevelM = 4.75,
            minorFloodLevelM = 6.00,
            majorFloodLevelM = 7.00,
            catchmentRainMm24h = 116.0,
            dischargeM3s = 480.0,
            lastUpdatedText = "Live • RiverWatch.LK / Irrigation Dept",
            hourlyLevelsM = listOf(4.20, 4.35, 4.50, 4.65, 4.80, 4.92),
            downstreamTowns = listOf("Wariyapola", "Nikaweratiya", "Bingiriya", "Chilaw")
        ),

        // ================= 15. MAHA OYA BASIN (RB 102) =================
        RiverGaugeStation(
            id = "maha_giriulla",
            stationName = "Giriulla",
            riverName = "Maha Oya",
            basinCode = "RB 102",
            district = "Kurunegala",
            latitude = 7.3311,
            longitude = 80.1228,
            mapXFraction = 0.33f,
            mapYFraction = 0.60f,
            currentLevelM = 5.85,
            previousLevelM = 5.48,
            alertLevelM = 5.50,
            minorFloodLevelM = 6.50,
            majorFloodLevelM = 7.50,
            catchmentRainMm24h = 144.0,
            dischargeM3s = 610.0,
            lastUpdatedText = "Live • RiverWatch.LK / Irrigation Dept",
            hourlyLevelsM = listOf(4.95, 5.12, 5.30, 5.48, 5.68, 5.85),
            downstreamTowns = listOf("Giriulla", "Alawwa", "Pannala", "Divulapitiya")
        ),
        RiverGaugeStation(
            id = "maha_badalgama",
            stationName = "Badalgama",
            riverName = "Maha Oya",
            basinCode = "RB 102",
            district = "Gampaha",
            latitude = 7.2856,
            longitude = 79.9814,
            mapXFraction = 0.28f,
            mapYFraction = 0.62f,
            currentLevelM = 6.42,
            previousLevelM = 5.95,
            alertLevelM = 5.00,
            minorFloodLevelM = 6.20,
            majorFloodLevelM = 9.60,
            catchmentRainMm24h = 152.0,
            dischargeM3s = 670.0,
            lastUpdatedText = "Live • RiverWatch.LK / Irrigation Dept",
            hourlyLevelsM = listOf(5.20, 5.45, 5.70, 5.95, 6.20, 6.42),
            downstreamTowns = listOf("Badalgama", "Kochchikade", "Dankotuwa", "Wennappuwa", "Negombo North")
        ),

        // ================= 16. ATTANAGALU OYA BASIN (RB 103) =================
        RiverGaugeStation(
            id = "attanagalu_dunamale",
            stationName = "Dunamale (Gampaha)",
            riverName = "Attanagalu Oya",
            basinCode = "RB 103",
            district = "Gampaha",
            latitude = 7.1219,
            longitude = 80.0281,
            mapXFraction = 0.31f,
            mapYFraction = 0.67f,
            currentLevelM = 4.62,
            previousLevelM = 4.30,
            alertLevelM = 3.30,
            minorFloodLevelM = 4.40,
            majorFloodLevelM = 5.50,
            catchmentRainMm24h = 136.0,
            dischargeM3s = 410.0,
            lastUpdatedText = "Live • RiverWatch.LK / Irrigation Dept",
            hourlyLevelsM = listOf(3.75, 3.95, 4.12, 4.30, 4.46, 4.62),
            downstreamTowns = listOf("Gampaha", "Ja-Ela", "Minuwangoda", "Wattala", "Ekala")
        ),

        // ===== More Irrigation Department gauges (same ArcGIS feed; official levels, metres) =====
        gauge("nilwala_thalgahagoda", "Thalgahagoda", "Nilwala Ganga", "RB 12", "Matara", 6.0103, 80.5329,
            1.40, 1.70, 2.80, 0.9, listOf("Thalgahagoda", "Matara", "Akuressa")),
        gauge("mahaweli_thaldena", "Thaldena (Badulu Oya)", "Mahaweli Ganga", "RB 60", "Badulla", 7.0907, 81.0489,
            3.00, 4.00, 5.00, 1.8, listOf("Thaldena", "Badulla", "Hali-Ela"), tributaryOf = "Badulu Oya"),
        gauge("kirindi_kudaoya", "Kuda Oya", "Kirindi Oya", "RB 22", "Monaragala", 6.5247, 81.1233,
            6.90, 8.40, 8.80, 2.6, listOf("Kuda Oya", "Lunugamvehera")),
        gauge("maduru_padiyathalawa", "Padiyathalawa", "Maduru Oya", "", "Ampara", 7.3823, 81.1946,
            4.00, 4.50, 6.00, 0.8, listOf("Padiyathalawa", "Maha Oya")),
        gauge("mee_galgamuwa", "Galgamuwa", "Mee Oya", "", "Kurunegala", 7.9694, 80.2623,
            4.84, 5.94, 8.00, 0.6, listOf("Galgamuwa")),
        gauge("ma_yakawewa", "Yaka Wewa", "Ma Oya", "", "Anuradhapura", 8.7260, 80.6750,
            4.00, 5.00, 6.00, 0.7, listOf("Yaka Wewa", "Kebithigollewa")),

        // ===== HMIS gauges (hmis.gov.lk; Irrigation Dept & ACCIMT sensors), via waterlevels.lk. HMIS levels, metres =====
        gauge("kelani_karawanella", "Karawanella Bridge", "Kelani Ganga", "RB 01", "Kegalle", 7.0202, 80.2624,
            3.00, 4.00, 6.00, 2.0, listOf("Karawanella", "Ruwanwella", "Yatiyantota"), feed = GaugeFeed.HMIS),
        gauge("mahaweli_mahiyangana", "Mahiyangana Bridge", "Mahaweli Ganga", "RB 60", "Badulla", 7.3381, 80.9866,
            5.00, 6.00, 8.00, 3.0, listOf("Mahiyangana", "Dambana"), feed = GaugeFeed.HMIS),
        gauge("maduru_vadamunai", "Vadamunai Bridge", "Maduru Oya", "", "Batticaloa", 7.9387, 81.2571,
            5.00, 7.00, 8.00, 3.5, listOf("Vadamunai", "Welikanda"), feed = GaugeFeed.HMIS),
        gauge("maduru_punani", "Punani Anicut", "Maduru Oya", "", "Batticaloa", 7.9464, 81.3972,
            3.00, 5.00, 6.00, 1.5, listOf("Punani", "Valaichchenai"), feed = GaugeFeed.HMIS),
        gauge("maduru_pulipanchakal", "Pulipanchakal Bridge", "Maduru Oya", "", "Batticaloa", 7.8634, 81.5063,
            3.00, 5.00, 6.00, 1.5, listOf("Pulipanchakal", "Chenkalady", "Eravur"), feed = GaugeFeed.HMIS),
        gauge("mundeni_galodaya", "Galodaya Bridge", "Mundeni Aru", "RB 52", "Batticaloa", 7.5622, 81.3331,
            3.00, 5.00, 6.00, 1.0, listOf("Galodaya", "Bakiella"), feed = GaugeFeed.HMIS),
        gauge("mundeni_lavanai", "Lavanai Aru Bridge", "Mundeni Aru", "RB 52", "Batticaloa", 7.7177, 81.4800,
            2.00, 4.00, 6.00, 1.0, listOf("Lavanai", "Vavunathivu"), feed = GaugeFeed.HMIS),
        gauge("magalawatavan_mullamunai", "Mullamunai Bridge", "Magalawatavan Aru", "", "Batticaloa", 7.6792, 81.5933,
            3.00, 5.00, 6.00, 0.5, listOf("Mullamunai", "Kokkadichcholai"), feed = GaugeFeed.HMIS),
        gauge("andella_maalayarkattu", "Maalayarkattu Bridge", "Andella Oya", "", "Ampara", 7.4340, 81.6922,
            3.00, 6.00, 9.00, 1.4, listOf("Maalayarkattu", "Navithanveli"), feed = GaugeFeed.HMIS),
        gauge("andella_rajagalathenna", "Rajagalathenna Bridge", "Andella Oya", "", "Ampara", 7.4714, 81.6330,
            3.00, 6.00, 9.00, 1.2, listOf("Rajagalathenna", "Uhana"), feed = GaugeFeed.HMIS),

        // ===== RiverNet.lk gauges, via waterlevels.lk. Metres =====
        // Mapitigama and Pugoda: levels RiverNet publishes (28 Oct 2026)
        gauge("kelani_mapitigama", "Mapitigama", "Kelani Ganga", "RB 01", "Kegalle", 7.0032, 80.2218,
            6.00, 7.00, 8.50, 2.8, listOf("Mapitigama", "Ruwanwella", "Yatiyantota"), feed = GaugeFeed.RIVERNET),
        gauge("kelani_pugoda", "Pugoda", "Kelani Ganga", "RB 01", "Gampaha", 6.9742, 80.1233,
            8.00, 10.00, 11.50, 4.1, listOf("Pugoda", "Hanwella", "Dompe"), feed = GaugeFeed.RIVERNET),
        // No official levels: set by the app from each gauge's readings, 27 Oct - 12 Nov 2026 (a period with
        // a Kalu Ganga flood at Ratnapura). Alert = level passed 1% of the time, minor = the highest held level
        // (99.9%), major = as far above that again as it is above the usual level. Replace when official ones exist.
        gauge("kalu_kuruvita", "Kuruvita (Kuru Ganga)", "Kalu Ganga", "RB 03", "Ratnapura", 6.7987, 80.3684,
            4.10, 5.10, 8.50, 1.7, listOf("Kuruvita", "Ratnapura"), tributaryOf = "Kuru Ganga", feed = GaugeFeed.RIVERNET, estimated = true),
        gauge("kalu_elapatha", "Elapatha (Niriella Ganga)", "Kalu Ganga", "RB 03", "Ratnapura", 6.6316, 80.3656,
            4.10, 4.40, 6.50, 2.3, listOf("Elapatha", "Niriella"), tributaryOf = "Niriella Ganga", feed = GaugeFeed.RIVERNET, estimated = true),
        gauge("kalu_ayagama", "Ayagama (Galathura Oya)", "Kalu Ganga", "RB 03", "Ratnapura", 6.6950, 80.3850,
            2.10, 2.40, 3.60, 1.3, listOf("Ayagama"), tributaryOf = "Galathura Oya", feed = GaugeFeed.RIVERNET, estimated = true),
        gauge("kalu_pelmadulla", "Pelmadulla (Denawaka Ganga)", "Kalu Ganga", "RB 03", "Ratnapura", 6.7093, 80.2909,
            2.70, 3.10, 5.00, 1.3, listOf("Pelmadulla"), tributaryOf = "Denawaka Ganga", feed = GaugeFeed.RIVERNET, estimated = true),
        gauge("kalu_kalawana", "Kalawana (Kukule Ganga)", "Kalu Ganga", "RB 03", "Ratnapura", 6.5802, 80.3270,
            6.20, 6.50, 8.50, 4.6, listOf("Kalawana", "Kukule"), tributaryOf = "Kukule Ganga", feed = GaugeFeed.RIVERNET, estimated = true),
        gauge("kalu_kahawatta", "Kahawatta (Wey Ganga)", "Kalu Ganga", "RB 03", "Ratnapura", 6.5817, 80.5736,
            3.30, 3.60, 4.70, 2.5, listOf("Kahawatta"), tributaryOf = "Wey Ganga", feed = GaugeFeed.RIVERNET, estimated = true),
        gauge("uruwal_miriswatta", "Miriswatta", "Uruwal Oya", "", "Gampaha", 7.0832, 79.9863,
            2.40, 2.70, 4.10, 1.3, listOf("Miriswatta", "Mudungoda", "Yakkala"), feed = GaugeFeed.RIVERNET, estimated = true),
        gauge("wattala_kaluela", "Wattala", "Wattala Kalu Ela", "", "Gampaha", 6.9912, 79.8922,
            2.90, 3.20, 4.00, 2.5, listOf("Wattala", "Hendala", "Mabola"), feed = GaugeFeed.RIVERNET, estimated = true)
    ).map { st -> st.copy(infoUrl = gaugeInfoPaths[st.id]?.let { "https://waterlevels.lk/$it" }) }

    /** A gauge added from the extra feeds, with its reference (typical) level shown until live data arrives. */
    private fun gauge(
        id: String,
        stationName: String,
        riverName: String,
        basinCode: String,
        district: String,
        latitude: Double,
        longitude: Double,
        alertM: Double,
        minorM: Double,
        majorM: Double,
        typicalM: Double,
        downstreamTowns: List<String>,
        tributaryOf: String? = null,
        feed: GaugeFeed = GaugeFeed.IRRIGATION,
        estimated: Boolean = false
    ) = RiverGaugeStation(
        id = id,
        stationName = stationName,
        riverName = riverName,
        basinCode = basinCode,
        district = district,
        latitude = latitude,
        longitude = longitude,
        mapXFraction = (0.3036 * longitude - 23.979).toFloat().coerceIn(0f, 1f),
        mapYFraction = (-0.2261 * latitude + 2.2792).toFloat().coerceIn(0f, 1f),
        currentLevelM = typicalM,
        previousLevelM = typicalM,
        alertLevelM = alertM,
        minorFloodLevelM = minorM,
        majorFloodLevelM = majorM,
        catchmentRainMm24h = 0.0,
        dischargeM3s = 0.0,
        lastUpdatedText = "",
        hourlyLevelsM = emptyList(),
        downstreamTowns = downstreamTowns,
        tributaryOf = tributaryOf,
        marksEstimated = estimated,
        feed = feed
    )

    fun regionById(id: String): SriLankaRegion? =
        regions.firstOrNull { it.id.equals(id, ignoreCase = true) || it.name.equals(id, ignoreCase = true) }

    fun findNearestRegion(latitude: Double, longitude: Double): SriLankaRegion {
        return regions.minByOrNull { region ->
            haversineKm(latitude, longitude, region.latitude, region.longitude)
        } ?: regions.first()
    }

    /** Nearest residing-area town (and its district) to a GPS fix, so weather is fetched for that town. */
    fun findNearestCity(latitude: Double, longitude: Double): Pair<SriLankaRegion, String> {
        return regions.flatMap { region -> region.nearestCities.map { city -> region to city } }
            .minByOrNull { (region, city) ->
                val (lat, lon) = region.getCoordinatesForCity(city)
                haversineKm(latitude, longitude, lat, lon)
            } ?: (findNearestRegion(latitude, longitude).let { it to (it.nearestCities.firstOrNull() ?: it.name) })
    }

    fun findPrimaryStationForRegion(
        region: SriLankaRegion,
        nearestCity: String,
        stations: List<RiverGaugeStation>
    ): RiverGaugeStation {
        // First match by city in downstreamTowns or stationName
        val cityMatch = stations.find { st ->
            st.stationName.contains(nearestCity, ignoreCase = true) ||
                st.downstreamTowns.any { town ->
                    town.contains(nearestCity, ignoreCase = true) ||
                        nearestCity.contains(town, ignoreCase = true)
                }
        }
        if (cityMatch != null) return cityMatch

        // Next match by district
        val districtMatch = stations
            .filter { it.district.equals(region.name, ignoreCase = true) }
            .maxByOrNull { it.floodStatus.severityRank }
        if (districtMatch != null) return districtMatch

        // Otherwise nearest by coordinates
        return stations.minByOrNull { st ->
            haversineKm(region.latitude, region.longitude, st.latitude, st.longitude)
        } ?: stations.first()
    }

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        return 2 * r * atan2(sqrt(a), sqrt(1 - a))
    }

    // Last live report per location, shown (marked offline) if a later sync fails
    private val lastLiveReports = ConcurrentHashMap<String, RegionalWeatherReport>()

    /** Key the forecast for a town is saved under (same for the app, the widget and background checks). */
    fun placeCacheKey(region: SriLankaRegion, nearestCity: String): String {
        val (lat, lon) = region.getCoordinatesForCity(nearestCity)
        return String.format(Locale.US, "%.3f,%.3f", lat, lon)
    }

    /** Last saved forecast for a town, or null if it has never loaded on this phone. */
    fun cachedReport(region: SriLankaRegion, nearestCity: String): RegionalWeatherReport? {
        val key = placeCacheKey(region, nearestCity)
        return (lastLiveReports[key] ?: OfflineCache.loadReport(key))?.asSavedCopy()
    }

    /** Marks a saved forecast as offline and drops the hours that have already passed since it was saved. */
    private fun RegionalWeatherReport.asSavedCopy(): RegionalWeatherReport {
        val hoursPassed = ((System.currentTimeMillis() - fetchedAtMillis) / 3_600_000L).toInt().coerceIn(0, hourly.size)
        return copy(isFromCache = true, hourly = hourly.drop(hoursPassed))
    }

    /** Last live river readings saved on this phone, or null. */
    /**
     * Last saved readings, with every current gauge present: a copy saved by an older version of the app may
     * lack the newer gauges, so those come from the reference list (shown as "no recent reading").
     */
    fun cachedStations(): List<RiverGaugeStation>? {
        val saved = lastLiveStations ?: OfflineCache.loadStations() ?: return null
        val byId = saved.associateBy { it.id }
        return baseStations.map { base ->
            byId[base.id]?.copy(
                stationName = base.stationName,
                riverName = base.riverName,
                district = base.district,
                tributaryOf = base.tributaryOf,
                marksEstimated = base.marksEstimated,
                feed = base.feed,
                downstreamTowns = base.downstreamTowns,
                infoUrl = base.infoUrl
            ) ?: base.copy(hasLiveReading = false, lastUpdatedText = "")
        }
    }

    /**
     * Weather for the selected town (or an exact GPS fix) rather than the district centre — hill-country
     * towns in one district can differ by 5–10 °C. Temperatures are the median of [TEMPERATURE_ENSEMBLE_MODELS].
     */
    suspend fun fetchRegionalReport(
        region: SriLankaRegion,
        nearestCity: String? = null,
        exactLatLon: Pair<Double, Double>? = null
    ): RegionalWeatherReport =
        withContext(Dispatchers.IO) {
            val now = Date()
            val timestamp = slstFormat("HH:mm 'SLST'").format(now)
            val (lat, lon) = exactLatLon
                ?: nearestCity?.let { region.getCoordinatesForCity(it) }
                ?: (region.latitude to region.longitude)
            val cacheKey = String.format(Locale.US, "%.3f,%.3f", lat, lon)
            try {
                val (response, ensembleResponse) = coroutineScope {
                    val forecast = async { WeatherNetworkClient.openMeteoService.getForecast(lat, lon) }
                    val ensemble = async {
                        // Optional: if the multi-model call fails we still have the single best-match model
                        runCatching { WeatherNetworkClient.openMeteoService.getMultiModelTemperatures(lat, lon) }.getOrNull()
                    }
                    forecast.await() to ensemble.await()
                }
                val current = response.current ?: error("No current conditions in response")
                val hourly = response.hourly
                val daily = response.daily

                val nowHourKey = slstFormat("yyyy-MM-dd'T'HH:00").format(now)
                val minuteFraction = slstFormat("mm").format(now).toDouble() / 60.0
                val ensembleHourly = ensembleResponse?.hourly
                val ensembleNowIdx = ensembleHourly.timeIndexOf(nowHourKey)
                val forecastNowIdx = hourly?.time?.indexOf(nowHourKey)?.takeIf { it >= 0 } ?: 0
                val modelCount = TEMPERATURE_ENSEMBLE_MODELS.count { model ->
                    ensembleHourly.modelSeries("temperature_2m", model).getOrNull(ensembleNowIdx) != null
                }

                val tempC = ensembleHourly.medianAt("temperature_2m", ensembleNowIdx, minuteFraction)
                    ?: current.temperature2m ?: error("No temperature in response")
                val feelsLikeC = ensembleHourly.medianAt("apparent_temperature", ensembleNowIdx, minuteFraction)
                    ?: current.apparentTemperature ?: tempC
                val humidity = ensembleHourly.medianAt("relative_humidity_2m", ensembleNowIdx, minuteFraction)?.roundToInt()
                    ?: current.relativeHumidity2m ?: 80
                val precipMm = current.precipitation ?: 0.0
                val windSpeed = current.windSpeed10m ?: 0.0
                val windGust = current.windGusts10m ?: windSpeed
                val pressure = current.surfacePressure ?: 1010.0
                val weatherCode = current.weatherCode ?: 0

                val hourlyItems = buildList {
                    val times = hourly?.time.orEmpty()
                    val temps = hourly?.temperature2m.orEmpty()
                    val rains = hourly?.precipitation.orEmpty()
                    val probs = hourly?.precipitationProbability.orEmpty()
                    val gusts = hourly?.windGusts10m.orEmpty()
                    val codes = hourly?.weatherCode.orEmpty()
                    val dayFlags = hourly?.isDay.orEmpty()
                    for (i in forecastNowIdx until minOf(forecastNowIdx + 24, times.size)) {
                        val rawTime = times.getOrNull(i) ?: ""
                        val ensembleIdx = ensembleHourly.timeIndexOf(rawTime)
                        val shortTime = rawTime.substringAfter("T", rawTime).ifEmpty { "${(i * 2) % 24}:00" }
                        add(
                            HourlyForecastItem(
                                timeLabel = shortTime,
                                tempC = ensembleHourly.medianAt("temperature_2m", ensembleIdx)
                                    ?: temps.getOrNull(i) ?: tempC,
                                rainMm = rains.getOrNull(i) ?: 0.0,
                                rainProbability = probs.getOrNull(i) ?: 65,
                                windGustKmh = gusts.getOrNull(i) ?: windGust,
                                weatherCode = codes.getOrNull(i) ?: weatherCode,
                                isDay = (dayFlags.getOrNull(i) ?: 1) == 1
                            )
                        )
                    }
                }.ifEmpty { fallbackHourly(region) }

                val dailyItems = buildList {
                    val dates = daily?.time.orEmpty()
                    val ensembleDaily = ensembleResponse?.daily
                    val maxTemps = daily?.temperature2mMax.orEmpty()
                    val minTemps = daily?.temperature2mMin.orEmpty()
                    val rainSums = daily?.precipitationSum.orEmpty()
                    val maxProbs = daily?.precipitationProbabilityMax.orEmpty()
                    val maxWinds = daily?.windGusts10mMax.orEmpty()
                    val codes = daily?.weatherCode.orEmpty()
                    val weekday = slstFormat("EEE")
                    val isoDate = slstFormat("yyyy-MM-dd")
                    val dayNames = dates.mapIndexed { i, d ->
                        if (i == 0) "Today" else runCatching { weekday.format(isoDate.parse(d)!!) }.getOrElse { "Day ${i + 1}" }
                    }
                    for (i in 0 until minOf(7, dates.size)) {
                        val rainTotal = rainSums.getOrNull(i) ?: 18.0
                        val gustMax = maxWinds.getOrNull(i) ?: 42.0
                        val risk = when {
                            rainTotal >= 100.0 || gustMax >= 65.0 -> AlertSeverity.CRITICAL_RED
                            rainTotal >= 50.0 || gustMax >= 50.0 -> AlertSeverity.MAJOR_AMBER
                            rainTotal >= 20.0 || gustMax >= 38.0 -> AlertSeverity.ADVISORY_YELLOW
                            else -> AlertSeverity.SAFE_GREEN
                        }
                        add(
                            DailyForecastItem(
                                dayLabel = dayNames.getOrElse(i) { "Day ${i + 1}" },
                                dateLabel = dates.getOrNull(i) ?: "",
                                maxTempC = ensembleDaily.medianAt("temperature_2m_max", i)
                                    ?: maxTemps.getOrNull(i) ?: tempC,
                                minTempC = ensembleDaily.medianAt("temperature_2m_min", i)
                                    ?: minTemps.getOrNull(i) ?: tempC,
                                totalRainMm = rainTotal,
                                maxRainProb = maxProbs.getOrNull(i) ?: 70,
                                maxWindKmh = gustMax,
                                weatherSummary = describeWmoCode(codes.getOrNull(i) ?: 61, rainTotal),
                                riskLevel = risk,
                                weatherCode = codes.getOrNull(i) ?: 3
                            )
                        )
                    }
                }.ifEmpty { fallbackDaily(region) }

                val rain24h = dailyItems.firstOrNull()?.totalRainMm ?: (precipMm * 8.0)

                RegionalWeatherReport(
                    region = region,
                    locationName = nearestCity ?: region.name,
                    currentTempC = tempC,
                    feelsLikeC = feelsLikeC,
                    humidityPercent = humidity,
                    precipitationMm = precipMm,
                    rain24hMm = rain24h,
                    windSpeedKmh = windSpeed,
                    windGustKmh = windGust,
                    weatherDescription = describeWmoCode(weatherCode, rain24h),
                    weatherCode = weatherCode,
                    isDay = (current.isDay ?: 1) == 1,
                    uvIndex = current.uvIndex,
                    windDirectionDeg = current.windDirection10m?.roundToInt(),
                    sunrise = daily?.sunrise?.firstOrNull()?.substringAfter("T"),
                    sunset = daily?.sunset?.firstOrNull()?.substringAfter("T"),
                    pressureHpa = pressure,
                    hourly = hourlyItems,
                    daily = dailyItems,
                    isLiveApiData = true,
                    syncedTimestamp = if (modelCount >= 2) {
                        "Live • $modelCount-model blend • $timestamp"
                    } else {
                        "Live Open-Meteo • $timestamp"
                    },
                    fetchedAtMillis = now.time
                ).also {
                    lastLiveReports[cacheKey] = it
                    OfflineCache.saveReport(cacheKey, it)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                (lastLiveReports[cacheKey] ?: OfflineCache.loadReport(cacheKey))?.asSavedCopy()
                    ?: buildFallbackRegionalReport(region, timestamp).copy(locationName = nearestCity ?: region.name)
            }
        }

    // App station id → gauge name in the Irrigation Department hydrology feed
    private val irrigationGaugeNames: Map<String, String> = mapOf(
        "kelani_nagalagam" to "Nagalagam Street",
        "kelani_hanwella" to "Hanwella",
        "kelani_glencorse" to "Glencourse",
        "kelani_kitulgala" to "Kithulgala",
        "kelani_holombuwa" to "Holombuwa",
        "kelani_deraniyagala" to "Deraniyagala",
        "kelani_norwood" to "Norwood",
        "kalu_ratnapura" to "Rathnapura",
        "kalu_ellagawa" to "Ellagawa",
        "kalu_putupaula" to "Putupaula",
        "kalu_millakanda" to "Kalawellawa (Millakanda)",
        "kalu_magura" to "Magura",
        "gin_baddegama" to "Baddegama",
        "gin_tawalama" to "Thawalama",
        "nilwala_panadugama" to "Panadugama",
        "nilwala_pitabeddara" to "Pitabeddara",
        "nilwala_urawa" to "Urawa",
        "walawe_moraketiya" to "Moraketiya",
        "kirindi_thanamalwila" to "Thanamalwila",
        "kirindi_wellawaya" to "Wellawaya",
        "menik_kataragama" to "Katharagama",
        "kumbukkan_nakkala" to "Nakkala",
        "heda_siyambalanduwa" to "Siyambalanduwa",
        "mahaweli_peradeniya" to "Peradeniya",
        "mahaweli_nawalapitiya" to "Nawalapitiya",
        "mahaweli_weragantota" to "Weraganthota",
        "mahaweli_manampitiya" to "Manampitiya",
        "yan_horowpothana" to "Horowpothana",
        "malwathu_thanthirimale" to "Thanthirimale",
        "deduru_moragaswewa" to "Moragaswewa",
        "maha_giriulla" to "Giriulla",
        "maha_badalgama" to "Badalgama",
        "attanagalu_dunamale" to "Dunamale",
        "nilwala_thalgahagoda" to "Thalgahagoda",
        "mahaweli_thaldena" to "Thaldena",
        "kirindi_kudaoya" to "Kuda Oya",
        "maduru_padiyathalawa" to "Padiyathalawa",
        "mee_galgamuwa" to "Galgamuwa",
        "ma_yakawewa" to "Yaka Wewa"
    )

    // Station names in the waterlevels.lk feed for the HMIS and RiverNet gauges
    private val waterLevelsGaugeNames: Map<String, String> = mapOf(
        "kelani_karawanella" to "Karawanella Bridge",
        "mahaweli_mahiyangana" to "Mahiyangana Bridge",
        "maduru_vadamunai" to "Vadamunai Bridge",
        "maduru_punani" to "Punani Anicut",
        "maduru_pulipanchakal" to "Pulipanchakal Bridge",
        "mundeni_galodaya" to "Galodaya Bridge",
        "mundeni_lavanai" to "Lavanai Aru Bridge",
        "magalawatavan_mullamunai" to "Mullamunai Bridge",
        "andella_maalayarkattu" to "Maalayarkattu Bridge",
        "andella_rajagalathenna" to "Rajagalathenna Bridge",
        "kelani_mapitigama" to "Mapitigama",
        "kelani_pugoda" to "Pugoda",
        "kalu_kuruvita" to "Kuru Ganga (Kuruvita)",
        "kalu_elapatha" to "Niriella Ganga (Elapatha)",
        "kalu_ayagama" to "Galathura Oya (Ayagama)",
        "kalu_pelmadulla" to "Denawaka Ganga (Pelmadulla)",
        "kalu_kalawana" to "Kukule Ganga (Kalawana)",
        "kalu_kahawatta" to "Wey Ganga (Kahawaththa)",
        "uruwal_miriswatta" to "Miriswatta",
        "wattala_kaluela" to "Wattala"
    )

    // Gauges the Irrigation Dept publishes in feet (hydrostations "Unit" column, checked Oct 2026).
    // Used until the live unit list loads, and if it can't be reached.
    private val fallbackFeetGauges = setOf("nagalagam street")

    @Volatile
    private var feetGauges: Set<String>? = null

    /** Lower-cased gauge names reported in feet; fetched once per app run, falls back to the known list. */
    private suspend fun feetGaugeNames(): Set<String> {
        feetGauges?.let { return it }
        return try {
            WeatherNetworkClient.irrigationGaugeService.queryStationUnits().features
                ?.mapNotNull { it.attributes }
                ?.filter { it.unit?.trim()?.lowercase(Locale.US) in setOf("ft", "feet") }
                ?.mapNotNull { it.station?.trim()?.lowercase(Locale.US) }
                ?.toSet()
                ?.takeIf { it.isNotEmpty() }
                ?.also { feetGauges = it }
                ?: fallbackFeetGauges
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            fallbackFeetGauges
        }
    }

    // Last successful live sync, reused if a later refresh fails so gauges don't snap back to reference values
    @Volatile
    private var lastLiveStations: List<RiverGaugeStation>? = null

    private suspend fun fetchIrrigationReadings24h(): List<IrrigationGaugeReading> {
        val readings = mutableListOf<IrrigationGaugeReading>()
        var offset = 0
        // ~1 reading/gauge/hour ≈ 950 rows per day; page in case of extra storm-time readings
        repeat(4) {
            val page = WeatherNetworkClient.irrigationGaugeService.queryReadings(resultOffset = offset)
            val pageReadings = page.features.orEmpty().mapNotNull { it.attributes }
            readings += pageReadings
            if (page.exceededTransferLimit != true || pageReadings.isEmpty()) return readings
            offset += pageReadings.size
        }
        return readings
    }

    private suspend fun fetchStationDischarges(): List<Double?> {
        val response = WeatherNetworkClient.floodService.getRiverDischargeForLocations(
            latitudes = baseStations.joinToString(",") { it.latitude.toString() },
            longitudes = baseStations.joinToString(",") { it.longitude.toString() }
        )
        return baseStations.indices.map { idx ->
            response.getOrNull(idx)?.daily?.riverDischarge?.firstOrNull()
        }
    }

    /** Latest reading of every gauge waterlevels.lk lists, by lower-cased station name; null if unreachable. */
    private suspend fun fetchWaterLevelsReadings(): Map<String, WaterLevelsStation>? = try {
        WeatherNetworkClient.waterLevelsService.getLevels().stations
            ?.filter { it.station != null }
            ?.associateBy { it.station!!.trim().lowercase(Locale.US) }
            ?.takeIf { it.isNotEmpty() }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }

    private fun parseIsoMillis(text: String?): Long? {
        if (text.isNullOrBlank()) return null
        for (pattern in listOf("yyyy-MM-dd'T'HH:mm:ss.SSSX", "yyyy-MM-dd'T'HH:mm:ssX")) {
            val parsed = runCatching {
                SimpleDateFormat(pattern, Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.parse(text)
            }.getOrNull()
            if (parsed != null) return parsed.time
        }
        return null
    }

    /**
     * [station] updated from a waterlevels.lk reading. That feed gives the latest level and the rate over the
     * last 3 hours but no 24-hour series, so the graph stays empty for these gauges. The alert and flood
     * levels stay the built-in ones (HMIS / RiverNet published levels, or the app's estimates).
     */
    private fun RiverGaugeStation.withWaterLevelsReading(reading: WaterLevelsStation, discharge: Double): RiverGaugeStation? {
        if (reading.status == "stale") return null
        val published = reading.published
        // The feed's "level" is always feet; "published" keeps the agency's own number and unit
        val levelM = when {
            published?.level != null && published.unit == "m" -> published.level
            reading.levelFt != null -> reading.levelFt * FEET_TO_METRES
            else -> return null
        }
        val readingTime = parseIsoMillis(reading.observedAt) ?: return null
        // Older than 3 hours: treat like the app treats a silent Irrigation gauge
        if (System.currentTimeMillis() - readingTime > 3 * 3_600_000L) return null
        val changeM = (reading.changePerHourFt ?: 0.0) * FEET_TO_METRES
        return copy(
            currentLevelM = levelM,
            previousLevelM = levelM - changeM,
            sourceUnit = if (published?.unit == "ft") "ft" else "m",
            catchmentRainMm24h = reading.rain?.fallen24hMm ?: 0.0,
            rainMeasured = reading.rain?.fallen24hMm != null,
            dischargeM3s = discharge,
            hourlyLevelsM = listOf(levelM),
            history24hM = emptyList(),
            hasLiveReading = true,
            readingTimeMillis = readingTime,
            lastUpdatedText = slstFormat("HH:mm").format(Date(readingTime))
        )
    }

    suspend fun fetchLiveRiverStations(selectedRegion: SriLankaRegion): List<RiverGaugeStation> =
        withContext(Dispatchers.IO) {
            val readingsByGauge = try {
                fetchIrrigationReadings24h()
                    .filter { it.gauge != null && it.waterLevelM != null && it.creationDateMillis != null }
                    .groupBy { it.gauge!!.trim().lowercase(Locale.US) }
                    .mapValues { (_, list) -> list.sortedBy { it.creationDateMillis } }
            } catch (_: Exception) {
                null
            }
            val waterLevels = fetchWaterLevelsReadings()
            val discharges = try {
                fetchStationDischarges()
            } catch (_: Exception) {
                null
            }
            val feetGauges = feetGaugeNames()

            if (readingsByGauge.isNullOrEmpty() && waterLevels.isNullOrEmpty()) {
                cachedStations()?.let { return@withContext it }
            }
            val cachedById = if (readingsByGauge.isNullOrEmpty() || waterLevels.isNullOrEmpty()) {
                cachedStations()?.associateBy { it.id }.orEmpty()
            } else {
                emptyMap()
            }

            val stations = baseStations.mapIndexed { idx, station ->
                val discharge = discharges?.getOrNull(idx)?.takeIf { it >= 0.0 } ?: station.dischargeM3s
                val noReading = station.copy(dischargeM3s = discharge, hasLiveReading = false, lastUpdatedText = "")

                if (station.feed != GaugeFeed.IRRIGATION) {
                    if (waterLevels.isNullOrEmpty()) return@mapIndexed cachedById[station.id] ?: noReading
                    val reading = waterLevelsGaugeNames[station.id]?.let { waterLevels[it.lowercase(Locale.US)] }
                    return@mapIndexed reading?.let { station.withWaterLevelsReading(it, discharge) } ?: noReading
                }

                val gaugeName = irrigationGaugeNames[station.id]?.lowercase(Locale.US)
                val history = gaugeName?.let { readingsByGauge?.get(it) }
                val latest = history?.lastOrNull()
                if (history == null || latest == null) {
                    // The Irrigation service has a shared request quota and can refuse on busy flood days;
                    // waterlevels.lk carries the same gauges, so use its latest reading before giving up
                    if (readingsByGauge.isNullOrEmpty() && gaugeName != null) {
                        val fallback = waterLevels?.get(gaugeName) ?: waterLevels?.get(gaugeName.replace("rathnapura", "ratnapura"))
                        fallback?.let { station.withWaterLevelsReading(it, discharge) }?.let { return@mapIndexed it }
                        return@mapIndexed cachedById[station.id] ?: noReading
                    }
                    return@mapIndexed noReading
                }
                // The feed sends raw numbers in the gauge's own unit; everything in the app is metres
                val inFeet = gaugeName in feetGauges
                val toM = if (inFeet) FEET_TO_METRES else 1.0
                val levels = history.mapNotNull { it.waterLevelM?.times(toM) }
                val readingTime = slstFormat("HH:mm").format(Date(latest.creationDateMillis!!))
                station.copy(
                    currentLevelM = levels.last(),
                    previousLevelM = levels.getOrElse(levels.size - 2) { levels.last() },
                    alertLevelM = latest.alertLevelM?.takeIf { it > 0.0 }?.times(toM) ?: station.alertLevelM,
                    minorFloodLevelM = latest.minorFloodLevelM?.takeIf { it > 0.0 }?.times(toM) ?: station.minorFloodLevelM,
                    majorFloodLevelM = latest.majorFloodLevelM?.takeIf { it > 0.0 }?.times(toM) ?: station.majorFloodLevelM,
                    sourceUnit = if (inFeet) "ft" else "m",
                    catchmentRainMm24h = history.sumOf { it.rainFallMm ?: 0.0 },
                    dischargeM3s = discharge,
                    hourlyLevelsM = levels.takeLast(6),
                    history24hM = levels,
                    hasLiveReading = true,
                    readingTimeMillis = latest.creationDateMillis,
                    lastUpdatedText = readingTime
                )
            }
            if (stations.any { it.hasLiveReading }) {
                lastLiveStations = stations
                OfflineCache.saveStations(stations)
            }
            stations
        }

    // ---- Reservoirs ----

    @Volatile
    private var lastReservoirs: List<Reservoir>? = null

    @Volatile
    private var lastReservoirFetchMillis: Long = 0L

    fun cachedReservoirs(): List<Reservoir>? = lastReservoirs ?: OfflineCache.loadReservoirs()

    /** Storage and releases of the reservoirs (daily reports); the saved copy if offline, empty if none. */
    suspend fun fetchReservoirs(): List<Reservoir> = withContext(Dispatchers.IO) {
        // The reports change once or twice a day, so don't ask more than every 30 minutes
        lastReservoirs?.let { if (System.currentTimeMillis() - lastReservoirFetchMillis < 30 * 60_000L) return@withContext it }
        val live = try {
            WeatherNetworkClient.waterLevelsService.getReservoirs().reservoirs.orEmpty().mapNotNull { dto ->
                val name = dto.name?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
                Reservoir(
                    id = dto.id ?: name.lowercase(Locale.US).replace(' ', '-'),
                    name = name,
                    river = dto.river?.takeIf { it.isNotBlank() },
                    district = dto.district?.takeIf { it.isNotBlank() },
                    type = dto.type ?: "major",
                    status = when (dto.status) {
                        "spilling" -> ReservoirStatus.SPILLING
                        "releasing" -> ReservoirStatus.RELEASING
                        "holding" -> ReservoirStatus.HOLDING
                        else -> ReservoirStatus.NO_DATA
                    },
                    storagePercent = dto.storagePercent?.takeIf { it >= 0.0 },
                    flowCusecs = dto.flow?.cusecs?.takeIf { it >= 0.0 },
                    flowKind = dto.flow?.kind,
                    readingDay = dto.readingDay,
                    agency = dto.source,
                    infoUrl = dto.url?.takeIf { it.startsWith("https://") }
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyList()
        }
        if (live.isNotEmpty()) {
            lastReservoirs = live
            lastReservoirFetchMillis = System.currentTimeMillis()
            OfflineCache.saveReservoirs(live)
            live
        } else {
            cachedReservoirs().orEmpty()
        }
    }

    // Towns shown on the island overview map (name, district)
    private val islandMapTowns = listOf(
        "Colombo" to "Colombo", "Ratnapura" to "Ratnapura", "Galle" to "Galle", "Matara" to "Matara",
        "Kandy" to "Kandy", "Nuwara Eliya" to "Nuwara Eliya", "Kalutara" to "Kalutara", "Gampaha" to "Gampaha",
        "Anuradhapura" to "Anuradhapura", "Jaffna" to "Jaffna", "Trincomalee" to "Trincomalee",
        "Batticaloa" to "Batticaloa", "Hambantota" to "Hambantota"
    )

    /** Live current conditions for every map town in a single request; empty if offline. */
    suspend fun fetchIslandSnapshots(): List<CityWeatherSnapshot> = withContext(Dispatchers.IO) {
        val coords = islandMapTowns.map { (town, district) ->
            val region = regions.firstOrNull { it.name == district } ?: regions.first()
            region.getCoordinatesForCity(town)
        }
        try {
            val responses = WeatherNetworkClient.openMeteoService.getCurrentForLocations(
                latitudes = coords.joinToString(",") { it.first.toString() },
                longitudes = coords.joinToString(",") { it.second.toString() }
            )
            islandMapTowns.mapIndexedNotNull { idx, (town, district) ->
                val current = responses.getOrNull(idx)?.current ?: return@mapIndexedNotNull null
                val rainToday = responses[idx].daily?.precipitationSum?.firstOrNull() ?: 0.0
                CityWeatherSnapshot(
                    name = town,
                    district = district,
                    tempC = current.temperature2m ?: return@mapIndexedNotNull null,
                    humidityPercent = current.relativeHumidity2m ?: 0,
                    precipitationMm = current.precipitation ?: 0.0,
                    rainTodayMm = rainToday,
                    windGustKmh = current.windGusts10m ?: 0.0
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyList()
        }
    }

    // Instant, offline-safe values shown until the first live sync completes
    fun getReferenceStations(): List<RiverGaugeStation> =
        baseStations.map { it.copy(hasLiveReading = false, lastUpdatedText = "") }

    fun getPlaceholderRegionalReport(region: SriLankaRegion, nearestCity: String): RegionalWeatherReport =
        buildFallbackRegionalReport(region, slstFormat("HH:mm 'SLST'").format(Date()))
            .copy(locationName = nearestCity, syncedTimestamp = "Syncing live weather…")

    // ---- Multi-model blend helpers (Open-Meteo returns one "<variable>_<model>" series per model) ----

    private fun Map<String, Any?>?.timeIndexOf(timeKey: String): Int =
        (this?.get("time") as? List<*>)?.indexOf(timeKey) ?: -1

    private fun Map<String, Any?>?.modelSeries(variable: String, model: String): List<Double?> =
        (this?.get("${variable}_$model") as? List<*>)?.map { (it as? Number)?.toDouble() }.orEmpty()

    /** Median across models at [index], linearly interpolated [fraction] of the way to the next step. */
    private fun Map<String, Any?>?.medianAt(variable: String, index: Int, fraction: Double = 0.0): Double? {
        if (this == null || index < 0) return null
        val values = TEMPERATURE_ENSEMBLE_MODELS.mapNotNull { model ->
            val series = modelSeries(variable, model)
            val a = series.getOrNull(index) ?: return@mapNotNull null
            val b = series.getOrNull(index + 1) ?: a
            a + (b - a) * fraction
        }.sorted()
        if (values.isEmpty()) return null
        val mid = values.size / 2
        val median = if (values.size % 2 == 1) values[mid] else (values[mid - 1] + values[mid]) / 2.0
        return (median * 10).roundToInt() / 10.0
    }

    private fun slstFormat(pattern: String): SimpleDateFormat =
        SimpleDateFormat(pattern, Locale.US).apply { timeZone = TimeZone.getTimeZone("Asia/Colombo") }

    private fun describeWmoCode(code: Int, rainMm: Double): String = when {
        code >= 95 || rainMm >= 120.0 -> "Heavy Monsoon Thunderstorms & Squalls"
        code in 80..82 || rainMm >= 60.0 -> "Intense Monsoon Downpours"
        code in 61..67 || rainMm >= 20.0 -> "Moderate to Heavy Rain Bands"
        code in 51..57 -> "Intermittent Monsoon Drizzle"
        code in 1..3 -> "Partly Cloudy with Passing Showers"
        else -> "Humid Tropical Skies"
    }

    private fun fallbackHourly(region: SriLankaRegion): List<HourlyForecastItem> {
        val baseRain = if (region.id in listOf("ratnapura", "kalutara", "colombo", "galle", "kandy", "kegalle", "gampaha")) 12.5 else 3.8
        val hours = listOf("06:00", "08:00", "10:00", "12:00", "14:00", "16:00", "18:00", "20:00", "22:00")
        return hours.mapIndexed { idx, time ->
            val factor = 1.0 + (idx % 4) * 0.35
            HourlyForecastItem(
                timeLabel = time,
                tempC = (28.5 - region.elevationMeters * 0.0055) + (idx % 3) * 0.6,
                rainMm = ((baseRain * factor) * 10).roundToInt() / 10.0,
                rainProbability = (68 + (idx * 4) % 28).coerceAtMost(98),
                windGustKmh = 38.0 + (idx % 5) * 5.2,
                weatherCode = if (baseRain > 8.0) 65 else 61
            )
        }
    }

    private fun fallbackDaily(region: SriLankaRegion): List<DailyForecastItem> {
        val wetZone = region.id in listOf("ratnapura", "kalutara", "colombo", "galle", "matara", "kandy", "nuwara_eliya", "gampaha", "kegalle")
        val days = listOf("Today", "Tomorrow", "Wed", "Thu", "Fri", "Sat", "Sun")
        val dates = listOf("Day 1", "Day 2", "Day 3", "Day 4", "Day 5", "Day 6", "Day 7")
        val rains = if (wetZone) {
            listOf(142.0, 118.0, 86.0, 64.0, 48.0, 34.0, 29.0)
        } else {
            listOf(46.0, 52.0, 38.0, 24.0, 18.0, 15.0, 12.0)
        }
        return days.mapIndexed { i, day ->
            val rain = rains[i]
            val gust = if (wetZone) 58.0 - i * 2.5 else 42.0 - i * 1.5
            val risk = when {
                rain >= 100.0 -> AlertSeverity.CRITICAL_RED
                rain >= 55.0 -> AlertSeverity.MAJOR_AMBER
                rain >= 25.0 -> AlertSeverity.ADVISORY_YELLOW
                else -> AlertSeverity.SAFE_GREEN
            }
            DailyForecastItem(
                dayLabel = day,
                dateLabel = dates[i],
                maxTempC = (30.5 - region.elevationMeters * 0.005).roundToInt().toDouble(),
                minTempC = (24.0 - region.elevationMeters * 0.005).roundToInt().toDouble(),
                totalRainMm = rain,
                maxRainProb = if (rain > 60) 92 else 65,
                maxWindKmh = gust,
                weatherSummary = describeWmoCode(63, rain),
                riskLevel = risk
            )
        }
    }

    private fun buildFallbackRegionalReport(region: SriLankaRegion, timestamp: String): RegionalWeatherReport {
        val daily = fallbackDaily(region)
        val todayRain = daily.first().totalRainMm
        val maxGust = daily.first().maxWindKmh
        val baseTemp = 29.0 - (region.elevationMeters * 0.0055)
        return RegionalWeatherReport(
            region = region,
            currentTempC = (baseTemp * 10).roundToInt() / 10.0,
            feelsLikeC = ((baseTemp + 3.4) * 10).roundToInt() / 10.0,
            humidityPercent = 88,
            precipitationMm = (todayRain / 12.0 * 10).roundToInt() / 10.0,
            rain24hMm = todayRain,
            windSpeedKmh = 31.0,
            windGustKmh = maxGust,
            weatherDescription = describeWmoCode(65, todayRain),
            pressureHpa = 1006.8,
            hourly = fallbackHourly(region),
            daily = daily,
            isLiveApiData = false,
            syncedTimestamp = "Offline estimate • no connection at $timestamp"
        )
    }
}
