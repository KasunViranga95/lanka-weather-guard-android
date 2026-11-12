"""
App text in English, Sinhala and Tamil, kept in one table so the three languages stay in step.

Run `python3 tools/strings_table.py` after editing to regenerate:
  app/src/main/res/values/strings.xml      (English)
  app/src/main/res/values-si/strings.xml   (Sinhala)
  app/src/main/res/values-ta/strings.xml   (Tamil)
"""
import os

# key: (English, Sinhala, Tamil)
S = {
    # Tabs
    "tab_weather": ("Weather", "කාලගුණය", "வானிலை"),
    "tab_map": ("Map", "සිතියම", "வரைபடம்"),
    "tab_rivers": ("Rivers", "ගංගා", "ஆறுகள்"),
    "tab_safety": ("Safety", "ආරක්ෂාව", "பாதுகாப்பு"),

    # River status and trend
    "flood_status_major": ("Major flooding", "විශාල ගංවතුර", "பெரும் வெள்ளம்"),
    "flood_status_minor": ("Minor flooding", "සුළු ගංවතුර", "சிறு வெள்ளம்"),
    "flood_status_alert": ("Near alert level", "අනතුරු මට්ටම ආසන්නයේ", "எச்சரிக்கை நிலைக்கு அருகில்"),
    "flood_status_normal": ("Normal", "සාමාන්‍යයි", "இயல்பு"),
    "trend_rising_fast": ("Rising fast", "වේගයෙන් ඉහළ යයි", "வேகமாக உயர்கிறது"),
    "trend_rising": ("Rising", "ඉහළ යයි", "உயர்கிறது"),
    "trend_steady": ("Steady", "ස්ථාවරයි", "நிலையானது"),
    "trend_falling": ("Falling", "පහළ බසියි", "குறைகிறது"),
    "range_6h": ("6 hours", "පැය 6", "6 மணி நேரம்"),
    "range_24h": ("24 hours", "පැය 24", "24 மணி நேரம்"),

    # Map layers
    "layer_rain_thunder": ("Rain & thunder", "වැසි සහ අකුණු", "மழை & இடி"),
    "layer_radar": ("Radar", "රේඩාර්", "ரேடார்"),
    "layer_clouds": ("Clouds", "වලාකුළු", "மேகங்கள்"),
    "layer_wind": ("Wind", "සුළඟ", "காற்று"),
    "layer_temperature": ("Temperature", "උෂ්ණත්වය", "வெப்பநிலை"),
    "layer_rain_total": ("Rain total", "මුළු වර්ෂාපතනය", "மொத்த மழை"),

    # Alerts worked out by the app
    "alert_river_major_title": ("Major flooding on %1$s", "%1$s හි විශාල ගංවතුර", "%1$s இல் பெரும் வெள்ளம்"),
    "alert_river_minor_title": ("Flooding on %1$s", "%1$s හි ගංවතුර", "%1$s இல் வெள்ளம்"),
    "alert_river_watch_title": ("%1$s near alert level", "%1$s අනතුරු මට්ටම ආසන්නයේ", "%1$s எச்சரிக்கை நிலைக்கு அருகில்"),
    "alert_river_reading_line": ("%1$s: %2$s m · %3$s · %4$s", "%1$s: මී. %2$s · %3$s · %4$s", "%1$s: %2$s மீ · %3$s · %4$s"),
    "action_river_major_1": (
        "If you live near the river, move to higher ground now.",
        "ඔබ ගඟ අසල ජීවත් වන්නේ නම්, දැන්ම උස් ස්ථානයකට යන්න.",
        "நீங்கள் ஆற்றுக்கு அருகில் வசித்தால், இப்போதே உயரமான இடத்துக்குச் செல்லுங்கள்.",
    ),
    "action_river_major_2": (
        "Do not walk or drive through flood water.",
        "ගංවතුර ජලය හරහා ඇවිදීමෙන් හෝ වාහන පැදවීමෙන් වළකින්න.",
        "வெள்ள நீரில் நடக்கவோ வாகனம் ஓட்டவோ வேண்டாம்.",
    ),
    "action_call_117": (
        "Call 117 (DMC) if you need help.",
        "උදව් අවශ්‍ය නම් 117 (ආපදා කළමනාකරණ මධ්‍යස්ථානය) අමතන්න.",
        "உதவி தேவைப்பட்டால் 117 (அனர்த்த முகாமைத்துவ நிலையம்) ஐ அழையுங்கள்.",
    ),
    "action_river_minor_1": (
        "Move documents, medicine and valuables above floor level.",
        "ලේඛන, බෙහෙත් සහ වටිනා දේ බිම් මට්ටමට ඉහළින් තබන්න.",
        "ஆவணங்கள், மருந்துகள், மதிப்புமிக்க பொருட்களைத் தரைக்கு மேலே வையுங்கள்.",
    ),
    "action_river_minor_2": (
        "Keep away from riverbanks and flooded roads.",
        "ගං ඉවුරු සහ ජලයෙන් යට වූ මාර්ගවලින් ඈත් වන්න.",
        "ஆற்றங்கரைகள் மற்றும் வெள்ளம் சூழ்ந்த சாலைகளிலிருந்து விலகி இருங்கள்.",
    ),
    "action_river_minor_3": (
        "Be ready to leave if the water keeps rising.",
        "ජලය දිගටම ඉහළ යන්නේ නම් පිටත්ව යාමට සූදානම්ව සිටින්න.",
        "நீர் தொடர்ந்து உயர்ந்தால் வெளியேறத் தயாராக இருங்கள்.",
    ),
    "action_river_watch_1": (
        "Keep an eye on the river level in the Rivers tab.",
        "ගංගා පටිත්තෙන් ජල මට්ටම නිරීක්ෂණය කරන්න.",
        "ஆறுகள் பகுதியில் நீர்மட்டத்தைக் கவனித்து வாருங்கள்.",
    ),
    "action_prepare_bag": (
        "Pack a bag with documents, medicine, water and a phone charger.",
        "ලේඛන, බෙහෙත්, ජලය සහ දුරකථන චාජරයක් සහිත බෑගයක් සූදානම් කරන්න.",
        "ஆவணங்கள், மருந்து, தண்ணீர், தொலைபேசி சார்ஜர் அடங்கிய பையைத் தயார் செய்யுங்கள்.",
    ),
    "alert_source_gauges": ("Irrigation Department river gauges", "වාරිමාර්ග දෙපාර්තමේන්තුවේ ජල මිනුම්", "நீர்ப்பாசனத் திணைக்கள ஆற்று அளவீடுகள்"),
    "alert_reading_at": ("Reading at %1$s", "කියවීම %1$s", "அளவீடு %1$s"),
    "alert_forecast_at": ("Forecast at %1$s", "අනාවැකිය %1$s", "முன்னறிவிப்பு %1$s"),
    "alert_source_forecast": ("Open-Meteo forecast", "Open-Meteo අනාවැකිය", "Open-Meteo முன்னறிவிப்பு"),
    "alert_source_nbro": ("Based on NBRO rainfall limits", "NBRO වර්ෂාපතන සීමා මත", "NBRO மழை வரம்புகளின் அடிப்படையில்"),
    "when_next_24h": ("in the next 24 hours", "ඉදිරි පැය 24 තුළ", "அடுத்த 24 மணி நேரத்தில்"),
    "when_tomorrow": ("tomorrow", "හෙට", "நாளை"),
    "alert_rain_heavy_title": ("Heavy rain expected", "අධික වැසි අපේක්ෂිතයි", "கனமழை எதிர்பார்க்கப்படுகிறது"),
    "alert_rain_very_heavy_title": ("Very heavy rain expected", "ඉතා අධික වැසි අපේක්ෂිතයි", "மிகக் கனமழை எதிர்பார்க்கப்படுகிறது"),
    "alert_rain_summary": (
        "About %1$d mm of rain is forecast %2$s in %3$s.",
        "%3$s හි %2$s මි.මී. %1$d ක පමණ වර්ෂාපතනයක් අපේක්ෂිතයි.",
        "%3$s இல் %2$s சுமார் %1$d மி.மீ. மழை எதிர்பார்க்கப்படுகிறது.",
    ),
    "action_rain_1": (
        "Avoid low-lying roads and places that flood easily.",
        "පහත් බිම්වල මාර්ග සහ පහසුවෙන් ජලයෙන් යට වන ස්ථාන මඟහරින්න.",
        "தாழ்வான சாலைகள் மற்றும் எளிதில் வெள்ளம் சூழும் இடங்களைத் தவிருங்கள்.",
    ),
    "action_rain_2": (
        "Keep drains around your home clear.",
        "ඔබේ නිවස අවට කාණු පිරිසිදුව තබා ගන්න.",
        "வீட்டைச் சுற்றியுள்ள வடிகால்களைச் சுத்தமாக வைத்திருங்கள்.",
    ),
    "action_rain_3": (
        "Be ready to move to a safer place if water rises.",
        "ජලය ඉහළ යන්නේ නම් ආරක්ෂිත ස්ථානයකට යාමට සූදානම් වන්න.",
        "நீர் உயர்ந்தால் பாதுகாப்பான இடத்துக்குச் செல்லத் தயாராக இருங்கள்.",
    ),
    "alert_wind_title": ("Strong winds expected", "තද සුළං අපේක්ෂිතයි", "பலத்த காற்று எதிர்பார்க்கப்படுகிறது"),
    "alert_wind_summary": (
        "Gusts up to %1$d km/h are forecast in the next 24 hours.",
        "ඉදිරි පැය 24 තුළ පැයට කි.මී. %1$d දක්වා සුළං හැමීම් අපේක්ෂිතයි.",
        "அடுத்த 24 மணி நேரத்தில் மணிக்கு %1$d கி.மீ. வரை காற்று வீசக்கூடும்.",
    ),
    "action_wind_1": (
        "Tie down loose roof sheets and outdoor items.",
        "ලිහිල් වහල තහඩු සහ එළිමහනේ ඇති දේ තදින් බැඳ තබන්න.",
        "தளர்வான கூரைத் தகடுகளையும் வெளியே உள்ள பொருட்களையும் கட்டி வையுங்கள்.",
    ),
    "action_wind_2": (
        "Stay away from weak trees and power lines.",
        "දුර්වල ගස් සහ විදුලි රැහැන්වලින් ඈත් වන්න.",
        "பலவீனமான மரங்கள், மின் கம்பிகளிலிருந்து விலகி இருங்கள்.",
    ),
    "action_wind_sea": (
        "Fishing and small boats should not go to sea.",
        "ධීවර සහ කුඩා යාත්‍රා මුහුදු යාමෙන් වළකින්න.",
        "மீன்பிடி மற்றும் சிறிய படகுகள் கடலுக்குச் செல்ல வேண்டாம்.",
    ),
    "alert_thunder_title": ("Thunderstorms likely", "ගිගුරුම් සහිත වැසි ඇතිවිය හැක", "இடியுடன் கூடிய மழை பெய்யக்கூடும்"),
    "alert_thunder_summary": (
        "Thunderstorms are likely from around %1$s.",
        "%1$s පමණ සිට ගිගුරුම් සහිත වැසි ඇතිවිය හැක.",
        "சுமார் %1$s முதல் இடியுடன் கூடிய மழை பெய்யக்கூடும்.",
    ),
    "alert_thunder_now": (
        "Thunderstorms are likely now and over the next few hours.",
        "දැන් සහ ඉදිරි පැය කිහිපය තුළ ගිගුරුම් සහිත වැසි ඇතිවිය හැක.",
        "இப்போதும் அடுத்த சில மணி நேரங்களிலும் இடியுடன் கூடிய மழை பெய்யக்கூடும்.",
    ),
    "action_thunder_1": ("Stay indoors while there is lightning.", "අකුණු ඇති විට ගෘහස්ථව සිටින්න.", "மின்னல் இருக்கும்போது வீட்டுக்குள்ளேயே இருங்கள்."),
    "action_thunder_2": ("Unplug electrical appliances.", "විදුලි උපකරණ විසන්ධි කරන්න.", "மின் சாதனங்களின் இணைப்பைத் துண்டியுங்கள்."),
    "action_thunder_3": (
        "Keep away from open fields, tall trees and water.",
        "විවෘත පිට්ටනි, උස් ගස් සහ ජලාශවලින් ඈත් වන්න.",
        "திறந்த வெளிகள், உயரமான மரங்கள், நீர்நிலைகளிலிருந்து விலகி இருங்கள்.",
    ),
    "alert_landslide_title": ("Landslide risk", "නායයෑම් අවදානම", "மண்சரிவு அபாயம்"),
    "alert_landslide_high_title": ("High landslide risk", "ඉහළ නායයෑම් අවදානම", "அதிக மண்சரிவு அபாயம்"),
    "alert_landslide_very_high_title": ("Very high landslide risk", "ඉතා ඉහළ නායයෑම් අවදානම", "மிக அதிக மண்சரிவு அபாயம்"),
    "alert_landslide_summary": (
        "%1$d mm of rain in 24 hours can cause landslides on slopes in %2$s District.",
        "පැය 24 කදී මි.මී. %1$d ක වර්ෂාපතනය %2$s දිස්ත්‍රික්කයේ බෑවුම්වල නායයෑම් ඇති කළ හැක.",
        "24 மணி நேரத்தில் %1$d மி.மீ. மழை %2$s மாவட்டச் சரிவுகளில் மண்சரிவை ஏற்படுத்தலாம்.",
    ),
    "action_landslide_1": (
        "Watch for cracks in walls or ground, tilting trees and muddy springs.",
        "බිත්ති හෝ බිමේ ඉරිතැලීම්, ඇල වන ගස් සහ මඩ සහිත උල්පත් ගැන අවධානයෙන් සිටින්න.",
        "சுவர் அல்லது நிலத்தில் வெடிப்புகள், சாயும் மரங்கள், சேற்று ஊற்றுகளைக் கவனியுங்கள்.",
    ),
    "action_landslide_2": (
        "If you see these signs, leave steep slopes for a safe place.",
        "මෙම ලකුණු දුටුවහොත් බෑවුම් ප්‍රදේශවලින් ආරක්ෂිත ස්ථානයකට යන්න.",
        "இந்த அறிகுறிகளைக் கண்டால் செங்குத்தான சரிவுகளை விட்டுப் பாதுகாப்பான இடத்துக்குச் செல்லுங்கள்.",
    ),
    "action_landslide_3": (
        "Follow NBRO warnings and call 117 for help.",
        "NBRO අනතුරු ඇඟවීම් අනුගමනය කර උදව් සඳහා 117 අමතන්න.",
        "NBRO எச்சரிக்கைகளைப் பின்பற்றி, உதவிக்கு 117 ஐ அழையுங்கள்.",
    ),
    "alert_heat_title": ("Hot weather: take care", "උණුසුම් කාලගුණය: ප්‍රවේශම් වන්න", "வெப்பமான வானிலை: கவனமாக இருங்கள்"),
    "alert_heat_summary": ("It feels like %1$d°C.", "එය %1$d°C ක් ලෙස දැනේ.", "%1$d°C போல் உணரப்படுகிறது."),
    "action_heat_1": ("Drink plenty of water.", "ජලය බහුලව පානය කරන්න.", "நிறையத் தண்ணீர் குடியுங்கள்."),
    "action_heat_2": ("Avoid hard outdoor work around midday.", "මධ්‍යාහ්නයේ එළිමහනේ වෙහෙසකර වැඩ මඟහරින්න.", "நண்பகலில் கடினமான வெளிவேலைகளைத் தவிருங்கள்."),
    "action_heat_3": ("Check on older people and children.", "වැඩිහිටියන් සහ දරුවන් ගැන සොයා බලන්න.", "முதியோர் மற்றும் குழந்தைகளைக் கவனித்துக் கொள்ளுங்கள்."),

    # Notifications
    "notification_channel_name": ("Weather and flood alerts", "කාලගුණ සහ ගංවතුර අනතුරු ඇඟවීම්", "வானிலை மற்றும் வெள்ள எச்சரிக்கைகள்"),
    "notification_channel_description": (
        "New alerts for your saved places: rising rivers, heavy rain, strong wind, thunderstorms and landslide risk.",
        "ඔබ සුරැකි ස්ථාන සඳහා නව අනතුරු ඇඟවීම්: ඉහළ යන ගංගා, අධික වැසි, තද සුළං, ගිගුරුම් සහ නායයෑම් අවදානම.",
        "நீங்கள் சேமித்த இடங்களுக்கான புதிய எச்சரிக்கைகள்: உயரும் ஆறுகள், கனமழை, பலத்த காற்று, இடிமழை, மண்சரிவு அபாயம்.",
    ),

    # Weather conditions
    "cond_clear": ("Clear", "පැහැදිලියි", "தெளிவானது"),
    "cond_mostly_clear": ("Mostly clear", "බොහෝ දුරට පැහැදිලියි", "பெரும்பாலும் தெளிவு"),
    "cond_partly_cloudy": ("Partly cloudy", "අර්ධ වශයෙන් වලාකුළු", "ஓரளவு மேகமூட்டம்"),
    "cond_cloudy": ("Cloudy", "වලාකුළු සහිතයි", "மேகமூட்டம்"),
    "cond_fog": ("Foggy", "මීදුම සහිතයි", "பனிமூட்டம்"),
    "cond_drizzle": ("Drizzle", "පොද වැසි", "தூறல்"),
    "cond_light_rain": ("Light rain", "සුළු වැසි", "லேசான மழை"),
    "cond_rain": ("Rain", "වැසි", "மழை"),
    "cond_heavy_rain": ("Heavy rain", "අධික වැසි", "கனமழை"),
    "cond_light_showers": ("Light showers", "ඉඳහිට සුළු වැසි", "லேசான இடைவிட்ட மழை"),
    "cond_showers": ("Showers", "ඉඳහිට වැසි", "இடைவிட்ட மழை"),
    "cond_heavy_showers": ("Heavy showers", "ඉඳහිට අධික වැසි", "இடைவிட்ட கனமழை"),
    "cond_thunderstorms": ("Thunderstorms", "ගිගුරුම් සහිත වැසි", "இடியுடன் கூடிய மழை"),
    "cond_thunderstorms_hail": ("Thunderstorms with hail", "හිම කැට සහිත ගිගුරුම් වැසි", "ஆலங்கட்டியுடன் இடிமழை"),
    "cond_mixed": ("Mixed conditions", "විවිධ තත්ත්ව", "கலவையான நிலை"),

    # Times, directions, UV, outlook
    "time_am": ("%1$s AM", "පෙ.ව. %1$s", "முற்பகல் %1$s"),
    "time_pm": ("%1$s PM", "ප.ව. %1$s", "பிற்பகல் %1$s"),
    "today": ("Today", "අද", "இன்று"),
    "uv_low": ("Low", "අඩුයි", "குறைவு"),
    "uv_moderate": ("Moderate", "මධ්‍යමයි", "மிதமானது"),
    "uv_high": ("High", "ඉහළයි", "அதிகம்"),
    "uv_very_high": ("Very high", "ඉතා ඉහළයි", "மிக அதிகம்"),
    "uv_extreme": ("Extreme", "අතිශය ඉහළයි", "மிகத் தீவிரம்"),
    "outlook_raining_now": (
        "It's raining now and may continue for a while.",
        "දැන් වැසි වසින අතර ටික වේලාවක් දිගටම පැවතිය හැක.",
        "இப்போது மழை பெய்கிறது, சிறிது நேரம் தொடரலாம்.",
    ),
    "outlook_rain_from": ("Rain likely from around %1$s.", "%1$s පමණ සිට වැසි ඇතිවීමේ ඉඩ ඇත.", "சுமார் %1$s முதல் மழை பெய்ய வாய்ப்புள்ளது."),
    "outlook_dry": ("No rain expected in the next 12 hours.", "ඉදිරි පැය 12 තුළ වැසි අපේක්ෂා නොකෙරේ.", "அடுத்த 12 மணி நேரத்தில் மழை எதிர்பார்க்கப்படவில்லை."),
    "outlook_showers_possible": (
        "A few showers possible today (%1$d%% chance).",
        "අද වැසි කිහිපයක් ඇතිවිය හැක (සම්භාවිතාව %1$d%%).",
        "இன்று சில மழைத்தூறல்கள் சாத்தியம் (%1$d%% வாய்ப்பு).",
    ),

    # Widget
    "widget_name": ("Weather", "කාලගුණය", "வானிலை"),
    "widget_description": (
        "Temperature, rain and your nearest river at a glance",
        "උෂ්ණත්වය, වැසි සහ ළඟම ගංගාව එක බැල්මෙන්",
        "வெப்பநிலை, மழை, அருகிலுள்ள ஆறு ஒரே பார்வையில்",
    ),
    "widget_no_data": ("Open the app to load the weather.", "කාලගුණය ලබා ගැනීමට යෙදුම විවෘත කරන්න.", "வானிலையைப் பெற செயலியைத் திறக்கவும்."),
    "widget_high_low": ("H %1$d° · L %2$d°", "ඉහළ %1$d° · අඩු %2$d°", "அதி %1$d° · குறை %2$d°"),
    "widget_river_line": ("%1$s: %2$s (%3$s m)", "%1$s: %2$s (මී. %3$s)", "%1$s: %2$s (%3$s மீ)"),
    "updated_at": ("Updated %1$s", "යාවත්කාලීන කළේ %1$s", "புதுப்பித்தது %1$s"),

    # Weather screen
    "now": ("Now", "දැන්", "இப்போது"),
    "feels_like": ("Feels like %1$s", "දැනෙන්නේ %1$s", "உணர்வு %1$s"),
    "high_low": ("High %1$s · Low %2$s", "ඉහළම %1$s · අඩුම %2$s", "அதிகபட்சம் %1$s · குறைந்தபட்சம் %2$s"),
    "loading_weather_for": ("Getting the latest weather for %1$s…", "%1$s සඳහා නවතම කාලගුණය ලබා ගනිමින්…", "%1$s இற்கான சமீபத்திய வானிலை பெறப்படுகிறது…"),
    "no_connection_pull": ("No connection. Pull down to try again.", "සම්බන්ධතාවක් නැත. නැවත උත්සාහ කිරීමට පහළට අදින්න.", "இணைப்பு இல்லை. மீண்டும் முயல கீழே இழுக்கவும்."),
    "offline_saved_at": ("Offline · saved at %1$s", "නොබැඳි · සුරැකුවේ %1$s", "இணைப்பில்லை · சேமித்தது %1$s"),
    "n_more": ("+%1$d more", "තවත් %1$d", "மேலும் %1$d"),
    "see_all_alerts": ("See all alerts", "සියලු අනතුරු ඇඟවීම් බලන්න", "அனைத்து எச்சரிக்கைகளையும் பார்க்க"),
    "weather_alerts_count": ("Alerts (%1$d)", "අනතුරු ඇඟවීම් (%1$d)", "எச்சரிக்கைகள் (%1$d)"),
    "hide_details": ("Hide details", "විස්තර සඟවන්න", "விவரங்களை மறை"),
    "show_details": ("Show details", "විස්තර පෙන්වන්න", "விவரங்களைக் காட்டு"),
    "alert_notifications": ("Alert notifications", "අනතුරු ඇඟවීමේ දැනුම්දීම්", "எச்சரிக்கை அறிவிப்புகள்"),
    "alert_notifications_summary": (
        "Notify me about new alerts for my saved places. Checks about every 30 minutes.",
        "මගේ සුරැකි ස්ථාන සඳහා නව අනතුරු ඇඟවීම් ගැන දැනුම් දෙන්න. විනාඩි 30 කට වරක් පමණ පරීක්ෂා කරයි.",
        "சேமித்த இடங்களுக்கான புதிய எச்சரிக்கைகளை அறிவிக்கவும். சுமார் 30 நிமிடங்களுக்கு ஒருமுறை சரிபார்க்கும்.",
    ),
    "alerts_disclaimer": (
        "These alerts are worked out automatically from live river gauges and the weather forecast. Always follow instructions from the DMC and local officials.",
        "මෙම අනතුරු ඇඟවීම් සජීවී ගංගා මිනුම් සහ කාලගුණ අනාවැකි අනුව ස්වයංක්‍රීයව සකස් කෙරේ. සැමවිටම ආපදා කළමනාකරණ මධ්‍යස්ථානයේ සහ ප්‍රාදේශීය නිලධාරීන්ගේ උපදෙස් පිළිපදින්න.",
        "இந்த எச்சரிக்கைகள் நேரடி ஆற்று அளவீடுகள் மற்றும் வானிலை முன்னறிவிப்பிலிருந்து தானாகக் கணிக்கப்படுகின்றன. அனர்த்த முகாமைத்துவ நிலையம் மற்றும் உள்ளூர் அதிகாரிகளின் அறிவுறுத்தல்களை எப்போதும் பின்பற்றுங்கள்.",
    ),
    "severity_severe": ("Severe", "දරුණුයි", "கடுமையானது"),
    "severity_warning": ("Warning", "අනතුරු ඇඟවීම", "எச்சரிக்கை"),
    "severity_advisory": ("Advisory", "උපදේශනය", "அறிவுறுத்தல்"),
    "severity_info": ("Info", "තොරතුරු", "தகவல்"),
    "hourly_forecast": ("Hourly forecast", "පැය අනුව අනාවැකිය", "மணிநேர முன்னறிவிப்பு"),
    "hourly_will_appear": (
        "The hourly forecast will appear once the latest weather loads.",
        "නවතම කාලගුණය ලැබුණු පසු පැය අනුව අනාවැකිය පෙන්වනු ඇත.",
        "சமீபத்திய வானிலை ஏற்றப்பட்டதும் மணிநேர முன்னறிவிப்பு தோன்றும்.",
    ),
    "n_day_forecast": ("%1$d-day forecast", "දින %1$d අනාවැකිය", "%1$d நாள் முன்னறிவிப்பு"),
    "current_conditions": ("Current conditions", "වත්මන් තත්ත්වය", "தற்போதைய நிலை"),
    "wind": ("Wind", "සුළඟ", "காற்று"),
    "speed_kmh": ("%1$d km/h", "%1$d km/h", "%1$d km/h"),
    "wind_from_gusts": ("From %1$s · gusts %2$d", "%1$s දෙසින් · හැමීම් %2$d", "%1$s திசையிலிருந்து · வீச்சு %2$d"),
    "gusts_n": ("Gusts %1$d km/h", "සුළං හැමීම් %1$d km/h", "காற்று வீச்சு %1$d km/h"),
    "humidity": ("Humidity", "ආර්ද්‍රතාව", "ஈரப்பதம்"),
    "humidity_very_humid": ("Very humid", "ඉතා තෙතමනයි", "மிக ஈரப்பதம்"),
    "humidity_humid": ("Humid", "තෙතමනයි", "ஈரப்பதம் அதிகம்"),
    "humidity_comfortable": ("Comfortable", "සුවපහසුයි", "இதமானது"),
    "uv_index": ("UV index", "UV දර්ශකය", "UV குறியீடு"),
    "sunrise": ("Sunrise", "හිරු උදාව", "சூரிய உதயம்"),
    "sunset_at": ("Sunset %1$s", "හිරු බැසීම %1$s", "சூரிய அஸ்தமனம் %1$s"),
    "river_near_you": ("River near you", "ඔබ අසල ගංගාව", "உங்களுக்கு அருகிலுள்ள ஆறு"),
    "all_rivers": ("All rivers", "සියලු ගංගා", "அனைத்து ஆறுகள்"),
    "loading_rivers": ("Loading river levels…", "ගංගා ජල මට්ටම් ලබා ගනිමින්…", "ஆற்று நீர்மட்டங்கள் ஏற்றப்படுகின்றன…"),
    "river_at_station": ("%1$s at %2$s", "%2$s හි %1$s", "%2$s இல் %1$s"),
    "no_recent_reading": ("No recent reading", "මෑත කියවීමක් නැත", "சமீபத்திய அளவீடு இல்லை"),
    "gauge_reads": ("Gauge reads %1$s", "මිනුම පෙන්වන්නේ %1$s", "அளவீடு %1$s"),
    "alert_and_major_levels": (
        "Alert at %1$s m · Major flood at %2$s m",
        "අනතුරු මට්ටම මී. %1$s · විශාල ගංවතුර මී. %2$s",
        "எச்சரிக்கை %1$s மீ · பெரும் வெள்ளம் %2$s மீ",
    ),
    "emergency_call_117": ("Emergency – call DMC 117", "හදිසි අවස්ථා – DMC 117 අමතන්න", "அவசரம் – DMC 117 ஐ அழையுங்கள்"),
    "updated_source": (
        "Updated %1$s · Open-Meteo, Irrigation Department",
        "යාවත්කාලීන කළේ %1$s · Open-Meteo, වාරිමාර්ග දෙපාර්තමේන්තුව",
        "புதுப்பித்தது %1$s · Open-Meteo, நீர்ப்பாசனத் திணைக்களம்",
    ),

    # Rivers screen
    "filter_all": ("All", "සියල්ල", "அனைத்தும்"),
    "filter_flooding": ("Flooding", "ගංවතුර", "வெள்ளம்"),
    "filter_near_alert": ("Near alert", "අනතුරු මට්ටම ආසන්නයේ", "எச்சரிக்கைக்கு அருகில்"),
    "filter_normal": ("Normal", "සාමාන්‍යයි", "இயல்பு"),
    "search_rivers_hint": ("Search river, gauge or town", "ගංගාව, මිනුම හෝ නගරය සොයන්න", "ஆறு அல்லது நகரத்தைத் தேடுங்கள்"),
    "clear_search": ("Clear search", "සෙවීම හිස් කරන්න", "தேடலை அழி"),
    "show_all_rivers": ("Show all rivers", "සියලු ගංගා පෙන්වන්න", "அனைத்து ஆறுகளையும் காட்டு"),
    "rivers_loading_or_offline": (
        "Live gauge readings are loading or unavailable right now.",
        "සජීවී මිනුම් කියවීම් ලැබෙමින් පවතී, නැතහොත් දැනට ලබා ගත නොහැක.",
        "நேரடி அளவீடுகள் ஏற்றப்படுகின்றன அல்லது தற்போது கிடைக்கவில்லை.",
    ),
    "rivers_summary": (
        "%1$d of %2$d gauges reporting · latest reading %3$s · Irrigation Department, HMIS, RiverNet.lk",
        "මිනුම් %2$d න් %1$d ක් වාර්තා කරයි · නවතම කියවීම %3$s · වාරිමාර්ග දෙපාර්තමේන්තුව, HMIS, RiverNet.lk",
        "%2$d அளவீடுகளில் %1$d தரவு அனுப்புகின்றன · சமீபத்திய அளவீடு %3$s · நீர்ப்பாசனத் திணைக்களம், HMIS, RiverNet.lk",
    ),
    "rivers_summary_no_time": (
        "%1$d of %2$d gauges reporting · Irrigation Department, HMIS, RiverNet.lk",
        "මිනුම් %2$d න් %1$d ක් වාර්තා කරයි · වාරිමාර්ග දෙපාර්තමේන්තුව, HMIS, RiverNet.lk",
        "%2$d அளவீடுகளில் %1$d தரவு அனுப்புகின்றன · நீர்ப்பாசனத் திணைக்களம், HMIS, RiverNet.lk",
    ),
    "feed_irrigation": ("Irrigation Department", "වාරිමාර්ග දෙපාර්තමේන්තුව", "நீர்ப்பாசனத் திணைக்களம்"),
    "feed_hmis": (
        "HMIS (hmis.gov.lk), via waterlevels.lk",
        "HMIS (hmis.gov.lk), waterlevels.lk හරහා",
        "HMIS (hmis.gov.lk), waterlevels.lk வழியாக",
    ),
    "feed_rivernet": ("RiverNet.lk, via waterlevels.lk", "RiverNet.lk, waterlevels.lk හරහා", "RiverNet.lk, waterlevels.lk வழியாக"),
    "gauge_source": ("Source: %1$s", "මූලාශ්‍රය: %1$s", "மூலம்: %1$s"),
    "estimated_levels_note": (
        "No official alert levels are published for this gauge. The levels shown were set by the app from its past readings, so use them as a rough guide only.",
        "මෙම මිනුම සඳහා නිල අනතුරු මට්ටම් ප්‍රකාශ කර නැත. පෙන්වන මට්ටම් යෙදුම විසින් එහි පෙර කියවීම් අනුව සකසා ඇති බැවින් ඒවා දළ මඟපෙන්වීමක් ලෙස පමණක් භාවිත කරන්න.",
        "இந்த அளவீட்டுக்கு அதிகாரப்பூர்வ எச்சரிக்கை நிலைகள் வெளியிடப்படவில்லை. காட்டப்படும் நிலைகள் அதன் முந்தைய அளவீடுகளைக் கொண்டு செயலியால் அமைக்கப்பட்டவை; அவற்றைத் தோராயமான வழிகாட்டியாக மட்டும் பயன்படுத்துங்கள்.",
    ),
    "estimated_levels_short": ("estimated levels", "ඇස්තමේන්තුගත මට්ටම්", "மதிப்பிடப்பட்ட நிலைகள்"),
    "no_graph_latest_only": (
        "This source gives the latest reading only, so there is no graph.",
        "මෙම මූලාශ්‍රය නවතම කියවීම පමණක් ලබා දෙන බැවින් ප්‍රස්තාරයක් නැත.",
        "இந்த மூலம் சமீபத்திய அளவீட்டை மட்டுமே தருவதால் வரைபடம் இல்லை.",
    ),
    "change_per_hour_3h": ("%1$s · %2$s m an hour (last 3 hours)", "%1$s · පැයකට මී. %2$s (පසුගිය පැය 3)", "%1$s · மணிக்கு %2$s மீ (கடந்த 3 மணி நேரம்)"),

    # Reservoirs (Rivers tab)
    "view_rivers": ("Rivers", "ගංගා", "ஆறுகள்"),
    "view_reservoirs": ("Reservoirs", "ජලාශ", "நீர்த்தேக்கங்கள்"),
    "search_reservoirs_hint": ("Search reservoir, river or district", "ජලාශය, ගංගාව හෝ දිස්ත්‍රික්කය සොයන්න", "நீர்த்தேக்கம், ஆறு அல்லது மாவட்டத்தைத் தேடுங்கள்"),
    "reservoir_spilling": ("Spilling", "වාන් දමයි", "வான் பாய்கிறது"),
    "reservoir_releasing": ("Releasing water", "ජලය මුදාහරියි", "நீர் திறந்துவிடப்படுகிறது"),
    "reservoir_holding": ("Holding", "ජලය රඳවා ඇත", "நீர் தேக்கப்பட்டுள்ளது"),
    "reservoir_no_data": ("No report today", "අද වාර්තාවක් නැත", "இன்று அறிக்கை இல்லை"),
    "reservoir_filter_out": ("Letting water out", "ජලය පිට කරයි", "நீர் வெளியேற்றப்படுகிறது"),
    "reservoir_nearly_full": ("Nearly full", "පිරීමට ආසන්නයි", "நிரம்பும் நிலையில்"),
    "reservoir_full_pct": ("%1$d%% full", "%1$d%% පිරී ඇත", "%1$d%% நிரம்பியுள்ளது"),
    "reservoir_flow_spill": (
        "%1$s cusecs (%2$s m³/s) over the spillway",
        "වාන් දොරටුවෙන් කියුසෙක් %1$s (m³/s %2$s)",
        "வான் கதவு வழியாக %1$s கியூசெக் (%2$s m³/s)",
    ),
    "reservoir_flow_outlets": (
        "%1$s cusecs (%2$s m³/s) through the gates",
        "දොරටු හරහා කියුසෙක් %1$s (m³/s %2$s)",
        "கதவுகள் வழியாக %1$s கியூசெக் (%2$s m³/s)",
    ),
    "reservoir_flow_downstream": (
        "%1$s cusecs (%2$s m³/s) let out downstream",
        "පහළට කියුසෙක් %1$s (m³/s %2$s) මුදාහරියි",
        "கீழ்நோக்கி %1$s கியூசெக் (%2$s m³/s) திறந்துவிடப்படுகிறது",
    ),
    "reservoir_report": ("Report of %1$s · %2$s", "%1$s වාර්තාව · %2$s", "%1$s அறிக்கை · %2$s"),
    "reservoirs_summary": (
        "%1$d reservoirs · %2$d letting water out · daily reports from the Irrigation Department and Mahaweli Authority, via waterlevels.lk",
        "ජලාශ %1$d · %2$d ක් ජලය පිට කරයි · වාරිමාර්ග දෙපාර්තමේන්තුවේ සහ මහවැලි අධිකාරියේ දෛනික වාර්තා, waterlevels.lk හරහා",
        "%1$d நீர்த்தேக்கங்கள் · %2$d நீரை வெளியேற்றுகின்றன · நீர்ப்பாசனத் திணைக்களம், மகாவலி அதிகாரசபையின் தினசரி அறிக்கைகள், waterlevels.lk வழியாக",
    ),
    "reservoirs_unavailable": (
        "Reservoir reports are loading or unavailable right now.",
        "ජලාශ වාර්තා ලැබෙමින් පවතී, නැතහොත් දැනට ලබා ගත නොහැක.",
        "நீர்த்தேக்க அறிக்கைகள் ஏற்றப்படுகின்றன அல்லது தற்போது கிடைக்கவில்லை.",
    ),
    "no_reservoir_matches": ("No reservoir matches \"%1$s\".", "\"%1$s\" ට ගැළපෙන ජලාශයක් නැත.", "\"%1$s\" உடன் பொருந்தும் நீர்த்தேக்கம் இல்லை."),
    "no_reservoirs_in_category": ("No reservoirs in this category right now.", "දැනට මෙම කාණ්ඩයේ ජලාශ නැත.", "இந்த வகையில் தற்போது நீர்த்தேக்கங்கள் இல்லை."),
    "reservoir_note": (
        "Water let out of a reservoir takes hours to reach towns downstream, so a river can rise even when it is not raining. A cusec is one cubic foot of water a second (about 28 litres). Nearly full means %1$d%% or more.",
        "ජලාශයකින් මුදාහරින ජලය පහළ නගරවලට ළඟා වීමට පැය ගණනක් ගත වන බැවින් වැසි නැති විටත් ගංගාවක් ඉහළ යා හැක. කියුසෙක් එකක් යනු තත්පරයකට ජලය ඝන අඩි එකකි (ලීටර් 28 ක් පමණ). පිරීමට ආසන්නයි යනු %1$d%% හෝ වැඩි බවයි.",
        "நீர்த்தேக்கத்திலிருந்து திறந்துவிடப்படும் நீர் கீழ்நிலை நகரங்களை அடைய மணிநேரங்கள் ஆகும்; எனவே மழை இல்லாவிட்டாலும் ஆறு உயரலாம். ஒரு கியூசெக் என்பது விநாடிக்கு ஒரு கன அடி நீர் (சுமார் 28 லிட்டர்). நிரம்பும் நிலை என்பது %1$d%% அல்லது அதற்கு மேல்.",
    ),
    "no_gauges_in_category": ("No gauges in this category right now.", "දැනට මෙම කාණ්ඩයේ මිනුම් නැත.", "இந்த வகையில் தற்போது அளவீடுகள் இல்லை."),
    "no_gauge_matches": ("No river or gauge matches \"%1$s\".", "\"%1$s\" ට ගැළපෙන ගංගාවක් හෝ මිනුමක් නැත.", "\"%1$s\" உடன் பொருந்தும் ஆறு அல்லது அளவீடு இல்லை."),
    "change_last_hour": ("%1$s · %2$s m in the last hour", "%1$s · පසුගිය පැයේ මී. %2$s", "%1$s · கடந்த மணிநேரத்தில் %2$s மீ"),
    "gauge_in_feet_note": (
        "This gauge is published in feet; it is shown here in metres.",
        "මෙම මිනුම අඩිවලින් ප්‍රකාශ කෙරේ; මෙහි මීටරවලින් පෙන්වයි.",
        "இந்த அளவீடு அடிகளில் வெளியிடப்படுகிறது; இங்கே மீட்டரில் காட்டப்படுகிறது.",
    ),
    "catchment_rain_flow": (
        "Rain in the catchment (24h): %1$d mm · Flow: %2$d m³/s",
        "ජල පෝෂක ප්‍රදේශයේ වැසි (පැය 24): මි.මී. %1$d · ජල ප්‍රවාහය: %2$d m³/s",
        "நீரேந்து பகுதி மழை (24 மணி): %1$d மி.மீ. · நீரோட்டம்: %2$d m³/s",
    ),
    "river_flow_only": ("Flow: %1$d m³/s", "ජල ප්‍රවාහය: %1$d m³/s", "நீரோட்டம்: %1$d m³/s"),
    "areas_may_flood": ("Areas that may flood", "ගංවතුර ඇතිවිය හැකි ප්‍රදේශ", "வெள்ளம் ஏற்படக்கூடிய பகுதிகள்"),
    "areas_may_flood_note": (
        "Towns along the river below this gauge. When the river rises, low-lying land near the banks floods first.",
        "මෙම මිනුමට පහළින් ගංගාව දිගේ පිහිටි නගර. ගංගාව ඉහළ යන විට ඉවුරු අසල පහත් බිම් මුලින්ම යටවේ.",
        "இந்த அளவீட்டுக்குக் கீழே ஆற்றோரம் உள்ள நகரங்கள். ஆறு உயரும்போது கரையோரத் தாழ்நிலங்கள் முதலில் வெள்ளத்தில் மூழ்கும்.",
    ),
    "areas_at_risk_now": (
        "The river is high here. Towns along it below this gauge may flood:",
        "මෙහි ගංගාව ඉහළයි. මෙම මිනුමට පහළින් ගංගාව දිගේ ඇති නගර යටවිය හැක:",
        "இங்கே ஆறு உயர்ந்துள்ளது. இந்த அளவீட்டுக்குக் கீழே ஆற்றோரம் உள்ள நகரங்களில் வெள்ளம் ஏற்படலாம்:",
    ),
    "show_on_map": ("Show on map", "සිතියමේ පෙන්වන්න", "வரைபடத்தில் காட்டு"),
    "more_info": ("More info", "වැඩි විස්තර", "மேலும் தகவல்"),
    "see_river_gauges": ("%1$s gauges", "%1$s මිනුම්", "%1$s அளவீடுகள்"),
    "reservoir_downstream_note": (
        "Water let out of this reservoir flows down %1$s. The gauges on %1$s show the towns along it.",
        "මෙම ජලාශයෙන් මුදාහරින ජලය %1$s දිගේ ගලා යයි. %1$s හි මිනුම් එය දිගේ ඇති නගර පෙන්වයි.",
        "இந்த நீர்த்தேக்கத்திலிருந்து திறந்துவிடப்படும் நீர் %1$s வழியாகப் பாய்கிறது. %1$s இன் அளவீடுகள் அதன் கரையோர நகரங்களைக் காட்டும்.",
    ),
    "reservoir_more_info_note": (
        "More info shows its storage over time and the places downstream (waterlevels.lk).",
        "වැඩි විස්තර තුළ කාලයත් සමඟ ගබඩාව සහ පහළ ප්‍රදේශ පෙන්වයි (waterlevels.lk).",
        "மேலும் தகவலில் காலப்போக்கில் நீர் இருப்பும் கீழ்நிலைப் பகுதிகளும் காட்டப்படும் (waterlevels.lk).",
    ),
    "alert_areas_at_risk": ("Areas at risk: %1$s", "අවදානම් ප්‍රදේශ: %1$s", "ஆபத்தான பகுதிகள்: %1$s"),
    "gauge_read_at": ("Gauge read at %1$s", "මිනුම කියවූයේ %1$s", "அளவீடு எடுக்கப்பட்டது %1$s"),
    "level_alert": ("Alert", "අනතුරු", "எச்சரிக்கை"),
    "level_minor": ("Minor", "සුළු", "சிறு"),
    "level_major": ("Major", "විශාල", "பெரும்"),
    "no_readings_period": ("No readings in this period.", "මෙම කාලය තුළ කියවීම් නැත.", "இந்தக் காலத்தில் அளவீடுகள் இல்லை."),
    "hours_ago": ("%1$dh ago", "පැය %1$d කට පෙර", "%1$d மணி முன்"),

    # Safety screen
    "emergency_numbers": ("Emergency numbers", "හදිසි ඇමතුම් අංක", "அவசர தொலைபேசி எண்கள்"),
    "share": ("Share", "බෙදාගන්න", "பகிர்"),
    "hotline_dmc": ("Disaster Management Centre", "ආපදා කළමනාකරණ මධ්‍යස්ථානය", "அனர்த்த முகாமைத்துவ நிலையம்"),
    "hotline_dmc_role": ("Floods, landslides and evacuation, 24/7", "ගංවතුර, නායයෑම් සහ ඉවත් කිරීම්, පැය 24 පුරා", "வெள்ளம், மண்சரிவு, வெளியேற்றம், 24/7"),
    "hotline_ambulance": ("Suwa Seriya ambulance", "සුව සැරිය ගිලන් රථ", "சுவசெரிய அம்புலன்ஸ்"),
    "hotline_ambulance_role": ("Free ambulance service", "නොමිලේ ගිලන් රථ සේවය", "இலவச அம்புலன்ஸ் சேவை"),
    "hotline_police": ("Police", "පොලිසිය", "பொலிஸ்"),
    "hotline_police_role": ("Police emergency", "පොලිස් හදිසි ඇමතුම", "பொலிஸ் அவசர அழைப்பு"),
    "hotline_fire": ("Fire and rescue", "ගිනි නිවීම සහ ගලවා ගැනීම", "தீயணைப்பு மற்றும் மீட்பு"),
    "hotline_fire_role": ("Fire brigade", "ගිනි නිවීමේ සේවය", "தீயணைப்புச் சேவை"),
    "hotline_nbro": ("NBRO landslide desk", "NBRO නායයෑම් අංශය", "NBRO மண்சரிவுப் பிரிவு"),
    "hotline_nbro_role": ("National Building Research Organisation", "ජාතික ගොඩනැගිලි පර්යේෂණ සංවිධානය", "தேசிய கட்டிட ஆராய்ச்சி நிறுவனம்"),
    "hotline_met": ("Department of Meteorology", "කාලගුණ විද්‍යා දෙපාර්තමේන්තුව", "வளிமண்டலவியல் திணைக்களம்"),
    "hotline_met_role": ("Weather forecasts and warnings", "කාලගුණ අනාවැකි සහ අනතුරු ඇඟවීම්", "வானிலை முன்னறிவிப்புகள் மற்றும் எச்சரிக்கைகள்"),
    "call_number": ("Call %1$s", "%1$s අමතන්න", "%1$s ஐ அழை"),
    "be_prepared": ("Be prepared", "සූදානම්ව සිටින්න", "தயாராக இருங்கள்"),
    "if_you_must_leave": ("If you need to leave home", "ඔබට නිවසින් පිටවීමට සිදු වුවහොත්", "வீட்டை விட்டு வெளியேற வேண்டியிருந்தால்"),
    "if_you_must_leave_sub": ("Where to go and what to take", "යා යුතු තැන සහ රැගෙන යා යුතු දේ", "எங்கே செல்வது, எதை எடுப்பது"),
    "leave_1": (
        "Your Grama Niladhari or Divisional Secretariat announces safe places (relief centres) near you. Call 117 if you don't know where to go.",
        "ඔබ අසල ආරක්ෂිත ස්ථාන (සහන මධ්‍යස්ථාන) ඔබේ ග්‍රාම නිලධාරී හෝ ප්‍රාදේශීය ලේකම් කාර්යාලය මගින් නිවේදනය කෙරේ. යා යුතු තැන නොදන්නේ නම් 117 අමතන්න.",
        "உங்கள் கிராம உத்தியோகத்தர் அல்லது பிரதேச செயலகம் அருகிலுள்ள பாதுகாப்பான இடங்களை (நிவாரண நிலையங்கள்) அறிவிக்கும். எங்கே செல்வது எனத் தெரியாவிட்டால் 117 ஐ அழையுங்கள்.",
    ),
    "leave_2": (
        "Leave early, in daylight if you can, before roads flood.",
        "හැකි නම් මාර්ග ජලයෙන් යට වීමට පෙර, දහවල් කාලයේදීම කලින් පිටත් වන්න.",
        "முடிந்தால் சாலைகள் வெள்ளத்தில் மூழ்குவதற்கு முன், பகலிலேயே முன்கூட்டியே வெளியேறுங்கள்.",
    ),
    "leave_3": (
        "Take documents, medicine, drinking water, a torch and a charged phone.",
        "ලේඛන, බෙහෙත්, පානීය ජලය, විදුලි පන්දමක් සහ ආරෝපණය කළ දුරකථනයක් රැගෙන යන්න.",
        "ஆவணங்கள், மருந்துகள், குடிநீர், டார்ச் லைட், சார்ஜ் செய்த தொலைபேசி ஆகியவற்றை எடுத்துச் செல்லுங்கள்.",
    ),
    "leave_4": (
        "Switch off the main power switch and gas cylinder before you go.",
        "පිටත් වීමට පෙර ප්‍රධාන විදුලි ස්විචය සහ ගෑස් සිලින්ඩරය වසා දමන්න.",
        "புறப்படுவதற்கு முன் பிரதான மின் சுவிட்சையும் எரிவாயு சிலிண்டரையும் அணைத்து விடுங்கள்.",
    ),
    "flood_checklist": ("Flood readiness checklist", "ගංවතුරට සූදානම් වීමේ ලැයිස්තුව", "வெள்ளத் தயார்நிலைப் பட்டியல்"),
    "n_of_m_done": ("%1$d of %2$d done", "%2$d න් %1$d ක් අවසන්", "%2$d இல் %1$d முடிந்தது"),
    "chk_documents": ("Seal ID cards, deeds and bank books", "හැඳුනුම්පත්, ඔප්පු සහ බැංකු පොත් ආරක්ෂිතව අසුරන්න", "அடையாள அட்டைகள், உறுதிகள், வங்கிப் புத்தகங்களைப் பாதுகாப்பாகப் பொதி செய்யுங்கள்"),
    "chk_documents_detail": ("Keep important documents in waterproof bags.", "වැදගත් ලේඛන ජලය නොවදින බෑග්වල තබන්න.", "முக்கிய ஆவணங்களை நீர் புகாத பைகளில் வையுங்கள்."),
    "chk_water": ("Store 3 days of drinking water and dry food", "දින 3 කට ප්‍රමාණවත් පානීය ජලය සහ වියළි ආහාර ගබඩා කරන්න", "3 நாட்களுக்குத் தேவையான குடிநீர், உலர் உணவைச் சேமியுங்கள்"),
    "chk_water_detail": (
        "Floods dirty wells and pipe water; keep 3 litres per person per day.",
        "ගංවතුර ළිං සහ නළ ජලය අපවිත්‍ර කරයි; එක් අයෙකුට දිනකට ලීටර් 3 ක් තබා ගන්න.",
        "வெள்ளம் கிணறு மற்றும் குழாய் நீரை மாசுபடுத்தும்; ஒருவருக்கு நாளொன்றுக்கு 3 லீட்டர் வைத்திருங்கள்.",
    ),
    "chk_electricity": ("Find the main power switch and gas valve", "ප්‍රධාන විදුලි ස්විචය සහ ගෑස් කපාටය සොයා තබා ගන්න", "பிரதான மின் சுவிட்ச், எரிவாயு வால்வைக் கண்டறியுங்கள்"),
    "chk_electricity_detail": (
        "Switch off power and unplug appliances if water comes near the floor.",
        "ජලය බිමට ළං වුවහොත් විදුලිය විසන්ධි කර උපකරණ ගලවන්න.",
        "நீர் தரைக்கு அருகில் வந்தால் மின்சாரத்தை நிறுத்தி சாதனங்களின் இணைப்பைத் துண்டியுங்கள்.",
    ),
    "chk_landslide": ("Check slopes for cracks and muddy springs", "බෑවුම්වල ඉරිතැලීම් සහ මඩ උල්පත් පරීක්ෂා කරන්න", "சரிவுகளில் வெடிப்புகள், சேற்று ஊற்றுகளைச் சரிபாருங்கள்"),
    "chk_landslide_detail": (
        "In hill districts, leave at once if new cracks appear in walls or ground.",
        "කඳුකර දිස්ත්‍රික්කවල, බිත්ති හෝ බිමේ නව ඉරිතැලීම් දිස් වුවහොත් වහාම පිටත් වන්න.",
        "மலைப்பகுதி மாவட்டங்களில், சுவர் அல்லது நிலத்தில் புதிய வெடிப்புகள் தோன்றினால் உடனே வெளியேறுங்கள்.",
    ),
    "chk_phone": ("Charge power banks and save 117 and 1990", "පවර් බැංකු ආරෝපණය කර 117 සහ 1990 සුරකින්න", "பவர் வங்கிகளை சார்ஜ் செய்து 117, 1990 எண்களைச் சேமியுங்கள்"),
    "chk_phone_detail": (
        "Keep your phone charged and turn on battery saver during storms.",
        "කුණාටු කාලයේ දුරකථනය ආරෝපණය කර බැටරි ඉතිරි කිරීම සක්‍රිය කරන්න.",
        "புயலின்போது தொலைபேசியை சார்ஜில் வைத்து பேட்டரி சேமிப்பை இயக்குங்கள்.",
    ),
    "official_sources": ("Official warnings", "නිල අනතුරු ඇඟවීම්", "அதிகாரப்பூர்வ எச்சரிக்கைகள்"),
    "official_sources_intro": (
        "Official warnings are published by these agencies. Tap to open their websites.",
        "නිල අනතුරු ඇඟවීම් මෙම ආයතන මගින් ප්‍රකාශයට පත් කෙරේ. ඔවුන්ගේ වෙබ් අඩවි විවෘත කිරීමට තට්ටු කරන්න.",
        "அதிகாரப்பூர்வ எச்சரிக்கைகளை இந்த நிறுவனங்கள் வெளியிடுகின்றன. அவற்றின் இணையதளங்களைத் திறக்கத் தட்டவும்.",
    ),
    "source_dmc": ("Disaster Management Centre", "ආපදා කළමනාකරණ මධ්‍යස්ථානය", "அனர்த்த முகாமைத்துவ நிலையம்"),
    "source_dmc_what": ("Situation reports and evacuation notices", "තත්ත්ව වාර්තා සහ ඉවත් කිරීමේ නිවේදන", "நிலைமை அறிக்கைகள் மற்றும் வெளியேற்ற அறிவிப்புகள்"),
    "source_met": ("Department of Meteorology", "කාලගුණ විද්‍යා දෙපාර්තමේන්තුව", "வளிமண்டலவியல் திணைக்களம்"),
    "source_met_what": ("Weather warnings and forecasts", "කාලගුණ අනතුරු ඇඟවීම් සහ අනාවැකි", "வானிலை எச்சரிக்கைகள் மற்றும் முன்னறிவிப்புகள்"),
    "source_irrigation": ("Irrigation Department", "වාරිමාර්ග දෙපාර්තමේන්තුව", "நீர்ப்பாசனத் திணைக்களம்"),
    "source_irrigation_what": ("River levels and flood warnings", "ගංගා ජල මට්ටම් සහ ගංවතුර අනතුරු ඇඟවීම්", "ஆற்று நீர்மட்டங்கள் மற்றும் வெள்ள எச்சரிக்கைகள்"),
    "source_nbro": ("National Building Research Organisation", "ජාතික ගොඩනැගිලි පර්යේෂණ සංවිධානය", "தேசிய கட்டிட ஆராய்ச்சி நிறுவனம்"),
    "source_nbro_what": ("Landslide early warnings", "නායයෑම් පූර්ව අනතුරු ඇඟවීම්", "மண்சரிவு முன்னெச்சரிக்கைகள்"),
    "open_website": ("Open website", "වෙබ් අඩවිය විවෘත කරන්න", "இணையதளத்தைத் திற"),

    # Map screen
    "reload_map": ("Reload map", "සිතියම නැවත පූරණය කරන්න", "வரைபடத்தை மீண்டும் ஏற்று"),
    "open_full_map": ("Open the full map", "සම්පූර්ණ සිතියම විවෘත කරන්න", "முழு வரைபடத்தைத் திற"),
    "towns_now": ("Towns now", "නගරවල දැන්", "நகரங்கள் இப்போது"),
    "mm_today": ("%1$d mm today", "අද මි.මී. %1$d", "இன்று %1$d மி.மீ."),

    # Places
    "choose_place": ("Choose a place", "ස්ථානයක් තෝරන්න", "இடத்தைத் தேர்ந்தெடுங்கள்"),
    "close": ("Close", "වසන්න", "மூடு"),
    "location_not_available": (
        "Your location isn't available. Search for your town instead.",
        "ඔබේ ස්ථානය ලබා ගත නොහැක. ඒ වෙනුවට ඔබේ නගරය සොයන්න.",
        "உங்கள் இருப்பிடம் கிடைக்கவில்லை. பதிலாக உங்கள் நகரத்தைத் தேடுங்கள்.",
    ),
    "use_my_location": ("Use my location", "මගේ ස්ථානය භාවිත කරන්න", "எனது இருப்பிடத்தைப் பயன்படுத்து"),
    "search_town_hint": ("Search town or district", "නගරය හෝ දිස්ත්‍රික්කය සොයන්න", "நகரம் அல்லது மாவட்டத்தைத் தேடுங்கள்"),
    "no_town_matches": ("No town matches your search.", "ඔබේ සෙවීමට ගැළපෙන නගරයක් නැත.", "உங்கள் தேடலுக்குப் பொருந்தும் நகரம் இல்லை."),
    "district_name": ("%1$s District", "%1$s දිස්ත්‍රික්කය", "%1$s மாவட்டம்"),
    "district": ("District", "දිස්ත්‍රික්කය", "மாவட்டம்"),
    "town": ("Town", "නගරය", "நகரம்"),
    "show_weather_for": ("Show weather for %1$s", "%1$s සඳහා කාලගුණය පෙන්වන්න", "%1$s இற்கான வானிலையைக் காட்டு"),
    "change_place": ("Change place", "ස්ථානය වෙනස් කරන්න", "இடத்தை மாற்று"),
    "remove_place": ("Remove %1$s", "%1$s ඉවත් කරන්න", "%1$s ஐ நீக்கு"),
    "add_place": ("Add a place", "ස්ථානයක් එක් කරන්න", "இடத்தைச் சேர்"),
    "more_options": ("More options", "තවත් විකල්ප", "மேலும் விருப்பங்கள்"),
    "refresh_now": ("Refresh now", "දැන් යාවත්කාලීන කරන්න", "இப்போது புதுப்பி"),

    # Settings
    "settings": ("Settings", "සැකසීම්", "அமைப்புகள்"),
    "language": ("Language", "භාෂාව", "மொழி"),
    "language_phone": ("Phone setting", "දුරකථන සැකසුම", "தொலைபேசி அமைப்பு"),
    "theme": ("Theme", "තේමාව", "தீம்"),
    "theme_light": ("Light", "ආලෝකමත්", "வெளிர்"),
    "theme_dark": ("Dark", "අඳුරු", "இருண்ட"),
    "about": ("About", "යෙදුම ගැන", "பற்றி"),
    "about_data": (
        "Weather forecasts from Open-Meteo. River levels from the Sri Lanka Irrigation Department, plus HMIS and RiverNet.lk gauges and reservoir reports (Irrigation Department, Mahaweli Authority) through Sri Lanka Water Levels (waterlevels.lk, Shakeeb Sadikeen). Live map by Windy.com.",
        "කාලගුණ අනාවැකි Open-Meteo වෙතින්. ගංගා ජල මට්ටම් ශ්‍රී ලංකා වාරිමාර්ග දෙපාර්තමේන්තුව වෙතින්; HMIS සහ RiverNet.lk මිනුම් සහ ජලාශ වාර්තා (වාරිමාර්ග දෙපාර්තමේන්තුව, මහවැලි අධිකාරිය) Sri Lanka Water Levels (waterlevels.lk, Shakeeb Sadikeen) හරහා. සජීවී සිතියම Windy.com මගින්.",
        "வானிலை முன்னறிவிப்புகள் Open-Meteo இலிருந்து. ஆற்று நீர்மட்டங்கள் இலங்கை நீர்ப்பாசனத் திணைக்களத்திலிருந்து; HMIS, RiverNet.lk அளவீடுகள் மற்றும் நீர்த்தேக்க அறிக்கைகள் (நீர்ப்பாசனத் திணைக்களம், மகாவலி அதிகாரசபை) Sri Lanka Water Levels (waterlevels.lk, Shakeeb Sadikeen) வழியாக. நேரடி வரைபடம் Windy.com வழங்குகிறது.",
    ),
    "privacy_policy": ("Privacy policy", "රහස්‍යතා ප්‍රතිපත්තිය", "தனியுரிமைக் கொள்கை"),
    "version_n": ("Version %1$s", "අනුවාදය %1$s", "பதிப்பு %1$s"),

    # AI helper
    "ai_title": ("Ask about the weather", "කාලගුණය ගැන අසන්න", "வானிலை பற்றிக் கேளுங்கள்"),
    "ai_subtitle": ("AI answers using the live data for %1$s", "%1$s සඳහා සජීවී දත්ත භාවිතයෙන් AI පිළිතුරු දෙයි", "%1$s இற்கான நேரடித் தரவைக் கொண்டு AI பதிலளிக்கும்"),
    "ai_intro": (
        "Ask anything about today's weather, the coming days or the river near you.",
        "අද කාලගුණය, ඉදිරි දින හෝ ඔබ අසල ගංගාව ගැන ඕනෑම දෙයක් අසන්න.",
        "இன்றைய வானிலை, வரும் நாட்கள் அல்லது அருகிலுள்ள ஆறு பற்றி எதையும் கேளுங்கள்.",
    ),
    "ai_failed": (
        "Couldn't reach the AI helper. Check your connection and try again.",
        "AI සහායකයා වෙත ළඟා විය නොහැකි විය. සම්බන්ධතාව පරීක්ෂා කර නැවත උත්සාහ කරන්න.",
        "AI உதவியாளரை அணுக முடியவில்லை. இணைப்பைச் சரிபார்த்து மீண்டும் முயலவும்.",
    ),
    "ai_thinking": ("Thinking…", "සිතමින්…", "யோசிக்கிறது…"),
    "ai_suggest_rain": ("Will it rain today?", "අද වැසි වසීද?", "இன்று மழை பெய்யுமா?"),
    "ai_suggest_river": ("Is the river near me safe?", "මා අසල ගංගාව ආරක්ෂිතද?", "எனக்கு அருகிலுள்ள ஆறு பாதுகாப்பானதா?"),
    "ai_suggest_prepare": ("How do I prepare for a flood?", "ගංවතුරකට සූදානම් වන්නේ කෙසේද?", "வெள்ளத்துக்கு எப்படித் தயாராவது?"),
    "ai_suggest_uv": ("What does the UV index mean?", "UV දර්ශකයෙන් අදහස් කරන්නේ කුමක්ද?", "UV குறியீடு என்றால் என்ன?"),
    "ai_hint": ("Type a question", "ප්‍රශ්නයක් ටයිප් කරන්න", "கேள்வியைத் தட்டச்சு செய்யுங்கள்"),
    "ai_send": ("Send", "යවන්න", "அனுப்பு"),

    # Share
    "share_title": ("Lanka Weather Guard: %1$s, %2$s District", "Lanka Weather Guard: %1$s, %2$s දිස්ත්‍රික්කය", "Lanka Weather Guard: %1$s, %2$s மாவட்டம்"),
    "share_weather": (
        "Weather: %1$s, %2$d°C, rain today %3$d mm, gusts %4$d km/h",
        "කාලගුණය: %1$s, %2$d°C, අද වැසි මි.මී. %3$d, සුළං හැමීම් %4$d km/h",
        "வானிலை: %1$s, %2$d°C, இன்றைய மழை %3$d மி.மீ., காற்று வீச்சு %4$d km/h",
    ),
    "share_river": ("River: %1$s at %2$s, %3$s m (%4$s)", "ගංගාව: %2$s හි %1$s, මී. %3$s (%4$s)", "ஆறு: %2$s இல் %1$s, %3$s மீ (%4$s)"),
    "share_hotlines": (
        "Emergency: DMC 117 · Ambulance 1990 · Police 119",
        "හදිසි: DMC 117 · ගිලන් රථ 1990 · පොලිසිය 119",
        "அவசரம்: DMC 117 · அம்புலன்ஸ் 1990 · பொலிஸ் 119",
    ),
}

