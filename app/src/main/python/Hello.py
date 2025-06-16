import cv2
import math
import numpy as np

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



# 全局變數，儲存模型
faceNet = None
ageNet = None
genderNet = None

def load_model(face_proto_path, face_model_path, age_proto_path, age_model_path, gender_proto_path, gender_model_path):
    global faceNet, ageNet, genderNet
    try:
        # 初始化模型
        faceNet = cv2.dnn.readNet(face_model_path, face_proto_path)
        ageNet = cv2.dnn.readNet(age_model_path, age_proto_path)
        genderNet = cv2.dnn.readNet(gender_model_path, gender_proto_path)

        # 檢查模型是否成功載入
        if faceNet is None or ageNet is None or genderNet is None:
            raise RuntimeError("Failed to load one or more models")

        print(f"✅ Model load successful")
        return True
    except Exception as e:
        print(f"❌ Model loading failed: {str(e)}")
        return False

def highlightFace(net, frame, conf_threshold=0.7):
    frameOpencvDnn = frame.copy()
    frameHeight, frameWidth = frameOpencvDnn.shape[:2]
    blob = cv2.dnn.blobFromImage(frameOpencvDnn, 1.0, (300, 300), [104, 117, 123], True, False)
    net.setInput(blob)
    detections = net.forward()
    faceBoxes = []
    for i in range(detections.shape[2]):
        confidence = detections[0, 0, i, 2]
        if confidence > conf_threshold:
            x1 = int(detections[0, 0, i, 3] * frameWidth)
            y1 = int(detections[0, 0, i, 4] * frameHeight)
            x2 = int(detections[0, 0, i, 5] * frameWidth)
            y2 = int(detections[0, 0, i, 6] * frameHeight)
            faceBoxes.append([x1, y1, x2, y2])
            cv2.rectangle(frameOpencvDnn, (x1, y1), (x2, y2), (0, 255, 0), int(round(frameHeight / 150)), 8)
    return frameOpencvDnn, faceBoxes

def process_nv21_for_age_gender(nv21_bytes: bytes, w: int, h: int) -> bytes:
    # 檢查模型是否已載入
    if faceNet is None or ageNet is None or genderNet is None:
        raise RuntimeError("Models not loaded. Call load_model() first.")

    # 將 NV21 數據轉為 NumPy 陣列，並使用 w 和 h 重新塑造
    nv21 = np.frombuffer(nv21_bytes, dtype=np.uint8)
    frame = cv2.cvtColor(nv21.reshape((h, w)), cv2.COLOR_YUV2BGR_NV21)
    # 0：水平翻轉（沿著垂直軸翻轉）。 1：垂直翻轉（沿著水平軸翻轉）。 -1：同時在水平和垂直方向上翻轉。
    frame = cv2.flip(frame, 1)

    # 人臉檢測
    MODEL_MEAN_VALUES = (78.4263377603, 87.7689143744, 114.895847746)
    ageList = ['(0-2)', '(4-6)', '(8-12)', '(15-20)', '(25-32)', '(38-43)', '(48-53)', '(60-100)']
    genderList = ['Male', 'Female']

    blob = cv2.dnn.blobFromImage(frame, 1.0, (227, 227), MODEL_MEAN_VALUES, swapRB=False)

    genderNet.setInput(blob)
    genderPreds = genderNet.forward()
    gender = genderList[genderPreds[0].argmax()]

    ageNet.setInput(blob)
    agePreds = ageNet.forward()
    age = ageList[agePreds[0].argmax()]

    # 旋轉影像（如果需要）並增添標籤
    rotated_img = cv2.rotate(frame, cv2.ROTATE_90_CLOCKWISE)
    cv2.putText(rotated_img, f'{gender}, {age}', (30, 60), cv2.FONT_HERSHEY_SIMPLEX, 2.0, (224, 224, 224), 5, cv2.LINE_AA)
    frame = cv2.rotate(rotated_img, cv2.ROTATE_90_COUNTERCLOCKWISE)

    # 編碼為 PNG
    ok, buf = cv2.imencode(".png", frame)
    if not ok:
        raise RuntimeError("encode failed")
    return buf.tobytes()