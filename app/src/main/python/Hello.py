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