# My Calculator – Android App

---

## 1. Tổng quan

**My Calculator** là ứng dụng máy tính Android hỗ trợ các phép tính từ cơ bản đến nâng cao. Ứng dụng được thiết kế cho hai chế độ hiển thị:

- **Portrait (dọc):** Bàn phím cơ bản với các số, bốn phép tính số học, dấu ngoặc và phần trăm.
- **Landscape (ngang):** Bàn phím khoa học mở rộng, bổ sung bảng hàm lượng giác (`sin`, `cos`, `tan`, và các hàm ngược), logarithm (`log`, `ln`), căn bậc hai (`√`), mũ (`xʸ`), giai thừa (`x!`), trị tuyệt đối (`|x|`), căn bậc ba (`∛`), hằng số `π` và `e`.

Các tính năng nổi bật:
- **DEG / RAD** — chuyển đổi đơn vị góc, lưu trạng thái qua `SharedPreferences`.
- **Dark / Light mode** — chuyển đổi giao diện tối/sáng, lưu trạng thái qua `SharedPreferences`.
- **Lịch sử tính toán** — lưu tối đa 50 phép tính gần nhất; có thể nhấn vào để tải lại biểu thức.
- **Live Preview** — hiển thị kết quả tạm ngay khi gõ (không cần nhấn `=`).
- **Con trỏ di chuyển** — người dùng chạm để đặt con trỏ bất kỳ đâu trong biểu thức và chỉnh sửa tại chỗ.

---

## 2. Công nghệ sử dụng

| Thành phần | Chi tiết |
|---|---|
| Ngôn ngữ | Java |
| UI Framework | `AppCompatActivity`, `MaterialComponents` |
| Layout chính | `LinearLayout` (tất cả màn hình) |
| Theme | `Theme.MyCalculator` kế thừa `Theme.MaterialComponents.DayNight.NoActionBar` |
| Dark Mode | `AppCompatDelegate.setDefaultNightMode(...)` |
| Lưu trữ cài đặt | `SharedPreferences` |
| Lưu trữ lịch sử | `SharedPreferences` (JSON array) |
| Danh sách lịch sử | `RecyclerView` + `RecyclerView.Adapter` |
| Trả dữ liệu giữa Activity | `ActivityResultLauncher` + `Intent` |

---

## 3. Cấu trúc package và công dụng

```
com.duyanhnguyen.myapplication/
├── ui/
│   ├── activity/     ← Các Activity Android (chỉ nhận sự kiện, ủy quyền cho controller)
│   └── adapter/      ← RecyclerView Adapter kết nối dữ liệu với danh sách UI
├── controller/       ← Điều phối, xử lý logic điều khiển, cầu nối giữa UI và core
│   └── helper/       ← Utility class thuần Java hỗ trợ controller (ánh xạ phím, guard logic nhập)
├── core/             ← Lõi tính toán toán học; hoàn toàn độc lập với Android
├── data/             ← Lớp truy cập dữ liệu: đọc/ghi SharedPreferences
└── model/            ← POJO thuần, định nghĩa cấu trúc dữ liệu, không có logic
```

---

## 4. Cấu trúc thư mục đầy đủ

```
app/src/main/
├── AndroidManifest.xml
├── java/com/duyanhnguyen/myapplication/
│   ├── ui/
│   │   ├── activity/
│   │   │   ├── MainActivity.java
│   │   │   └── HistoryActivity.java
│   │   └── adapter/
│   │       └── HistoryAdapter.java
│   ├── controller/
│   │   ├── MainActivityController.java
│   │   ├── MainCalculatorController.java
│   │   ├── MainUiShellController.java
│   │   └── helper/
│   │       ├── KeyMappingContext.java
│   │       └── CalculatorInputManager.java
│   ├── core/
│   │   ├── ExpressionConverter.java
│   │   ├── ExpressionEvaluator.java
│   │   └── ExpressionValidator.java
│   ├── data/
│   │   └── HistoryManager.java
│   └── model/
│       └── HistoryItem.java
└── res/
    ├── layout/               ← Layout Portrait
    │   ├── activity_main.xml
    │   ├── activity_history.xml
    │   └── item_history.xml
    ├── layout-land/          ← Layout Landscape (tự động chọn khi xoay ngang)
    │   └── activity_main.xml
    ├── drawable/             ← Drawable nút bấm (pill-shape)
    ├── values/               ← colors.xml, strings.xml, styles.xml, themes.xml
    ├── values-land/          ← Ghi đè giá trị resource cho Landscape
    └── values-night/         ← Ghi đè màu sắc cho Dark Mode
```

---

## 5. Công dụng chi tiết từng file Java

### `ui/activity/MainActivity.java`
Entry point duy nhất của ứng dụng. Kế thừa `AppCompatActivity`, **không chứa bất kỳ logic nào**. Toàn bộ logic ủy quyền cho `MainActivityController`. Ba việc nó làm:
1. Khởi tạo `MainActivityController` trong `onCreate`.
2. Chuyển tiếp `onSaveInstanceState` xuống controller để lưu trạng thái khi xoay màn hình.
3. Nhận sự kiện click nút (`android:onClick="onButtonClick"` trong XML) rồi chuyển xuống controller.

