import numpy as np
import cv2

def Python_say_Hello():
    print("Hello Python")


#使用numpy计算两个矩阵的乘积
def matrix_multiply():
    a = np.array([[1, 2], [3, 4]])
    b = np.array([[5, 6], [7, 8]])
    c = np.matmul(a, b)
    return c

#使用numpy生成随机数组，并计算其平均值和标准差。
def numpy_example():
    # Generate a random array with shape (3, 3)
    a = np.random.rand(3, 3)
    # Calculate the mean of the array
    mean = np.mean(a)
    # Calculate the standard deviation of the array
    std = np.std(a)
    return mean, std


def opencv_process_image(data):
    # 读取图片数据
    image = cv2.imdecode(np.asarray(data),cv2.IMREAD_COLOR)
    # 将图像转换为灰度图像
    gray_image = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
     # 将处理后的图像转换为png格式并转换为byte数组
    is_success, im_buf_arr = cv2.imencode(".png", gray_image)
    byte_im = im_buf_arr.tobytes()
     # 返回处理后的图像数据
    return byte_im

def process_image(input_bytes: bytes) -> bytes:
    img = cv2.imdecode(np.frombuffer(input_bytes, np.uint8), cv2.IMREAD_COLOR)

    # ======= 影像處理範例：Canny 邊緣 =======
    gray  = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    edges = cv2.Canny(gray, 80, 160)
    out   = cv2.cvtColor(edges, cv2.COLOR_GRAY2BGR)
    # ========================================

    ok, buf = cv2.imencode(".png", out)
    if not ok:
        raise RuntimeError("encode failed")
    return buf.tobytes()


def process_nv21(nv21_bytes: bytes, w: int, h: int) -> bytes:
     # NV21 → YUV420 → BGR
     yuv   = np.frombuffer(nv21_bytes, dtype=np.uint8).reshape((h * 3 // 2, w))
     bgr   = cv2.cvtColor(yuv, cv2.COLOR_YUV2BGR_NV21)

     # ======= 影像處理，示範 Canny =========
     gray  = cv2.cvtColor(bgr, cv2.COLOR_BGR2GRAY)
     edge  = cv2.Canny(gray, 80, 160)
     out   = cv2.cvtColor(edge, cv2.COLOR_GRAY2BGR)
     # ======================================

     ok, buf = cv2.imencode(".png", out)
     if not ok:
         raise RuntimeError("encode failed")
     return buf.tobytes()


# Rock, scissors, paper
def detect_gesture(nv21_bytes: bytes, w: int, h: int) -> bytes:
    # NV21 → YUV420
    # 讀取圖片
    yuv  = np.frombuffer(nv21_bytes, dtype=np.uint8).reshape((h * 3 // 2, w))
    img   = cv2.cvtColor(yuv, cv2.COLOR_YUV2BGR_NV21)

    # ============= 影像處理 ==============
    gesture = "Unknown"
    if img is None:
        print(f"無法載入圖片: {image_path}")
        ok, buf = cv2.imencode(".png", img)
        if not ok:
            raise RuntimeError("encode failed")
        return buf.tobytes()

    # 設定 ROI
    top_left = (100, 100) # Android 向右旋轉 90 度，因此是右上座標
    bottom_right = (w-100, h-100) # Android 向右旋轉 90 度，因此是左下座標

    # 提取 ROI 座標
    x1, y1 = top_left
    x2, y2 = bottom_right

    # 檢查 ROI 座標是否有效
    if x1 < 0 or y1 < 0 or x2 > img.shape[1] or y2 > img.shape[0] or x1 >= x2 or y1 >= y2:
        print(f"無效的 ROI 座標: top_left={top_left}, bottom_right={bottom_right}")
        ok, buf = cv2.imencode(".png", img)
        if not ok:
            raise RuntimeError("encode failed")
        return buf.tobytes()

    # 定義 ROI
    roi = img[y1:y2, x1:x2]

    # 繪製 ROI 矩形
    cv2.rectangle(img, top_left, bottom_right, (0, 255, 0), 2)

    # 轉換為 HSV 色彩空間
    hsv = cv2.cvtColor(roi, cv2.COLOR_BGR2HSV)

    # 定義膚色範圍（可根據實際環境調整）
    lower_skin = np.array([0, 20, 70], dtype=np.uint8)
    upper_skin = np.array([20, 255, 255], dtype=np.uint8)
    mask = cv2.inRange(hsv, lower_skin, upper_skin)

    # 形態學處理（去除噪點）
    mask = cv2.GaussianBlur(mask, (5, 5), 0)
    mask = cv2.dilate(mask, None, iterations=2)
    mask = cv2.erode(mask, None, iterations=2)

    # 尋找輪廓
    contours, _ = cv2.findContours(mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)

    if contours:
        # 找到最大輪廓（假設為手）
        max_contour = max(contours, key=cv2.contourArea)
        if cv2.contourArea(max_contour) > 1000:  # 過濾小雜訊
            # 計算凸包
            hull = cv2.convexHull(max_contour, returnPoints=False)
            defects = cv2.convexityDefects(max_contour, hull)

            # 計數缺陷數量
            defect_count = 0
            if defects is not None:
                for i in range(defects.shape[0]):
                    s, e, f, d = defects[i, 0]
                    start = tuple(max_contour[s][0])
                    end = tuple(max_contour[e][0])
                    far = tuple(max_contour[f][0])

                    # 計算缺陷深度
                    a = np.sqrt((end[0] - start[0])**2 + (end[1] - start[1])**2)
                    b = np.sqrt((far[0] - start[0])**2 + (far[1] - start[1])**2)
                    c = np.sqrt((end[0] - far[0])**2 + (end[1] - far[1])**2)
                    angle = np.arccos((b**2 + c**2 - a**2) / (2*b*c)) * 57

                    # 過濾角度過大的缺陷
                    if angle <= 90 and d > 2000:
                        defect_count += 1
                        cv2.circle(roi, far, 5, [0, 0, 255], -1)

            # 根據缺陷數量判斷手勢
            if defect_count == 0:
                gesture = "Rock"
            elif defect_count == 1:
                gesture = "Scissors"
            elif defect_count >= 2:
                gesture = "Paper"

            # 繪製輪廓和凸包
            cv2.drawContours(roi, [max_contour], -1, (255, 0, 0), 2)
            hull_points = cv2.convexHull(max_contour, returnPoints=True)
            cv2.drawContours(roi, [hull_points], -1, (0, 255, 255), 2)


    # 旋轉影像（向右旋轉 90 度）
    rotated_img = cv2.rotate(img, cv2.ROTATE_90_CLOCKWISE)

    # 在旋轉後的影像上繪製水平文字
    # 旋轉後尺寸為 (w, h)，文字放在左上角 (10, 50)
    cv2.putText(rotated_img, f"Gesture: {gesture}", (y1-10, x1-50),
                cv2.FONT_HERSHEY_SIMPLEX, 2, (127, 255, 255), 2)

    img = cv2.rotate(rotated_img, cv2.ROTATE_90_COUNTERCLOCKWISE)
    # ======================================

    ok, buf = cv2.imencode(".png", img)
    if not ok:
        raise RuntimeError("encode failed")
    return buf.tobytes()