PLURALS = {
    "gauges_flooding": (
        {"one": "%1$d river gauge is at flood level across Sri Lanka", "other": "%1$d river gauges are at flood level across Sri Lanka"},
        {"one": "ශ්‍රී ලංකාව පුරා ගංගා මිනුම් %1$d ක් ගංවතුර මට්ටමේ ඇත", "other": "ශ්‍රී ලංකාව පුරා ගංගා මිනුම් %1$d ක් ගංවතුර මට්ටමේ ඇත"},
        {"one": "இலங்கை முழுவதும் %1$d ஆற்று அளவீடு வெள்ள நிலையில் உள்ளது", "other": "இலங்கை முழுவதும் %1$d ஆற்று அளவீடுகள் வெள்ள நிலையில் உள்ளன"},
    ),
}

ARRAYS = {
    "compass_points": (
        ["N", "NE", "E", "SE", "S", "SW", "W", "NW"],
        ["උතුර", "ඊසාන", "නැගෙනහිර", "ගිනිකොන", "දකුණ", "නිරිත", "බටහිර", "වයඹ"],
        ["வடக்கு", "வடகிழக்கு", "கிழக்கு", "தென்கிழக்கு", "தெற்கு", "தென்மேற்கு", "மேற்கு", "வடமேற்கு"],
    ),
}

