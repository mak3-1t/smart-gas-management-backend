# Smart Gas Management – Backend

## Yêu cầu
- Java 17+
- Maven 3.8+
- IntelliJ IDEA (khuyến nghị)

## Cài đặt môi trường (Mỗi Dev làm 1 lần)

### Bước 1 – Tạo file `.env`
```bash
# Copy file mẫu
copy .env.example .env
```

### Bước 2 – Điền thông tin vào `.env`
Mở file `.env` và điền:
```
MONGODB_URI=mongodb+srv://shiginami2510_db_user:<PASSWORD>@cluster0.gezbq0t.mongodb.net/gas_management_db?retryWrites=true&w=majority&appName=Cluster0
JWT_SECRET=<xin-từ-team-lead>
```

> ⚠️ **Password MongoDB Atlas và JWT Secret** – hỏi Team Lead qua kênh bảo mật (Zalo/Messenger riêng).  
> Không nhận qua nhóm chat công khai!

### Bước 3 – Chạy project
```bash
# Trong IntelliJ: Run SmartGasManagementApplication
# Hoặc terminal:
mvn spring-boot:run
```

### Bước 4 – Kiểm tra kết nối
Mở trình duyệt: `http://localhost:8080`

---

## Thông tin Database chung

| Thông số | Giá trị |
|---|---|
| Provider | MongoDB Atlas |
| Cluster | `cluster0.gezbq0t.mongodb.net` |
| Database | `gas_management_db` |
| Username | `shiginami2510_db_user` |

> Password – hỏi Team Lead riêng, không để trong tài liệu này.

---

## Phân công Dev

| Module | Dev |
|---|---|
| Customer, Cart, Order, Payment, Review | **DEV 1** |
| Manager, Staff, Delivery, Inventory, Admin | **DEV 2** |

## Branch Git

| Dev | Branch |
|---|---|
| DEV 1 | `feature/customer-order-payment` |
| DEV 2 | `feature/manager-delivery-inventory` |
