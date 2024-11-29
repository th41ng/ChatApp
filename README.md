# ChatApp
GK_LTTBDĐ
##############################################################
@ Khi clone về mà nút run không có màu xanh: (thường gặp)
  -Cách sửa:
    1. Trong Android Studio nhấn File
    2. Sync Project with Gradle Files

  
@ Khi clone về nếu xảy ra lỗi: SDK location not found. Define a valid SDK location with an ANDROID_HOME environment variable or by setting the sdk.dir path in your project's local properties file at 'D:\ANDORID\ChatApp\local.properties'.
  -Cách sửa: 
    1. Trong thư mục ChatApp thêm file txt: đổi tên thành: local.properties
    2. Thêm code này vào bên trong file vừa tạo: nhớ là \\ 
        sdk.dir=C\:\\Users\\Admin\\AppData\\Local\\Android\\Sdk (xem đường dẫn ở dưới *)
    4.Khởi động lại Android studio

(*) cách xem đường dẫn SDK:
  -Trong Android studio chọn:
    1.File
    2.Project Structure
    3.SDK Location
  
