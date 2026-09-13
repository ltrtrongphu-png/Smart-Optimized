package com.smartopt.util;

/**
 * Tiện ích tạo chữ màu Hex chuyển sắc (gradient) cho tên GUI, lore, tin nhắn chat.
 *
 * Dùng đúng định dạng Hex "legacy" mà chính bản thân {@code org.bukkit.ChatColor.of("#RRGGBB")}
 * sinh ra trên Paper/Spigot 1.16+: mỗi ký tự hex được đặt sau 1 dấu §, mở đầu bằng "§x".
 * Client Minecraft 1.16+ hiểu định dạng này trực tiếp trong tên item/lore/chat/tiêu đề
 * inventory — KHÔNG cần thêm thư viện Adventure/MiniMessage nào, không phá vỡ khả năng
 * tương thích, và hoạt động y hệt trên mọi bản Paper 1.16 trở lên (bao gồm 1.21.4).
 *
 * Toàn bộ class thuần Java (không import Bukkit) nên có thể unit-test độc lập.
 */
public final class GradientUtil {

    private static final char COLOR_CHAR = '\u00A7'; // §
    public static final String RESET = COLOR_CHAR + "r";
    public static final String BOLD = COLOR_CHAR + "l";
    public static final String ITALIC = COLOR_CHAR + "o";

    // Bảng màu thương hiệu SmartOptimizer — phối theo tông "modern dashboard" (kiểu Tailwind),
    // ít chói hơn màu neon cũ, nhìn chuyên nghiệp hơn nhưng vẫn nổi bật trên nền tối của GUI.
    public static final String BRAND_A = "#38BDF8"; // xanh da trời (sky)
    public static final String BRAND_B = "#6366F1"; // xanh chàm (indigo)
    public static final String BRAND_C = "#A855F7"; // tím (violet)

    public static final String OK_A = "#34D399";    // xanh ngọc lục bảo (emerald) - an toàn / NORMAL
    public static final String OK_B = "#10B981";

    public static final String WARN_A = "#FBBF24";  // hổ phách (amber) - MILD
    public static final String WARN_B = "#F59E0B";

    public static final String DANGER_A = "#FB923C"; // cam (MODERATE)
    public static final String DANGER_B = "#EF4444"; // đỏ  (SEVERE)

    private GradientUtil() {}

    /** Sinh chuỗi §x§R§R§G§G§B§B cho 1 màu hex "#RRGGBB" hoặc "RRGGBB". */
    public static String hexPrefix(String hex) {
        String h = hex.startsWith("#") ? hex.substring(1) : hex;
        if (h.length() != 6) {
            throw new IllegalArgumentException("Mã màu hex không hợp lệ: " + hex);
        }
        StringBuilder sb = new StringBuilder();
        sb.append(COLOR_CHAR).append('x');
        for (char c : h.toCharArray()) {
            sb.append(COLOR_CHAR).append(c);
        }
        return sb.toString();
    }

    /** Chuyển sắc mượt giữa 2 màu hex, áp cho từng ký tự của {@code text}. */
    public static String gradient(String text, String fromHex, String toHex) {
        return gradient(text, new String[]{fromHex, toHex});
    }

    /** Chuyển sắc mượt giữa 2 màu hex, kèm in đậm. */
    public static String gradientBold(String text, String fromHex, String toHex) {
        return BOLD + gradient(text, new String[]{fromHex, toHex});
    }

    /** Chuyển sắc qua nhiều điểm dừng màu (multi-stop gradient), vd 3 màu thương hiệu. */
    public static String gradient(String text, String... hexStops) {
        if (hexStops == null || hexStops.length == 0) return text;
        if (hexStops.length == 1) return hexPrefix(hexStops[0]) + text;

        int[][] stops = new int[hexStops.length][3];
        for (int i = 0; i < hexStops.length; i++) {
            stops[i] = toRgb(hexStops[i]);
        }

        int len = text.length();
        StringBuilder out = new StringBuilder();
        if (len == 1) {
            out.append(hexPrefix(hexStops[0])).append(text);
            return out.toString();
        }

        for (int i = 0; i < len; i++) {
            char ch = text.charAt(i);
            double t = (double) i / (double) (len - 1); // 0..1
            double scaled = t * (stops.length - 1);
            int segment = Math.min(stops.length - 2, (int) Math.floor(scaled));
            double localT = scaled - segment;

            int[] a = stops[segment];
            int[] b = stops[segment + 1];
            int r = lerp(a[0], b[0], localT);
            int g = lerp(a[1], b[1], localT);
            int bl = lerp(a[2], b[2], localT);

            out.append(hexPrefix(toHex(r, g, bl)));
            if (ch == ' ') {
                // Không tô màu khoảng trắng để tránh lỗi hiển thị trên một số client cũ,
                // nhưng vẫn giữ mã màu trước đó liên tục cho ký tự tiếp theo.
                out.append(' ');
            } else {
                out.append(ch);
            }
        }
        return out.toString();
    }

    /**
     * Trả về 1 màu hex duy nhất nội suy theo tỉ lệ {@code ratio} (0 = khoẻ mạnh, 1 = nguy cấp)
     * trong dải xanh lá -> vàng -> cam -> đỏ. Dùng để tô màu số TPS theo mức độ lag thực tế.
     */
    public static String healthColor(double ratio) {
        double r = Math.max(0, Math.min(1, ratio));
        String[] stops = {OK_A, WARN_A, DANGER_A, DANGER_B};
        double scaled = r * (stops.length - 1);
        int seg = Math.min(stops.length - 2, (int) Math.floor(scaled));
        double localT = scaled - seg;
        int[] a = toRgb(stops[seg]);
        int[] b = toRgb(stops[seg + 1]);
        return toHex(lerp(a[0], b[0], localT), lerp(a[1], b[1], localT), lerp(a[2], b[2], localT));
    }

    /** Tô màu 1 giá trị TPS (0-20) theo nguong da cau hinh, tra ve chuoi da co ma mau san. */
    public static String colorizeTps(double tps, double mildThreshold, double severeThreshold) {
        double ratio;
        if (tps >= mildThreshold) {
            ratio = 0.0;
        } else if (tps <= severeThreshold) {
            ratio = 1.0;
        } else {
            ratio = 1.0 - ((tps - severeThreshold) / (mildThreshold - severeThreshold));
        }
        String hex = healthColor(ratio);
        return hexPrefix(hex) + String.format("%.1f", tps);
    }

    private static int lerp(int a, int b, double t) {
        return (int) Math.round(a + (b - a) * t);
    }

    private static int[] toRgb(String hex) {
        String h = hex.startsWith("#") ? hex.substring(1) : hex;
        return new int[]{
                Integer.parseInt(h.substring(0, 2), 16),
                Integer.parseInt(h.substring(2, 4), 16),
                Integer.parseInt(h.substring(4, 6), 16)
        };
    }

    private static String toHex(int r, int g, int b) {
        r = clamp(r); g = clamp(g); b = clamp(b);
        return String.format("#%02X%02X%02X", r, g, b);
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
