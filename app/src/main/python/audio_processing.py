import librosa

def extract_features(file_path):
    audio, sr = librosa.load(file_path, sr=None)
    #bien doi sang spectrogram
    spectrogram = librosa.feature.melspectrogram(y=audio, sr=sr, n_mels=128)
    spectrogram_db = librosa.power_to_db(spectrogram, ref=np.max)

    return spectrogram_db