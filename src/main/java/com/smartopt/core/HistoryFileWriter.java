package com.smartopt.core;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Ghi lịch sử TPS ra file CSV (plugins/SmartOptimizer/history.csv) để dữ liệu
 * KHÔNG mất khi server restart (khác với bộ nhớ RAM chỉ giữ 60 điểm gần nhất).
 *
 * Tự động "xoay vòng" (rotate): khi file vượt quá số dòng tối đa cấu hình,
 * chỉ giữ lại nửa sau (các dòng mới nhất), tránh file phình to vô hạn.
 * Toàn bộ thao tác file đều bắt lỗi và chỉ log cảnh báo - KHÔNG BAO GIỜ
 * làm crash hay làm gián đoạn chức năng chính của plugin nếu ghi file thất bại
 * (vd: đĩa đầy, không có quyền ghi).
 */
public class HistoryFileWriter {

    private static final SimpleDateFormat TS_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final String HEADER = "timestamp,epoch_millis,tps,level";

    private final File file;
    private final int maxLines;
    private final Logger logger;
    private boolean warnedOnce = false;

    public HistoryFileWriter(File dataFolder, String fileName, int maxLines, Logger logger) {
        this.file = new File(dataFolder, fileName);
        this.maxLines = Math.max(100, maxLines);
        this.logger = logger;
    }

    /** Ghi thêm 1 dòng lịch sử, tự động tạo file+header nếu chưa tồn tại. */
    public synchronized void append(long epochMillis, double tps, String level) {
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                Files.createDirectories(parent.toPath());
            }
            boolean isNew = !file.exists();
            try (FileWriter fw = new FileWriter(file, true)) {
                if (isNew) {
                    fw.write(HEADER);
                    fw.write(System.lineSeparator());
                }
                fw.write(TS_FORMAT.format(new Date(epochMillis)));
                fw.write(",");
                fw.write(String.valueOf(epochMillis));
                fw.write(",");
                fw.write(String.format("%.2f", tps));
                fw.write(",");
                fw.write(level);
                fw.write(System.lineSeparator());
            }
            rotateIfNeeded();
        } catch (IOException ex) {
            warnOnce("Không thể ghi history.csv: " + ex.getMessage());
        }
    }

    private void rotateIfNeeded() {
        try {
            Path path = file.toPath();
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            if (lines.size() <= maxLines) return;

            int keepFrom = lines.size() - (maxLines / 2);
            List<String> kept = lines.subList(0, 1); // giữ header (dòng 0)
            List<String> tail = lines.subList(Math.max(1, keepFrom), lines.size());

            StringBuilder sb = new StringBuilder();
            sb.append(kept.get(0)).append(System.lineSeparator());
            for (String line : tail) {
                sb.append(line).append(System.lineSeparator());
            }
            Files.write(path, sb.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException ex) {
            warnOnce("Không thể xoay vòng history.csv: " + ex.getMessage());
        }
    }

    private void warnOnce(String message) {
        if (logger == null) return;
        if (!warnedOnce) {
            logger.log(Level.WARNING, "[SmartOptimizer] " + message
                    + " (cảnh báo này chỉ hiện 1 lần, chức năng chính không bị ảnh hưởng)");
            warnedOnce = true;
        }
    }

    public File getFile() {
        return file;
    }
}