### `ui/activity/HistoryActivity.java`
Hiển thị danh sách lịch sử trong `RecyclerView`. Khi người dùng chọn một mục lịch sử, Activity đặt kết quả vào `Intent` (với key `EXTRA_SELECTED_EXPRESSION`) rồi gọi `setResult(RESULT_OK)` và `finish()` — `MainActivity` sẽ nhận lại biểu thức này qua `ActivityResultLauncher`. Có nút "Xóa tất cả" gọi `HistoryManager.clear()` và nút "Quay lại" gọi `finish()`.

### `ui/adapter/HistoryAdapter.java`
`RecyclerView.Adapter` kết nối danh sách `HistoryItem` với các view item trong danh sách. Mỗi item hiển thị: biểu thức (xám, nhỏ), kết quả (trắng, đậm, lớn), thời gian định dạng `HH:mm dd/MM/yyyy`. Dùng `ViewHolder pattern` để tái sử dụng view, tránh `findViewById` lặp lại.

### `controller/MainActivityController.java`
Lớp duy nhất `MainActivity` biết đến. Tạo và giữ hai controller con: `MainUiShellController` và `MainCalculatorController`. Trong `onButtonClick`, gọi cả hai controller; đặc biệt khi nút `btn_deg_rad` được nhấn, gọi thêm `calculatorController.onDegRadChanged()` để tính lại kết quả ngay lập tức.

### `controller/MainUiShellController.java`
Quản lý ba chức năng shell không liên quan đến tính toán:
- **DEG/RAD:** Toggle trạng thái lưu trong `SharedPreferences` (mặc định: DEG). Cập nhật label nút `btn_deg_rad`. Cung cấp `isDegMode()` cho `MainCalculatorController`.
- **Dark/Light Mode:** Toggle dùng `AppCompatDelegate.setDefaultNightMode(...)`. Lưu trạng thái vào `SharedPreferences`. Đổi icon nút `btn_theme`.
- **Xoay màn hình:** Đọc `Configuration.orientation` hiện tại, gọi `setRequestedOrientation(...)` để yêu cầu xoay Portrait ↔ Landscape.

### `controller/MainCalculatorController.java`
Controller trung tâm, xử lý toàn bộ logic tương tác giữa bàn phím và engine tính toán. **Quản lý UI** bằng cách giữ trực tiếp tham chiếu:
- `EditText expressionDisplay` — điều khiển bằng `getText()`, `setText()`, `setSelection()` để thao tác nội dung và con trỏ; `setTextSize()` để điều chỉnh cỡ chữ động.
- `TextView resultDisplay` — `setText()`, `setTextColor()` (xám = live preview, cùng màu chính = kết quả chốt, đỏ = lỗi), `setTextSize()` để đổi cỡ glữa typing mode và result mode.
- **Long-press delete:** `Handler` + `Runnable` lặp mỗi 80ms.
- **TextWatcher:** `afterTextChanged` tự động gọi `refreshPreview()` sau mỗi thay đổi biểu thức.
- **Bundle:** `onSaveInstanceState` lưu biểu thức, vị trí con trỏ, kết quả, trạng thái `isResultShown` để khôi phục khi xoay màn hình.
- **ActivityResultLauncher:** Khởi chạy `HistoryActivity` và xử lý biểu thức trả về.

### `controller/helper/KeyMappingContext.java`
Dữ liệu tĩnh thuần Java, không phụ thuộc Android runtime:
- `portraitKeyMap` (`HashMap<Integer, String>`): ánh xạ Resource ID nút → chuỗi token (vd: `R.id.btn_sin → "sin("`).
- `FUNCTION_TOKENS` (`String[]`): danh sách chuỗi hàm để `CalculatorInputManager` biết cần xóa bao nhiêu ký tự khi backspace.
- `getKeyValue(id)`: tra portrait map trước, nếu không có thì tra landscape map.
- `isBinaryOperator(String)`: phân biệt toán tử hai ngôi để áp dụng guard.

### `controller/helper/CalculatorInputManager.java`
Thuần Java logic, không biết Android View là gì:
- `getLeadingZeroToRemove`: Phát hiện số `0` độc lập đứng ngay trước con trỏ. Trả về `1` nếu cần xóa để tránh tạo ra `05`, `07`,...
- `shouldAddImplicitMultiply`: Kiểm tra có cần chèn `×` tự động không — sau `)`, `!`, `π`, `e`, hoặc sau chữ số nếu nhập hàm/hằng số.
- `hasInvalidLeadingZero`: Quét toàn chuỗi để phát hiện `0` dư ở đầu sau khi chèn giữa chừng.
- `currentNumberHasDecimal`: Kiểm tra số tại vị trí con trỏ đã có dấu `.` hay chưa — ngăn gõ hai dấu thập phân.
- `getFunctionDeleteLength`: Khi backspace, kiểm tra ký tự trước con trỏ có là cuối cụm hàm không (vd: `sin(`); nếu có, trả về độ dài cả cụm để xóa hết thay vì xóa từng ký tự.

