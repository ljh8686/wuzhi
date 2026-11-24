package com.wuzhi.core;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

/**
 * 免费天气（Open-Meteo，无需 API Key）。
 *
 * 1) 有城市名 → 用 Open-Meteo Geocoding 查经纬度 → 查当前天气；
 * 2) 没有城市名 → 用 ipapi.co 按出口 IP 猜城市（免 Key），再查天气。
 *
 * 全部是普通 HTTPS，不申请定位权限，也不会读用户位置。
 */
public final class Weather {

    public static final class Now {
        public String city = "";
        public int temp = Cloth.NO_TEMP;
        public int tmin = Cloth.NO_TEMP;
        public int tmax = Cloth.NO_TEMP;
        public int code = -1;
        public long at = 0;

        public String text() { return describe(code); }

        public String emoji() { return emojiOf(code); }

        /** 例如 "8~15°C"；缺失时返回空串。 */
        public String range() {
            if (tmin == Cloth.NO_TEMP || tmax == Cloth.NO_TEMP) return "";
            return tmin + "~" + tmax + "°C";
        }

        public String line() {
            StringBuilder sb = new StringBuilder();
            sb.append(text());
            if (temp != Cloth.NO_TEMP) sb.append(' ').append(temp).append("°C");
            if (range().length() > 0) sb.append(" · 今日 ").append(range());
            return sb.toString();
        }
    }

    private Weather() {}

    /** 拉当前天气；city 为空时按 IP 猜城市。失败抛异常，调用方自己 toast。 */
    public static Now load(String city) throws Exception {
        String c = city == null ? "" : city.trim();
        if (c.length() == 0) return byIp();
        double[] ll = geocode(c);
        if (ll == null) throw new Exception("没找到「" + c + "」，换个城市名试试");
        return forecast(ll[0], ll[1], c);
    }

    // ------------------------------------------------------------ 具体接口

    private static double[] geocode(String name) throws Exception {
        String u = "https://geocoding-api.open-meteo.com/v1/search?count=1&language=zh&format=json&name="
                + URLEncoder.encode(name, "UTF-8");
        Map<String, Object> root = Json.obj(Json.parse(get(u)));
        List<Object> rs = Json.arr(root.get("results"));
        if (rs == null || rs.isEmpty()) return null;
        Map<String, Object> r = Json.obj(rs.get(0));
        double lat = Json.dbl(r, "latitude", 0);
        double lon = Json.dbl(r, "longitude", 0);
        if (lat == 0 && lon == 0) return null;
        return new double[]{lat, lon};
    }

    private static Now byIp() throws Exception {
        double lat = 0, lon = 0;
        String city = "";
        try {
            Map<String, Object> root = Json.obj(Json.parse(get("https://ipinfo.io/json")));
            city = Json.str(root, "city", "");
            String loc = Json.str(root, "loc", "");
            if (loc.contains(",")) {
                String[] p = loc.split(",");
                lat = Double.parseDouble(p[0].trim());
                lon = Double.parseDouble(p[1].trim());
            }
        } catch (Exception ignored) {
        }
        if (lat == 0 && lon == 0) {
            Map<String, Object> root = Json.obj(Json.parse(get("https://ipwho.is/")));
            lat = Json.dbl(root, "latitude", 0);
            lon = Json.dbl(root, "longitude", 0);
            city = Json.str(root, "city", "");
        }
        if (lat == 0 && lon == 0) throw new Exception("没拿到位置，先去设置里填个城市");
        // 用英文城市名反查一次中文名；查不到就沿用原名
        String zh = city;
        try {
            if (city.length() > 0) {
                double[] ll = geocode(city);
                if (ll != null) { lat = ll[0]; lon = ll[1]; zh = geoName(city); }
            }
        } catch (Exception ignored) {
        }
        Now n = forecast(lat, lon, zh);
        return n;
    }

