<div align="center">

# ⚔️ GomokuBot - AI Engine & Web Platform

**Engine cờ Caro (Gomoku) hiệu năng cao viết bằng Java 21 & Spring Boot**  
*Tích hợp thuật toán tìm kiếm PVS, Bộ giải chiến thuật VCF/VCT, Nhận diện bẫy không gian, Opening Book và Giao diện Web Responsive hỗ trợ Mobile.*

[![Java](https://img.shields.io/badge/Java-21-orange.svg?style=for-the-badge&logo=openjdk)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.2-brightgreen.svg?style=for-the-badge&logo=springboot)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Build-Maven-C71A36.svg?style=for-the-badge&logo=apache-maven)](https://maven.apache.org/)
[![Tests](https://img.shields.io/badge/Tests-531%20Passed-success.svg?style=for-the-badge&logo=junit5)](https://junit.org/junit5/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg?style=for-the-badge)](LICENSE)

[Tính năng nổi bật](#-tính-năng-nổi-bật) •
[Kiến trúc hệ thống](#-kiến-trúc-hệ-thống) •
[Thuật toán AI](#-thuật-toán-ai-engine) •
[Hướng dẫn cài đặt](#-hướng-dẫn-cài-đặt--chạy-dự-án) •
[REST API](#-tài-liệu-rest-api) •
[Kiểm thử](#-hệ-thống-kiểm-thử-testing)

---

</div>

## 🌟 Giới thiệu

**GomokuBot** là một dự án cờ Caro (Gomoku) thi đấu chuẩn luật quốc tế 15x15. Hệ thống kết hợp giữa các kỹ thuật AI tìm kiếm cây game cổ điển đỉnh cao (Principal Variation Search, Iterative Deepening, Zobrist Hashing, Transposition Tables) cùng các bộ giải chiến thuật nâng cao (**VCF - Victory of Continuous Fours** và **VCT - Victory of Continuous Threes**) có cơ chế chống phản công (Anti-Counterattack Verification) và nhận diện bẫy không gian (Spatial Trap Detection).

Dự án đi kèm một giao diện Web hiện đại (HTML5 Canvas 2D) được tối ưu hóa responsive 100% cho cả Desktop và Mobile/Tablet, hỗ trợ thao tác cảm ứng chạm-rê mượt mà và trực quan.

---

## 🚀 Tính năng nổi bật

### 🤖 AI Engine Siêu Cấp
- **Cơ chế tìm kiếm đa luồng kết hợp:** Negamax với **Principal Variation Search (PVS)**, **Alpha-Beta Pruning** và **Aspiration Windows**.
- **Đào sâu lặp tiến (Iterative Deepening):** Tối ưu hóa quỹ thời gian thực tế, cho phép bot đưa ra quyết định tốt nhất ngay cả khi bị ngắt thời gian đột ngột.
- **Bộ giải VCF/VCT chuyên sâu (Tactical Solver):**
  - **VCF Solver:** Tìm chuỗi nước ép 4 liên tiếp kết liễu trận đấu nhanh chóng trong vòng 10-20 plies.
  - **VCT Solver:** Tấn công bằng chuỗi nước Ba liên tục với cơ chế kiểm tra phản công nghiêm ngặt (ngăn chặn ảo tưởng chiến thắng khi đối thủ đã có sẵn nước 3 mở / 4 mở).
- **Phát hiện bẫy không gian (Spatial Trap Detector):** Sử dụng cửa sổ trượt 9 ô nhận diện toàn diện các thế nhảy cóc hiểm hóc (`oo.o`, `o.oo`, `o.o.o`), bẫy chẽ 3-3, 4-3, 4-4.
- **Sắp xếp nước đi thông minh (Move Ordering):** Ưu tiên nước thắng ngay > Nước chặn thắng > Nước tạo 4 mở > Chặn 4 mở > Nước trong Transposition Table > Killer Moves > Bẫy không gian > Khoảng cách tới tâm.
- **Bảng chuyển vị Zobrist Hashing (Transposition Table):** Tối ưu hóa lưu trữ trạng thái bàn cờ, tái sử dụng độ sâu đã tính với chính sách thay thế tối ưu.
- **Sách khai cuộc (Opening Book):** Tích hợp các thế khai cuộc chuẩn quốc tế (Trực tiếp, Gián tiếp).

### 🎮 Giao diện Web Hiện đại & Trực quan
- **Bàn cờ Goban Canvas 2D:** Hiệu ứng đổ bóng 3D chân thực, hiển thị tọa độ chuẩn A-O và 1-15.
- **Telemetry theo thời gian thực:** Hiển thị thời gian suy nghĩ (ms), độ sâu tìm kiếm (depth), số nút đã duyệt (nodes searched), điểm đánh giá vị thế hoặc trạng thái `Mate in N`.
- **Đa dạng cấp độ Bot:**
  - 🌱 **Dễ (Tập chơi):** Độ sâu 2, thời gian suy nghĩ 300ms, phù hợp cho người mới làm quen.
  - ⚔️ **Trung bình:** Độ sâu 4, thời gian 800ms, nhận biết các đòn cơ bản.
  - 🔥 **Khó (Thi đấu):** Độ sâu 6, thời gian 2s, kích hoạt bộ giải VCF và bẫy 3-3/4-3.
  - 👑 **Kiện tướng (Master):** Độ sâu 8-12+, thời gian 4-8s, kích hoạt toàn bộ VCF + VCT + Spatial Trap Search.
- **Linh hoạt trong trận đấu:**
  - Tùy chọn cầm quân Đen (đi trước) hoặc Trắng (đi sau).
  - Đổi bên / đổi lượt ngay trong ván cờ (`Switch Side`).
  - Đi lại (`Undo Move`).
  - Gợi ý nước đi (`Bot đi hộ`).
  - Âm thanh đặt quân sinh động (Web Audio API).
- **Hỗ trợ Mobile 100%:**
  - Tự động co giãn bàn cờ theo kích thước màn hình điện thoại.
  - Chạm và rê ngón tay để xem trước giao điểm (`Ghost Stone Preview`), thả tay để đặt quân chuẩn xác.

---

## 📐 Kiến trúc hệ thống

```
src/main/java/com/hieupt/gomokubot/
├── api/                  # Giao diện cấp cao cho Engine (AiPlayer, BotLevel, GomokuEngine)
├── book/                 # Sách khai cuộc (OpeningBook)
├── config/               # Cấu hình Bean của ứng dụng (GomokuConfig)
├── core/                 # Cấu trúc dữ liệu lõi (Board, Move, GameState, GameStatus, Player)
├── evaluation/           # Đánh giá thế cờ (Evaluator, PatternRecognizer, Pattern)
├── hash/                 # Băm Zobrist & Bảng chuyển vị (ZobristHash, TranspositionTable)
├── movegen/              # Sinh nước đi ứng viên trong bán kính lân cận (CandidateMoveGenerator)
├── rules/                # Luật chơi Gomoku & kiểm tra thắng thua (WinChecker, MoveValidator)
├── search/               # Thuật toán tìm kiếm (NegamaxSearch, AlphaBeta, MoveOrdering, SearchLimits)
├── tactical/             # Bộ giải chiến thuật (VCFSearch, VCTSearch, ThreatDetector, SpatialTrapDetector)
├── time/                 # Quản lý thời gian suy nghĩ động (TimeManager)
└── web/                  # Spring Boot MVC Web & REST Controllers, DTOs, Services
```

---

## 🧠 Thuật toán AI Engine

### 1. Principal Variation Search (PVS) & Iterative Deepening
Thuật toán tìm kiếm chính dựa trên Negamax PVS:
- Ở mỗi độ sâu, nước đi đầu tiên (thường lấy từ PV hoặc Transposition Table) được tìm kiếm với khung đầy đủ `[alpha, beta]`.
- Các nước đi tiếp theo được thăm dò bằng **Null Window Search** `[-alpha - 1, -alpha]` để chứng minh nhánh đó kém hơn PV một cách nhanh nhất.
- Nếu một nước đi vượt qua alpha (fail-high), thuật toán tiến hành tái tìm kiếm (Re-search) với khung đầy đủ.

### 2. Bộ giải chiến thuật VCF / VCT
- **VCF (Victory of Continuous Fours):** Tìm kiếm cây AND/OR các nước đi tạo chuỗi Bốn. Mỗi nước đi của bên tấn công buộc bên phòng thủ chỉ có duy nhất 1-2 ô để chặn. Nếu bên tấn công tạo được 4 mở (Open Four), chiến thắng là tuyệt đối.
- **VCT (Victory of Continuous Threes):** Tìm kiếm cây tấn công bằng các nước Ba.
  - *Chống ảo tưởng chiến thắng (Anti-Phantom Mate):* Ngay sau khi Attacker đi nước 3, hệ thống lập tức kiểm tra Defender có đòn phản công tạo **Five** hoặc **Open Four** hay không. Nếu có, Attacker không thể duy trì thế chủ động và nhánh VCT bị loại bỏ ngay lập tức.

### 3. Đánh giá vị thế (Evaluation Function)
Sử dụng thuật toán **Cửa sổ trượt 9 ô (9-cell Sliding Window)** quét trên cả 4 hướng (Ngang, Dọc, Chéo chính, Chéo phụ):
- `FIVE`: Thắng cuộc lập tức ($1.000.000$ điểm)
- `OPEN_FOUR` (.XXXX.): Không thể ngăn chặn ($150.000$ điểm)
- `CLOSED_FOUR` / `BROKEN_FOUR` (.XXX.X., #XXXX.): Nước 4 bị chặn 1 đầu ($25.000$ điểm)
- `OPEN_THREE` (.XXX., .XX.X., .X.XX.): Nước 3 mở 2 đầu ($9.000$ điểm)
- `COMBO_BONUS`: Điểm cộng đặc biệt cho đòn chẽ:
  - Bốn - Bốn (Double Four): $+125.000$ điểm
  - Bốn - Ba (Four - Three): $+80.000$ điểm
  - Ba - Ba (Double Three): $+60.000$ điểm

---

## 💻 Hướng dẫn cài đặt & Chạy dự án

### Yêu cầu hệ thống
- **Java:** JDK 21 trở lên (Khuyến nghị Eclipse Temurin hoặc Amazon Corretto 21).
- **Maven:** 3.8+ (hoặc sử dụng wrapper `./mvnw` đi kèm).
- **Trình duyệt:** Chrome, Firefox, Safari, Edge phiên bản mới nhất.

### 1. Clone repository
```bash
git clone https://github.com/hieu080/GomokuBot.git
cd GomokuBot
```

### 2. Biên dịch và kiểm thử
```bash
./mvnw clean test
```

### 3. Khởi chạy ứng dụng
```bash
./mvnw spring-boot:run
```

Sau khi ứng dụng khởi động thành công:
- Truy cập từ máy tính: **`http://localhost:8080`**
- Truy cập từ điện thoại qua mạng Wi-Fi chung: **`http://<IP_MÁY_TÍNH>:8080`**  
  *(Ví dụ: `http://192.168.1.145:8080`)*

---

## 📡 Tài liệu REST API

Backend cung cấp đầy đủ các RESTful API cho phép tích hợp với client ngoài:

| Phương thức | Endpoint | Mô tả | Payload mẫu |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/game/state` | Lấy toàn bộ trạng thái hiện tại của bàn cờ | Không có |
| `POST` | `/api/game/new` | Khởi tạo ván cờ mới | `{"width": 15, "height": 15, "humanPlayer": 1, "moveTimeMs": 2000, "level": "HARD"}` |
| `POST` | `/api/game/move` | Người chơi thực hiện nước đi `(x, y)` | `{"x": 7, "y": 7}` |
| `POST` | `/api/game/ai-move` | Yêu cầu AI tính toán và đi hộ | Không có |
| `POST` | `/api/game/undo` | Thu hồi lại lượt đi trước đó | Không có |
| `POST` | `/api/game/switch-side` | Đổi bên cầm quân giữa người và bot | Không có |

---

## 🧪 Hệ thống kiểm thử (Testing)

Dự án bao gồm bộ kiểm thử tự động toàn diện với **531 unit tests** được viết bằng **JUnit 5** và **Mockito**, bao phủ toàn diện:
- Kiểm tra tính hợp lệ của luật chơi Gomoku và điều kiện thắng/thua.
- Kiểm tra tính toàn vẹn trạng thái bàn cờ (Board immutability & undo safety).
- Kiểm tra bảng băm Zobrist Hashing và va chạm băm (Hash collision resilience).
- Kiểm tra các đòn chiến thuật VCF (1-ply, 3-plies, 5-plies, 7-plies) và VCT.
- Kiểm tra nhận diện mẫu hình bẫy không gian (`SpatialTrapDetectorTest`).
- Kiểm tra tích hợp MockMvc cho toàn bộ Game REST Controller.

Để chạy toàn bộ kiểm thử:
```bash
./mvnw test
```

---

## 👨‍💻 Tác giả

* **HieuPT** - *AI Engineer & Full-stack Developer*
* **GitHub:** [@hieu080](https://github.com/hieu080)
* **Email:** [phantrunghieu020103@gmail.com](mailto:phantrunghieu020103@gmail.com)

---

## 📄 Bản quyền

Dự án được phát hành theo giấy phép [MIT License](LICENSE).