### `core/ExpressionConverter.java`
Bước 1 và 2 trong pipeline tính toán:

**Tokenize:** Quét chuỗi ký tự → nhóm số liên tiếp (`"123.45"`), nhóm chữ liên tiếp thành tên hàm (`"sin"`), map ký tự đặc biệt (`√→"sqrt"`, `×→"*"`, `÷→"/"`, `−→"-"`). Sau đó `markUnaryMinus` đổi dấu `-` đứng đầu/sau `(`/toán tử thành token `~`.

**Infix → Postfix (Shunting-Yard):** Dùng `Stack<String>` làm operator stack. Precedence: `+`,`-`=1, `*`,`/`=2, `~`=3, `^`=4. `^` và `~` right-associative. `!` và `%` là postfix unary, đẩy thẳng vào output.

### `core/ExpressionEvaluator.java`
Bước 3: Đánh giá Postfix bằng `Stack<Double>`. Duyệt token: số/hằng số → push; `~` → đảo dấu; `!` → giai thừa (0–170); `%` → chia 100; hàm → `Math.*`; toán tử hai ngôi → pop 2, tính, push. Hàm lượng giác nhận `degreeMode`: DEG thì `Math.toRadians()` trước khi tính, hàm ngược thì `Math.toDegrees()` sau khi tính. Ném `EvalException` nếu stack cuối ≠ 1 phần tử hoặc kết quả `NaN`/`Infinity`.

### `core/ExpressionValidator.java`
Được gọi **trước** `ExpressionEvaluator`. Kiểm tra theo thứ tự:
1. Tokenize: gọi `ExpressionConverter.tokenize()`.
2. Duyệt cặp token: toán tử thiếu số hạng, ngoặc rỗng `()`, hàm không có `(` sau, biểu thức kết thúc bằng toán tử/hàm chưa hoàn chỉnh.
Trả `Result { valid, message }`.

### `data/HistoryManager.java`
- Lưu trữ trong `SharedPreferences` file `"calculator_history_prefs"`, key `"history_json"` dạng JSON array.
- `addEntry`: Thêm mục mới vào đầu danh sách, cắt bỏ nếu vượt 50 mục, lưu lại.
- `getAll`: Parse JSON → `List<HistoryItem>`. Mỗi object: `"expr"`, `"result"`, `"time"`.
- `clear`: Xóa key khỏi SharedPreferences.

### `model/HistoryItem.java`
POJO immutable: `String expression`, `String result`, `long timestamp`.

---

## 6. Thành phần Android được sử dụng — Giải thích chi tiết

### Activity & Lifecycle

**`AppCompatActivity`**
> *Là gì:* Base class cho mọi màn hình trong Android. Quản lý vòng đời (lifecycle): `onCreate → onStart → onResume → onPause → onStop → onDestroy`. Hỗ trợ ActionBar và tương thích ngược với các API cũ.
>
> *Dùng trong app:* `MainActivity` và `HistoryActivity` đều kế thừa `AppCompatActivity`. `onCreate` là nơi khởi tạo controller và inflate layout. `onSaveInstanceState` lưu trạng thái biểu thức để không bị mất khi xoay màn hình.

---

### View & Layout

**`LinearLayout`**
> *Là gì:* Layout sắp xếp các con theo một hướng duy nhất (ngang hoặc dọc). Thuộc tính `layout_weight` cho phép chia tỷ lệ không gian còn lại.
>
> *Dùng trong app:*
> - Root layout của cả `activity_main.xml` (portrait và land): `orientation="vertical"`, chia thành ba vùng bằng `layout_weight` (vùng hiển thị / thanh công cụ / bàn phím).
> - Mỗi hàng bàn phím là một `LinearLayout` ngang với 4 (hoặc 3) `Button` con, mỗi nút có `layout_weight="1"` để chia đều chiều ngang.
> - Landscape chia khu vực bàn phím thành hai cột: cột trái (scientific, weight=3) và cột phải (số, weight=4).
> - Root layout của `item_history.xml`: `orientation="vertical"`, chứa ba `TextView` xếp chồng.

**`RelativeLayout`**
> *Là gì:* Layout định vị các view con tương đối với nhau hoặc với parent bằng thuộc tính như `layout_alignParentStart`, `layout_alignParentEnd`, `layout_centerInParent`... Khác với `LinearLayout`, các view con được đo và đặt vị trí **độc lập** với nhau.
>
> *Dùng trong app:* Header row của `activity_history.xml`. Nút back neo `alignParentStart`, nút xóa neo `alignParentEnd`, tiêu đề dùng `layout_centerInParent="true"` — căn giữa thực sự so với toàn bộ màn hình, không phụ thuộc kích thước hai nút hai bên.

