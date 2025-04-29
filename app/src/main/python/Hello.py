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