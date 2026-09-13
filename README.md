# SmartOptimizer — Plugin tự động tối ưu Paper 1.21.4 (SMP + RPG + MythicMobs)

Bản **nâng cấp toàn diện**: thêm module tối ưu sâu hơn, GUI quản lý đầy đủ trong
game, và tích hợp bên ngoài (PlaceholderAPI, Discord webhook, bStats). Vẫn giữ
nguyên triết lý ban đầu: **tự động, an toàn cho RPG, không đụng NMS**.

## v2.2.0 — Armor Stand Limiter + Tái cấu trúc GUI chuyên nghiệp hơn

- **Module mới — Armor Stand Limiter** (mặc định tắt, giới hạn **16
  armor-stand/chunk** theo đúng yêu cầu): khi kích hoạt và server đang lag
  (MODERATE/SEVERE), tự động dọn bớt armor stand dư thừa mỗi chunk. Để giảm
  rủi ro phá trang trí/công trình người chơi, thứ tự ưu tiên xoá là: **không
  tên → có tên nhưng không trang bị → có tên và có trang bị (được bảo vệ sau
  cùng)**. Armor stand có tên/trang bị chỉ bị đụng đến khi đã xoá hết loại
  không tên mà vẫn còn dư.
- **Tái cấu trúc toàn bộ GUI để gọn và chuyên nghiệp hơn**: 10 module bật/tắt
  (thêm Armor Stand Limiter) giờ được định nghĩa **1 lần duy nhất** trong
  `ToggleDefinitions.java` (dùng Java record `ToggleSpec`) — cả việc vẽ nút
  trong `OptGuiMenu` lẫn xử lý click trong `OptGuiListener` đều tự động đọc
  từ danh sách này thay vì lặp lại code cho từng module như trước (từ ~9 khối
  code gần giống hệt nhau ở mỗi file, còn lại 1 vòng lặp mỗi nơi). Thêm 1
  module mới trong tương lai giờ chỉ cần thêm 1 dòng, không phải sửa 2-3 nơi.
- **GUI đẹp hơn**: viền kính gradient dài hơn/mượt hơn (11 điểm màu thay vì
  6), panel trạng thái hiển thị thêm "Module đang bật: X/9" để nhìn thoáng
  qua biết ngay bao nhiêu module đang hoạt động, tách các khối vẽ giao diện
  (`drawStatusPanel`, `drawToggles`, `drawSimulationButtons`,
  `drawUtilityButtons`, `drawBorder`) rõ ràng theo từng khu vực chức năng.
- Cập nhật `config.yml`: thêm mục `armor-stand-limiter` (cap-per-chunk: 16).
- Biên dịch sạch (87 class, gồm cả lớp lambda sinh tự động), chạy lại 45/45 test PASS — không hồi quy.

## v2.1.0 — Nâng cấp toàn diện: Top Chunks, lịch sử bền vững, GUI cập nhật

- **Module mới — Top Chunks Reporter** (`ChunkHealthReporter`, `/sopt topchunks [n]`):
  quét toàn bộ chunk đang tải, xếp hạng theo tổng số entity, phân loại
  mob/item/orb-XP/armor-stand/khối-đang-rơi/khác. **Chỉ đọc, KHÔNG bao giờ
  xoá gì** — an toàn tuyệt đối cho build/trang trí của người chơi, để admin
  tự quyết định hành động. Có nút bấm nhanh ngay trong GUI (`Top Chunk Lag`).
- **Lịch sử TPS bền vững qua restart** (`HistoryFileWriter`): mỗi chu kỳ kiểm
  tra được ghi thêm 1 dòng vào `plugins/SmartOptimizer/history.csv` (không
  còn chỉ nằm trong RAM và mất khi restart như trước), tự động "xoay vòng"
  khi vượt quá số dòng cấu hình (`history-file.max-lines`, mặc định 20000)
  để không phình to vô hạn. Mọi lỗi ghi file chỉ log cảnh báo 1 lần, không
  bao giờ làm gián đoạn chức năng chính.
- Cân nhắc kỹ về an toàn: đã **chủ động không** làm module tự động xoá Armor
  Stand/Item Frame như dự tính ban đầu, vì đây thường là đồ trang trí có chủ
  đích của người chơi — xoá nhầm sẽ phá huỷ công trình. Thay vào đó chọn
  phương án báo cáo an toàn hơn (Top Chunks).