**`EditText`**
> *Là gì:* View nhập liệu, kế thừa `TextView`. Có con trỏ (cursor), hỗ trợ chọn text và `Editable` để thao tác nội dung từ code.
>
> *Dùng trong app:* `text_expression` — hiển thị biểu thức đang nhập. Cấu hình đặc biệt:
> - `android:inputType="text"` + `setShowSoftInputOnFocus(false)`: Ngăn bàn phím ảo của hệ thống bật lên khi focus (vì app dùng bàn phím nút bấm riêng).
> - `android:focusableInTouchMode="true"` + `requestFocus()`: Đảm bảo con trỏ nhấp nháy ngay khi mở app.
> - `android:background="@null"`: Ẩn đường gạch chân mặc định của `EditText`.
> - `setSelection(pos)`: Đặt vị trí con trỏ sau mỗi thao tác nhập/xóa.
> - `getText().insert(cursor, str)` / `getText().delete(start, end)`: Thao tác `Editable` trực tiếp tại vị trí con trỏ.
> - Portrait: `maxLines=3`, `textSize=36sp`. Landscape: `maxLines=2`, `textSize=28sp`.

**`TextView`**
> *Là gì:* View hiển thị văn bản, chỉ đọc.
>
> *Dùng trong app:*
> - `text_result`: Hiển thị kết quả live preview (màu xám) hoặc kết quả chốt. `ellipsize="start"` cắt bớt đầu chuỗi nếu quá dài (thay vì cắt đuôi).
> - Portrait: `textSize=24sp`. Landscape: `textSize=18sp`.
> - `setTextColor(color)`: Đổi màu động — xám = preview, trắng/đen = kết quả, đỏ = lỗi.
> - `setTextSize(TypedValue.COMPLEX_UNIT_SP, size)`: Đổi cỡ chữ động giữa typing mode và result mode.
> - Tiêu đề "Lịch sử", `text_empty_history` (hiện khi danh sách rỗng), các `TextView` trong `item_history.xml`.

**`Button`**
> *Là gì:* View phản hồi click, hiển thị text. Kế thừa `TextView`.
>
> *Dùng trong app:* Tất cả các nút bàn phím (số, toán tử, hàm, C, =) và nút `btn_deg_rad`. Gắn sự kiện bằng `android:onClick="onButtonClick"` trong XML (Android tự gọi phương thức `onButtonClick(View v)` của Activity khi click). Mỗi loại nút dùng một style khác nhau từ `styles.xml` (`CalcButton.Number`, `.Operator`, `.Function`, `.Equals`, `.Clear`).

**`ImageButton`**
> *Là gì:* Giống `Button` nhưng hiển thị hình ảnh (drawable) thay vì text.
>
> *Dùng trong app:* Thanh công cụ: `btn_theme` (icon dark/light mode), `btn_history` (icon đồng hồ lịch sử), `btn_rotate` (icon xoay màn hình), `btn_delete` (icon backspace). Dùng `android:src` để đặt icon và `android:scaleType="centerInside"` để icon không bị méo trong vùng kích thước cố định.

**`View`** (trống)
> *Là gì:* View cơ sở, không hiển thị nội dung. Dùng như spacer hoặc divider.
>
> *Dùng trong app:* Đường kẻ ngang mỏng 1dp giữa vùng hiển thị và bàn phím (`background="@color/colorDivider"`). Trong thanh công cụ, một `View` với `layout_weight="1"` hoạt động như spacer đẩy nút Backspace sang cạnh phải.

---

### RecyclerView & Adapter

**`RecyclerView`**
> *Là gì:* Danh sách cuộn hiệu năng cao. Tái sử dụng view item (recycle) thay vì tạo mới, chỉ render các item thực sự visible. Linh hoạt hơn `ListView` truyền thống. Cần kết hợp với `LayoutManager` và `Adapter`.
>
> *Dùng trong app:* `recycler_history` trong `activity_history.xml`. `layout_width=0dp` + `layout_height=0dp` + constraint bốn phía = lấp đầy vùng còn lại của `ConstraintLayout`. `clipToPadding="false"` + `padding="8dp"` để item cuối không bị che bởi padding.

**`LinearLayoutManager`**
> *Là gì:* `LayoutManager` sắp xếp item theo danh sách dọc (mặc định) hoặc ngang. Cần thiết để `RecyclerView` biết cách sắp xếp các item.
>
> *Dùng trong app:* `recyclerView.setLayoutManager(new LinearLayoutManager(this))` — sắp xếp lịch sử theo danh sách dọc, item mới nhất ở trên cùng.

**`RecyclerView.Adapter` + `ViewHolder`**
> *Là gì:* Adapter là cầu nối giữa dữ liệu và RecyclerView. `ViewHolder` là đối tượng giữ tham chiếu đến các view con của một item (tránh gọi `findViewById` liên tục khi scroll). Adapter tái sử dụng các `ViewHolder` cũ thay vì tạo mới.
>
> *Dùng trong app:* `HistoryAdapter` bind `HistoryItem` vào `item_history.xml`. `onCreateViewHolder` inflate XML; `onBindViewHolder` gán text và click listener. `ViewHolder` giữ ba `TextView`: `expressionText`, `resultText`, `timeText`.

---

### State & Navigation

