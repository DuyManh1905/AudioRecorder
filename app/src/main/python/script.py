import librosa
import math
import os
import numpy as np
import soundfile

n_mfcc=13
n_fft=2048
hop_length=512
SAMPLE_RATE = 22050
DURATION = 10
SAMPLES_PER_TRACK = SAMPLE_RATE * DURATION
# ="/storage/emulated/0/Ringtones/piano_4.wav"
# /storage/emulated/0/Recordings/GenreMusic/recording_1723710306898.wav

def main(file_path):
    n_mfcc=13
    n_fft=2048
    hop_length=512
    num_segments=1

    # Data storage dictionary
    data = {
        "mapping": [],
        "mfcc": [],
        "labels": [],
    }
    samples_ps = int(SAMPLES_PER_TRACK/num_segments) # ps = per segment
    expected_vects_ps = math.ceil(samples_ps/hop_length)
    # expected_vects_ps = 427
    print("expected_vects_ps:"+str(expected_vects_ps))

#     signal,sr = librosa.load(file_path,sr=SAMPLE_RATE)
    data,sr = soundfile.read(file_path)
    signal = librosa.resample(data,sr,SAMPLE_RATE)

    # print("number of segments:"+str(num_segments))
    for s in range(num_segments):
        start_sample = samples_ps * s
        finish_sample = start_sample + samples_ps

        signal_audio = signal[start_sample:finish_sample]
        # print(len(signal_audio))

        mfcc = librosa.feature.mfcc(y=signal[start_sample:finish_sample],
                                    sr = sr,
                                    n_fft = n_fft,
                                    n_mfcc = n_mfcc,
                                    hop_length = hop_length)

        mfcc = mfcc.T
        if len(mfcc)==expected_vects_ps:
            print('da trich xuat thannh cong mfcc')
            return mfcc.tolist()

