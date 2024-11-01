# ChatApp
GK_LTTBDĐ
##############################################################
Khi clone về nếu xảy ra lỗi: SDK location not found. Define a valid SDK location with an ANDROID_HOME environment variable or by setting the sdk.dir path in your project's local properties file at 'D:\ANDORID\ChatApp\local.properties'.
  -Cách sửa: 
    1. Mở System Properties:
        Nhấn Windows + R, gõ sysdm.cpl và nhấn Enter.
        Chọn tab Advanced và nhấn vào nút Environment Variables.
    2.Tạo biến môi trường mới:
        Trong phần System Variables, nhấn vào New.
        Nhập:
          Variable name: ANDROID_HOME
          Variable value: Đường dẫn đến SDK Android của bạn (ví dụ: C:\Users\TenUser\AppData\Local\Android\Sdk). (xem *)
    3.Cập nhật biến PATH:
        Tìm biến Path trong phần System Variables và chọn Edit.
        Thêm các đường dẫn sau vào biến Path:
          %ANDROID_HOME%\tools
          %ANDROID_HOME%\platform-tools
    4.Khởi động lại Android studio

(*) cách xem đường dẫn SDK:
  -Trong Android studio chọn:
    1.File
    2.Project Structure
    3.SDK Location
  