**`Bundle`**
> *Là gì:* Container key-value để truyền dữ liệu đơn giản giữa các thành phần Android hoặc lưu trạng thái trước khi Activity bị hủy (vd: khi xoay màn hình).
>
> *Dùng trong app:* `onSaveInstanceState(Bundle outState)` lưu 4 key: `key_expression` (chuỗi biểu thức hiện tại), `key_cursor_pos` (vị trí con trỏ), `key_result_shown` (đang ở trạng thái hiển thị kết quả hay không), `key_result_text` (chuỗi kết quả). `onCreate(Bundle savedInstanceState)` khôi phục lại các giá trị này.

**`SharedPreferences`**
> *Là gì:* Cơ chế lưu trữ key-value đơn giản, lâu dài (persist) trên bộ nhớ thiết bị. Phù hợp cho cài đặt (settings) và dữ liệu nhỏ.
>
> *Dùng trong app:*
> - File `"theme_prefs"` (dùng bởi `MainUiShellController`): lưu `is_dark_mode` (Boolean) và `is_deg_mode` (Boolean).
> - File `"calculator_history_prefs"` (dùng bởi `HistoryManager`): lưu `history_json` (String — JSON array của lịch sử tính toán).

**`ActivityResultLauncher`**
> *Là gì:* API hiện đại (thay thế `startActivityForResult` cũ) để khởi chạy một Activity và nhận kết quả trả về một cách type-safe, với callback rõ ràng.
>
> *Dùng trong app:* `MainCalculatorController` đăng ký launcher với contract `StartActivityForResult`. Khi nhấn `btn_history`, launcher khởi chạy `HistoryActivity`. Khi người dùng chọn một mục, `HistoryActivity` trả về `Intent` chứa `EXTRA_SELECTED_EXPRESSION`. Callback trong `MainCalculatorController` nhận Intent, lấy chuỗi biểu thức, điền vào `EditText` và tính live preview.

**`Intent`**
> *Là gì:* Đối tượng thông điệp dùng để khởi chạy Activity khác hoặc truyền dữ liệu giữa các Activity.
>
> *Dùng trong app:* `historyLauncher.launch(new Intent(activity, HistoryActivity.class))` để mở màn hình lịch sử. `HistoryActivity` tạo `new Intent()` với `putExtra(EXTRA_SELECTED_EXPRESSION, expression)` rồi `setResult(RESULT_OK, data)` để trả kết quả về.

---

### Cơ chế sự kiện

**`android:onClick` (XML attribute)**
> *Là gì:* Khai báo trong XML để gắn click listener cho một View. Android sẽ tìm và gọi phương thức có tên tương ứng trong Activity đang chứa View đó.
>
> *Dùng trong app:* Tất cả nút bấm (`Button`, `ImageButton`) đều có `android:onClick="onButtonClick"`. Khi nhấn, Android gọi `MainActivity.onButtonClick(View v)`, nơi đây chuyển tiếp xuống controller theo ID của nút.

**`TextWatcher`**
> *Là gì:* Interface callback được gọi mỗi khi nội dung của `EditText` thay đổi. Có ba phương thức: `beforeTextChanged`, `onTextChanged`, `afterTextChanged`.
>
> *Dùng trong app:* `MainCalculatorController` đăng ký `TextWatcher` vào `expressionDisplay`. Trong `afterTextChanged`, nếu `isResultShown == false`, gọi `refreshPreview()` để cập nhật live preview. Điều này đảm bảo preview luôn đồng bộ với biểu thức, kể cả khi người dùng di chuyển con trỏ và chỉnh sửa giữa chừng.

