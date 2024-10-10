import numpy as np
import librosa
import soundfile as sf

def predict_audio_file2(file_path):
    print(f'xxxxxxxxxx{file_path}')
    audio_data, sample_rate = sf.read(file_path)
    # Tiếp tục xử lý với librosa hoặc mô hình của bạn
    print("load data thanh cong")

    return 1

def predict_audio_file(file_path, duration=10, sr=22050, hop_length=512, n_mels=128):
    # Tải âm thanh
    print(f'xxxxxxxxxx{file_path}')
    audio, sr = librosa.load(file_path, sr=sr, duration=duration)

    # # Biến đổi sang spectrogram
    # spectrogram = librosa.feature.melspectrogram(y=audio, sr=sr, n_mels=n_mels, hop_length=hop_length)
    # spectrogram_db = librosa.power_to_db(spectrogram, ref=np.max)
    #
    # # Điều chỉnh kích thước của spectrogram
    # max_frames = int(np.ceil(duration * sr / hop_length))
    # spectrogram_db = pad_or_truncate(spectrogram_db, max_frames)
    #
    # return spectrogram_db.flatten()
    return  1


# Hàm điều chỉnh độ dài của spectrogram
def pad_or_truncate(spectrogram, max_len):
    if spectrogram.shape[1] > max_len:
        return spectrogram[:, :max_len]
    else:
        pad_width = max_len - spectrogram.shape[1]
        return np.pad(spectrogram, ((0, 0), (0, pad_width)), mode='constant')
def main(file_path):
    return 1