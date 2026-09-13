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
        check("File duoc tao", w.getFile().exists());
        check("Co header + 5 dong du lieu", lines.size() == 6);
        check("Header dung dinh dang", lines.get(0).equals("timestamp,epoch_millis,tps,level"));
        check("Dong du lieu co 4 truong", lines.get(1).split(",").length == 4);

        // Ghi vuot qua max-lines (100) de kich hoat xoay vong
        for (int i = 0; i < 200; i++) {
            w.append(2000L + i, 10.0, "SEVERE");
        }
        List<String> after = Files.readAllLines(w.getFile().toPath());
        check("Da xoay vong, khong phinh to vo han", after.size() <= 102);
        check("Header van con sau khi xoay vong", after.get(0).equals("timestamp,epoch_millis,tps,level"));

        tmpDir.deleteOnExit();
        for (File f : tmpDir.listFiles()) f.deleteOnExit();

        System.out.println("\n===== KET QUA HistoryFileWriter: " + pass + " PASS / " + fail + " FAIL =====");
        if (fail > 0) System.exit(1);
    }
}
