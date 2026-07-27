# Setup cho tính năng Vẽ đồ thị 2D

## 1. Dependency

`app/build.gradle.kts` cần có `androidx.cardview` (thường đã có sẵn nếu
project dùng Material Components):

```kotlin
dependencies {
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
}
```

## 2. Đăng ký Activity trong AndroidManifest.xml

```xml
<activity
    android:name=".ui.activity.GraphActivity"
    android:exported="false" />
```

Rồi từ màn hình chính, thêm nút/menu để mở `GraphActivity` bằng Intent:

```java
Intent intent = new Intent(MainActivity.this, GraphActivity.class);
startActivity(intent);
```

## 3. Cách hoạt động (tóm tắt để dễ chỉnh sau)

- **Toạ độ màn hình**: `GraphView` giữ 2 biến `originPxX/Y` (pixel ứng với
  gốc toạ độ toán học) và `pxPerUnit` (số pixel / 1 đơn vị, dùng chung cho
  cả 2 trục để không méo hình).
- **Cực trị**: quét đạo hàm số (sai phân trung tâm) trên miền cố định
  `[-100, 100]`, tìm điểm đổi dấu, refine bằng bisection. Nghĩa là nếu hàm
  có cực trị ngoài khoảng ±100, sẽ không được đánh dấu — nếu cần mở rộng,
  sửa `EXTREMA_DOMAIN_MIN/MAX` trong `GraphView.java`, đánh đổi là tính
  toán lâu hơn 1 chút mỗi lần đổi hàm (không ảnh hưởng lúc pan/zoom vì chỉ
  tính 1 lần khi `setFunction()` được gọi).
- **Trace 1 ngón tay / pan-zoom 2 ngón tay**: đây là lựa chọn UX để tránh
  xung đột giữa "kéo để xem giá trị" và "kéo để di chuyển khung nhìn". Nếu
  bạn muốn đổi (vd 1 ngón luôn pan, có nút riêng để bật trace mode), sửa
  logic trong `onTouchEvent()` của `GraphView`.
- **Gián đoạn đồ thị** (như tiệm cận đứng của tan(x)): phát hiện bằng
  ngưỡng "nhảy pixel quá lớn giữa 2 điểm liên tiếp" — cách đơn giản, đủ
  dùng, nhưng có thể vẽ sai với hàm dao động cực nhanh; muốn chuẩn hơn thì
  cần adaptive sampling.

## 4. Có thể mở rộng sau

- Nhiều đồ thị cùng lúc (list các hàm, mỗi hàm 1 màu).
- Lưu lịch sử hàm đã nhập.
- Đánh dấu giao điểm với trục / giữa 2 đồ thị.