**`View.OnLongClickListener`**
> *Là gì:* Callback được gọi khi người dùng nhấn và giữ một View (thay vì tap nhanh).
>
> *Dùng trong app:* `btn_delete` có `OnLongClickListener` khởi động cơ chế xóa liên tiếp. `btn_pi` có `OnLongClickListener` để chèn hằng số `e` (Euler's number) như một phím tắt.

**`Handler` + `Runnable`**
> *Là gì:* `Handler` là cơ chế lên lịch chạy code trên UI thread sau một khoảng delay, hoặc lặp lại định kỳ. `Runnable` là đơn vị công việc cần thực thi.
>
> *Dùng trong app:* Khi giữ `btn_delete`, `OnLongClickListener` tạo một `Runnable deleteRunnable` tự lên lịch lại cho chính nó bằng `deleteHandler.postDelayed(this, 80)` sau mỗi lần chạy. Khi ngón tay nhả ra (`ACTION_UP`/`ACTION_CANCEL`), `deleteHandler.removeCallbacks(deleteRunnable)` hủy các lần chạy tiếp theo.

---

### Theming & Style

**`AppCompatDelegate`**
> *Là gì:* Class hỗ trợ áp dụng Night Mode (Dark/Light) cho toàn bộ ứng dụng.
>
> *Dùng trong app:* `AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_YES / MODE_NIGHT_NO)` trong `MainUiShellController`. Khi chế độ thay đổi, Android tự động reload Activity với theme mới, đọc màu từ `values/colors.xml` (Light) hoặc `values-night/colors.xml` (Dark).

**`DayNight` Theme + `values-night/`**
> *Là gì:* Cơ chế của Android cho phép định nghĩa hai bộ resource riêng biệt cho Light và Dark mode. Khi Night Mode được bật, hệ thống tự động ưu tiên đọc resource từ thư mục `values-night/` thay vì `values/`.
>
> *Dùng trong app:* `Theme.MyCalculator` kế thừa `Theme.MaterialComponents.DayNight.NoActionBar`. `values/colors.xml` định nghĩa màu cho Light mode (nền `#F2F2F7`, chữ đen `#000000`). `values-night/colors.xml` ghi đè bằng màu Dark mode. Hệ thống tự động chọn đúng file mà không cần code điều kiện.

**`style` trong `styles.xml`**
> *Là gì:* Tập hợp các thuộc tính giao diện được đặt tên, có thể áp dụng cho nhiều View để đảm bảo nhất quán và tránh lặp code.
>
> *Dùng trong app:* `CalcButtonBase` định nghĩa các thuộc tính chung: `layout_weight=1`, `layout_height=match_parent`, `layout_margin=5dp`, `textAllCaps=false`, `stateListAnimator=@null` (tắt animation nhấn mặc định), `elevation=0dp`, `insetTop/Bottom=0dp`. Năm style kế thừa và ghi đè màu + cỡ chữ riêng: `CalcButton.Number` (24sp), `.Operator` (26sp), `.Function` (17sp), `.Clear` (20sp), `.Equals` (28sp).

**Drawable `<ripple>` + `<shape>`**
> *Là gì:* `<ripple>` tạo hiệu ứng gợn sóng khi nhấn nút. `<shape>` định nghĩa hình dạng nền. `corners radius` tạo bo góc.
>
> *Dùng trong app:* Mỗi loại nút có một file drawable riêng (`bg_btn_number.xml`, `bg_btn_operator.xml`, v.v.). Tất cả dùng `<shape android:shape="rectangle">` với `<corners android:radius="100dp">` — tạo dạng **pill shape**. Khi nút gần vuông (Portrait), trông như hình tròn; khi nút dẹp ngang (Landscape), vẫn bo góc đẹp thay vì bị méo như `oval`.

**`?attr/selectableItemBackgroundBorderless`**
> *Là gì:* Attribute tham chiếu đến drawable ripple hình tròn (không có background nền), lấy từ theme hiện tại.
>
> *Dùng trong app:* Tất cả `ImageButton` trên thanh công cụ và nút `btn_deg_rad` dùng attribute này làm `android:background` — cho phép hiệu ứng ripple khi nhấn mà không có hình nền vuông/tròn rõ ràng.

---

## 7. Cấu trúc Layout chi tiết

### `activity_main.xml` (Portrait)
Root: `LinearLayout` dọc.

```
LinearLayout (vertical, root)
│
├── LinearLayout (vertical, weight=3) — Vùng hiển thị
│   ├── EditText   id=text_expression  (maxLines=3, textSize=36sp, gravity=end)
│   └── TextView   id=text_result      (maxLines=1, ellipsize=start, textSize=24sp)
│
├── LinearLayout (horizontal) — Thanh công cụ
│   ├── Button        id=btn_deg_rad
│   ├── ImageButton   id=btn_theme
│   ├── ImageButton   id=btn_history
│   ├── ImageButton   id=btn_rotate
│   ├── View          (spacer, weight=1)
│   └── ImageButton   id=btn_delete
│
├── View (1dp divider)
│
└── LinearLayout (vertical, weight=5) — Bàn phím 5×4
    ├── LinearLayout (horizontal, weight=1) — Hàng 1: C  (  )  ÷
    ├── LinearLayout (horizontal, weight=1) — Hàng 2: 7  8  9  ×
    ├── LinearLayout (horizontal, weight=1) — Hàng 3: 4  5  6  −
    ├── LinearLayout (horizontal, weight=1) — Hàng 4: 1  2  3  +
    └── LinearLayout (horizontal, weight=1) — Hàng 5: %  0  .  =
```

### `activity_main.xml` (Landscape — `layout-land/`)
Root: `LinearLayout` dọc. Bàn phím chia làm hai cột:

```
LinearLayout (vertical, root)
│
├── LinearLayout (vertical, weight=2) — Vùng hiển thị
│   ├── EditText   (maxLines=2, textSize=28sp)
│   └── TextView   (maxLines=1, textSize=18sp)
│
├── LinearLayout (horizontal) — Thanh công cụ (giống Portrait)
│
├── View (1dp divider)
│
└── LinearLayout (horizontal, weight=5) — Khu bàn phím
    │
    ├── LinearLayout (vertical, weight=3) — Cột trái: Hàm khoa học, 5 hàng × 3 cột
    │   ├── LinearLayout (horizontal, weight=1): sin    cos    tan
    │   ├── LinearLayout (horizontal, weight=1): log    ln     √
    │   ├── LinearLayout (horizontal, weight=1): xʸ     x!     π
    │   ├── LinearLayout (horizontal, weight=1): e      |x|    ∛
    │   └── LinearLayout (horizontal, weight=1): sin⁻¹  cos⁻¹  tan⁻¹
    │
    └── LinearLayout (vertical, weight=4) — Cột phải: Số, 5 hàng × 4 cột
        ├── LinearLayout (horizontal, weight=1): C   (   )   ÷
        ├── LinearLayout (horizontal, weight=1): 7   8   9   ×
        ├── LinearLayout (horizontal, weight=1): 4   5   6   −
        ├── LinearLayout (horizontal, weight=1): 1   2   3   +
        └── LinearLayout (horizontal, weight=1): %   0   .   =
```

### `activity_history.xml`
Root: `LinearLayout` dọc (root). Header row dùng `RelativeLayout` để căn giữa tiêu đề thực sự.

```
LinearLayout (vertical, root)
│
├── RelativeLayout — Header
│   ├── ImageButton  id=button_back           (alignParentStart, 40×40dp)
│   ├── Button       id=button_clear_history  (alignParentEnd, màu đỏ)
│   └── TextView     (centerInParent → căn giữa thực sự so với parent)
│
└── FrameLayout (layout_weight=1) — Vùng nội dung
    ├── RecyclerView  id=recycler_history    (match_parent)
    └── TextView      id=text_empty_history  (layout_gravity=center, visibility=gone)
```

`RelativeLayout` được chọn cho header vì `LinearLayout` không thể căn giữa thực sự khi hai nút hai bên có kích thước khác nhau. `FrameLayout` cho phép `text_empty_history` và `RecyclerView` chồng lên nhau — khi rỗng, text hiện giữa màn hình; khi có dữ liệu, text gone và RecyclerView hiển thị danh sách.

### `item_history.xml`
Root: `LinearLayout` dọc — một mục trong danh sách lịch sử.

```
LinearLayout (vertical, background=selectableItemBackground)
├── TextView  id=text_expression_item  (biểu thức, xám, 15sp, maxLines=2)
├── TextView  id=text_result_item      (kết quả "= ...", trắng/đen, 22sp, bold)
├── TextView  id=text_time_item        (giờ:phút ngày/tháng/năm, xám, 11sp)
└── View      (1dp divider mờ giữa các item)
```

---

## 8. Luồng tính toán

```
Người dùng nhấn nút
        │
        ▼
MainActivity.onButtonClick(View v)
        │
        ▼
MainActivityController.onButtonClick(v)
  ├── MainUiShellController.onButtonClick(v)   ← theme / DEG-RAD / rotate
  └── MainCalculatorController.onButtonClick(v)
            │
            ├── [btn_clear]   → clearAll()
            ├── [btn_delete]  → deleteLast()
            │                    └── CalculatorInputManager.getFunctionDeleteLength()
            ├── [btn_equals]  → calculateResult()
            ├── [btn_history] → historyLauncher.launch(→ HistoryActivity)
            └── [other]       → KeyMappingContext.getKeyValue(id) → appendToExpression(value)
                                          │
                                          ├── CalculatorInputManager.currentNumberHasDecimal()
                                          ├── CalculatorInputManager.getLeadingZeroToRemove()
                                          ├── CalculatorInputManager.shouldAddImplicitMultiply()
                                          └── CalculatorInputManager.hasInvalidLeadingZero()
                                                      │
                                                      ▼
                                            getText().insert(cursor, str)
                                                      │
                                                      ▼  (TextWatcher.afterTextChanged)
                                            refreshPreview()
                                                      │
                          ┌───────────────────────────┘
                          ▼
              ExpressionValidator.validate(expr)
                  └── ExpressionConverter.tokenize() → kiểm tra token sequence
                            │
                      invalid → bỏ qua (chỉ khi preview)
                      valid   ↓
              ExpressionConverter.infixToPostfix(tokens)  [Shunting-Yard, Stack<String>]
                          ↓   List<String> postfix
              ExpressionEvaluator.evaluatePostfix(postfix, isDegMode)  [Stack<Double>]
                          ↓   double result
              formatResult()  [BigDecimal làm tròn]
                          ↓
              resultDisplay.setText(...)
```

**Khi nhấn `=`:**
```
validate() → invalid: showInvalid(message) — đỏ, kết thúc
           → valid:  evaluate() → EvalException: showInvalid(message)
                               → success:
                                    formatResult(value)
                                    HistoryManager.addEntry(expr, result)
                                    expressionDisplay.setText(result)
                                    resultDisplay.setText(expr)
                                    isResultShown = true
```

**Khi đổi DEG ↔ RAD:**
```
MainUiShellController.toggleDegRad()     ← đảo SharedPreferences
MainUiShellController.updateDegRadLabel() ← cập nhật label nút
MainCalculatorController.onDegRadChanged()
  ├── isResultShown == true  → calculateResult()
  └── isResultShown == false → refreshPreview()
```

---

## 9. Lưu trữ trạng thái

| Dữ liệu | Cơ chế | File / Key |
|---|---|---|
| Dark / Light mode | `SharedPreferences` | `theme_prefs` / `is_dark_mode` |
| DEG / RAD | `SharedPreferences` | `theme_prefs` / `is_deg_mode` |
| Lịch sử tính toán (≤50) | `SharedPreferences` (JSON) | `calculator_history_prefs` / `history_json` |
| Biểu thức hiện tại | `Bundle` | `key_expression` |
| Vị trí con trỏ | `Bundle` | `key_cursor_pos` |
| Trạng thái hiển thị kết quả | `Bundle` | `key_result_shown` |
| Chuỗi kết quả | `Bundle` | `key_result_text` |

---

## 10. Phân chia công việc (4 người)

### Người 1 — Lõi tính toán & Lưu trữ
Phụ trách toàn bộ logic thuần toán học và lớp dữ liệu, **không phụ thuộc Android**.

| File | Nhiệm vụ |
|---|---|
| `core/ExpressionConverter.java` | Tokenizer + thuật toán Shunting-Yard (Infix → Postfix) |
| `core/ExpressionEvaluator.java` | Tính giá trị biểu thức Postfix bằng Stack |
| `core/ExpressionValidator.java` | Kiểm tra hợp lệ biểu thức (ngoặc + token-sequence) trước khi tính |
| `data/HistoryManager.java` | Đọc/ghi lịch sử tính toán qua SharedPreferences (JSON) |
| `model/HistoryItem.java` | POJO dữ liệu lịch sử |

> **Giao diện cần thống nhất với Người 2:** Method signature của `ExpressionEvaluator.evaluate(expression, isDegMode)` phải được chốt trước để Người 2 có thể gọi đúng từ `MainCalculatorController`.

---

### Người 2 — Bàn phím máy tính & Logic nhập liệu
Phụ trách toàn bộ UI bàn phím (cả hai orientation) và luồng xử lý input từ nút bấm đến engine.

| File | Nhiệm vụ                                                                   |
|---|----------------------------------------------------------------------------|
| `res/layout/activity_main.xml` | Layout màn hình chính — Portrait                                           |
| `res/layout-land/activity_main.xml` | Layout màn hình chính — Landscape (bàn phím khoa học)                      |
| `res/drawable/bg_btn_*.xml` | Drawable nút bấm (pill-shape, 5 loại)                                      |
| `res/values/styles.xml` | Style hệ thống nút bấm (`CalcButtonBase` và các kế thừa)                   |
| `controller/helper/KeyMappingContext.java` | Map View ID → token; danh sách `FUNCTION_TOKENS`                           |
| `controller/helper/CalculatorInputManager.java` | Guard logic: leading zero, implicit multiply, smart delete                 |
| `controller/MainCalculatorController.java` | Trung tâm điều khiển: gắn sự kiện, gọi core, quản lý `EditText`/`TextView` |

> **Giao diện cần thống nhất với Người 3:** Cần nhận `isDegMode()` từ `MainUiShellController` khi gọi evaluate.
> **Giao diện cần thống nhất với Người 4:** Cần xử lý callback `ActivityResultLauncher` nhận biểu thức trả về từ `HistoryActivity`.

---

### Người 3 — Shell UI: Theme, DEG/RAD, Xoay màn hình
Phụ trách các chức năng điều khiển toàn cục không liên quan đến phép tính.

| File | Nhiệm vụ |
|---|---|
| `controller/MainUiShellController.java` | Toggle Dark/Light mode, DEG/RAD, xoay màn hình |
| `controller/MainActivityController.java` | Facade điều phối hai controller con |
| `ui/activity/MainActivity.java` | Entry point, delegate sự kiện xuống controller |
| `res/values/colors.xml` | Bảng màu Light mode |
| `res/values-night/colors.xml` | Ghi đè bảng màu Dark mode |
| `res/values-land/` | Ghi đè resource cho Landscape |
| `res/values/themes.xml` | Định nghĩa `Theme.MyCalculator` |

> **Giao diện cần thống nhất với Người 2:** `isDegMode()` phải được cung cấp sẵn để `MainCalculatorController` gọi; `onDegRadChanged()` phải được trigger đúng thời điểm qua `MainActivityController`.

---

### Người 4 — Màn hình lịch sử
Phụ trách toàn bộ màn hình lịch sử tính toán, độc lập với các màn hình khác.

| File | Nhiệm vụ |
|---|---|
| `ui/activity/HistoryActivity.java` | Hiển thị danh sách, xóa lịch sử, trả biểu thức về `MainActivity` |
| `ui/adapter/HistoryAdapter.java` | `RecyclerView.Adapter` bind `HistoryItem` vào view item |
| `res/layout/activity_history.xml` | Layout màn hình lịch sử (LinearLayout + RelativeLayout header + FrameLayout) |
| `res/layout/item_history.xml` | Layout một dòng lịch sử (biểu thức, kết quả, thời gian) |

> **Giao diện cần thống nhất với Người 2:** Key `EXTRA_SELECTED_EXPRESSION` trong `Intent` phải được thỏa thuận để `MainCalculatorController` đọc đúng khi nhận kết quả từ `HistoryActivity`.
> **Phụ thuộc Người 1:** Dùng `HistoryManager.getAll()` và `HistoryManager.clear()` — cần Người 1 hoàn thiện trước.