- GUI: thêm nút Top Chunks, cập nhật số phiên bản hiển thị.
- 6 test mới cho `HistoryFileWriter` (tạo file, ghi đúng định dạng CSV, xoay
  vòng đúng, giữ nguyên header) — tổng cộng **45 test PASS / 0 FAIL**.
- Biên dịch lại toàn bộ (83 class) với stub mở rộng thêm `ArmorStand`,
  `getDataFolder()`, `Chunk.getX()/getZ()`, `Player.performCommand()` — sạch,
  0 lỗi, 0 cảnh báo.

## v2.0.1 — Hotfix build Maven thật (CI)

- Sửa lỗi biên dịch thật trên Maven/Paper API: `MobCapEnforcer` dùng sai tên
  interface `org.bukkit.entity.Animal` (không tồn tại) → đổi đúng thành
  `org.bukkit.entity.Animals` (có "s") theo đúng Paper/Spigot API 1.21.4.
  Lỗi này không bị phát hiện lúc kiểm tra bằng bộ stub trong sandbox vì bộ
  stub tự viết cũng vô tình đặt sai tên giống hệt — đã sửa luôn `stub-api/`
  để khớp chữ ký thật, tránh việc kiểm tra tương lai bị che giấu lỗi.
- Biên dịch lại + chạy lại 39/39 test: PASS, không có hồi quy.

## v2.0.0 — Bản nâng cấp tối đa (chữ Hex chuyển sắc + module mới)

- **GUI/chữ đổi mã màu Hex chuyển sắc (gradient)**: toàn bộ tiêu đề GUI, tên
  item, dòng trạng thái trong `/sopt status|diag|history`, và tin nhắn khi đổi
  mức tối ưu giờ dùng `GradientUtil` — sinh đúng định dạng Hex "legacy"
  `§x§R§R§G§G§B§B` mà client Minecraft 1.16+ hiểu trực tiếp, **không cần** thêm
  thư viện Adventure/MiniMessage, không phá vỡ tương thích. Số TPS còn được tô
  màu chuyển dần xanh lá → vàng → cam → đỏ theo đúng mức độ lag thực tế.
- **GUI mở rộng 27 → 45 ô**: viền kính màu gradient bo quanh, thêm 2 nút module
  mới, panel thông tin phiên bản.
- **2 module tối ưu mới:**
  - **Gộp Orb Kinh Nghiệm** (bật mặc định) — gộp các quả cầu EXP đứng gần nhau
    ở farm mob/grinder, giảm entity mà không mất EXP người chơi.
  - **Giới hạn Khối Đang Rơi** (tắt mặc định) — chặn lag vật lý từ tháp
    cát/sỏi/bê tông bột hoặc máy TNT-dup bằng cách giới hạn số khối "đang rơi"
    mỗi chunk khi server đang lag.
- Đã thêm bộ test độc lập cho `GradientUtil` (9 test) vào `AllSimulators`,
  nâng tổng số test logic thuần Java lên **39 PASS / 0 FAIL**.
- Biên dịch lại toàn bộ mã nguồn (27 file mã nguồn) với bộ stub Bukkit/Paper đã mở
  rộng (`ExperienceOrb`, `FallingBlock`, thêm `Material` cho kính màu/icon
  mới) — **sạch, 0 lỗi, 0 cảnh báo**.

## 1. Cơ chế quyết định mức tối ưu

Mỗi 5 giây, plugin đọc TPS trung bình 1 phút và xếp vào 1 trong 4 mức qua
`OptimizationEngine` — hạ cấp ngay khi lag, chỉ phục hồi khi TPS ổn định qua
nhiều lần đo liên tiếp, và phục hồi từng bậc một (chống nhấp nháy cấu hình).

| Mức | TPS | 
|---|---|
| NORMAL | ≥ 19.3 |
| MILD | 17.0 – 19.3 |
| MODERATE | 14.0 – 17.0 |
| SEVERE | < 14.0 |

## 2. Toàn bộ module (10 module)

