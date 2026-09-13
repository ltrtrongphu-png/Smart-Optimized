package com.smartopt.gui;

import com.smartopt.SmartOptimizerPlugin;
import org.bukkit.Material;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Nơi duy nhất định nghĩa toàn bộ module bật/tắt hiện trong GUI.
 *
 * Trước đây mỗi module cần sửa ở 3 nơi khác nhau (OptGuiMenu để vẽ item,
 * OptGuiListener để xử lý click, và SmartOptimizerPlugin để wiring) - riêng
 * phần hiển thị/click đã bị lặp code 9 lần gần giống hệt nhau. Giờ chỉ cần
 * thêm 1 dòng vào {@link #ALL} là GUI tự động vẽ nút + tự động xử lý click +
 * tự động lưu xuống config.yml, không còn chỗ nào khác phải dùng tay sửa.
 *
 * @param slot        vị trí ô trong inventory 45 ô của OptGuiMenu
 * @param material    icon hiển thị
 * @param label       tên hiển thị (chưa màu, GUI sẽ tự gradient theo trạng thái)
 * @param description 1 dòng mô tả ngắn trong lore
 * @param getter      đọc trạng thái bật/tắt hiện tại từ plugin
 * @param setter      ghi trạng thái mới (kèm lưu config.yml nếu cần) - Auto-Optimize
 *                    là ngoại lệ duy nhất KHÔNG lưu xuống config (chỉ là công tắc tạm thời)
 */
public record ToggleSpec(
        int slot,
        Material material,
        String label,
        String description,
        Function<SmartOptimizerPlugin, Boolean> getter,
        BiConsumer<SmartOptimizerPlugin, Boolean> setter
) {
    public boolean isEnabled(SmartOptimizerPlugin plugin) {
        return getter.apply(plugin);
    }

    public void toggle(SmartOptimizerPlugin plugin) {
        setter.accept(plugin, !isEnabled(plugin));
    }
}
