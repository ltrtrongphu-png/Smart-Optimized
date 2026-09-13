package com.smartopt.core;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

public class HistoryFileWriterSimulator {
    static int pass=0, fail=0;
    static void check(String label, boolean cond) {
        if (cond) { pass++; System.out.println("  [OK] " + label); }
        else { fail++; System.out.println("  [FAIL] " + label); }
    }
    public static void main(String[] args) throws Exception {
        File tmpDir = Files.createTempDirectory("sopt-test").toFile();
        HistoryFileWriter w = new HistoryFileWriter(tmpDir, "history.csv", 100, null);

        for (int i = 0; i < 5; i++) {
            w.append(1000L + i, 19.5 - i * 0.1, "NORMAL");
        }
        List<String> lines = Files.readAllLines(w.getFile().toPath());
        check("File được tạo", w.getFile().exists());
        check("Có header + 5 dòng dữ liệu", lines.size() == 6);
        check("Header đúng định dạng", lines.get(0).equals("timestamp,epoch_millis,tps,level"));
        check("Dòng dữ liệu có 4 trường", lines.get(1).split(",").length == 4);

        // Ghi vượt qua max-lines (100) để kích hoạt xoay vòng
        for (int i = 0; i < 200; i++) {
            w.append(2000L + i, 10.0, "SEVERE");
        }
        List<String> after = Files.readAllLines(w.getFile().toPath());
        check("Đã xoay vòng, không phình to vô hạn", after.size() <= 102);
        check("Header vẫn còn sau khi xoay vòng", after.get(0).equals("timestamp,epoch_millis,tps,level"));

        tmpDir.deleteOnExit();
        for (File f : tmpDir.listFiles()) f.deleteOnExit();

        System.out.println("\n===== Kết Quả HistoryFileWriter: " + pass + " PASS / " + fail + " FAIL =====");
        if (fail > 0) System.exit(1);
    }
}