| Module | Mặc định | Mô tả |
|---|---|---|
| **View/Sim Distance** | Bật | Giảm `World#setViewDistance/setSimulationDistance` theo mức tối ưu |
| **Mob Spawn Limit** | Bật | Giảm giới hạn spawn **thêm** mob (Monster/Animal/Water/Ambient), không đụng mob đang sống |
| **Item Merge** | Bật | Gộp item rơi gần nhau, bỏ qua item có tên/lore/enchant/PDC (đồ RPG) |
| **Hopper Throttle** | Bật | Bỏ qua ngẫu nhiên % lượt chuyển hopper khi lag nặng, chỉ giữa container thật |
| **Redstone Limiter** *(mới)* | Bật | Chặn máy lag redstone (clock đổi tín hiệu quá nhanh tại 1 block) bằng rate-limiter theo cửa sổ thời gian |
| **Mob Cap / chunk** *(mới)* | **Tắt** | Giới hạn số mob vanilla không bảo vệ mỗi chunk — can thiệp mạnh nhất nên mặc định tắt, admin tự bật |
| **Join Ramp** *(mới)* | Bật | Tăng dần view/sim distance của người chơi mới vào trong vài giây, tránh spike TPS lúc đông người join |
| **Gộp Orb Kinh Nghiệm** *(mới v2.0)* | Bật | Gộp XP orb rơi gần nhau ở farm mob/grinder, không mất EXP người chơi |
| **Giới hạn Khối Đang Rơi** *(mới v2.0)* | **Tắt** | Giới hạn số khối cát/sỏi/bê tông bột "đang rơi" mỗi chunk (tháp tự động, TNT-dup) — can thiệp mạnh nên mặc định tắt |
| **Giới hạn Armor Stand** *(mới v2.2)* | **Tắt** | Giới hạn 16 armor stand/chunk khi đang lag; ưu tiên xoá loại không tên trước, bảo vệ loại có tên/trang bị đến cùng |

### Bảo vệ tuyệt đối cho server RPG + MythicMobs

Tất cả module xoá/gộp đều dùng chung 1 bộ quy tắc (`ProtectionRules`):
- **Item**: bỏ qua nếu có tên riêng, lore, enchant, hoặc PDC (kể cả PDC gắn thẳng
  lên thực thể item, không chỉ trên ItemStack).
- **Mob**: bỏ qua nếu có tên riêng, PDC, đã thuần hoá, đang bị dắt dây, đang có
  passenger, **hoặc được phát hiện là mob MythicMobs** (tự động detect qua
  metadata chuẩn, không cần cài đặt thêm gì).
- Plugin tự phát hiện MythicMobs lúc khởi động và báo trong console + `/sopt status`.
- Có thể loại trừ hẳn 1 world khỏi mọi can thiệp bằng `world-exclusions` trong config.

## 3. GUI quản lý (`/sopt gui`)

Mở bảng điều khiển **45 ô**, toàn bộ chữ dùng **màu Hex chuyển sắc (gradient)**:
- Viền kính màu gradient bo quanh (cyan → xanh dương → tím → hồng)
- Ô trạng thái: TPS tô màu chuyển xanh lá → vàng → cam → đỏ theo mức lag thực
  tế, mức hiện tại, auto-optimize bật/tắt
- **9 nút** bật/tắt module riêng lẻ (đổi màu xanh/đỏ theo trạng thái, **lưu
  thẳng vào config.yml** nên không mất khi restart server)
- 4 nút mô phỏng nhanh: MILD / MODERATE / SEVERE / Quay về trạng thái thật
- Nút tải lại config, ô thông tin phiên bản, nút đóng

## 4. Lệnh (`/smartoptimizer`, alias `/sopt`, `/smartopt`)

- `/sopt status` — TPS, mức hiện tại, MythicMobs/PlaceholderAPI có kết nối không, chi tiết từng world
- `/sopt diag` — số entity / item rơi / chunk load mỗi world để tìm nguyên nhân lag
- `/sopt history` — 15 lần kiểm tra TPS gần nhất (bộ nhớ), cộng với lịch sử
  đầy đủ được lưu bền vững ra `plugins/SmartOptimizer/history.csv`
- `/sopt topchunks [n]` — xếp hạng n chunk (mặc định 5) nhiều entity nhất
  server-wide, phân loại mob/item/orb-XP/armor-stand/khối-đang-rơi/khác.
  **Chỉ đọc — không tự xoá gì**, để bạn tự quyết định
- `/sopt toggle` — bật/tắt toàn bộ auto-optimize
- `/sopt reload` — tải lại config.yml
- `/sopt gui` — mở bảng điều khiển trong game
- `/sopt simulate <NORMAL|MILD|MODERATE|SEVERE|RESET>` — mô phỏng 1 mức TPS ngay trên server thật để test trước, gõ `RESET` để quay lại TPS thật bất cứ lúc nào

## 5. Tích hợp bên ngoài

- **PlaceholderAPI** *(mới, soft-depend)* — tự động hook nếu có cài:
  `%smartopt_tps%`, `%smartopt_level%`, `%smartopt_status%`
