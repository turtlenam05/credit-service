Requirement hiện tại đã thống nhất: 
Main Flow 4: Credit Service
1. Mua Credit
Người dùng đã đăng ký tài khoản có thể mua credit với giá cố định: 1.000 VNĐ / 1 credit.
Người dùng truy cập Credit Wallet để:
Xem số dư credit
Mua thêm credit
Thanh toán được thực hiện thông qua PayOS. Sau khi thanh toán thành công:
Credit được cộng ngay vào ví người dùng
Hệ thống ghi nhận giao dịch và gửi thông báo xác nhận

Bổ sung: Số lượng credit mua mỗi lần phải nằm trong khoảng tối thiểu 10 credit (tương đương 10.000) và tối đa 1.000 credit (tương đương 1 triệu VNĐ). Hệ thống cần validate và hiển thị lỗi nếu người dùng nhập ngoài khoảng này.

2. Sử dụng Credit cho AI Chat
Mỗi người dùng được chat với AI tìm nhà miễn phí tối đa 30 tin nhắn mỗi ngày.
Khi vượt quá 30 tin nhắn trong ngày:
Mỗi tin nhắn tiếp theo sẽ bị trừ 1 credit
Nếu số dư credit không đủ:
Hệ thống yêu cầu người dùng nạp thêm credit để tiếp tục chat

Bổ sung: Giới hạn 30 tin nhắn miễn phí được reset mỗi ngày vào lúc 00:00 theo giờ Việt Nam (GMT+7). Bộ đếm tin nhắn được tính riêng cho từng ngày theo timezone này.

3. Sử dụng Credit cho Đăng Bài (Seller)
Người bán được:
Miễn phí bài đăng đầu tiên (tính trên toàn bộ lịch sử tài khoản, áp dụng một lần duy nhất)
Từ bài đăng thứ hai trở đi:
Mỗi bài đăng tốn 10 credit

Bổ sung: Trước khi Seller xác nhận submit bài đăng (từ bài thứ hai), hệ thống hiển thị cảnh báo rõ ràng rằng 10 credit sẽ bị tạm khóa. Seller cần bấm xác nhận đồng ý trước khi bài được gửi đi duyệt.

Khi Seller submit bài đăng:
Hệ thống lock tạm 10 credit

Quy trình duyệt bài:
Nếu bị từ chối duyệt → hoàn lại toàn bộ credit đã lock
Nếu được duyệt và publish → credit bị trừ chính thức

4. Theo dõi & Hoàn Credit
Người dùng có thể xem lịch sử giao dịch credit:
Credit đã mua
Credit dùng cho AI chat
Credit dùng cho đăng bài
Credit được hoàn (nếu bài bị reject)

Trong trường hợp có lỗi hệ thống hoặc tranh chấp:
Người dùng có thể gửi Refund Request
Admin kiểm tra và quyết định hoàn credit theo quy định

Bổ sung: Để gửi Refund Request, người dùng bắt buộc phải cung cấp:
Lý do yêu cầu hoàn credit (chọn từ danh sách có sẵn: lỗi hệ thống, bị trừ nhầm, tranh chấp khác)
Mô tả chi tiết sự việc
Bằng chứng đính kèm nếu có (ảnh chụp màn hình, v.v.)

Hệ thống chỉ tiếp nhận Refund Request khi đầy đủ thông tin bắt buộc. Mỗi giao dịch chỉ được gửi tối đa 1 Refund Request.

============================================================================================================================================
Hướng dẫn Tích hợp Credit Service (Module Payment)
Module này đã hoàn thành Bước 1: Nạp Credit qua PayOS. Để chạy được module này trên máy cá nhân, vui lòng thực hiện các bước cấu hình sau.

 1. Cấu hình PayOS (Sandbox)
Mỗi thành viên nên tạo một kênh Sandbox riêng để không bị trùng lặp giao dịch và Webhook:

Đăng ký/Đăng nhập vào PayOS Dashboard.

Tạo một Kênh thanh toán mới (chọn loại Sandbox).

Lấy 3 mã key và dán vào file src/main/resources/application.properties:

PAYOS_CLIENT_ID=...

PAYOS_API_KEY=...

PAYOS_CHECKSUM_KEY=...

 2. Cấu hình Webhook qua Ngrok
Vì PayOS cần gửi tin nhắn về máy Local của bạn, bạn phải dùng Ngrok làm cầu nối:

Mở Terminal và chạy lệnh: ngrok http 8082 (Hoặc port bạn đang chạy App).

Copy link Forwarding (dạng https://xxxx.ngrok-free.dev).

Truy cập Dashboard PayOS -> Thiết lập kênh -> Webhook URL.

Dán link theo cấu trúc: https://xxxx.ngrok-free.dev/api/webhook/payos và nhấn lưu.

3. Cấu trúc Database & Logic
Bảng UserWallet: Quản lý số dư (balance). Cần có sẵn dữ liệu cho user_id trước khi test.

Bảng CreditTransaction: Lưu lịch sử giao dịch.

Khi tạo link: Trạng thái là PENDING.

Khi PayOS báo Webhook thành công: Trạng thái chuyển sang SUCCESS và tự động cộng tiền vào ví.

Validation: Hệ thống chỉ chấp nhận nạp từ 10.000 VNĐ (10 credit) đến 1.000.000 VNĐ (1.000 credit).