# Only in English: the app name stays the same in every language
ENGLISH_ONLY = {"app_name": "Lanka Weather Guard"}


def esc(text: str) -> str:
    return (
        text.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\\", "\\\\")
        .replace("'", "\\'")
        .replace('"', '\\"')
    )


def write(folder: str, idx: int) -> None:
    lines = ['<?xml version="1.0" encoding="utf-8"?>', "<!-- Generated by tools/strings_table.py: edit that file, not this one. -->", "<resources>"]
    if idx == 0:
        for key, value in ENGLISH_ONLY.items():
            lines.append(f'    <string name="{key}" translatable="false">{esc(value)}</string>')
    for key, values in S.items():
        lines.append(f'    <string name="{key}">{esc(values[idx])}</string>')
    for key, values in PLURALS.items():
        lines.append(f'    <plurals name="{key}">')
        for qty, text in values[idx].items():
            lines.append(f'        <item quantity="{qty}">{esc(text)}</item>')
        lines.append("    </plurals>")
    for key, values in ARRAYS.items():
        lines.append(f'    <string-array name="{key}">')
        for text in values[idx]:
            lines.append(f"        <item>{esc(text)}</item>")
        lines.append("    </string-array>")
    lines.append("</resources>")
    root = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main", "res", folder)
    os.makedirs(root, exist_ok=True)
    with open(os.path.join(root, "strings.xml"), "w", encoding="utf-8") as fh:
        fh.write("\n".join(lines) + "\n")


if __name__ == "__main__":
    for i, folder in enumerate(["values", "values-si", "values-ta"]):
        write(folder, i)
    print(f"{len(S)} strings, {len(PLURALS)} plurals, {len(ARRAYS)} arrays written for en, si, ta")