- **Discord Webhook** *(mới, tắt mặc định)* — báo qua Discord khi vào mức
  SEVERE hoặc khi phục hồi hoàn toàn về NORMAL. Bật trong `config.yml` mục
  `discord:`, dùng `java.net` thuần (không cần thư viện ngoài), chạy async
  không bao giờ làm treo main thread.
- **bStats** *(mới, bật mặc định)* — thống kê ẩn danh số server dùng plugin.
  Cần thay plugin ID thật (đăng ký miễn phí tại bstats.org) trong
  `SmartOptimizerPlugin.setupBStats()` trước khi public plugin.

## 6. Cấu hình (config.yml)

Mặc định chạy hoàn toàn tự động. Cách dễ nhất để chỉnh là `/sopt gui`. Xem file
`config.yml` (chú thích tiếng Việt đầy đủ) cho toàn bộ tuỳ chọn nâng cao: ngưỡng
TPS, redstone-limiter, mob-cap, join-ramp, world-exclusions, discord,
placeholderapi, bstats.

## 7. Đã kiểm thử trước khi giao (đúng yêu cầu ban đầu)

### a) 45 test logic thuần Java (6 bộ mô phỏng độc lập, không cần Bukkit)

```
OptimizationEngine   -> 12 PASS / 0 FAIL   (chuyển mức, chống flap, hồi phục từng bậc, giá trị an toàn)
RateLimiter           -> 5 PASS / 0 FAIL   (chặn redstone-clock nhanh, không chặn nhầm hành động thường)
ProtectionRules        -> 7 PASS / 0 FAIL  (bảo vệ item/mob RPG + MythicMobs, cho phép dọn mob/item thường)
JoinRampCalculator      -> 6 PASS / 0 FAIL (tăng dần đúng tuyến tính, không vượt target)
GradientUtil            -> 9 PASS / 0 FAIL (định dạng Hex legacy đúng, gradient nhiều điểm đúng, màu TPS)
HistoryFileWriter       -> 6 PASS / 0 FAIL (tạo file đúng, định dạng CSV đúng, xoay vòng đúng, giữ header)

TỔNG: 45 PASS / 0 FAIL
```

Chạy lại:
```bash
javac -d out src/main/java/com/smartopt/core/*.java src/main/java/com/smartopt/util/*.java \
    test-sim/com/smartopt/core/*.java test-sim/com/smartopt/util/*.java
java -cp out com.smartopt.core.AllSimulators
```

### b) Biên dịch toàn bộ 27 file mã nguồn của plugin với bộ stub Bukkit/Paper/bStats/PlaceholderAPI

Vì sandbox không có mạng tới repo.papermc.io, mình tự viết bộ stub (thư mục
`stub-api/`) tái tạo đúng chữ ký toàn bộ API Bukkit/Paper/bStats/PlaceholderAPI
được dùng trong plugin (`World`, `LivingEntity`, `BlockRedstoneEvent`,
`InventoryClickEvent`, `Metrics`, `PlaceholderExpansion`, `ExperienceOrb`,
`FallingBlock`, v.v.), rồi biên dịch toàn bộ mã nguồn thật với bộ stub đó.

**Kết quả: biên dịch sạch, 0 lỗi, 0 cảnh báo** (kể cả cảnh báo deprecation đã
được sửa - đổi `new URL(String)` sang `URI.create(...).toURL()` theo chuẩn Java
mới).

> Đây là kiểm tra cú pháp & tính nhất quán của code, không thay thế build Maven
> thật + test trên server Paper 1.21.4 thật. Khi build ở máy bạn: `mvn clean package`
> (cần mạng tới repo.papermc.io + repo.extendedclip.com, bình thường với mọi
> plugin Paper, không liên quan chất lượng code).

## 8. Build

```bash
mvn clean package
```

File jar ở `target/SmartOptimizer-2.0.0.jar` → bỏ vào `plugins/` server Paper 1.21.4.

**Trước khi dùng thật:** đổi plugin ID trong `setupBStats()` (mặc định đang để
`00000` placeholder), và nếu dùng Discord alert thì điền `discord.webhook-url`
trong config.yml.

## 9. Gợi ý nâng cấp thêm nếu cần

- Per-player throttle cho packet (cần thư viện packet như PacketEvents — sẽ phá vỡ nguyên tắc "không NMS/độc lập version" nên chưa đưa vào mặc định)
- Dashboard web xem TPS real-time (cần HTTP server nhúng)
- Đa ngôn ngữ EN/VI cho GUI và tin nhắn (messages.yml theo locale)
- Ngưỡng TPS riêng cho từng world (hiện chỉ có bật/tắt loại trừ world)
