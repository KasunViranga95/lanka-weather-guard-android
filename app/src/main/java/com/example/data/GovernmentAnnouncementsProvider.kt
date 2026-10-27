package com.example.data

import com.example.model.AlertSeverity
import com.example.model.GovernmentAnnouncement
import com.example.model.RegionalWeatherReport
import com.example.model.RiverGaugeStation
import com.example.model.SriLankaRegion
import java.util.Locale
import kotlin.math.roundToInt

object GovernmentAnnouncementsProvider {

    fun getAnnouncementsForArea(
        selectedRegion: SriLankaRegion,
        nearestCity: String,
        primaryStation: RiverGaugeStation,
        allRegions: List<SriLankaRegion>,
        report: RegionalWeatherReport
    ): List<GovernmentAnnouncement> {
        val localRain = maxOf(report.rain24hMm, primaryStation.catchmentRainMm24h)
        val localSeverity = when {
            primaryStation.floodStatus.severityRank >= 4 -> AlertSeverity.CRITICAL_RED
            primaryStation.floodStatus.severityRank == 3 || localRain >= 95.0 -> AlertSeverity.MAJOR_AMBER
            else -> AlertSeverity.ADVISORY_YELLOW
        }

        val residingOfficialDirective = GovernmentAnnouncement(
            id = "gov_residing_${selectedRegion.id}_${nearestCity.lowercase().replace(" ", "_")}",
            headline = "Official Government Weather & Flood Directive for $nearestCity (${selectedRegion.name} District)",
            issuingMinistryOrAgency = "Disaster Management Centre (DMC) • District Secretariat ${selectedRegion.name}",
            referenceCode = "DMC/SL/EOC/${selectedRegion.id.uppercase()}/2026-10",
            category = "Residing Area Directive",
            publishedTimeText = "Updated 15 mins ago • Official Gazette / Situation Report",
            targetDistricts = listOf(selectedRegion.name),
            targetCities = listOf(nearestCity) + primaryStation.downstreamTowns.take(3),
            summary = "The District Disaster Management Coordinating Unit (${selectedRegion.name}) and Department of Irrigation confirm that ${primaryStation.riverName} at ${primaryStation.stationName} is currently recording ${String.format(Locale.US, "%.2f m", primaryStation.currentLevelM)} (${primaryStation.floodStatus.label}). Cumulative 24-hour rainfall in the $nearestCity catchment has reached ${String.format(Locale.US, "%.1f mm", localRain)} with wind gusts up to ${report.windGustKmh.roundToInt()} km/h.",
            keyDirectives = listOf(
                "Grama Niladhari divisions in $nearestCity and low-lying corridors (${primaryStation.downstreamTowns.take(4).joinToString(", ")}) are placed on active flood readiness.",
                if (selectedRegion.isLandslideProne) {
                    "NBRO Geotechnical Directive: Residents on cut slopes and high-gradient embankments in $nearestCity (${selectedRegion.name}) must evacuate to certified relief centres if soil cracking or muddy water seepage occurs."
                } else {
                    "Urban & Riverine Drainage Directive: Divisional Secretariats and local authorities in $nearestCity have deployed mobile flood pumps and Navy rescue dinghies."
                },
                "Official DMC Situation Reports, river hydrographs, and Divisional Secretariat relief allocations can be verified via the official portal link below."
            ),
            officialUrl = "https://www.dmc.gov.lk",
            officialPortalName = "DMC Official Portal (dmc.gov.lk)",
            severity = localSeverity,
            isResidingAreaMatch = true
        )

        val nationalAnnouncements = listOf(
            GovernmentAnnouncement(
                id = "gov_irrigation_flood_bulletin",
                headline = "Special Flood Warning Bulletin #14: Kalu, Kelani, Gin, Nilwala, Maha & Attanagalu Basins",
                issuingMinistryOrAgency = "Department of Irrigation • Hydrology & Disaster Management Division",
                referenceCode = "ID/HYD/FLOOD/2026/10-26",
                category = "DMC Flood Bulletin",
                publishedTimeText = "30 mins ago",
                targetDistricts = listOf("Ratnapura", "Colombo", "Kalutara", "Gampaha", "Kegalle", "Galle", "Matara", "Kurunegala"),
                targetCities = listOf("Ratnapura Town", "Wellampitiya", "Kaduwela", "Hanwella", "Avissawella", "Horana", "Bulathsinhala", "Baddegama", "Akuressa", "Giriulla", "Badalgama"),
                summary = "The Hydrology Division of the Department of Irrigation announces that water levels in Kalu Ganga (Ratnapura 9.72m — Major Flood; Ellagawa 11.40m), Kelani Ganga (Nagalagam Street 1.92m, Hanwella 8.45m, Glencorse 16.80m), Maha Oya (Badalgama 6.42m), and Attanagalu Oya (Dunamale 4.62m) remain at critical flood thresholds.",
                keyDirectives = listOf(
                    "Motorists and residents along the Kelani Valley, Ratnapura–Panadura (A8), and Colombo–Ratnapura (A4) corridors must avoid submerged sections.",
                    "Real-time telemetric gauge readings are published every 30 minutes on RiverWatch.LK and irrigation.gov.lk.",
                    "Sluice gates of Laxapana, Canyon, Kukule Ganga, and Deduru Oya reservoirs are being regulated under strict flood-mitigation supervision."
                ),
                officialUrl = "https://riverwatch.lk",
                officialPortalName = "RiverWatch.LK & Irrigation Dept",
                severity = AlertSeverity.CRITICAL_RED
            ),
            GovernmentAnnouncement(
                id = "gov_met_red_warning",
                headline = "Red Advisory for Heavy Rain (>150mm), Thunderstorms & Rough Sea Areas",
                issuingMinistryOrAgency = "Department of Meteorology Sri Lanka • National Early Warning Centre",
                referenceCode = "MET/WARN/2026/RED-48",
                category = "Met Dept Warning",
                publishedTimeText = "45 mins ago",
                targetDistricts = listOf("Colombo", "Gampaha", "Kalutara", "Galle", "Matara", "Ratnapura", "Kegalle", "Kandy", "Nuwara Eliya"),
                targetCities = listOf("Colombo Fort & Pettah", "Negombo", "Kalutara Town", "Galle City", "Matara Town", "Ratnapura Town", "Kegalle Town"),
                summary = "Active monsoon shear zone and deep convective cloud bands across the island will bring very heavy falls exceeding 150 mm in Western, Sabaragamuwa, Southern, and Central provinces, with sudden squall gusts of 60–70 km/h.",
                keyDirectives = listOf(
                    "Naval and fishing communities are strictly advised not to venture into the sea areas off the coast extending from Puttalam to Pottuvil via Colombo, Galle, and Hambantota.",
                    "Public advised to take precautions against strong winds and lightning strikes during afternoon and night thundershowers."
                ),
                officialUrl = "https://www.meteo.gov.lk",
                officialPortalName = "Dept of Meteorology (meteo.gov.lk)",
                severity = AlertSeverity.CRITICAL_RED
            ),
            GovernmentAnnouncement(
                id = "gov_nbro_landslide_red",
                headline = "NBRO Level 3 (Red Evacuation) & Level 2 (Amber) Landslide Early Warning",
                issuingMinistryOrAgency = "National Building Research Organisation (NBRO) • Landslide Research Division",
                referenceCode = "NBRO/LSRMD/EW/2026-109",
                category = "NBRO Landslide",
                publishedTimeText = "1 hour ago",
                targetDistricts = listOf("Ratnapura", "Kegalle", "Kalutara", "Kandy", "Nuwara Eliya", "Badulla", "Matale", "Galle", "Matara"),
                targetCities = listOf("Ratnapura Town", "Kuruwita", "Eheliyagoda", "Kitulgala", "Deraniyagala", "Bulathsinhala", "Nawalapitiya", "Hatton", "Norwood", "Passara"),
                summary = "Continuous rainfall exceeding 150mm within 24 hours has saturated hill slopes in Ratnapura, Kegalle, Kalutara, Kandy, and Nuwara Eliya districts. Level 3 (Red Evacuation) warnings are in force for high-risk Divisional Secretariat divisions.",
                keyDirectives = listOf(
                    "Evacuate immediately from steep hill slopes, cut embankments, and boulder-prone valleys in red-flagged DS divisions.",
                    "Report any ground cracks, slumping retaining walls, or turbid spring water to the NBRO 24/7 hotline (011-2588946) or DMC (117)."
                ),
                officialUrl = "https://www.nbro.gov.lk",
                officialPortalName = "NBRO Official Portal (nbro.gov.lk)",
                severity = AlertSeverity.CRITICAL_RED
            ),
            GovernmentAnnouncement(
                id = "gov_news_lk_public_services",
                headline = "Government Information Bulletin: Tri-Forces Rescue Deployment, School Safety & Transport Notice",
                issuingMinistryOrAgency = "Department of Government Information (news.lk) & Ministry of Defence",
                referenceCode = "DGI/PRESS/2026/1026-B",
                category = "Public Services & Schools",
                publishedTimeText = "1 hour ago",
                targetDistricts = allRegions.map { it.name },
                targetCities = listOf(nearestCity, "Colombo Fort & Pettah", "Kandy City", "Galle City", "Ratnapura Town"),
                summary = "The Presidential Secretariat and Ministry of Defence have deployed 42 Sri Lanka Navy Rapid Action Boat Squadron (RABS) teams, Army disaster relief contingents, and Air Force Bell-212/Mi-17 helicopters on standby for flood rescue and relief distribution.",
                keyDirectives = listOf(
                    "Provincial Education Directors have been authorized to close schools in flood-inundated zones of Western, Sabaragamuwa, and Southern provinces.",
                    "Road Development Authority (RDA) advises maximum speed of 60 km/h on Southern (E01), Outer Circular (E02), and Central (E04) Expressways.",
                    "Ceylon Electricity Board (CEB) has activated 1987 emergency breakdown teams to isolate submerged substations safely."
                ),
                officialUrl = "https://www.news.lk",
                officialPortalName = "Govt Official News Portal (news.lk)",
                severity = AlertSeverity.MAJOR_AMBER
            ),
            GovernmentAnnouncement(
                id = "gov_irrigation_portal",
                headline = "Reservoir Spillway & Major Tank Status Report — Island-Wide Irrigation Schemes",
                issuingMinistryOrAgency = "Ministry of Irrigation & Water Resources Management",
                referenceCode = "MIWRM/RES/2026/88",
                category = "DMC Flood Bulletin",
                publishedTimeText = "2 hours ago",
                targetDistricts = listOf("Anuradhapura", "Polonnaruwa", "Kurunegala", "Ampara", "Monaragala", "Hambantota", "Badulla"),
                targetCities = listOf("Anuradhapura City", "Polonnaruwa Town", "Manampitiya", "Kurunegala City", "Chilaw", "Wellawaya", "Kataragama"),
                summary = "Out of 73 major irrigation reservoirs across Sri Lanka, 34 reservoirs are spilling or above 85% storage capacity. Rajanganaya, Deduru Oya, Parakrama Samudraya, Senanayake Samudraya, and Lunugamvehera are closely monitored.",
                keyDirectives = listOf(
                    "Communities using downstream bathing and crossing points below Deduru Oya, Rajanganaya, and Polgolla barrages must heed siren warnings before automated spill gate opening.",
                    "Official hourly hydrological bulletins are accessible on the Department of Irrigation portal."
                ),
                officialUrl = "https://www.irrigation.gov.lk",
                officialPortalName = "Dept of Irrigation (irrigation.gov.lk)",
                severity = AlertSeverity.ADVISORY_YELLOW
            )
        ).map { item ->
            val matchesArea = item.targetDistricts.any { it.equals(selectedRegion.name, ignoreCase = true) } ||
                item.targetCities.any { it.equals(nearestCity, ignoreCase = true) }
            item.copy(isResidingAreaMatch = matchesArea)
        }

        return listOf(residingOfficialDirective) + nationalAnnouncements
    }
}
