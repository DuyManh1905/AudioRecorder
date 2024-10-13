import onnxruntime as ort
import numpy as np
import librosa


# Thông số âm thanh
SAMPLE_RATE = 22050
DURATION = 10  # Giới hạn âm thanh trong 10 giây
N_MFCC = 13
N_FFT = 2048
HOP_LENGTH = 512
MAX_FRAMES = int(np.ceil(DURATION * SAMPLE_RATE / HOP_LENGTH))

# Hàm để xử lý âm thanh đầu vào và trích xuất đặc trưng MFCC
def preprocess_audio(file_path):
    # Tải âm thanh
    signal, sr = librosa.load(file_path, sr=SAMPLE_RATE, duration=DURATION)

    # Trích xuất đặc trưng MFCC
    mfcc = librosa.feature.mfcc(y=signal, sr=sr, n_mfcc=N_MFCC, n_fft=N_FFT, hop_length=HOP_LENGTH, center=False)

    # Điều chỉnh số khung thời gian của MFCC (padding hoặc cắt bớt)
    if mfcc.shape[1] > MAX_FRAMES:
        mfcc = mfcc[:, :MAX_FRAMES]  # Cắt bớt nếu dài hơn
    else:
        pad_width = MAX_FRAMES - mfcc.shape[1]
        mfcc = np.pad(mfcc, ((0, 0), (0, pad_width)), mode='constant')  # Thêm padding nếu ngắn hơn

    return mfcc

# Hàm để thực hiện suy luận với ONNX
def predict_with_onnx(mfcc, model_path):
    # Load mô hình ONNX
    ort_session = ort.InferenceSession(model_path)

    # Đảm bảo MFCC có dạng [1, 13, 431, 1], theo thứ tự [batch_size, n_mfcc, num_frames, channels]
    mfcc = np.expand_dims(mfcc, axis=0)  # Thêm chiều batch size (1)
    mfcc = np.expand_dims(mfcc, axis=-1)  # Thêm chiều channels (1)

    # Chuẩn bị đầu vào cho mô hình ONNX
    input_name = ort_session.get_inputs()[0].name  # Tên input của mô hình
    outputs = ort_session.run(None, {input_name: mfcc.astype(np.float32)})  # Chạy suy luận

    # Trả về kết quả dự đoán
    return np.argmax(outputs)


def main(model_path,audio_file_path):
# Tiền xử lý âm thanh để lấy đặc trưng MFCC
    mfcc = preprocess_audio(audio_file_path)
# Dự đoán với mô hình ONNX
    prediction = predict_with_onnx(mfcc, model_path)

    return prediction