    private static String geoName(String name) throws Exception {
        String u = "https://geocoding-api.open-meteo.com/v1/search?count=1&language=zh&format=json&name="
                + URLEncoder.encode(name, "UTF-8");
        Map<String, Object> root = Json.obj(Json.parse(get(u)));
        List<Object> rs = Json.arr(root.get("results"));
        if (rs.isEmpty()) return name;
        return Json.str(Json.obj(rs.get(0)), "name", name);
    }

    private static Now forecast(double lat, double lon, String city) throws Exception {
        String u = "https://api.open-meteo.com/v1/forecast?forecast_days=1&timezone=auto"
                + "&current=temperature_2m,weather_code"
                + "&daily=temperature_2m_max,temperature_2m_min"
                + "&latitude=" + trim(lat) + "&longitude=" + trim(lon);
        Map<String, Object> root = Json.obj(Json.parse(get(u)));
        Map<String, Object> cur = Json.obj(root.get("current"));
        Map<String, Object> daily = Json.obj(root.get("daily"));

        Now n = new Now();
        n.city = city == null ? "" : city;
        n.temp = (int) Math.round(Json.dbl(cur, "temperature_2m", Cloth.NO_TEMP));
        n.code = Json.integer(cur, "weather_code", -1);
        n.at = System.currentTimeMillis() / 1000L;
        List<Object> mx = Json.arr(daily.get("temperature_2m_max"));
        List<Object> mn = Json.arr(daily.get("temperature_2m_min"));
        if (mx != null && !mx.isEmpty()) n.tmax = (int) Math.round(num(mx.get(0)));
        if (mn != null && !mn.isEmpty()) n.tmin = (int) Math.round(num(mn.get(0)));
        return n;
    }

    private static double num(Object o) {
        return o instanceof Number ? ((Number) o).doubleValue() : 0;
    }

    private static String trim(double d) {
        return String.format(java.util.Locale.US, "%.4f", d);
    }

    private static String get(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(8000);
        c.setReadTimeout(10000);
        c.setRequestProperty("User-Agent", "wuzhi-android/1.3 (+https://github.com/ljh8686/wuzhi)");
        c.setRequestProperty("Accept", "application/json");
        try {
            int code = c.getResponseCode();
            InputStream in = code >= 400 ? c.getErrorStream() : c.getInputStream();
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
            in.close();
            String body = new String(bo.toByteArray(), "UTF-8");
            if (code >= 400) throw new Exception("天气服务返回 " + code);
            return body;
        } finally {
            c.disconnect();
        }
    }

    // ------------------------------------------------------------ 天气代码

    public static String describe(int code) {
        switch (code) {
            case 0: return "晴";
            case 1: return "晴间多云";
            case 2: return "多云";
            case 3: return "阴";
            case 45: case 48: return "有雾";
            case 51: case 53: case 55: return "毛毛雨";
            case 56: case 57: return "冻雨";
            case 61: return "小雨";
            case 63: return "中雨";
            case 65: return "大雨";
            case 66: case 67: return "冻雨";
            case 71: return "小雪";
            case 73: return "中雪";
            case 75: return "大雪";
            case 77: return "米雪";
            case 80: case 81: return "阵雨";
            case 82: return "暴雨";
            case 85: case 86: return "阵雪";
            case 95: return "雷阵雨";
            case 96: case 99: return "雷暴冰雹";
            default: return "天气未知";
        }
    }

    public static String emojiOf(int code) {
        switch (code) {
            case 0: case 1: return "☀️";
            case 2: return "⛅";
            case 3: return "☁️";
            case 45: case 48: return "🌫";
            case 51: case 53: case 55: case 56: case 57: case 61: case 63: case 65: case 66: case 67:
            case 80: case 81: case 82: return "🌧";
            case 71: case 73: case 75: case 77: case 85: case 86: return "❄️";
            case 95: case 96: case 99: return "⛈";
            default: return "🌤";
        }
    }
}
